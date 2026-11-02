package com.clinica.agenda.eps;

import com.clinica.agenda.dominio.AutorizadorEps;
import com.clinica.agenda.dominio.PacienteConocido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptador HTTP hacia el sistema de autorizaciones de la EPS.
 * Táctica de disponibilidad: timeout corto + degradación. Cualquier fallo de la EPS
 * (lentitud, caída, error 5xx) se traduce en NO_DISPONIBLE, nunca en un error al paciente.
 */
@Component
public class EpsHttpAdapter implements AutorizadorEps {

    private static final Logger log = LoggerFactory.getLogger(EpsHttpAdapter.class);

    private final RestClient restClient;

    public EpsHttpAdapter(@Value("${eps.url}") String epsUrl,
                          @Value("${eps.timeout-ms}") int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restClient = RestClient.builder().baseUrl(epsUrl).requestFactory(factory).build();
    }

    @Override
    public ResultadoAutorizacion autorizar(PacienteConocido paciente, String especialidad) {
        try {
            RespuestaEps respuesta = restClient.post()
                    .uri("/autorizaciones")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SolicitudEps(paciente.documentNumber(), paciente.eps(), especialidad))
                    .retrieve()
                    .body(RespuestaEps.class);
            if (respuesta == null) {
                return ResultadoAutorizacion.noDisponible();
            }
            log.info("EPS {} respondió {} para {} ({})", paciente.eps(), respuesta.decision(),
                    paciente.documentNumber(), especialidad);
            return "APROBADA".equalsIgnoreCase(respuesta.decision())
                    ? ResultadoAutorizacion.aprobada(respuesta.numeroAutorizacion())
                    : ResultadoAutorizacion.negada();
        } catch (RestClientException e) {
            log.warn("EPS no disponible ({}): la cita se degrada a PENDING_AUTHORIZATION", e.getMessage());
            return ResultadoAutorizacion.noDisponible();
        }
    }

    record SolicitudEps(String documentNumber, String eps, String specialty) {}

    record RespuestaEps(String decision, String numeroAutorizacion) {}
}
