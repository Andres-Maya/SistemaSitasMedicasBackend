# 1. Dominio, restricciones y drivers priorizados

## 1.1 Propuesta (texto para el formulario del 5 de noviembre)

> **Dominio:** plataforma de citas médicas para una IPS ambulatoria con varias especialidades, que atiende pacientes afiliados a distintas EPS.
>
> **Qué lo hace complejo:**
> - Reglas de negocio no triviales: un cupo no puede asignarse dos veces aunque lleguen solicitudes simultáneas; un paciente no puede tener dos citas activas en la misma especialidad; las especialidades requieren autorización previa de la EPS; las citas solo se cancelan con 24 h de antelación; no se puede prescribir un medicamento que entre en conflicto con una alergia registrada.
> - Varios tipos de usuario con necesidades distintas: paciente, médico, recepcionista y auditor.
> - Restricciones externas: la historia clínica está regulada (confidencialidad, integridad, custodia y auditoría) y la autorización de servicios depende del sistema de cada EPS, que el equipo no controla.
>
> **Tres drivers principales:**
> 1. Integridad de la agenda cuando hay concurrencia (cero cupos asignados dos veces).
> 2. Confidencialidad y trazabilidad de la historia clínica (restricción normativa).
> 3. Disponibilidad del agendamiento aunque fallen dependencias externas (la EPS o el envío de avisos).

## 1.2 Actores y necesidades

| Actor | Qué necesita del sistema | Qué le preocupa |
|---|---|---|
| **Paciente** | Registrarse, agendar o cancelar citas, ver su historia y sus resultados, recibir recordatorios | Que la cita sea real (que no se la quiten) y que nadie más vea su historia |
| **Médico** | Consultar la historia completa, prescribir, pedir exámenes | Que la información esté completa y que el sistema le avise de conflictos con alergias |
| **Recepcionista** | Gestionar la agenda y resolver las citas *pendientes de autorización* | Que una EPS caída no bloquee la fila de pacientes |
| **Auditor / Protección de datos** | Saber quién consultó qué historia y cuándo | Cumplir la norma y poder responder ante un reclamo |

## 1.3 Restricciones identificadas

Las restricciones no se negocian: limitan el espacio de diseño antes de elegir un estilo.

| # | Tipo | Restricción | Efecto en el diseño |
|---|---|---|---|
| RS1 | Normativa | **Ley 1581 de 2012** (protección de datos personales). Los datos de salud son *datos sensibles* | Hay que minimizar dónde se copian los datos personales y controlar su acceso |
| RS2 | Normativa | **Resolución 1995 de 1999** (manejo de la historia clínica): es confidencial, no se puede alterar, se custodia y su acceso queda restringido | Los registros clínicos solo admiten inserción y cada consulta queda auditada. Los datos clínicos tienen un único dueño |
| RS3 | Normativa | Retención prolongada de la historia clínica (Res. 839 de 2017) e interoperabilidad de la historia clínica electrónica (**Ley 2015 de 2020**) | Los datos clínicos deben poder conservarse y exportarse aparte del resto del sistema |
| RS4 | Sistema externo | **Autorización de servicios de la EPS** (relación prestador–pagador, Decreto 4747 de 2007 / Res. 3047 de 2008). Cada EPS tiene su API, su latencia y sus caídas | Hay que integrarse con un sistema que no controlamos y que puede no responder |
| RS5 | Operación | La demanda llega en picos cuando se abren las agendas (por ejemplo, lunes a las 7:00) | La parte que agenda debe soportar ráfagas y escalar sin arrastrar al resto |
| RS6 | Equipo / proyecto | Tres personas, un semestre, Java 17 + Spring Boot y React (ya conocidos), presupuesto cero, ejecución local con Docker | Tecnología conocida y sin servicios de pago. Los datos se guardan en memoria en el esqueleto |

> ⚠️ Antes de la entrega, verifiquen la vigencia y el número exacto de cada norma citada. El diseño depende del **tipo** de restricción, no del número de la resolución.

## 1.4 Árbol de utilidad

Notación de cada hoja: **(Importancia para el negocio, Dificultad técnica)**, con **A** = alta, **M** = media y **B** = baja.

```
Utilidad
├── Integridad de la agenda
│   ├── C1 (A,A) Dos pacientes piden el mismo cupo a la vez → exactamente uno lo obtiene; 0 cupos dobles
│   └── C2 (M,B) Un paciente no puede tener dos citas activas en la misma especialidad
├── Disponibilidad
│   ├── D1 (A,M) La EPS no responde → se contesta en < 3 s con el cupo reservado como "pendiente"; 0 errores al paciente
│   ├── D2 (A,B) El servicio de notificaciones cae → agendar sigue funcionando; 0 avisos perdidos al recuperarse
│   └── D3 (M,A) Agendamiento disponible ≥ 99,5 % al mes entre 6:00 y 20:00
├── Seguridad (confidencialidad y trazabilidad)
│   ├── S1 (A,M) Los datos clínicos solo existen en un componente; ningún evento ni otro servicio los contiene
│   ├── S2 (A,B) El 100 % de las consultas a una historia quedan auditadas (quién, cuándo, qué)
│   ├── S3 (M,B) Ninguna contraseña se almacena en claro
│   └── S4 (A,M) Autenticación con token y autorización por rol (paciente, médico, auditor)
├── Rendimiento / escalabilidad
│   └── P1 (M,A) 300 solicitudes de cita/min durante 15 min al abrir agenda; p95 < 2 s sin contar la EPS
├── Modificabilidad
│   ├── M1 (M,M) Integrar una EPS nueva con API distinta en ≤ 3 días-persona, tocando solo el adaptador
│   └── M2 (B,B) Agregar un canal de aviso (WhatsApp) sin modificar Agenda ni Historia Clínica
├── Interoperabilidad
│   └── I1 (M,A) Exportar la historia clínica en el formato de la historia clínica electrónica interoperable
└── Usabilidad
    └── U1 (B,M) Un paciente nuevo agenda su primera cita en < 3 min sin ayuda
```

### Lectura del árbol (por qué discrimina)

| Cuadrante | Hojas | Tratamiento |
|---|---|---|
| **(A,A)** | C1 | **Driver.** Condiciona la estructura: el cupo necesita un único dueño |
| **(A,M)** | D1, S1, S4 | **Drivers.** Condicionan las fronteras (S1), la integración externa (D1) y el borde (S4) |
| **(A,B)** | D2, S2 | Importantes pero baratas **una vez** elegida la estructura: salen casi gratis con eventos y con un dueño único de la historia |
| **(M,A)** | D3, P1, I1 | Se tienen en cuenta al decidir (escalar Agenda por separado, aislar los datos clínicos), pero no se implementan en el esqueleto |
| **(M,M) / (M,B)** | M1, C2, S3 | Se resuelven con tácticas locales (adaptador, regla de dominio, hash) |
| **(B,·)** | M2, U1 | No influyen en la arquitectura |

De las 15 hojas, solo una es (A,A) y tres son (A,M). El resto queda explícitamente por debajo.

## 1.5 Drivers priorizados

| Prioridad | Driver | Hojas | Restricción de origen | Escenario |
|---|---|---|---|---|
| **1** | Integridad de la agenda bajo concurrencia | C1, C2 | RS5 (picos de demanda) | [E1](02-escenarios.md#e1--dos-pacientes-piden-el-mismo-cupo-al-mismo-tiempo) |
| **2** | Confidencialidad y trazabilidad de la historia clínica | S1, S2, S4 | RS1, RS2, RS3 | [E3](02-escenarios.md#e3--consulta-de-una-historia-clínica) |
| **3** | Disponibilidad del agendamiento frente a dependencias externas | D1, D2 | RS4 (EPS externa) | [E2](02-escenarios.md#e2--la-eps-no-responde) y [E4](02-escenarios.md#e4--el-servicio-de-notificaciones-se-cae) |

**De dónde salió cada driver** (pregunta de sustentación):

- **Driver 1** sale de la operación: al abrir agenda llegan muchas solicitudes a la vez por los mismos cupos de los especialistas más pedidos. Un cupo asignado dos veces significa dos pacientes en la sala para un mismo turno. Es el error más visible y más costoso para la IPS.
- **Driver 2** sale de la norma (RS1, RS2): la historia clínica es un dato sensible y la IPS responde legalmente por quién la consulta. No depende de nuestras preferencias.
- **Driver 3** sale de la integración con la EPS (RS4): la autorización es obligatoria para los especialistas, pero el sistema de la EPS no es nuestro. Si su caída tumba el agendamiento, la recepción se paraliza.
