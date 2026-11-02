package com.clinica.gestion.service;

import com.clinica.gestion.model.LabResult;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LaboratorioService {

    private final Map<String, LabResult> labResultStore = new HashMap<>();

    private static final Map<String, LabResult.ExamResult> SIMULATED_RESULTS = new HashMap<>();

    static {
        SIMULATED_RESULTS.put("hemograma", new LabResult.ExamResult(
                "Hemograma Completo", "14.5", "g/dL", "13.5-17.5", true));
        SIMULATED_RESULTS.put("glicemia", new LabResult.ExamResult(
                "Glicemia en Ayunas", "115", "mg/dL", "70-100", false));
        SIMULATED_RESULTS.put("perfil lipidico", new LabResult.ExamResult(
                "Perfil Lipídico - Colesterol Total", "185", "mg/dL", "< 200", true));
        SIMULATED_RESULTS.put("hemoglobina", new LabResult.ExamResult(
                "Hemoglobina", "13.8", "g/dL", "12.0-16.0", true));
        SIMULATED_RESULTS.put("creatinina", new LabResult.ExamResult(
                "Creatinina Sérica", "1.2", "mg/dL", "0.6-1.2", true));
        SIMULATED_RESULTS.put("urea", new LabResult.ExamResult(
                "Urea en Sangre", "55", "mg/dL", "15-45", false));
    }

    public LabResult requestExams(String patientId, List<String> examNames) {
        if (examNames == null || examNames.isEmpty()) {
            throw new IllegalArgumentException("At least one exam must be requested");
        }

        List<LabResult.ExamResult> results = new ArrayList<>();
        for (String examName : examNames) {
            String key = examName.toLowerCase().trim();
            LabResult.ExamResult result = SIMULATED_RESULTS.getOrDefault(key,
                    new LabResult.ExamResult(examName, "N/A", "-", "Pending analysis", true));
            results.add(new LabResult.ExamResult(
                    result.getExamName(), result.getValue(),
                    result.getUnit(), result.getReferenceRange(),
                    result.isWithinNormalRange()));
        }

        String id = "LAB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String requestedDate = LocalDate.now().toString();
        LabResult labResult = new LabResult(id, patientId, requestedDate, "COMPLETED", results);
        labResultStore.put(id, labResult);
        return labResult;
    }

    public List<LabResult> getPatientLabResults(String patientId) {
        return labResultStore.values().stream()
                .filter(l -> l.getPatientId().equals(patientId))
                .sorted(Comparator.comparing(LabResult::getRequestedDate).reversed())
                .collect(Collectors.toList());
    }

    public List<String> getAvailableExams() {
        return new ArrayList<>(SIMULATED_RESULTS.keySet());
    }
}
