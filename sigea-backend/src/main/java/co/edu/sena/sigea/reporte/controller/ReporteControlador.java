package co.edu.sena.sigea.reporte.controller;

// =============================================================================
// CONTROLADOR: ReporteControlador
// =============================================================================
// Endpoints REST para generar y descargar reportes en XLSX y PDF.
// RF-REP-01 a RF-REP-06.
//
// BASE URL: /api/v1/reportes
//
// Todos los endpoints requieren rol ADMINISTRADOR (@PreAuthorize).
// El parámetro "formato" acepta: xlsx | pdf (default: xlsx).
// =============================================================================

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.sena.sigea.common.enums.EstadoEquipo;
import co.edu.sena.sigea.common.enums.EstadoPrestamo;
import co.edu.sena.sigea.reporte.service.ReporteServicio;

@RestController
@RequestMapping("/reportes")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ReporteControlador {

    private final ReporteServicio reporteServicio;

    public ReporteControlador(ReporteServicio reporteServicio) {
        this.reporteServicio = reporteServicio;
    }

    /**
     * RF-REP-01 + RF-REP-05/06: Reporte de inventario con filtros opcionales.
     * GET /reportes/inventario?formato=xlsx|pdf&ambienteId=&categoriaId=&estado=
     */
    @GetMapping("/inventario")
    public ResponseEntity<ByteArrayResource> reporteInventario(
            @RequestParam(defaultValue = "xlsx") String formato,
            @RequestParam(required = false) Long ambienteId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) EstadoEquipo estado) {

        byte[] contenido = reporteServicio.generarReporteInventario(formato, ambienteId, categoriaId, estado);
        String extension = "pdf".equalsIgnoreCase(formato) ? "pdf" : "xlsx";
        String nombreArchivo = "reporte-inventario." + extension;

        return responderConArchivo(contenido, nombreArchivo, extension);
    }

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * RF-REP-02 + RF-REP-05/06: Historial de préstamos con filtros opcionales.
     * GET /reportes/prestamos?formato=xlsx|pdf&usuarioId=&equipoId=&desde=&hasta=&estado=
     * desde/hasta: ISO (2026-01-01T00:00:00) o solo fecha (2026-01-01).
     */
    @GetMapping("/prestamos")
    public ResponseEntity<ByteArrayResource> reportePrestamos(
            @RequestParam(defaultValue = "xlsx") String formato,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Long equipoId,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) EstadoPrestamo estado) {

        LocalDateTime desdeDt = parseFechaHora(desde);
        LocalDateTime hastaDt = parseFechaHora(hasta);
        byte[] contenido = reporteServicio.generarReportePrestamos(formato, usuarioId, equipoId, desdeDt, hastaDt, estado);
        String extension = "pdf".equalsIgnoreCase(formato) ? "pdf" : "xlsx";
        String nombreArchivo = "reporte-prestamos." + extension;

        return responderConArchivo(contenido, nombreArchivo, extension);
    }

    /**
     * RF-REP-03 + RF-REP-05/06: Equipos más solicitados (ranking).
     * GET /reportes/equipos-mas-solicitados?formato=xlsx|pdf
     */
    @GetMapping("/equipos-mas-solicitados")
    public ResponseEntity<ByteArrayResource> reporteEquiposMasSolicitados(
            @RequestParam(defaultValue = "xlsx") String formato) {

        byte[] contenido = reporteServicio.generarReporteEquiposMasSolicitados(formato);
        String extension = "pdf".equalsIgnoreCase(formato) ? "pdf" : "xlsx";
        String nombreArchivo = "reporte-equipos-mas-solicitados." + extension;

        return responderConArchivo(contenido, nombreArchivo, extension);
    }

    /**
     * RF-REP-04 + RF-REP-05/06: Usuarios con préstamos pendientes o vencidos.
     * GET /reportes/usuarios-en-mora?formato=xlsx|pdf
     */
    @GetMapping("/usuarios-en-mora")
    public ResponseEntity<ByteArrayResource> reporteUsuariosEnMora(
            @RequestParam(defaultValue = "xlsx") String formato) {

        byte[] contenido = reporteServicio.generarReporteUsuariosEnMora(formato);
        String extension = "pdf".equalsIgnoreCase(formato) ? "pdf" : "xlsx";
        String nombreArchivo = "reporte-usuarios-en-mora." + extension;

        return responderConArchivo(contenido, nombreArchivo, extension);
    }

    /** Parsea parámetro de fecha/hora desde la URL (ISO o solo fecha). */
    private LocalDateTime parseFechaHora(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return LocalDateTime.parse(valor, FECHA_HORA);
        } catch (DateTimeParseException e1) {
            try {
                return LocalDateTime.parse(valor + "T00:00:00", FECHA_HORA);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    /**
     * Construye la respuesta HTTP con el archivo para descarga.
     * Content-Disposition: attachment hace que el navegador ofrezca "Guardar como".
     */
    private ResponseEntity<ByteArrayResource> responderConArchivo(byte[] contenido,
                                                                  String nombreArchivo,
                                                                  String extension) {
        String contentType = "pdf".equals(extension)
                ? MediaType.APPLICATION_PDF_VALUE
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", nombreArchivo);
        headers.setContentLength(contenido.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(new ByteArrayResource(contenido));
    }
}
