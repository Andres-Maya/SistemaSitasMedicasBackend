package com.clinica.agenda.api;

import com.clinica.agenda.dominio.AgendaService;
import com.clinica.agenda.dominio.Appointment;
import com.clinica.agenda.dominio.Doctor;
import com.clinica.agenda.dominio.ReglaNegocioException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clinica")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    public record ApiResponse<T>(boolean success, String message, T data) {}

    public record AppointmentRequest(String patientId, String specialty, String doctorId, String date) {}

    // ── GET /api/clinica/medicos ──────────────────────────────────────────────
    @GetMapping("/medicos")
    public ApiResponse<List<Doctor>> getDoctors(@RequestParam(required = false) String especialidad) {
        return new ApiResponse<>(true, "Doctors retrieved", agendaService.getDoctors(especialidad));
    }

    // ── GET /api/clinica/especialidades ───────────────────────────────────────
    @GetMapping("/especialidades")
    public ApiResponse<List<String>> getSpecialties() {
        return new ApiResponse<>(true, "Specialties retrieved", agendaService.getAvailableSpecialties());
    }

    // ── POST /api/clinica/cita ────────────────────────────────────────────────
    @PostMapping("/cita")
    public ApiResponse<Appointment> scheduleAppointment(@RequestBody AppointmentRequest request) {
        Appointment cita = agendaService.agendar(request.patientId(), request.doctorId(), request.date());
        return new ApiResponse<>(true, "Appointment " + cita.getStatus(), cita);
    }

    // ── DELETE /api/clinica/cita/{appointmentId} ──────────────────────────────
    @DeleteMapping("/cita/{appointmentId}")
    public ApiResponse<Appointment> cancelAppointment(@PathVariable String appointmentId) {
        return new ApiResponse<>(true, "Appointment cancelled", agendaService.cancelar(appointmentId));
    }

    // ── GET /api/clinica/agenda/citas?patientId= ──────────────────────────────
    @GetMapping("/agenda/citas")
    public ApiResponse<List<Appointment>> getAppointments(@RequestParam String patientId) {
        return new ApiResponse<>(true, "Appointments retrieved", agendaService.getPatientAppointments(patientId));
    }

    // ── GET /api/clinica/agenda/pacientes/{patientId} ─────────────────────────
    @GetMapping("/agenda/pacientes/{patientId}")
    public ApiResponse<Map<String, Boolean>> knowsPatient(@PathVariable String patientId) {
        return new ApiResponse<>(true, "Projection status",
                Map.of("known", agendaService.conocePaciente(patientId)));
    }

    // ── GET /api/clinica/agenda/health ────────────────────────────────────────
    @GetMapping("/agenda/health")
    public ApiResponse<String> health() {
        return new ApiResponse<>(true, "agenda-service running", "OK");
    }

    @ExceptionHandler({ReglaNegocioException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Void>> handleRule(RuntimeException e) {
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
    }
}
