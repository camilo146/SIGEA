package co.edu.sena.sigea.common.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

// =============================================================================
// UTILIDAD: FechasUtil
// =============================================================================
// Métodos estáticos para cálculos con fechas (días hábiles).
// RF-RES-01: "Máximo 5 días hábiles de anticipación" para reservas.
//
// PRINCIPIO: Single Responsibility — solo lógica de fechas.
// =============================================================================

public final class FechasUtil {

    private FechasUtil() {
        // Clase de utilidad: no instanciable.
    }

    /**
     * Suma n días hábiles a una fecha (excluye sábado y domingo).
     *
     * @param fechaInicio fecha desde la cual contar
     * @param diasHabiles número de días hábiles a sumar
     * @return fecha resultante después de sumar los días hábiles
     */
    public static LocalDate sumarDiasHabiles(LocalDate fechaInicio, int diasHabiles) {
        if (diasHabiles <= 0) {
            return fechaInicio;
        }
        LocalDate resultado = fechaInicio;
        int contador = 0;
        while (contador < diasHabiles) {
            resultado = resultado.plusDays(1);
            if (resultado.getDayOfWeek() != DayOfWeek.SATURDAY
                    && resultado.getDayOfWeek() != DayOfWeek.SUNDAY) {
                contador++;
            }
        }
        return resultado;
    }
}
