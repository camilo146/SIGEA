package co.edu.sena.sigea.reporte.service;

// =============================================================================
// SERVICIO: ReportePdfServicio
// =============================================================================
// Genera archivos PDF usando OpenPDF (LibrePDF).
// RF-REP-06: "El sistema debe exportar todos los reportes en formato PDF."
//
// OpenPDF usa el paquete com.lowagie.text (herencia de iText).
// Cada método construye un Document, añade tablas y párrafos, y devuelve byte[].
// =============================================================================

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.springframework.stereotype.Service;

import co.edu.sena.sigea.equipo.entity.Equipo;
import co.edu.sena.sigea.prestamo.entity.Prestamo;
import co.edu.sena.sigea.usuario.entity.Usuario;

@Service
public class ReportePdfServicio {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final Font FONT_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9);

    /**
     * RF-REP-01: Reporte de inventario en PDF.
     */
    public byte[] generarReporteInventario(List<Equipo> equipos) {
        Document document = new Document();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("Reporte de Inventario - SIGEA", FONT_TITULO));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(10);
            table.setWidthPercentage(100f);
            table.setWidths(new float[] { 1.4f, 2.2f, 1.2f, 1.4f, 1.8f, 1.8f, 1.1f, 0.8f, 0.8f, 0.8f });
            agregarCeldaHeader(table, "Código");
            agregarCeldaHeader(table, "Nombre");
            agregarCeldaHeader(table, "Categoría");
            agregarCeldaHeader(table, "Ubicación");
            agregarCeldaHeader(table, "Dueño");
            agregarCeldaHeader(table, "Inventario actual");
            agregarCeldaHeader(table, "Estado");
            agregarCeldaHeader(table, "Total");
            agregarCeldaHeader(table, "Disp.");
            agregarCeldaHeader(table, "Umbral");

            for (Equipo e : equipos) {
                agregarCelda(table, e.getCodigoUnico());
                agregarCelda(table, e.getNombre());
                agregarCelda(table, e.getCategoria() != null ? e.getCategoria().getNombre() : "");
                agregarCelda(table, e.getAmbiente() != null ? e.getAmbiente().getNombre() : "");
                agregarCelda(table, e.getPropietario() != null ? e.getPropietario().getNombreCompleto() : "");
                agregarCelda(table,
                        e.getInventarioActualInstructor() != null
                                ? e.getInventarioActualInstructor().getNombreCompleto()
                                : "");
                agregarCelda(table, e.getEstado() != null ? e.getEstado().name() : "");
                agregarCelda(table, e.getCantidadTotal() != null ? e.getCantidadTotal().toString() : "0");
                agregarCelda(table, e.getCantidadDisponible() != null ? e.getCantidadDisponible().toString() : "0");
                agregarCelda(table, e.getUmbralMinimo() != null ? e.getUmbralMinimo().toString() : "0");
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar PDF de inventario", e);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de inventario", e);
        }
    }

    /**
     * RF-REP-02: Historial de préstamos en PDF.
     */
    public byte[] generarReportePrestamos(List<Prestamo> prestamos) {
        Document document = new Document();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("Historial de Préstamos - SIGEA", FONT_TITULO));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100f);
            table.setWidths(new float[] { 0.5f, 2f, 2f, 1.2f, 1.5f, 1.5f, 1.5f });
            agregarCeldaHeader(table, "ID");
            agregarCeldaHeader(table, "Solicitante");
            agregarCeldaHeader(table, "Correo");
            agregarCeldaHeader(table, "Estado");
            agregarCeldaHeader(table, "F. solicitud");
            agregarCeldaHeader(table, "F. dev. est.");
            agregarCeldaHeader(table, "F. dev. real");

            for (Prestamo p : prestamos) {
                agregarCelda(table, p.getId() != null ? p.getId().toString() : "");
                agregarCelda(table,
                        p.getUsuarioSolicitante() != null ? p.getUsuarioSolicitante().getNombreCompleto() : "");
                agregarCelda(table,
                        p.getUsuarioSolicitante() != null ? p.getUsuarioSolicitante().getCorreoElectronico() : "");
                agregarCelda(table, p.getEstado() != null ? p.getEstado().name() : "");
                agregarCelda(table,
                        p.getFechaHoraSolicitud() != null ? p.getFechaHoraSolicitud().format(FECHA_HORA) : "");
                agregarCelda(table,
                        p.getFechaHoraDevolucionEstimada() != null
                                ? p.getFechaHoraDevolucionEstimada().format(FECHA_HORA)
                                : "");
                agregarCelda(table,
                        p.getFechaHoraDevolucionReal() != null ? p.getFechaHoraDevolucionReal().format(FECHA_HORA)
                                : "");
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar PDF de préstamos", e);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de préstamos", e);
        }
    }

    /**
     * RF-REP-03: Equipos más solicitados en PDF.
     */
    public byte[] generarReporteEquiposMasSolicitados(List<Equipo> equipos, List<Long> cantidades) {
        Document document = new Document();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("Equipos más solicitados - SIGEA", FONT_TITULO));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100f);
            table.setWidths(new float[] { 1.5f, 3f, 2f, 1.2f });
            agregarCeldaHeader(table, "Código");
            agregarCeldaHeader(table, "Nombre");
            agregarCeldaHeader(table, "Categoría");
            agregarCeldaHeader(table, "Veces");

            for (int i = 0; i < equipos.size(); i++) {
                Equipo e = equipos.get(i);
                Long countValue = i < cantidades.size() ? cantidades.get(i) : null;
                long count = countValue != null ? countValue : 0L;
                agregarCelda(table, e.getCodigoUnico());
                agregarCelda(table, e.getNombre());
                agregarCelda(table, e.getCategoria() != null ? e.getCategoria().getNombre() : "");
                agregarCelda(table, String.valueOf(count));
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar PDF de equipos más solicitados", e);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de equipos más solicitados", e);
        }
    }

    /**
     * RF-REP-04: Usuarios con préstamos activos o en mora en PDF.
     */
    public byte[] generarReporteUsuariosEnMora(List<Usuario> usuarios) {
        Document document = new Document();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("Usuarios con préstamos activos o en mora - SIGEA", FONT_TITULO));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100f);
            table.setWidths(new float[] { 1.2f, 2.5f, 2f, 1.2f, 1f });
            agregarCeldaHeader(table, "Documento");
            agregarCeldaHeader(table, "Nombre completo");
            agregarCeldaHeader(table, "Correo");
            agregarCeldaHeader(table, "Teléfono");
            agregarCeldaHeader(table, "Rol");

            for (Usuario u : usuarios) {
                agregarCelda(table, u.getNumeroDocumento() != null ? u.getNumeroDocumento() : "");
                agregarCelda(table, u.getNombreCompleto() != null ? u.getNombreCompleto() : "");
                agregarCelda(table, u.getCorreoElectronico() != null ? u.getCorreoElectronico() : "");
                agregarCelda(table, u.getTelefono() != null ? u.getTelefono() : "");
                agregarCelda(table, u.getRol() != null ? u.getRol().name() : "");
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar PDF de usuarios en mora", e);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de usuarios en mora", e);
        }
    }

    private void agregarCeldaHeader(PdfPTable table, String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FONT_HEADER));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void agregarCelda(PdfPTable table, String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto != null ? texto : "", FONT_NORMAL));
        cell.setPadding(4);
        table.addCell(cell);
    }
}
