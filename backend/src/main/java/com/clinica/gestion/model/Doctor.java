package com.clinica.gestion.model;

import java.util.List;

public class Doctor {
    private String id;
    private String fullName;
    private String specialty;
    private List<String> availableSlots;

    public Doctor() {}

    public Doctor(String id, String fullName, String specialty, List<String> availableSlots) {
        this.id = id;
        this.fullName = fullName;
        this.specialty = specialty;
        this.availableSlots = availableSlots;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    public List<String> getAvailableSlots() { return availableSlots; }
    public void setAvailableSlots(List<String> availableSlots) { this.availableSlots = availableSlots; }
}
