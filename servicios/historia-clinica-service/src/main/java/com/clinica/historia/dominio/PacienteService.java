package com.clinica.historia.dominio;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PacienteService {

    private static final Set<String> EPS_CONVENIO =
            Set.of("Sura", "Sanitas", "Nueva EPS", "Compensar", "Salud Total");

    private final Map<String, Patient> patientStore = new ConcurrentHashMap<>();
    private final Map<String, String> credentialStore = new ConcurrentHashMap<>(); // documentNumber -> hash
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final EventosHistoria eventos;

    public PacienteService(EventosHistoria eventos) {
        this.eventos = eventos;
    }

    public Patient registerPatient(String documentNumber, String firstName, String lastName,
                                   String email, String phone, String birthDate, String eps,
                                   String password, List<String> allergies) {
        if (documentNumber == null || documentNumber.isBlank() || password == null || password.isBlank()) {
            throw new ReglaNegocioException("Documento y contraseña son obligatorios");
        }
        if (eps == null || !EPS_CONVENIO.contains(eps)) {
            throw new ReglaNegocioException("La EPS debe ser una con convenio: " + EPS_CONVENIO);
        }
        if (credentialStore.putIfAbsent(documentNumber, passwordEncoder.encode(password)) != null) {
            throw new ReglaNegocioException("Document number already registered: " + documentNumber);
        }

        String id = "PAT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Patient patient = new Patient(id, documentNumber, firstName, lastName,
                email, phone, birthDate, eps, allergies);
        patientStore.put(id, patient);
        eventos.pacienteRegistrado(patient);
        return patient;
    }

    public Patient authenticate(String documentNumber, String password) {
        String hash = credentialStore.get(documentNumber);
        if (hash == null || password == null || !passwordEncoder.matches(password, hash)) {
            throw new ReglaNegocioException("Invalid credentials");
        }
        return patientStore.values().stream()
                .filter(p -> p.getDocumentNumber().equals(documentNumber))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("Patient not found"));
    }

    public Patient getPatientById(String patientId) {
        Patient patient = patientStore.get(patientId);
        if (patient == null) {
            throw new ReglaNegocioException("Patient not found with id: " + patientId);
        }
        return patient;
    }

    public List<String> getPatientAllergies(String patientId) {
        return getPatientById(patientId).getAllergies();
    }
}
