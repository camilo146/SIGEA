package co.edu.sena.sigea.mantenimiento.service;

// =============================================================================
// SERVICIO: MantenimientoServicio
// =============================================================================
// Lógica de negocio para mantenimientos de equipos (preventivo/correctivo).
// RF-MAN-01, RN-11: registrar con tipo, descripción, fecha, responsable.
// =============================================================================

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.sena.sigea.common.enums.EstadoEquipo;
import co.edu.sena.sigea.common.enums.TipoMantenimiento;
import co.edu.sena.sigea.common.exception.OperacionNoPermitidaException;
import co.edu.sena.sigea.common.exception.RecursoNoEncontradoException;
import co.edu.sena.sigea.equipo.repository.EquipoRepository;
import co.edu.sena.sigea.mantenimiento.dto.MantenimientoCerrarDTO;
import co.edu.sena.sigea.mantenimiento.dto.MantenimientoCrearDTO;
import co.edu.sena.sigea.mantenimiento.dto.MantenimientoRespuestaDTO;
import co.edu.sena.sigea.mantenimiento.entity.Mantenimiento;
import co.edu.sena.sigea.mantenimiento.repository.MantenimientoRepository;

@Service
@Transactional
public class MantenimientoServicio {

    private final MantenimientoRepository mantenimientoRepository;
    private final EquipoRepository equipoRepository;

    public MantenimientoServicio(MantenimientoRepository mantenimientoRepository,
                                EquipoRepository equipoRepository) {
        this.mantenimientoRepository = mantenimientoRepository;
        this.equipoRepository = equipoRepository;
    }

    public MantenimientoRespuestaDTO crear(MantenimientoCrearDTO dto) {

        var equipo = equipoRepository.findById(dto.getEquipoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Equipo no encontrado con ID: " + dto.getEquipoId()));

        if (!Boolean.TRUE.equals(equipo.getActivo())) {
            throw new OperacionNoPermitidaException(
                    "No se puede registrar mantenimiento para el equipo '" + equipo.getNombre() + "' porque no está activo.");
        }

        if (dto.getFechaFin() != null && dto.getFechaFin().isBefore(dto.getFechaInicio())) {
            throw new OperacionNoPermitidaException(
                    "La fecha de fin no puede ser anterior a la fecha de inicio.");
        }

        Mantenimiento m = Mantenimiento.builder()
                .equipo(equipo)
                .tipo(dto.getTipo())
                .descripcion(dto.getDescripcion())
                .fechaInicio(dto.getFechaInicio())
                .fechaFin(dto.getFechaFin())
                .responsable(dto.getResponsable())
                .observaciones(dto.getObservaciones())
                .build();

        Mantenimiento guardado = mantenimientoRepository.save(m);

        // Al registrar mantenimiento, el equipo pasa a estado EN_MANTENIMIENTO en inventario
        equipo.setEstado(EstadoEquipo.EN_MANTENIMIENTO);
        equipoRepository.save(equipo);

        return mapear(guardado);
    }

    public MantenimientoRespuestaDTO cerrar(Long id, MantenimientoCerrarDTO dto) {

        Mantenimiento m = mantenimientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Mantenimiento no encontrado con ID: " + id));

        if (m.getFechaFin() != null) {
            throw new OperacionNoPermitidaException(
                    "El mantenimiento ya está cerrado (tiene fecha de fin).");
        }

        if (dto.getFechaFin().isBefore(m.getFechaInicio())) {
            throw new OperacionNoPermitidaException(
                    "La fecha de fin no puede ser anterior a la fecha de inicio del mantenimiento.");
        }

        m.setFechaFin(dto.getFechaFin());
        if (dto.getObservaciones() != null && !dto.getObservaciones().isBlank()) {
            String obs = m.getObservaciones() != null
                    ? m.getObservaciones() + "\n" + dto.getObservaciones()
                    : dto.getObservaciones();
            m.setObservaciones(obs);
        }
        mantenimientoRepository.save(m);

        // Al cerrar el mantenimiento, el equipo vuelve a ACTIVO si no tiene otros en curso
        long enCurso = mantenimientoRepository.countByEquipoIdAndFechaFinIsNull(m.getEquipo().getId());
        if (enCurso == 0) {
            m.getEquipo().setEstado(EstadoEquipo.ACTIVO);
            equipoRepository.save(m.getEquipo());
        }

        return mapear(m);
    }

    @Transactional(readOnly = true)
    public List<MantenimientoRespuestaDTO> listarTodos() {
        return mantenimientoRepository.findAll().stream()
                .map(this::mapear)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MantenimientoRespuestaDTO> listarPorEquipo(Long equipoId) {
        return mantenimientoRepository.findByEquipoId(equipoId).stream()
                .map(this::mapear)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MantenimientoRespuestaDTO> listarPorTipo(TipoMantenimiento tipo) {
        return mantenimientoRepository.findByTipo(tipo).stream()
                .map(this::mapear)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MantenimientoRespuestaDTO> listarEnCurso() {
        return mantenimientoRepository.findByFechaFinIsNull().stream()
                .map(this::mapear)
                .toList();
    }

    @Transactional(readOnly = true)
    public MantenimientoRespuestaDTO buscarPorId(Long id) {
        Mantenimiento m = mantenimientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Mantenimiento no encontrado con ID: " + id));
        return mapear(m);
    }

    /** Actualiza un mantenimiento solo si aún no está cerrado (fechaFin null). */
    public MantenimientoRespuestaDTO actualizar(Long id, MantenimientoCrearDTO dto) {
        Mantenimiento m = mantenimientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Mantenimiento no encontrado con ID: " + id));
        if (m.getFechaFin() != null) {
            throw new OperacionNoPermitidaException(
                    "No se puede editar un mantenimiento ya cerrado.");
        }
        var equipo = equipoRepository.findById(dto.getEquipoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Equipo no encontrado con ID: " + dto.getEquipoId()));
        m.setEquipo(equipo);
        m.setTipo(dto.getTipo());
        m.setDescripcion(dto.getDescripcion());
        m.setFechaInicio(dto.getFechaInicio());
        m.setFechaFin(dto.getFechaFin());
        m.setResponsable(dto.getResponsable());
        m.setObservaciones(dto.getObservaciones());
        return mapear(mantenimientoRepository.save(m));
    }

    /** Elimina un mantenimiento solo si aún no está cerrado. Ajusta estado del equipo si aplica. */
    public void eliminar(Long id) {
        Mantenimiento m = mantenimientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Mantenimiento no encontrado con ID: " + id));
        if (m.getFechaFin() != null) {
            throw new OperacionNoPermitidaException(
                    "No se puede eliminar un mantenimiento ya cerrado.");
        }
        Long equipoId = m.getEquipo().getId();
        mantenimientoRepository.delete(m);
        long enCurso = mantenimientoRepository.countByEquipoIdAndFechaFinIsNull(equipoId);
        if (enCurso == 0) {
            var equipo = equipoRepository.findById(equipoId).orElse(null);
            if (equipo != null && equipo.getEstado() == EstadoEquipo.EN_MANTENIMIENTO) {
                equipo.setEstado(EstadoEquipo.ACTIVO);
                equipoRepository.save(equipo);
            }
        }
    }

    private MantenimientoRespuestaDTO mapear(Mantenimiento m) {
        return MantenimientoRespuestaDTO.builder()
                .id(m.getId())
                .equipoId(m.getEquipo().getId())
                .nombreEquipo(m.getEquipo().getNombre())
                .codigoEquipo(m.getEquipo().getCodigoUnico())
                .tipo(m.getTipo())
                .descripcion(m.getDescripcion())
                .fechaInicio(m.getFechaInicio())
                .fechaFin(m.getFechaFin())
                .responsable(m.getResponsable())
                .observaciones(m.getObservaciones())
                .fechaCreacion(m.getFechaCreacion())
                .build();
    }
}
