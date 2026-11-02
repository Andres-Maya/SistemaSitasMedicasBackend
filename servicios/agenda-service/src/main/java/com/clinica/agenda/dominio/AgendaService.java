package com.clinica.agenda.dominio;

import com.clinica.agenda.dominio.AutorizadorEps.ResultadoAutorizacion;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Reglas del contexto Agenda.
 *
 * R1 Un cupo se asigna a lo sumo a una cita (Agenda es el único escritor de cupos).
 * R2 Un paciente no puede tener dos citas activas en la misma especialidad.
 * R3 Las especialidades distintas de Medicina General requieren autorización de la EPS.
 *    Si la EPS no responde a tiempo la cita queda PENDING_AUTHORIZATION (ADR-003).
 * R4 Una cita solo se cancela con al menos 24 horas de antelación.
 */
@Service
public class AgendaService {

    static final String ESPECIALIDAD_SIN_AUTORIZACION = "Medicina General";
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Duration ANTELACION_CANCELACION = Duration.ofHours(24);

    private final Map<String, Doctor> medicos = new ConcurrentHashMap<>();
    private final Map<String, Appointment> citas = new ConcurrentHashMap<>();
    private final Map<String, PacienteConocido> pacientes = new ConcurrentHashMap<>();

    private final AutorizadorEps autorizadorEps;
    private final EventosAgenda eventos;

    public AgendaService(AutorizadorEps autorizadorEps, EventosAgenda eventos) {
        this.autorizadorEps = autorizadorEps;
        this.eventos = eventos;
        initializeDoctors();
    }

    private void initializeDoctors() {
        agregarMedico("DOC-001", "Dr. Andrés Morales", "Cardiología");
        agregarMedico("DOC-002", "Dra. Laura Perez", "Cardiología");
        agregarMedico("DOC-003", "Dr. Carlos Ruiz", "Neurología");
        agregarMedico("DOC-004", "Dra. Sofia Herrera", "Neurología");
        agregarMedico("DOC-005", "Dr. Miguel Torres", "Pediatría");
        agregarMedico("DOC-006", "Dra. Valeria Castro", "Medicina General");
        agregarMedico("DOC-007", "Dr. Pablo Jimenez", "Dermatología");
    }

    // Cupos relativos al arranque: uno dentro de 4 horas (su cancelación siempre choca con R4)
    // y tres por día durante los tres días siguientes
    private void agregarMedico(String id, String nombre, String especialidad) {
        Set<String> cupos = new TreeSet<>();
        cupos.add(LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(4).format(FORMATO));
        LocalDate hoy = LocalDate.now();
        for (int dia = 1; dia <= 3; dia++) {
            for (int hora : new int[]{8, 10, 14}) {
                cupos.add(hoy.plusDays(dia).atTime(hora, 0).format(FORMATO));
            }
        }
        medicos.put(id, new Doctor(id, nombre, especialidad, new ArrayList<>(cupos)));
    }

    // ── Proyección de pacientes (alimentada por paciente.registrado) ─────────
    public void registrarPacienteConocido(PacienteConocido paciente) {
        pacientes.put(paciente.patientId(), paciente);
    }

    public boolean conocePaciente(String patientId) {
        return pacientes.containsKey(patientId);
    }

    // ── Consultas ────────────────────────────────────────────────────────────
    public List<Doctor> getDoctors(String especialidad) {
        return medicos.values().stream()
                .filter(d -> especialidad == null || especialidad.isBlank()
                        || d.getSpecialty().equalsIgnoreCase(especialidad))
                .sorted(Comparator.comparing(Doctor::getId))
                .collect(Collectors.toList());
    }

    public List<String> getAvailableSpecialties() {
        return medicos.values().stream().map(Doctor::getSpecialty).distinct().sorted().collect(Collectors.toList());
    }

    public List<Appointment> getPatientAppointments(String patientId) {
        return citas.values().stream()
                .filter(c -> c.getPatientId().equals(patientId))
                .sorted(Comparator.comparing(Appointment::getDate))
                .collect(Collectors.toList());
    }

    // ── Agendar ──────────────────────────────────────────────────────────────
    public Appointment agendar(String patientId, String doctorId, String cupo) {
        PacienteConocido paciente = pacientes.get(patientId);
        if (paciente == null) {
            throw new ReglaNegocioException("Agenda no conoce al paciente " + patientId
                    + " (no existe o el evento paciente.registrado aún no ha llegado)");
        }
        Doctor medico = buscarMedico(doctorId);
        Appointment cita = reservarCupo(paciente, medico, cupo);

        // La llamada externa ocurre FUERA del bloqueo: una EPS lenta no frena otras reservas
        if (!ESPECIALIDAD_SIN_AUTORIZACION.equalsIgnoreCase(medico.getSpecialty())) {
            ResultadoAutorizacion resultado = autorizadorEps.autorizar(paciente, medico.getSpecialty());
            switch (resultado.decision()) {
                case APROBADA -> cita.setAuthorizationNumber(resultado.numeroAutorizacion());
                case NEGADA -> {
                    liberarReserva(cita, medico);
                    throw new ReglaNegocioException("La EPS " + paciente.eps()
                            + " negó la autorización para " + medico.getSpecialty());
                }
                case NO_DISPONIBLE -> {
                    cita.setStatus(Appointment.PENDING_AUTHORIZATION);
                    cita.setNotes("La EPS no respondió a tiempo: el cupo queda reservado "
                            + "y la autorización se confirmará después.");
                }
            }
        }
        eventos.citaAgendada(cita);
        return cita;
    }

    // R1 + R2 en una sola sección crítica: único escritor de cupos dentro de la instancia
    private synchronized Appointment reservarCupo(PacienteConocido paciente, Doctor medico, String cupo) {
        boolean yaTieneCita = citas.values().stream().anyMatch(c ->
                c.getPatientId().equals(paciente.patientId())
                        && c.getSpecialty().equalsIgnoreCase(medico.getSpecialty())
                        && c.isActive());
        if (yaTieneCita) {
            throw new ReglaNegocioException("El paciente ya tiene una cita activa en " + medico.getSpecialty());
        }
        if (!medico.getAvailableSlots().remove(cupo)) {
            throw new ReglaNegocioException("El cupo " + cupo + " ya no está disponible");
        }
        String id = "APT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Appointment cita = new Appointment(id, paciente.patientId(), medico.getId(), medico.getFullName(),
                medico.getSpecialty(), cupo, Appointment.SCHEDULED,
                "Recuerde llegar 15 minutos antes con su documento.");
        citas.put(id, cita);
        return cita;
    }

    private synchronized void liberarReserva(Appointment cita, Doctor medico) {
        citas.remove(cita.getId());
        medico.devolverCupo(cita.getDate());
    }

    // ── Cancelar ─────────────────────────────────────────────────────────────
    public Appointment cancelar(String appointmentId) {
        Appointment cita = marcarCancelada(appointmentId);
        eventos.citaCancelada(cita);
        return cita;
    }

    private synchronized Appointment marcarCancelada(String appointmentId) {
        Appointment cita = citas.get(appointmentId);
        if (cita == null) {
            throw new ReglaNegocioException("Cita no encontrada: " + appointmentId);
        }
        if (!cita.isActive()) {
            throw new ReglaNegocioException("La cita " + appointmentId + " ya estaba cancelada");
        }
        LocalDateTime inicio = LocalDateTime.parse(cita.getDate(), FORMATO);
        if (Duration.between(LocalDateTime.now(), inicio).compareTo(ANTELACION_CANCELACION) < 0) {
            throw new ReglaNegocioException("Las citas solo se pueden cancelar con 24 horas de antelación");
        }
        cita.setStatus(Appointment.CANCELLED);
        buscarMedico(cita.getDoctorId()).devolverCupo(cita.getDate());
        return cita;
    }

    private Doctor buscarMedico(String doctorId) {
        Doctor medico = medicos.get(doctorId);
        if (medico == null) {
            throw new ReglaNegocioException("Médico no encontrado: " + doctorId);
        }
        return medico;
    }
}
