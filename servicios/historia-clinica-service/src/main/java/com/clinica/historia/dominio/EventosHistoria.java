package com.clinica.historia.dominio;

/** Puerto de salida: hechos que Historia Clínica anuncia. La implementación vive en el paquete eventos. */
public interface EventosHistoria {
    void pacienteRegistrado(Patient paciente);
}
