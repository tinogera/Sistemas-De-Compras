package com.Sistem.Solicitude_Compra.ordenes.servicio;

import com.Sistem.Solicitude_Compra.ordenes.dominio.OrdenPedido;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Arma el PDF de una orden de pedido. */
@Component
public class OrdenPdf {

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("America/Argentina/Buenos_Aires"));
    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font ETIQUETA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font TEXTO = FontFactory.getFont(FontFactory.HELVETICA, 11);

    public byte[] generar(OrdenPedido orden) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document();
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Paragraph titulo = new Paragraph("Orden de pedido N° " + orden.getNumero(), TITULO);
            titulo.setSpacingAfter(16);
            documento.add(titulo);

            PdfPTable tabla = new PdfPTable(2);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[] {1, 2});
            fila(tabla, "Fecha", FECHA.format(orden.getEmitidaEn()));
            fila(tabla, "Proveedor", orden.getProveedorRazonSocial());
            fila(tabla, "Detalle", orden.getDetalle() == null ? "-" : orden.getDetalle());
            fila(tabla, "Cantidad", orden.getCantidad().stripTrailingZeros().toPlainString());
            fila(tabla, "Precio unitario", importe(orden.getPrecioUnitario(), orden));
            fila(tabla, "Precio total", importe(orden.getPrecioTotal(), orden));
            if (orden.getMontoEnvio() != null) {
                fila(tabla, "Monto de envío", importe(orden.getMontoEnvio(), orden));
            }
            documento.add(tabla);
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF de la orden " + orden.getNumero(), e);
        } finally {
            documento.close();
        }
        return salida.toByteArray();
    }

    private static String importe(BigDecimal valor, OrdenPedido orden) {
        return orden.getMoneda() + " " + valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static void fila(PdfPTable tabla, String etiqueta, String valor) {
        tabla.addCell(celda(etiqueta, ETIQUETA));
        tabla.addCell(celda(valor, TEXTO));
    }

    private static PdfPCell celda(String texto, Font fuente) {
        PdfPCell celda = new PdfPCell(new Paragraph(texto, fuente));
        celda.setPadding(6);
        celda.setHorizontalAlignment(Element.ALIGN_LEFT);
        return celda;
    }
}
