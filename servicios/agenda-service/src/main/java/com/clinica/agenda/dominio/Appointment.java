package com.clinica.agenda.dominio;

public class Appointment {
    public static final String SCHEDULED = "SCHEDULED";
    public static final String PENDING_AUTHORIZATION = "PENDING_AUTHORIZATION";
    public static final String CANCELLED = "CANCELLED";

    private final String id;
    private final String patientId;
    private final String doctorId;
    private final String doctorName;
    private final String specialty;
    private final String date;
    private String status;
    private String notes;
    private String authorizationNumber;

    public Appointment(String id, String patientId, String doctorId, String doctorName,
                       String specialty, String date, String status, String notes) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.specialty = specialty;
        this.date = date;
        this.status = status;
        this.notes = notes;
    }

    public boolean isActive() { return !CANCELLED.equals(status); }

    public String getId() { return id; }
    public String getPatientId() { return patientId; }
    public String getDoctorId() { return doctorId; }
    public String getDoctorName() { return doctorName; }
    public String getSpecialty() { return specialty; }
    public String getDate() { return date; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getAuthorizationNumber() { return authorizationNumber; }
    public void setAuthorizationNumber(String authorizationNumber) { this.authorizationNumber = authorizationNumber; }
}
