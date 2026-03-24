package co.edu.sena.sigea.reporte.service;

// =============================================================================
// SERVICIO: ReporteServicio
// =============================================================================
// Contiene la lógica de negocio para los reportes del sistema.
// RF-REP-01 a RF-REP-06: inventario, préstamos, usuarios en mora, ranking,
// exportación XLSX y PDF.
//
// Responsabilidad (SRP): orquestar la obtención de datos desde los repositorios
// y delegar la generación del archivo (Excel o PDF) a ReporteExcelServicio y
// ReportePdfServicio.
// =============================================================================

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.sena.sigea.common.enums.EstadoEquipo;
import co.edu.sena.sigea.common.enums.EstadoPrestamo;
import co.edu.sena.sigea.equipo.entity.Equipo;
import co.edu.sena.sigea.equipo.repository.EquipoRepository;
import co.edu.sena.sigea.prestamo.entity.Prestamo;
import co.edu.sena.sigea.prestamo.repository.DetallePrestamoRepository;
import co.edu.sena.sigea.prestamo.repository.PrestamoRepository;
import co.edu.sena.sigea.usuario.entity.Usuario;
import co.edu.sena.sigea.usuario.repository.UsuarioRepository;

@Service
public class ReporteServicio {

    private final EquipoRepository equipoRepository;
    private final PrestamoRepository prestamoRepository;
    private final DetallePrestamoRepository detallePrestamoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReporteExcelServicio reporteExcelServicio;
    private final ReportePdfServicio reportePdfServicio;

    public ReporteServicio(EquipoRepository equipoRepository,
            PrestamoRepository prestamoRepository,
            DetallePrestamoRepository detallePrestamoRepository,
            UsuarioRepository usuarioRepository,
            ReporteExcelServicio reporteExcelServicio,
            ReportePdfServicio reportePdfServicio) {
        this.equipoRepository = equipoRepository;
        this.prestamoRepository = prestamoRepository;
        this.detallePrestamoRepository = detallePrestamoRepository;
        this.usuarioRepository = usuarioRepository;
        this.reporteExcelServicio = reporteExcelServicio;
        this.reportePdfServicio = reportePdfServicio;
    }

    /**
     * RF-REP-01: Reporte de inventario general con filtros opcionales.
     * Filtros: ambienteId, categoriaId, estado (ACTIVO | EN_MANTENIMIENTO).
     */
    @Transactional(readOnly = true)
    public byte[] generarReporteInventario(String formato,
            Long inventarioInstructorId,
            Long categoriaId,
            EstadoEquipo estado) {
        List<Equipo> equipos = equipoRepository.findByActivoTrue();

        if (inventarioInstructorId != null) {
            equipos = equipos.stream()
                    .filter(e -> e.getInventarioActualInstructor() != null
                            && e.getInventarioActualInstructor().getId().equals(inventarioInstructorId))
                    .collect(Collectors.toList());
        }
        if (categoriaId != null) {
            equipos = equipos.stream()
                    .filter(e -> e.getCategoria() != null && e.getCategoria().getId().equals(categoriaId))
                    .collect(Collectors.toList());
        }
        if (estado != null) {
            equipos = equipos.stream()
                    .filter(e -> e.getEstado() == estado)
                    .collect(Collectors.toList());
        }

        if ("pdf".equalsIgnoreCase(formato)) {
            return reportePdfServicio.generarReporteInventario(equipos);
        }
        return reporteExcelServicio.generarReporteInventario(equipos);
    }

    /**
     * RF-REP-02: Historial de préstamos con filtros opcionales.
     * Filtros: usuarioId, equipoId, desde, hasta, estado.
     */
    @Transactional(readOnly = true)
    public byte[] generarReportePrestamos(String formato,
            Long usuarioId,
            Long equipoId,
            LocalDateTime desde,
            LocalDateTime hasta,
            EstadoPrestamo estado) {
        List<Prestamo> prestamos;

        if (equipoId != null) {
            prestamos = prestamoRepository.findByDetallesEquipoId(equipoId);
        } else {
            prestamos = prestamoRepository.findAll();
        }

        if (usuarioId != null) {
            prestamos = prestamos.stream()
                    .filter(p -> p.getUsuarioSolicitante() != null
                            && p.getUsuarioSolicitante().getId().equals(usuarioId))
                    .collect(Collectors.toList());
        }
        if (desde != null) {
            prestamos = prestamos.stream()
                    .filter(p -> p.getFechaHoraSolicitud() != null && !p.getFechaHoraSolicitud().isBefore(desde))
                    .collect(Collectors.toList());
        }
        if (hasta != null) {
            prestamos = prestamos.stream()
                    .filter(p -> p.getFechaHoraSolicitud() != null && !p.getFechaHoraSolicitud().isAfter(hasta))
                    .collect(Collectors.toList());
        }
        if (estado != null) {
            prestamos = prestamos.stream()
                    .filter(p -> p.getEstado() == estado)
                    .collect(Collectors.toList());
        }

        if ("pdf".equalsIgnoreCase(formato)) {
            return reportePdfServicio.generarReportePrestamos(prestamos);
        }
        return reporteExcelServicio.generarReportePrestamos(prestamos);
    }

    /**
     * RF-REP-03: Reporte de equipos más solicitados (ranking).
     */
    @Transactional(readOnly = true)
    public byte[] generarReporteEquiposMasSolicitados(String formato) {
        List<Object[]> ranking = detallePrestamoRepository.countPrestamosByEquipoId();
        List<Long> equipoIds = ranking.stream()
                .map(row -> (Long) row[0])
                .collect(Collectors.toList());

        List<Equipo> equipos = new ArrayList<>();
        for (Long id : equipoIds) {
            equipoRepository.findById(id).ifPresent(equipos::add);
        }

        List<Long> cantidades = ranking.stream()
                .map(row -> ((Number) row[1]).longValue())
                .collect(Collectors.toList());

        if ("pdf".equalsIgnoreCase(formato)) {
            return reportePdfServicio.generarReporteEquiposMasSolicitados(equipos, cantidades);
        }
        return reporteExcelServicio.generarReporteEquiposMasSolicitados(equipos, cantidades);
    }

    /**
     * RF-REP-04: Reporte de usuarios con préstamos pendientes o vencidos (ACTIVO o
     * EN_MORA).
     */
    @Transactional(readOnly = true)
    public byte[] generarReporteUsuariosEnMora(String formato) {
        List<Prestamo> activos = prestamoRepository.findByEstado(EstadoPrestamo.ACTIVO);
        List<Prestamo> enMora = prestamoRepository.findByEstado(EstadoPrestamo.EN_MORA);

        List<Long> usuarioIds = new ArrayList<>();
        activos.stream()
                .map(p -> p.getUsuarioSolicitante().getId())
                .distinct()
                .forEach(usuarioIds::add);
        enMora.stream()
                .map(p -> p.getUsuarioSolicitante().getId())
                .distinct()
                .filter(id -> !usuarioIds.contains(id))
                .forEach(usuarioIds::add);

        List<Usuario> usuarios = usuarioIds.stream()
                .flatMap(id -> usuarioRepository.findById(id).stream())
                .collect(Collectors.toList());

        if ("pdf".equalsIgnoreCase(formato)) {
            return reportePdfServicio.generarReporteUsuariosEnMora(usuarios);
        }
        return reporteExcelServicio.generarReporteUsuariosEnMora(usuarios);
    }
}
