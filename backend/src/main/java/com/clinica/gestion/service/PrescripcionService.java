package com.clinica.gestion.service;

import com.clinica.gestion.model.Prescription;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PrescripcionService {

    private final Map<String, Prescription> prescriptionStore = new HashMap<>();

    // Known drug-allergy interactions
    private static final Map<String, List<String>> DRUG_ALLERGY_MAP = new HashMap<>();

    static {
        DRUG_ALLERGY_MAP.put("Penicilina", Arrays.asList("penicilina", "amoxicilina", "ampicilina"));
        DRUG_ALLERGY_MAP.put("Ibuprofeno", Arrays.asList("aines", "ibuprofeno", "aspirina"));
        DRUG_ALLERGY_MAP.put("Sulfonamidas", Arrays.asList("sulfas", "sulfonamidas"));
    }

    public Prescription createPrescription(String patientId, List<String> patientAllergies,
                                            List<Prescription.Medication> medications, String notes) {
        validateAllergyConflicts(medications, patientAllergies);

        String id = "PRE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String issuedDate = LocalDate.now().toString();
        Prescription prescription = new Prescription(id, patientId, issuedDate,
                "Dr. Sistema", medications, notes);
        prescriptionStore.put(id, prescription);
        return prescription;
    }

    private void validateAllergyConflicts(List<Prescription.Medication> medications,
                                           List<String> patientAllergies) {
        if (patientAllergies == null || patientAllergies.isEmpty()) return;

        for (Prescription.Medication med : medications) {
            for (String allergy : patientAllergies) {
                String allergyLower = allergy.toLowerCase();
                for (Map.Entry<String, List<String>> entry : DRUG_ALLERGY_MAP.entrySet()) {
                    if (entry.getValue().contains(allergyLower) &&
                        med.getName().toLowerCase().contains(allergyLower)) {
                        throw new IllegalArgumentException(
                                "Drug-allergy conflict detected: " + med.getName() +
                                " conflicts with patient allergy: " + allergy);
                    }
                }
            }
        }
    }

    public List<Prescription> getPatientPrescriptions(String patientId) {
        return prescriptionStore.values().stream()
                .filter(p -> p.getPatientId().equals(patientId))
                .sorted(Comparator.comparing(Prescription::getIssuedDate).reversed())
                .collect(Collectors.toList());
    }
}
