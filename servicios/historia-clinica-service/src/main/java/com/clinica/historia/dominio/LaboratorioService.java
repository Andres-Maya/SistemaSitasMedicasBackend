package com.clinica.historia.dominio;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class LaboratorioService {

    private final Map<String, LabResult> labResultStore = new ConcurrentHashMap<>();

    private static final Map<String, LabResult.ExamResult> SIMULATED_RESULTS = Map.of(
            "hemograma", new LabResult.ExamResult("Hemograma Completo", "14.5", "g/dL", "13.5-17.5", true),
            "glicemia", new LabResult.ExamResult("Glicemia en Ayunas", "115", "mg/dL", "70-100", false),
            "perfil lipidico", new LabResult.ExamResult("Perfil Lipídico - Colesterol Total", "185", "mg/dL", "< 200", true),
            "hemoglobina", new LabResult.ExamResult("Hemoglobina", "13.8", "g/dL", "12.0-16.0", true),
            "creatinina", new LabResult.ExamResult("Creatinina Sérica", "1.2", "mg/dL", "0.6-1.2", true),
            "urea", new LabResult.ExamResult("Urea en Sangre", "55", "mg/dL", "15-45", false));

    public LabResult requestExams(String patientId, List<String> examNames) {
        if (examNames == null || examNames.isEmpty()) {
            throw new ReglaNegocioException("At least one exam must be requested");
        }
        List<LabResult.ExamResult> results = new ArrayList<>();
        for (String examName : examNames) {
            LabResult.ExamResult r = SIMULATED_RESULTS.getOrDefault(examName.toLowerCase().trim(),
                    new LabResult.ExamResult(examName, "N/A", "-", "Pending analysis", true));
            results.add(new LabResult.ExamResult(r.getExamName(), r.getValue(), r.getUnit(),
                    r.getReferenceRange(), r.isWithinNormalRange()));
        }
        String id = "LAB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LabResult labResult = new LabResult(id, patientId, LocalDate.now().toString(), "COMPLETED", results);
        labResultStore.put(id, labResult);
        return labResult;
    }

    public List<LabResult> getPatientLabResults(String patientId) {
        return labResultStore.values().stream()
                .filter(l -> l.getPatientId().equals(patientId))
                .sorted(Comparator.comparing(LabResult::getRequestedDate).reversed())
                .collect(Collectors.toList());
    }
}
