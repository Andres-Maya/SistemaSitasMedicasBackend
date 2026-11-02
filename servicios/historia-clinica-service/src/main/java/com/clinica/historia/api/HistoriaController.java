package com.clinica.historia.api;

import com.clinica.historia.dominio.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clinica")
public class HistoriaController {

    private final PacienteService pacienteService;
    private final HistoriaClinicaService historiaClinicaService;

    public HistoriaController(PacienteService pacienteService, HistoriaClinicaService historiaClinicaService) {
        this.pacienteService = pacienteService;
        this.historiaClinicaService = historiaClinicaService;
    }

    public record ApiResponse<T>(boolean success, String message, T data) {}

    public record RegisterPatientRequest(String documentNumber, String firstName, String lastName, String email,
                                         String phone, String birthDate, String eps, String password,
                                         List<String> allergies) {}

    public record LoginRequest(String documentNumber, String password) {}

    public record MedicationDto(String name, String dosage, String duration, String frequency) {}

    public record PrescriptionRequest(String patientId, List<MedicationDto> medications, String notes) {}

    public record LaboratoryRequest(String patientId, List<String> exams) {}

    // ── POST /api/clinica/paciente ────────────────────────────────────────────
    @PostMapping("/paciente")
    public ApiResponse<Patient> registerPatient(@RequestBody RegisterPatientRequest r) {
        Patient patient = pacienteService.registerPatient(r.documentNumber(), r.firstName(), r.lastName(),
                r.email(), r.phone(), r.birthDate(), r.eps(), r.password(), r.allergies());
        return new ApiResponse<>(true, "Patient registered successfully", patient);
    }

    // ── POST /api/clinica/login ───────────────────────────────────────────────
    @PostMapping("/login")
    public ApiResponse<Patient> login(@RequestBody LoginRequest r) {
        return new ApiResponse<>(true, "Login successful", pacienteService.authenticate(r.documentNumber(), r.password()));
    }

    // ── GET /api/clinica/historia/{pacienteId} ────────────────────────────────
    // X-Usuario identifica a quien consulta; en producción saldría del token validado en el gateway
    @GetMapping("/historia/{pacienteId}")
    public ApiResponse<Map<String, Object>> getCompleteHistory(
            @PathVariable String pacienteId,
            @RequestHeader(value = "X-Usuario", required = false) String usuario) {
        String quien = usuario != null ? usuario : "paciente:" + pacienteId;
        return new ApiResponse<>(true, "Complete history retrieved",
                historiaClinicaService.verHistoriaCompleta(pacienteId, quien));
    }

    // ── POST /api/clinica/prescripcion ────────────────────────────────────────
    @PostMapping("/prescripcion")
    public ApiResponse<Prescription> createPrescription(@RequestBody PrescriptionRequest r) {
        List<Prescription.Medication> medications = r.medications() == null ? List.of() : r.medications().stream()
                .map(m -> new Prescription.Medication(m.name(), m.dosage(), m.duration(), m.frequency()))
                .collect(Collectors.toList());
        return new ApiResponse<>(true, "Prescription created",
                historiaClinicaService.generarPrescripcion(r.patientId(), medications, r.notes()));
    }

    // ── POST /api/clinica/laboratorio ─────────────────────────────────────────
    @PostMapping("/laboratorio")
    public ApiResponse<LabResult> requestLabExams(@RequestBody LaboratoryRequest r) {
        return new ApiResponse<>(true, "Lab exams processed",
                historiaClinicaService.solicitarExamenes(r.patientId(), r.exams()));
    }

    // ── GET /api/clinica/auditoria/{pacienteId} ───────────────────────────────
    @GetMapping("/auditoria/{pacienteId}")
    public ApiResponse<List<AuditEntry>> getAudit(@PathVariable String pacienteId) {
        return new ApiResponse<>(true, "Audit log retrieved", historiaClinicaService.getAuditLog(pacienteId));
    }

    // ── GET /api/clinica/historia-health ──────────────────────────────────────
    @GetMapping("/historia-health")
    public ApiResponse<String> health() {
        return new ApiResponse<>(true, "historia-clinica-service running", "OK");
    }

    @ExceptionHandler({ReglaNegocioException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Void>> handleRule(RuntimeException e) {
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
    }
}
