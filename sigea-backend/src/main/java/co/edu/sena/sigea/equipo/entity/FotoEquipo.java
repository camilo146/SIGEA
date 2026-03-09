package co.edu.sena.sigea.equipo.entity;

// =============================================================================
// ENTIDAD: FotoEquipo
// =============================================================================
// Almacena la referencia (ruta en el servidor) a una fotografía de un equipo.
// Las fotos NO se guardan en la BD (binary large objects en BD es mala práctica).
// Se guardan en el sistema de archivos del servidor y aquí solo guardamos la RUTA.
//
// TABLA EN BD: foto_equipo
//
// REQUERIMIENTO QUE CUBRE:
//   RF-INV-07: Registro fotográfico de cada equipo (máx. 3 fotos,
//              formatos JPG/PNG, tamaño máx. 5 MB por imagen)
//
// NORMALIZACIÓN:
//   ¿Por qué una tabla separada y no columnas foto1, foto2, foto3 en equipo?
//   Porque eso violaría la Primera Forma Normal (1FN):
//   "No puede haber grupos repetitivos de columnas en una tabla."
//
//   Si pusiéramos foto1, foto2, foto3:
//   - ¿Qué pasa si mañana quieren 5 fotos? Hay que agregar foto4, foto5.
//   - ¿Qué pasa si un equipo solo tiene 1 foto? foto2 y foto3 son NULL.
//   - ¿Cómo haces una consulta "todos los equipos que tienen al menos 1 foto"?
//     WHERE foto1 IS NOT NULL OR foto2 IS NOT NULL OR foto3 IS NOT NULL → feo.
//
//   Con tabla separada:
//   - Si quieren 5 fotos, solo cambias la validación de negocio.
//   - No hay NULLs innecesarios.
//   - Consulta simple: WHERE equipo_id = ? → todas las fotos de ese equipo.
// =============================================================================

import java.time.LocalDateTime;

import co.edu.sena.sigea.common.entity.EntidadBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "foto_equipo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FotoEquipo extends EntidadBase {

    // =========================================================================
    // CAMPO: equipo
    // =========================================================================
    // El equipo al que pertenece esta foto.
    //
    // @ManyToOne: "Muchas fotos pertenecen a un equipo"
    //   Relación: FotoEquipo N ←→ 1 Equipo
    //   (un equipo tiene como máximo 3 fotos, pero esa validación es de NEGOCIO,
    //    no de estructura de BD)
    //
    // nullable = false → Toda foto DEBE estar asociada a un equipo.
    //   No tiene sentido una foto "suelta" sin equipo.
    // =========================================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    // =========================================================================
    // CAMPO: nombreArchivo
    // =========================================================================
    // Nombre original del archivo subido por el usuario.
    // Ejemplo: "multimetro_fluke_frontal.jpg"
    //
    // Se conserva para mostrar al usuario un nombre reconocible.
    // En el servidor se guarda con un nombre único (UUID) para evitar colisiones.
    // =========================================================================
    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    // =========================================================================
    // CAMPO: rutaArchivo
    // =========================================================================
    // Ruta COMPLETA donde está almacenado el archivo en el sistema de archivos.
    // Ejemplo: "/uploads/equipos/e32f4a8b-foto1.jpg"
    //
    // Esta ruta es la que usa el backend para servir la imagen al frontend.
    // =========================================================================
    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    // =========================================================================
    // CAMPO: tamanoBytes
    // =========================================================================
    // Tamaño del archivo en bytes.
    //
    // RF-INV-07: "Tamaño máximo 5 MB por imagen" = 5,242,880 bytes.
    // Se valida ANTES de guardar (en el servicio), no en la BD.
    //
    // ¿Por qué guardarlo?
    //   Para reportes: "Espacio total usado por fotos de equipos."
    //   Para validaciones: verificar que nadie suba archivos muy grandes.
    // =========================================================================
    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    // =========================================================================
    // CAMPO: fechaSubida
    // =========================================================================
    // Cuándo se subió esta foto al sistema.
    //
    // ¿No es redundante con fechaCreacion de EntidadBase?
    //   Sí, técnicamente son iguales. Pero fechaSubida es un nombre más
    //   semánticamente correcto para este contexto. En la práctica podrías
    //   usar solo fechaCreacion, pero tener fechaSubida mejora la legibilidad
    //   al hacer reportes o consultas SQL.
    // =========================================================================
    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;
}
