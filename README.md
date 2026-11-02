# Plataforma de Citas Médicas — Proyecto de Arquitectura de Software

Plataforma para una IPS ambulatoria: los pacientes agendan citas con especialistas que requieren autorización de su EPS, los médicos consultan y alimentan la historia clínica, y el sistema avisa a los pacientes de cada cambio.

**Estilo:** microservicios por contexto delimitado (Agenda, Historia Clínica, Notificaciones), integrados por **eventos sobre RabbitMQ**, con un **API Gateway** en el borde.

```
Navegador ──► Gateway (nginx + portal React) ──► Agenda ─────────► EPS (externa, simulada)
                                            ├──► Historia Clínica
                                            └──► Notificaciones
              Agenda ⇄ RabbitMQ ⇄ Historia Clínica, Notificaciones   (eventos, sin HTTP entre servicios)
```

---

## Cómo levantarlo (un comando)

**Requisito único:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) encendido. No hace falta tener Java, Maven ni Node instalados: todo se compila dentro de Docker.

El sistema vive en dos repositorios, que deben clonarse **uno al lado del otro** (el gateway compila el portal desde el repositorio del frontend):

```bash
git clone https://github.com/Andres-Maya/SistemaSitasMedicasBackend.git
```
```bash
git clone https://github.com/Andres-Maya/SistemaSitasMedicasFrontend.git
```
```bash
cd SistemaSitasMedicasBackend
```

Y se levanta con un solo comando:

```bash
docker compose up --build
```

> Si el frontend está en otra ruta: `FRONTEND_DIR=/ruta/al/frontend docker compose up --build`.
>
> Si el equipo tiene poca memoria, asignen a Docker Desktop al menos 4 GB y 2 CPU (*Settings → Resources*). Con 2,8 GB los servicios Java tardan varios minutos en arrancar, así que levanten el sistema **antes** de la sustentación.

La primera vez tarda unos minutos porque descarga las imágenes y las dependencias. Estará listo cuando el log muestre `Started AgendaApplication`, `Started HistoriaClinicaApplication` y `Started NotificacionesApplication`.

| Qué | URL |
|---|---|
| Portal del paciente (React) + API | http://localhost:8080 |
| Consola de RabbitMQ (colas y mensajes) | http://localhost:15672 (usuario `guest`, clave `guest`) |
| Agenda / Historia Clínica / Notificaciones (directo) | http://localhost:8081 · :8082 · :8083 |
| Simulador de la EPS (WireMock) | http://localhost:8090/__admin/mappings |

### Demostración automática

En otra terminal (en Windows, **Git Bash**):

```bash
bash demo.sh
```

Recorre el sistema de punta a punta y verifica: un evento con dos consumidores, la ráfaga concurrente sobre un mismo cupo (exactamente uno gana), el timeout de la EPS con degradación, la negación de la EPS, las reglas de cancelación y la auditoría.

### Pruebas de falla en vivo

```bash
docker compose stop notificaciones
```
Agenden una cita en el portal: funciona igual. Luego:
```bash
docker compose start notificaciones
```
El aviso pendiente se entrega al volver (escenario E4).

```bash
docker compose stop eps-simulador
```
Toda cita de especialista queda `PENDING_AUTHORIZATION` en menos de 3 s (escenario E2).

Datos de prueba para la EPS simulada: documentos que empiezan por **999** → la EPS niega; por **888** → la EPS tarda 5 s; cualquier otro → aprueba.

### Detener

```bash
docker compose down
```

---

## Documentación de diseño

| # | Documento | Contenido |
|---|---|---|
| 1 | [Dominio y drivers](docs/01-dominio-y-drivers.md) | Propuesta, actores, restricciones, **árbol de utilidad** y drivers priorizados |
| 2 | [Escenarios](docs/02-escenarios.md) | Seis escenarios completos (fuente, estímulo, artefacto, entorno, respuesta y medida) |
| 3 | [Estilo, contextos y tácticas](docs/03-estilo-contextos-tacticas.md) | Estilo y qué sacrifica, **contextos delimitados** con su señal, **tácticas** y qué degradan |
| 4 | [Contratos de eventos](docs/04-contratos-eventos.md) | Eventos, colas, consumidores y contrato con la EPS |
| 5 | [Diagramas C4](docs/05-c4.md) | Niveles 1 y 2 con flechas anotadas y leyenda |
| — | [ADR-001](docs/adr/ADR-001-microservicios-por-contexto.md) · [ADR-002](docs/adr/ADR-002-integracion-por-eventos-rabbitmq.md) · [ADR-003](docs/adr/ADR-003-autorizacion-eps-con-timeout-y-degradacion.md) | Decisiones con alternativas descartadas y consecuencias |
| 6 | [Riesgos](docs/06-riesgos.md) | Siete riesgos con lo que hay que verificar para descartarlos |
| 7 | [Guía de sustentación](docs/07-guia-sustentacion.md) | Las cuatro preguntas aplicadas a cada decisión y el guion de la demo |

## Estructura

```
SistemaSitasMedicasBackend/          ← este repositorio
├── docker-compose.yml               ← arranque con un comando
├── demo.sh                          ← demostración de punta a punta
├── docs/                            ← entregable 1: diseño documentado
├── servicios/                       ← entregable 2: walking skeleton
│   ├── agenda-service/              ← Spring Boot · cupos, citas, EPS · publica cita.*
│   ├── historia-clinica-service/    ← Spring Boot · pacientes y datos clínicos · publica paciente.registrado
│   ├── notificaciones-service/      ← Spring Boot · solo consume eventos
│   ├── gateway/                     ← nginx: enrutamiento por contexto + portal compilado
│   └── eps-simulador/               ← WireMock: sistema externo de autorizaciones
└── backend/                         ← versión 0: monolito con Facade (alternativa A de ADR-001)

SistemaSitasMedicasFrontend/         ← repositorio hermano: portal React (se compila dentro del gateway)
```

> `backend/` se conserva como **línea base**: es el monolito desde el que partimos y la alternativa descartada en ADR-001. No forma parte del despliegue.
