package co.edu.sena.sigea.reporte.service;

// =============================================================================
// SERVICIO: ReporteExcelServicio
// =============================================================================
// Genera archivos Excel (XLSX) usando Apache POI.
// RF-REP-05: "El sistema debe exportar todos los reportes en formato XLSX."
//
// Cada método recibe los datos ya cargados y devuelve el contenido del archivo
// como byte[] para que el controlador lo envíe en la respuesta HTTP.
// =============================================================================

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import co.edu.sena.sigea.equipo.entity.Equipo;
import co.edu.sena.sigea.prestamo.entity.Prestamo;
import co.edu.sena.sigea.usuario.entity.Usuario;

@Service
public class ReporteExcelServicio {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * RF-REP-01: Reporte de inventario en XLSX.
     */
    public byte[] generarReporteInventario(List<Equipo> equipos) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Inventario");
            CellStyle headerStyle = crearEstiloEncabezado(wb);
            CellStyle cellStyle = crearEstiloCelda(wb);

            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = { "Código", "Nombre", "Categoría", "Ubicación", "Dueño", "Inventario actual", "Estado",
                    "Cant. total", "Cant. disponible", "Umbral mín." };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (Equipo e : equipos) {
                Row row = sheet.createRow(rowNum++);
                crearCelda(row, 0, e.getCodigoUnico(), cellStyle);
                crearCelda(row, 1, e.getNombre(), cellStyle);
                crearCelda(row, 2, e.getCategoria() != null ? e.getCategoria().getNombre() : "", cellStyle);
                crearCelda(row, 3, e.getAmbiente() != null ? e.getAmbiente().getNombre() : "", cellStyle);
                crearCelda(row, 4, e.getPropietario() != null ? e.getPropietario().getNombreCompleto() : "", cellStyle);
                crearCelda(row, 5,
                        e.getInventarioActualInstructor() != null
                                ? e.getInventarioActualInstructor().getNombreCompleto()
                                : "",
                        cellStyle);
                crearCelda(row, 6, e.getEstado() != null ? e.getEstado().name() : "", cellStyle);
                crearCelda(row, 7, e.getCantidadTotal() != null ? e.getCantidadTotal().doubleValue() : 0, cellStyle);
                crearCelda(row, 8, e.getCantidadDisponible() != null ? e.getCantidadDisponible().doubleValue() : 0,
                        cellStyle);
                crearCelda(row, 9, e.getUmbralMinimo() != null ? e.getUmbralMinimo().doubleValue() : 0, cellStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            return workbookToBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar reporte Excel de inventario", e);
        }
    }

    /**
     * RF-REP-02: Historial de préstamos en XLSX.
     */
    public byte[] generarReportePrestamos(List<Prestamo> prestamos) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Préstamos");
            CellStyle headerStyle = crearEstiloEncabezado(wb);
            CellStyle cellStyle = crearEstiloCelda(wb);

            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = { "ID", "Solicitante", "Correo", "Estado", "Fecha solicitud", "Fecha dev. estimada",
                    "Fecha dev. real" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (Prestamo p : prestamos) {
                Row row = sheet.createRow(rowNum++);
                crearCelda(row, 0, p.getId() != null ? p.getId().doubleValue() : 0, cellStyle);
                crearCelda(row, 1,
                        p.getUsuarioSolicitante() != null ? p.getUsuarioSolicitante().getNombreCompleto() : "",
                        cellStyle);
                crearCelda(row, 2,
                        p.getUsuarioSolicitante() != null ? p.getUsuarioSolicitante().getCorreoElectronico() : "",
                        cellStyle);
                crearCelda(row, 3, p.getEstado() != null ? p.getEstado().name() : "", cellStyle);
                crearCelda(row, 4,
                        p.getFechaHoraSolicitud() != null ? p.getFechaHoraSolicitud().format(FECHA_HORA) : "",
                        cellStyle);
                crearCelda(row, 5,
                        p.getFechaHoraDevolucionEstimada() != null
                                ? p.getFechaHoraDevolucionEstimada().format(FECHA_HORA)
                                : "",
                        cellStyle);
                crearCelda(row, 6,
                        p.getFechaHoraDevolucionReal() != null ? p.getFechaHoraDevolucionReal().format(FECHA_HORA) : "",
                        cellStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            return workbookToBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar reporte Excel de préstamos", e);
        }
    }

    /**
     * RF-REP-03: Equipos más solicitados en XLSX.
     */
    public byte[] generarReporteEquiposMasSolicitados(List<Equipo> equipos, List<Long> cantidades) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Equipos más solicitados");
            CellStyle headerStyle = crearEstiloEncabezado(wb);
            CellStyle cellStyle = crearEstiloCelda(wb);

            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = { "Código", "Nombre", "Categoría", "Veces solicitado" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < equipos.size(); i++) {
                Equipo e = equipos.get(i);
                Long countValue = i < cantidades.size() ? cantidades.get(i) : null;
                long count = countValue != null ? countValue : 0L;
                Row row = sheet.createRow(rowNum++);
                crearCelda(row, 0, e.getCodigoUnico(), cellStyle);
                crearCelda(row, 1, e.getNombre(), cellStyle);
                crearCelda(row, 2, e.getCategoria() != null ? e.getCategoria().getNombre() : "", cellStyle);
                crearCelda(row, 3, count, cellStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            return workbookToBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar reporte Excel de equipos más solicitados", e);
        }
    }

    /**
     * RF-REP-04: Usuarios con préstamos pendientes o vencidos en XLSX.
     */
    public byte[] generarReporteUsuariosEnMora(List<Usuario> usuarios) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Usuarios con préstamos activos o en mora");
            CellStyle headerStyle = crearEstiloEncabezado(wb);
            CellStyle cellStyle = crearEstiloCelda(wb);

            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = { "Documento", "Nombre completo", "Correo", "Teléfono", "Rol" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (Usuario u : usuarios) {
                Row row = sheet.createRow(rowNum++);
                crearCelda(row, 0, u.getNumeroDocumento() != null ? u.getNumeroDocumento() : "", cellStyle);
                crearCelda(row, 1, u.getNombreCompleto() != null ? u.getNombreCompleto() : "", cellStyle);
                crearCelda(row, 2, u.getCorreoElectronico() != null ? u.getCorreoElectronico() : "", cellStyle);
                crearCelda(row, 3, u.getTelefono() != null ? u.getTelefono() : "", cellStyle);
                crearCelda(row, 4, u.getRol() != null ? u.getRol().name() : "", cellStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            return workbookToBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar reporte Excel de usuarios en mora", e);
        }
    }

    private CellStyle crearEstiloEncabezado(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle crearEstiloCelda(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void crearCelda(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void crearCelda(Row row, int column, double value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void crearCelda(Row row, int column, long value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private byte[] workbookToBytes(Workbook wb) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }
}
