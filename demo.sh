#!/usr/bin/env bash
# Demostración del walking skeleton de punta a punta.
# Requisitos: el sistema levantado con `docker compose up --build` y curl.
# En Windows ejecútese desde Git Bash:  bash demo.sh
set -u

GW="${GW:-http://localhost:8080}"
API="$GW/api/clinica"

paso()  { printf '\n\033[1;36m== %s\033[0m\n' "$1"; }
ok()    { printf '\033[1;32m   ✔ %s\033[0m\n' "$1"; }
falla() { printf '\033[1;31m   ✘ %s\033[0m\n' "$1"; }
post()  { curl -s -H 'Content-Type: application/json' -X POST "$1" -d "$2"; }
campo() { grep -o "\"$1\":\"[^\"]*\"" | head -1 | cut -d'"' -f4; }
cupos() { curl -s "$API/medicos" | grep -o "\"id\":\"$1\"[^]]*\]" | grep -o '"20[^"]*"' | tr -d '"' | sort; }
# Espera (máx. 15 s) a que la copia local de Agenda reciba paciente.registrado
# Reintenta hasta 10 s: los consumidores reaccionan de forma asíncrona (consistencia eventual)
contiene() { local url=$1 texto=$2; shift 2
  for _ in $(seq 1 20); do curl -s "$@" "$url" | grep -q "$texto" && return 0; sleep 0.5; done; return 1; }
esperar_agenda() { for _ in $(seq 1 30); do curl -s "$API/agenda/pacientes/$1" | grep -q '"known":true' && return 0; sleep 0.5; done; return 1; }

registrar() { # documento nombre -> patientId
  post "$API/paciente" "{\"documentNumber\":\"$1\",\"firstName\":\"$2\",\"lastName\":\"Demo\",
    \"email\":\"$(echo "$2" | tr 'A-Z' 'a-z')@correo.com\",\"phone\":\"3001234567\",\"birthDate\":\"1990-01-01\",
    \"eps\":\"Sura\",\"password\":\"Demo1234\",\"allergies\":[\"penicilina\"]}" | campo id
}
agendar() { # patientId doctorId cupo -> respuesta JSON
  post "$API/cita" "{\"patientId\":\"$1\",\"doctorId\":\"$2\",\"date\":\"$3\"}"
}

paso "1. Los tres servicios responden a través del gateway"
for url in "$API/agenda/health" "$API/historia-health" "$GW/api/notificaciones/health"; do
  curl -sf "$url" >/dev/null && ok "$url" || { falla "$url (¿ya terminó de arrancar? docker compose ps)"; exit 1; }
done

paso "2. Registrar un paciente en Historia Clínica → publica paciente.registrado"
PACIENTE=$(registrar "10$RANDOM$RANDOM" "Ana")
[ -n "$PACIENTE" ] && ok "Paciente $PACIENTE creado" || { falla "No se pudo registrar"; exit 1; }

paso "3. Un evento, dos consumidores que reaccionan por separado"
esperar_agenda "$PACIENTE" && ok "Agenda guardó al paciente en su copia local" || falla "Agenda aún no conoce al paciente"
contiene "$GW/api/notificaciones?patientId=$PACIENTE" 'Bienvenido' \
  && ok "Notificaciones envió la bienvenida" || falla "Notificaciones no reaccionó"

paso "4. Agendar Cardiología → Agenda consulta la EPS (síncrono) y publica cita.agendada"
CUPO=$(cupos DOC-001 | head -1)
R=$(agendar "$PACIENTE" DOC-001 "$CUPO"); echo "   $R"
CITA=$(echo "$R" | campo id)
echo "$R" | grep -q '"status":"SCHEDULED"' \
  && ok "Cita $CITA confirmada, autorización $(echo "$R" | campo authorizationNumber)" || falla "Se esperaba SCHEDULED"
contiene "$API/historia/$PACIENTE" "$CITA" -H 'X-Usuario: medico:DOC-001' \
  && ok "Historia Clínica reflejó la cita (consumidor 1 de cita.agendada)" || falla "La historia no muestra la cita"
contiene "$GW/api/notificaciones?patientId=$PACIENTE" 'confirmada' \
  && ok "Notificaciones avisó la confirmación (consumidor 2 de cita.agendada)" || falla "Sin aviso de confirmación"

paso "5. Reglas de negocio de Agenda"
R=$(agendar "$PACIENTE" DOC-002 "$(cupos DOC-002 | head -1)")
echo "$R" | grep -q 'ya tiene una cita activa' && ok "R2: segunda cita activa en Cardiología rechazada" || falla "R2: $R"
CERCANO=$(cupos DOC-006 | head -1)   # Medicina General: no pasa por la EPS
if [ $(( $(date -d "$CERCANO" +%s 2>/dev/null || echo 0) - $(date +%s) )) -lt 86400 ]; then
  CITA_MG=$(agendar "$PACIENTE" DOC-006 "$CERCANO" | campo id)
  R=$(curl -s -X DELETE "$API/cita/$CITA_MG")
  echo "$R" | grep -q '24 horas' && ok "R4: cancelar la cita de $CERCANO (< 24 h) rechazado" || falla "R4: $R"
else
  echo "   (R4 omitido: ya no quedan cupos a menos de 24 h; reinicie con 'docker compose restart agenda')"
fi

paso "6. Escenario E1: 8 pacientes piden el MISMO cupo al mismo tiempo"
CUPO_DISPUTADO=$(cupos DOC-003 | head -1)
TMP=$(mktemp -d)
for i in 1 2 3 4 5 6 7 8; do registrar "20$RANDOM$i" "Paciente$i" > "$TMP/p$i" & done; wait
for i in 1 2 3 4 5 6 7 8; do esperar_agenda "$(cat "$TMP/p$i")" || falla "Agenda no conoce a Paciente$i"; done
for i in 1 2 3 4 5 6 7 8; do agendar "$(cat "$TMP/p$i")" DOC-003 "$CUPO_DISPUTADO" > "$TMP/r$i" & done; wait
GANADORAS=$(grep -l '"success":true' "$TMP"/r* | wc -l | tr -d ' ')
RECHAZADAS=$(grep -l 'ya no está disponible' "$TMP"/r* | wc -l | tr -d ' ')
[ "$GANADORAS" = "1" ] && ok "Exactamente 1 reserva exitosa y $RECHAZADAS rechazadas para $CUPO_DISPUTADO" \
  || falla "$GANADORAS reservas exitosas (se esperaba 1)"
[ "$((GANADORAS + RECHAZADAS))" = "8" ] \
  || falla "Respuestas inesperadas: $(grep -L -e 'ya no está disponible' -e '"success":true' "$TMP"/r* | head -2 | xargs cat)"
rm -rf "$TMP"

paso "7. Escenario E2: la EPS tarda 5 s (documento 888…) → timeout de 2 s y degradación"
LENTO=$(registrar "888$RANDOM" "Lento"); esperar_agenda "$LENTO"
INICIO=$(date +%s)
R=$(agendar "$LENTO" DOC-004 "$(cupos DOC-004 | head -1)")
echo "   $R"
echo "$R" | grep -q 'PENDING_AUTHORIZATION' \
  && ok "Cupo reservado como PENDING_AUTHORIZATION en $(( $(date +%s) - INICIO )) s (la EPS habría tardado 5 s)" \
  || falla "Se esperaba PENDING_AUTHORIZATION"

paso "8. La EPS niega la autorización (documento 999…) → se libera el cupo"
NEGADO=$(registrar "999$RANDOM" "Negado"); esperar_agenda "$NEGADO"
R=$(agendar "$NEGADO" DOC-007 "$(cupos DOC-007 | head -1)")
echo "$R" | grep -q 'negó la autorización' && ok "Cita rechazada por la EPS; el cupo vuelve a estar disponible" || falla "$R"

paso "9. Cancelar con más de 24 h de antelación → publica cita.cancelada"
LEJANO=$(cupos DOC-007 | tail -1)
CITA2=$(agendar "$PACIENTE" DOC-007 "$LEJANO" | campo id)
R=$(curl -s -X DELETE "$API/cita/$CITA2")
echo "$R" | grep -q '"status":"CANCELLED"' && ok "Cita $CITA2 ($LEJANO) cancelada" || falla "Cancelación: $R"
contiene "$API/historia/$PACIENTE" "\"id\":\"$CITA2\"[^}]*CANCELLED" \
  && ok "Historia Clínica la marcó como cancelada" || falla "La historia no refleja la cancelación"
contiene "$GW/api/notificaciones?patientId=$PACIENTE" 'fue cancelada' \
  && ok "Notificaciones avisó la cancelación" || falla "Sin aviso de cancelación"

paso "10. Escenario E3: auditoría de accesos a la historia clínica"
curl -s "$API/auditoria/$PACIENTE"; echo

printf '\n\033[1mPruebas de falla en vivo (escenario E4):\033[0m\n'
echo "   docker compose stop notificaciones   → agendar en http://localhost:8080 → sigue funcionando"
echo "   docker compose start notificaciones  → GET $GW/api/notificaciones?patientId=$PACIENTE muestra el aviso"
echo "   docker compose stop eps-simulador    → toda cita de especialista queda PENDING_AUTHORIZATION"
