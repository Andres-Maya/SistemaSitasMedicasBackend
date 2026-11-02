package com.clinica.historia.dominio;

/** Registro de quién consultó la historia clínica de un paciente (Res. 1995 de 1999: confidencialidad). */
public record AuditEntry(String timestamp, String user, String patientId, String action) {}
