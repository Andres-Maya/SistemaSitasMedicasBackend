package com.clinica.gestion.dto;

import java.util.List;

public class Dtos {

    // ── Request DTOs ──────────────────────────────────────────────────────────

    public static class RegisterPatientRequest {
        private String documentNumber;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String birthDate;
        private String password;
        private List<String> allergies;

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
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public List<String> getAllergies() { return allergies; }
        public void setAllergies(List<String> allergies) { this.allergies = allergies; }
    }

    public static class LoginRequest {
        private String documentNumber;
        private String password;

        public String getDocumentNumber() { return documentNumber; }
        public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class AppointmentRequest {
        private String patientId;
        private String specialty;
        private String doctorId;
        private String date;

        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        public String getSpecialty() { return specialty; }
        public void setSpecialty(String specialty) { this.specialty = specialty; }
        public String getDoctorId() { return doctorId; }
        public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
    }

    public static class PrescriptionRequest {
        private String patientId;
        private List<MedicationDto> medications;
        private String notes;

        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        public List<MedicationDto> getMedications() { return medications; }
        public void setMedications(List<MedicationDto> medications) { this.medications = medications; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class MedicationDto {
        private String name;
        private String dosage;
        private String duration;
        private String frequency;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDosage() { return dosage; }
        public void setDosage(String dosage) { this.dosage = dosage; }
        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }
        public String getFrequency() { return frequency; }
        public void setFrequency(String frequency) { this.frequency = frequency; }
    }

    public static class LaboratoryRequest {
        private String patientId;
        private List<String> exams;

        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        public List<String> getExams() { return exams; }
        public void setExams(List<String> exams) { this.exams = exams; }
    }

    // ── Response DTOs ─────────────────────────────────────────────────────────

    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public ApiResponse(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public T getData() { return data; }
    }
}
