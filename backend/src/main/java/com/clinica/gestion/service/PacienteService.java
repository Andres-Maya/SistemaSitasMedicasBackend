package com.clinica.gestion.service;

import com.clinica.gestion.model.Patient;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PacienteService {

    private final Map<String, Patient> patientStore = new HashMap<>();
    private final Map<String, String> credentialStore = new HashMap<>(); // documentNumber -> password

    public Patient registerPatient(String documentNumber, String firstName, String lastName,
                                   String email, String phone, String birthDate,
                                   String password, List<String> allergies) {
        if (patientStore.values().stream()
                .anyMatch(p -> p.getDocumentNumber().equals(documentNumber))) {
            throw new IllegalArgumentException("Document number already registered: " + documentNumber);
        }

        String id = "PAT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Patient patient = new Patient(id, documentNumber, firstName, lastName,
                email, phone, birthDate, allergies);
        patientStore.put(id, patient);
        credentialStore.put(documentNumber, password);
        return patient;
    }

    public Patient authenticate(String documentNumber, String password) {
        String storedPassword = credentialStore.get(documentNumber);
        if (storedPassword == null || !storedPassword.equals(password)) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return patientStore.values().stream()
                .filter(p -> p.getDocumentNumber().equals(documentNumber))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
    }

    public Patient getPatientById(String patientId) {
        Patient patient = patientStore.get(patientId);
        if (patient == null) {
            throw new IllegalArgumentException("Patient not found with id: " + patientId);
        }
        return patient;
    }

    public boolean documentExists(String documentNumber) {
        return patientStore.values().stream()
                .anyMatch(p -> p.getDocumentNumber().equals(documentNumber));
    }

    public List<String> getPatientAllergies(String patientId) {
        return getPatientById(patientId).getAllergies();
    }
}
