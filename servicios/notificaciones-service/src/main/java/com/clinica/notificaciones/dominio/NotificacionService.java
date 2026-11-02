package com.clinica.notificaciones.dominio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Envía avisos al paciente. El proveedor de correo/SMS se simula con el log:
 * lo que importa en el esqueleto es que el envío ocurre por reacción a un evento,
 * sin que Agenda ni Historia Clínica sepan que este servicio existe.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    public record Contacto(String fullName, String email, String phone) {}

    private final Map<String, Contacto> contactos = new ConcurrentHashMap<>();
    private final List<Notificacion> enviadas = new CopyOnWriteArrayList<>();
    // Claves ya procesadas: la entrega de RabbitMQ es "al menos una vez"
    private final Set<String> procesados = ConcurrentHashMap.newKeySet();

    public void registrarContacto(String patientId, Contacto contacto) {
        contactos.put(patientId, contacto);
        enviar("bienvenida:" + patientId, patientId,
                "Bienvenido(a) " + contacto.fullName() + ". Ya puede agendar sus citas.");
    }

    public void avisarCambioCita(String appointmentId, String patientId, String status,
                                 String specialty, String doctorName, String date) {
        String mensaje = switch (status) {
            case "SCHEDULED" -> "Su cita de " + specialty + " con " + doctorName + " quedó confirmada para el " + date + ".";
            case "PENDING_AUTHORIZATION" -> "Reservamos su cita de " + specialty + " del " + date
                    + "; está pendiente de autorización de su EPS.";
            case "CANCELLED" -> "Su cita de " + specialty + " del " + date + " fue cancelada.";
            default -> "Su cita " + appointmentId + " cambió a " + status + ".";
        };
        enviar(appointmentId + ":" + status, patientId, mensaje);
    }

    private void enviar(String claveIdempotencia, String patientId, String mensaje) {
        if (!procesados.add(claveIdempotencia)) {
            log.info("Evento duplicado ignorado: {}", claveIdempotencia);
            return;
        }
        Contacto contacto = contactos.get(patientId);
        String destino = contacto != null ? contacto.email() : "(contacto aún desconocido)";
        Notificacion n = new Notificacion("NOT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                patientId, "EMAIL", destino, mensaje, Instant.now().toString());
        enviadas.add(n);
        log.info("[EMAIL simulado] para {} <{}>: {}", patientId, destino, mensaje);
    }

    public List<Notificacion> listar(String patientId) {
        return enviadas.stream()
                .filter(n -> patientId == null || n.patientId().equals(patientId))
                .collect(Collectors.toList());
    }
}
