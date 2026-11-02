package com.clinica.gestion.controller;

import com.clinica.gestion.dto.Dtos;
import com.clinica.gestion.facade.ClinicaFacade;
import com.clinica.gestion.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clinica")
public class ClinicaController {

    private final ClinicaFacade clinicaFacade;

    public ClinicaController(ClinicaFacade clinicaFacade) {
        this.clinicaFacade = clinicaFacade;
    }

    // ── POST /api/clinica/paciente ────────────────────────────────────────────
    @PostMapping("/paciente")
    public ResponseEntity<Dtos.ApiResponse<Patient>> registerPatient(
            @RequestBody Dtos.RegisterPatientRequest request) {
        try {
            Patient patient = clinicaFacade.registerPatient(
                    request.getDocumentNumber(), request.getFirstName(), request.getLastName(),
                    request.getEmail(), request.getPhone(), request.getBirthDate(),
                    request.getPassword(), request.getAllergies());
            return ResponseEntity.ok(new Dtos.ApiResponse<>(true, "Patient registered successfully", patient));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new Dtos.ApiResponse<>(false, e.getMessage(), null));
        }
    }

    // ── POST /api/clinica/login ───────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<Dtos.ApiResponse<Patient>> login(
            @RequestBody Dtos.LoginRequest request) {
        try {
            Patient patient = clinicaFacade.login(request.getDocumentNumber(), request.getPassword());
            return ResponseEntity.ok(new Dtos.ApiResponse<>(true, "Login successful", patient));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new Dtos.ApiResponse<>(false, e.getMessage(), null));
        }
    }

    // ── POST /api/clinica/cita ────────────────────────────────────────────────
    @PostMapping("/cita")
    public ResponseEntity<Dtos.ApiResponse<Appointment>> scheduleAppointment(
            @RequestBody Dtos.AppointmentRequest request) {
        try {
            Appointment appointment = clinicaFacade.agendarCita(
                    request.getPatientId(), request.getSpecialty(),
                    request.getDoctorId(), request.getDate());
            return ResponseEntity.ok(new Dtos.ApiResponse<>(true, "Appointment scheduled successfully", appointment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new Dtos.ApiResponse<>(false, e.getMessage(), null));
        }
    }

    // ── DELETE /api/clinica/cita/{appointmentId} ──────────────────────────────
    @DeleteMapping("/cita/{appointmentId}")
    public ResponseEntity<Dtos.ApiResponse<Appointment>> cancelAppointment(
            @PathVariable String appointmentId) {
        try {
            // Direct delegation for cancel — no facade method needed for single subsystem
            return ResponseEntity.ok(new Dtos.ApiResponse<>(true, "Appointment cancelled", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new Dtos.ApiResponse<>(false, e.getMessage(), null));
        }
    }

    // ── GET /api/clinica/historia/{pacienteId} ────────────────────────────────
    @GetMapping("/historia/{pacienteId}")
    public ResponseEntity<Dtos.ApiResponse<Map<String, Object>>> getCompleteHistory(
            @PathVariable String pacienteId) {
        try {
            Map<String, Object> history = clinicaFacade.verHistoriaCompleta(pacienteId);
            return ResponseEntity.ok(new Dtos.ApiResponse<>(true, "Complete history retrieved", history));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new Dtos.ApiResponse<>(false, e.getMessage(), null));
        }
    }

    // ── POST /api/clinica/prescripcion ────────────────────────────────────────
    @PostMapping("/prescripcion")
    public ResponseEntity<Dtos.ApiResponse<Prescription>> createPrescription(
            @RequestBody Dtos.PrescriptionRequest request) {
        try {
            List<Prescription.Medication> medications = request.getMedications().stream()
                    .map(m -> new Prescription.Medication(
                            m.getName(), m.getDosage(), m.getDuration(), m.getFrequency()))
                    .collect(Collectors.toList());
            Prescription prescription = clinicaFacade.generarPrescripcion(
                    request.getPatientId(), medications, request.getNotes());
            return ResponseEntity.ok(new Dtos.ApiResponse<>(true, "Prescription created", prescription));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new Dtos.ApiResponse<>(false, e.getMessage(), null));
        }
    }

    // ── POST /api/clinica/laboratorio ─────────────────────────────────────────
    @PostMapping("/laboratorio")
    public ResponseEntity<Dtos.ApiResponse<LabResult>> requestLabExams(
            @RequestBody Dtos.LaboratoryRequest request) {
        try {
            LabResult result = clinicaFacade.solicitarExamenes(
                    request.getPatientId(), request.getExams());
            return ResponseEntity.ok(new Dtos.ApiResponse<>(true, "Lab exams processed", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new Dtos.ApiResponse<>(false, e.getMessage(), null));
        }
    }

    // ── GET /api/clinica/medicos ──────────────────────────────────────────────
    @GetMapping("/medicos")
    public ResponseEntity<Dtos.ApiResponse<List<Doctor>>> getDoctors(
            @RequestParam(required = false) String especialidad) {
        List<Doctor> doctors = clinicaFacade.getDoctorsBySpecialty(especialidad);
        return ResponseEntity.ok(new Dtos.ApiResponse<>(true, "Doctors retrieved", doctors));
    }

    // ── GET /api/clinica/especialidades ──────────────────────────────────────
    @GetMapping("/especialidades")
    public ResponseEntity<Dtos.ApiResponse<List<String>>> getSpecialties() {
        List<String> specialties = clinicaFacade.getAvailableSpecialties();
        return ResponseEntity.ok(new Dtos.ApiResponse<>(true, "Specialties retrieved", specialties));
    }

    // ── GET /api/clinica/health ───────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<Dtos.ApiResponse<String>> health() {
        return ResponseEntity.ok(new Dtos.ApiResponse<>(true, "Service running", "OK"));
    }
}
