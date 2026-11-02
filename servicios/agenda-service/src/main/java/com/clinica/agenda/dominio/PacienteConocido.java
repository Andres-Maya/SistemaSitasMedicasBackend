package com.clinica.agenda.dominio;

/**
 * Proyección local del paciente dentro del contexto Agenda.
 * Se alimenta del evento paciente.registrado: Agenda nunca consulta la base de
 * Historia Clínica y solo guarda lo que necesita para pedir la autorización a la EPS.
 */
public record PacienteConocido(String patientId, String documentNumber, String fullName, String eps) {}
