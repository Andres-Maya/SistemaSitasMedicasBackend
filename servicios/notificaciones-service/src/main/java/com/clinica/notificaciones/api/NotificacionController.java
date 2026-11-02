package com.clinica.notificaciones.api;

import com.clinica.notificaciones.dominio.Notificacion;
import com.clinica.notificaciones.dominio.NotificacionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    public record ApiResponse<T>(boolean success, String message, T data) {}

    // ── GET /api/notificaciones?patientId= ────────────────────────────────────
    @GetMapping
    public ApiResponse<List<Notificacion>> list(@RequestParam(required = false) String patientId) {
        return new ApiResponse<>(true, "Notifications retrieved", notificacionService.listar(patientId));
    }

    // ── GET /api/notificaciones/health ────────────────────────────────────────
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return new ApiResponse<>(true, "notificaciones-service running", "OK");
    }
}
