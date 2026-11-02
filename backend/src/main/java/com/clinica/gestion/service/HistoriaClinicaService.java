package com.clinica.gestion.service;

import com.clinica.gestion.model.ClinicalHistory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HistoriaClinicaService {

    private final Map<String, ClinicalHistory> historyStore = new HashMap<>();

    public ClinicalHistory addClinicalRecord(String patientId, String doctorName,
                                              String diagnosis, String treatment,
                                              List<String> allergiesNoted, String observations) {
        String id = "HIS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String visitDate = LocalDate.now().toString();
        ClinicalHistory record = new ClinicalHistory(id, patientId, visitDate, doctorName,
                diagnosis, treatment, allergiesNoted, observations);
        historyStore.put(id, record);
        return record;
    }

    public List<ClinicalHistory> getPatientHistory(String patientId) {
        return historyStore.values().stream()
                .filter(h -> h.getPatientId().equals(patientId))
                .sorted(Comparator.comparing(ClinicalHistory::getVisitDate).reversed())
                .collect(Collectors.toList());
    }

    public List<String> getPatientDiagnoses(String patientId) {
        return getPatientHistory(patientId).stream()
                .map(ClinicalHistory::getDiagnosis)
                .collect(Collectors.toList());
    }
}
