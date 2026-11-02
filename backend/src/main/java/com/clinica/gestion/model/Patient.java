package com.clinica.gestion.model;

import java.util.ArrayList;
import java.util.List;

public class Patient {
    private String id;
    private String documentNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String birthDate;
    private List<String> allergies;

    public Patient() {
        this.allergies = new ArrayList<>();
    }

    public Patient(String id, String documentNumber, String firstName, String lastName,
                   String email, String phone, String birthDate, List<String> allergies) {
        this.id = id;
        this.documentNumber = documentNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.birthDate = birthDate;
        this.allergies = allergies != null ? allergies : new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
    public List<String> getAllergies() { return allergies; }
    public void setAllergies(List<String> allergies) { this.allergies = allergies; }
    public String getFullName() { return firstName + " " + lastName; }
}
