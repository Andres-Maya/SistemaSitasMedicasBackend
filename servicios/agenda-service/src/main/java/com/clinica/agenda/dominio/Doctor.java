package com.clinica.agenda.dominio;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Doctor {
    private final String id;
    private final String fullName;
    private final String specialty;
    // Lecturas concurrentes (GET /medicos) mientras se reservan cupos
    private final List<String> availableSlots;

    public Doctor(String id, String fullName, String specialty, List<String> availableSlots) {
        this.id = id;
        this.fullName = fullName;
        this.specialty = specialty;
        this.availableSlots = new CopyOnWriteArrayList<>(availableSlots);
    }

    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getSpecialty() { return specialty; }
    public List<String> getAvailableSlots() { return availableSlots; }

    // Un cupo liberado vuelve a su posición cronológica ("yyyy-MM-dd HH:mm" ordena como texto)
    public void devolverCupo(String cupo) {
        availableSlots.add(cupo);
        availableSlots.sort(null);
    }
}
