package com.clinica.historia.dominio;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Historia clínica: registros de solo inserción (no hay actualización ni borrado,
 * Res. 1995 de 1999), copia de lectura de las citas y auditoría de cada consulta.
 */
@Service
public class HistoriaClinicaService {

    private final Map<String, ClinicalHistory> historyStore = new ConcurrentHashMap<>();
    private final Map<String, AppointmentView> appointmentViews = new ConcurrentHashMap<>();
    private final List<AuditEntry> auditLog = new CopyOnWriteArrayList<>();

    private final PacienteService pacienteService;
    private final PrescripcionService prescripcionService;
    private final LaboratorioService laboratorioService;

    public HistoriaClinicaService(PacienteService pacienteService,
                                  PrescripcionService prescripcionService,
                                  LaboratorioService laboratorioService) {
        this.pacienteService = pacienteService;
        this.prescripcionService = prescripcionService;
        this.laboratorioService = laboratorioService;
    }

    public ClinicalHistory addClinicalRecord(String patientId, String doctorName, String diagnosis,
                                             String treatment, List<String> allergiesNoted, String observations) {
        String id = "HIS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ClinicalHistory record = new ClinicalHistory(id, patientId, LocalDate.now().toString(), doctorName,
                diagnosis, treatment, allergiesNoted, observations);
        historyStore.put(id, record);
        return record;
    }

    // Idempotente: el mismo evento entregado dos veces sobrescribe la misma entrada
    public void actualizarCita(AppointmentView cita) {
        appointmentViews.put(cita.id(), cita);
    }

    public Map<String, Object> verHistoriaCompleta(String patientId, String usuario) {
        Patient patient = pacienteService.getPatientById(patientId);
        auditLog.add(new AuditEntry(Instant.now().toString(), usuario, patientId, "CONSULTA_HISTORIA"));

        Map<String, Object> completeHistory = new HashMap<>();
        completeHistory.put("patient", patient);
        completeHistory.put("appointments", appointmentViews.values().stream()
                .filter(a -> a.patientId().equals(patientId))
                .sorted(Comparator.comparing(AppointmentView::date))
                .collect(Collectors.toList()));
        completeHistory.put("clinicalHistory", historyStore.values().stream()
                .filter(h -> h.getPatientId().equals(patientId))
                .sorted(Comparator.comparing(ClinicalHistory::getVisitDate).reversed())
                .collect(Collectors.toList()));
        completeHistory.put("prescriptions", prescripcionService.getPatientPrescriptions(patientId));
        completeHistory.put("labResults", laboratorioService.getPatientLabResults(patientId));
        return completeHistory;
    }

    public Prescription generarPrescripcion(String patientId, List<Prescription.Medication> medications, String notes) {
        List<String> allergies = pacienteService.getPatientAllergies(patientId);
        return prescripcionService.createPrescription(patientId, allergies, medications, notes);
    }

    public LabResult solicitarExamenes(String patientId, List<String> exams) {
        pacienteService.getPatientById(patientId);
        LabResult result = laboratorioService.requestExams(patientId, exams);
        addClinicalRecord(patientId, "Sistema Laboratorio", "Lab exams requested: " + String.join(", ", exams),
                "Awaiting results", null, "Automatic registration via lab request");
        return result;
    }

    public List<AuditEntry> getAuditLog(String patientId) {
        return auditLog.stream().filter(a -> a.patientId().equals(patientId)).collect(Collectors.toList());
    }
}
