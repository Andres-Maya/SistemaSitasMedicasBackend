package com.clinica.notificaciones.dominio;

public record Notificacion(String id, String patientId, String channel, String destination,
                           String message, String sentAt) {}
