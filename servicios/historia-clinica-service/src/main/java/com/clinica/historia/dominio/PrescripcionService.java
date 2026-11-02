package com.clinica.historia.dominio;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PrescripcionService {

    private final Map<String, Prescription> prescriptionStore = new ConcurrentHashMap<>();

    // Alergia declarada -> principios activos que no se pueden prescribir
    private static final Map<String, List<String>> CONFLICTOS = Map.of(
            "penicilina", List.of("penicilina", "amoxicilina", "ampicilina", "dicloxacilina"),
            "aines", List.of("ibuprofeno", "aspirina", "naproxeno", "diclofenaco"),
            "ibuprofeno", List.of("ibuprofeno", "aspirina", "naproxeno", "diclofenaco"),
            "sulfas", List.of("sulfametoxazol", "sulfadiazina", "sulfonamida"));

    public Prescription createPrescription(String patientId, List<String> patientAllergies,
                                           List<Prescription.Medication> medications, String notes) {
        if (medications == null || medications.isEmpty()) {
            throw new ReglaNegocioException("A prescription needs at least one medication");
        }
        validateAllergyConflicts(medications, patientAllergies);

        String id = "PRE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Prescription prescription = new Prescription(id, patientId, LocalDate.now().toString(),
                "Dr. Sistema", medications, notes);
        prescriptionStore.put(id, prescription);
        return prescription;
    }

    private void validateAllergyConflicts(List<Prescription.Medication> medications, List<String> patientAllergies) {
        if (patientAllergies == null) return;
        for (String allergy : patientAllergies) {
            String allergyLower = allergy.toLowerCase().trim();
            List<String> prohibidos = CONFLICTOS.getOrDefault(allergyLower, List.of(allergyLower));
            for (Prescription.Medication med : medications) {
                String medLower = med.getName().toLowerCase();
                if (prohibidos.stream().anyMatch(medLower::contains)) {
                    throw new ReglaNegocioException("Drug-allergy conflict detected: " + med.getName()
                            + " conflicts with patient allergy: " + allergy);
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
