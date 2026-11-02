package com.clinica.gestion.model;

import java.util.List;

public class LabResult {
    private String id;
    private String patientId;
    private String requestedDate;
    private String status;
    private List<ExamResult> results;

    public static class ExamResult {
        private String examName;
        private String value;
        private String unit;
        private String referenceRange;
        private boolean withinNormalRange;

        public ExamResult() {}

        public ExamResult(String examName, String value, String unit,
                          String referenceRange, boolean withinNormalRange) {
            this.examName = examName;
            this.value = value;
            this.unit = unit;
            this.referenceRange = referenceRange;
            this.withinNormalRange = withinNormalRange;
        }

        public String getExamName() { return examName; }
        public void setExamName(String examName) { this.examName = examName; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getReferenceRange() { return referenceRange; }
        public void setReferenceRange(String referenceRange) { this.referenceRange = referenceRange; }
        public boolean isWithinNormalRange() { return withinNormalRange; }
        public void setWithinNormalRange(boolean withinNormalRange) { this.withinNormalRange = withinNormalRange; }
    }

    public LabResult() {}

    public LabResult(String id, String patientId, String requestedDate,
                     String status, List<ExamResult> results) {
        this.id = id;
        this.patientId = patientId;
        this.requestedDate = requestedDate;
        this.status = status;
        this.results = results;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getRequestedDate() { return requestedDate; }
    public void setRequestedDate(String requestedDate) { this.requestedDate = requestedDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<ExamResult> getResults() { return results; }
    public void setResults(List<ExamResult> results) { this.results = results; }
}
