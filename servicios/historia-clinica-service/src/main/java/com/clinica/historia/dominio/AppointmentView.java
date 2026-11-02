package com.clinica.historia.dominio;

/**
 * Copia de lectura de una cita, construida a partir de los eventos cita.*.
 * Historia Clínica no consulta a Agenda en línea: si Agenda está caída, la historia
 * completa sigue respondiendo con la última información recibida (consistencia eventual).
 */
public record AppointmentView(String id, String patientId, String doctorName, String specialty,
                              String date, String status, String notes) {}
