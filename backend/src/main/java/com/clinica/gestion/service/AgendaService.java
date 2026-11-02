package com.clinica.gestion.service;

import com.clinica.gestion.model.Appointment;
import com.clinica.gestion.model.Doctor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AgendaService {

    private final Map<String, Doctor> doctorStore = new HashMap<>();
    private final Map<String, Appointment> appointmentStore = new HashMap<>();

    public AgendaService() {
        initializeDoctors();
    }

    private void initializeDoctors() {
        List<String> slots1 = Arrays.asList("2025-06-10 08:00", "2025-06-10 09:00", "2025-06-11 10:00");
        List<String> slots2 = Arrays.asList("2025-06-10 10:00", "2025-06-11 08:00", "2025-06-12 11:00");
        List<String> slots3 = Arrays.asList("2025-06-10 11:00", "2025-06-11 09:00", "2025-06-13 08:00");
        List<String> slots4 = Arrays.asList("2025-06-10 14:00", "2025-06-12 10:00", "2025-06-14 09:00");
        List<String> slots5 = Arrays.asList("2025-06-11 14:00", "2025-06-13 10:00", "2025-06-15 08:00");

        doctorStore.put("DOC-001", new Doctor("DOC-001", "Dr. Andrés Morales", "Cardiología", new ArrayList<>(slots1)));
        doctorStore.put("DOC-002", new Doctor("DOC-002", "Dra. Laura Perez", "Cardiología", new ArrayList<>(slots2)));
        doctorStore.put("DOC-003", new Doctor("DOC-003", "Dr. Carlos Ruiz", "Neurología", new ArrayList<>(slots3)));
        doctorStore.put("DOC-004", new Doctor("DOC-004", "Dra. Sofia Herrera", "Neurología", new ArrayList<>(slots4)));
        doctorStore.put("DOC-005", new Doctor("DOC-005", "Dr. Miguel Torres", "Pediatría", new ArrayList<>(slots5)));
        doctorStore.put("DOC-006", new Doctor("DOC-006", "Dra. Valeria Castro", "Medicina General", new ArrayList<>(slots1)));
        doctorStore.put("DOC-007", new Doctor("DOC-007", "Dr. Pablo Jimenez", "Dermatología", new ArrayList<>(slots2)));
    }

    public List<Doctor> getDoctorsBySpecialty(String specialty) {
        return doctorStore.values().stream()
                .filter(d -> d.getSpecialty().equalsIgnoreCase(specialty))
                .collect(Collectors.toList());
    }

    public List<Doctor> getAllDoctors() {
        return new ArrayList<>(doctorStore.values());
    }

    public List<String> getAvailableSpecialties() {
        return doctorStore.values().stream()
                .map(Doctor::getSpecialty)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public Appointment scheduleAppointment(String patientId, String specialty, String doctorId, String date) {
        Doctor doctor = doctorStore.get(doctorId);
        if (doctor == null) {
            throw new IllegalArgumentException("Doctor not found: " + doctorId);
        }
        if (!doctor.getAvailableSlots().contains(date)) {
            throw new IllegalArgumentException("Slot not available: " + date);
        }

        doctor.getAvailableSlots().remove(date);

        String id = "APT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Appointment appointment = new Appointment(id, patientId, doctorId,
                doctor.getFullName(), specialty, date, "SCHEDULED",
                "Reminder: Please arrive 15 minutes early and bring your ID.");
        appointmentStore.put(id, appointment);
        return appointment;
    }

    public Appointment cancelAppointment(String appointmentId) {
        Appointment appointment = appointmentStore.get(appointmentId);
        if (appointment == null) {
            throw new IllegalArgumentException("Appointment not found: " + appointmentId);
        }
        appointment.setStatus("CANCELLED");
        Doctor doctor = doctorStore.get(appointment.getDoctorId());
        if (doctor != null) {
            doctor.getAvailableSlots().add(appointment.getDate());
        }
        return appointment;
    }

    public List<Appointment> getPatientAppointments(String patientId) {
        return appointmentStore.values().stream()
                .filter(a -> a.getPatientId().equals(patientId))
                .collect(Collectors.toList());
    }
}
