package com.clinica.gestion.facade;

import com.clinica.gestion.model.*;
import com.clinica.gestion.service.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ClinicaFacade — Facade Pattern Implementation
 *
 * Provides a simplified, unified interface for the patient portal.
 * Internally coordinates multiple subsystems (PacienteService, AgendaService,
 * HistoriaClinicaService, PrescripcionService, LaboratorioService) without
 * exposing their complexity to the client (REST controllers).
 */
@Component
public class ClinicaFacade {

    private final PacienteService pacienteService;
    private final AgendaService agendaService;
    private final HistoriaClinicaService historiaClinicaService;
    private final PrescripcionService prescripcionService;
    private final LaboratorioService laboratorioService;

    public ClinicaFacade(PacienteService pacienteService,
                         AgendaService agendaService,
                         HistoriaClinicaService historiaClinicaService,
                         PrescripcionService prescripcionService,
                         LaboratorioService laboratorioService) {
        this.pacienteService = pacienteService;
        this.agendaService = agendaService;
        this.historiaClinicaService = historiaClinicaService;
        this.prescripcionService = prescripcionService;
        this.laboratorioService = laboratorioService;
    }

    // ── 1. Register a new patient ─────────────────────────────────────────────
    // Subsystems: PacienteService (validation + registration)
    public Patient registerPatient(String documentNumber, String firstName, String lastName,
                                   String email, String phone, String birthDate,
                                   String password, List<String> allergies) {
        return pacienteService.registerPatient(
                documentNumber, firstName, lastName, email, phone, birthDate, password, allergies);
    }

    // ── 2. Login ──────────────────────────────────────────────────────────────
    public Patient login(String documentNumber, String password) {
        return pacienteService.authenticate(documentNumber, password);
    }

    // ── 3. Schedule appointment ───────────────────────────────────────────────
    // Subsystems: PacienteService (validate patient), AgendaService (check & book slot)
    public Appointment agendarCita(String patientId, String specialty, String doctorId, String date) {
        pacienteService.getPatientById(patientId); // validate patient exists
        return agendaService.scheduleAppointment(patientId, specialty, doctorId, date);
    }

    // ── 4. Complete clinical history ──────────────────────────────────────────
    // Subsystems: PacienteService, AgendaService, HistoriaClinicaService,
    //             PrescripcionService, LaboratorioService
    public Map<String, Object> verHistoriaCompleta(String patientId) {
        Patient patient = pacienteService.getPatientById(patientId);
        List<Appointment> appointments = agendaService.getPatientAppointments(patientId);
        List<ClinicalHistory> history = historiaClinicaService.getPatientHistory(patientId);
        List<Prescription> prescriptions = prescripcionService.getPatientPrescriptions(patientId);
        List<LabResult> labResults = laboratorioService.getPatientLabResults(patientId);

        Map<String, Object> completeHistory = new HashMap<>();
        completeHistory.put("patient", patient);
        completeHistory.put("appointments", appointments);
        completeHistory.put("clinicalHistory", history);
        completeHistory.put("prescriptions", prescriptions);
        completeHistory.put("labResults", labResults);
        return completeHistory;
    }

    // ── 5. Generate prescription with allergy validation ──────────────────────
    // Subsystems: PacienteService (get allergies), PrescripcionService (validate + create)
    public Prescription generarPrescripcion(String patientId,
                                             List<Prescription.Medication> medications,
                                             String notes) {
        List<String> allergies = pacienteService.getPatientAllergies(patientId);
        return prescripcionService.createPrescription(patientId, allergies, medications, notes);
    }

    // ── 6. Request lab exams ──────────────────────────────────────────────────
    // Subsystems: PacienteService (validate), LaboratorioService (process),
    //             HistoriaClinicaService (record event)
    public LabResult solicitarExamenes(String patientId, List<String> exams) {
        pacienteService.getPatientById(patientId); // validate patient
        LabResult result = laboratorioService.requestExams(patientId, exams);
        String examList = String.join(", ", exams);
        historiaClinicaService.addClinicalRecord(patientId, "Sistema Laboratorio",
                "Lab exams requested: " + examList,
                "Awaiting results", null, "Automatic registration via lab request");
        return result;
    }

    // ── 7. Get doctors (delegated) ────────────────────────────────────────────
    public List<Doctor> getDoctorsBySpecialty(String specialty) {
        if (specialty == null || specialty.isBlank()) {
            return agendaService.getAllDoctors();
        }
        return agendaService.getDoctorsBySpecialty(specialty);
    }

    public List<String> getAvailableSpecialties() {
        return agendaService.getAvailableSpecialties();
    }
}
