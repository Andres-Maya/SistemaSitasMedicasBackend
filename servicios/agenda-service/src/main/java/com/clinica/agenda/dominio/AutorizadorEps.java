package com.clinica.agenda.dominio;

/**
 * Puerto hacia el sistema externo de autorizaciones de la EPS.
 * El dominio depende de esta interfaz, no del adaptador HTTP: cambiar de EPS o de
 * protocolo solo toca el paquete eps (escenario de modificabilidad M1).
 */
public interface AutorizadorEps {

    ResultadoAutorizacion autorizar(PacienteConocido paciente, String especialidad);

    record ResultadoAutorizacion(Decision decision, String numeroAutorizacion) {
        public enum Decision { APROBADA, NEGADA, NO_DISPONIBLE }

        public static ResultadoAutorizacion aprobada(String numero) { return new ResultadoAutorizacion(Decision.APROBADA, numero); }
        public static ResultadoAutorizacion negada() { return new ResultadoAutorizacion(Decision.NEGADA, null); }
        public static ResultadoAutorizacion noDisponible() { return new ResultadoAutorizacion(Decision.NO_DISPONIBLE, null); }
    }
}
