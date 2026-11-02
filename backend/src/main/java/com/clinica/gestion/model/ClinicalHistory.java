package com.clinica.gestion.model;

import java.util.List;

public class ClinicalHistory {
    private String id;
    private String patientId;
    private String visitDate;
    private String doctorName;
    private String diagnosis;
    private String treatment;
    private List<String> allergiesNoted;
    private String observations;

    public ClinicalHistory() {}

    public ClinicalHistory(String id, String patientId, String visitDate, String doctorName,
                           String diagnosis, String treatment,
                           List<String> allergiesNoted, String observations) {
        this.id = id;
        this.patientId = patientId;
        this.visitDate = visitDate;
        this.doctorName = doctorName;
        this.diagnosis = diagnosis;
        this.treatment = treatment;
        this.allergiesNoted = allergiesNoted;
        this.observations = observations;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getVisitDate() { return visitDate; }
    public void setVisitDate(String visitDate) { this.visitDate = visitDate; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getTreatment() { return treatment; }
    public void setTreatment(String treatment) { this.treatment = treatment; }
    public List<String> getAllergiesNoted() { return allergiesNoted; }
    public void setAllergiesNoted(List<String> allergiesNoted) { this.allergiesNoted = allergiesNoted; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
}
