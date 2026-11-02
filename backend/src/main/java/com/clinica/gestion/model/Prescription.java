package com.clinica.gestion.model;

import java.util.List;

public class Prescription {
    private String id;
    private String patientId;
    private String issuedDate;
    private String doctorName;
    private List<Medication> medications;
    private String notes;

    public static class Medication {
        private String name;
        private String dosage;
        private String duration;
        private String frequency;

        public Medication() {}

        public Medication(String name, String dosage, String duration, String frequency) {
            this.name = name;
            this.dosage = dosage;
            this.duration = duration;
            this.frequency = frequency;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDosage() { return dosage; }
        public void setDosage(String dosage) { this.dosage = dosage; }
        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }
        public String getFrequency() { return frequency; }
        public void setFrequency(String frequency) { this.frequency = frequency; }
    }

    public Prescription() {}

    public Prescription(String id, String patientId, String issuedDate, String doctorName,
                        List<Medication> medications, String notes) {
        this.id = id;
        this.patientId = patientId;
        this.issuedDate = issuedDate;
        this.doctorName = doctorName;
        this.medications = medications;
        this.notes = notes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getIssuedDate() { return issuedDate; }
    public void setIssuedDate(String issuedDate) { this.issuedDate = issuedDate; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public List<Medication> getMedications() { return medications; }
    public void setMedications(List<Medication> medications) { this.medications = medications; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
