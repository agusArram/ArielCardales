package com.arielcardales.arielcardales.controller;

import com.arielcardales.arielcardales.Entidades.Producto;
import com.arielcardales.arielcardales.Util.ExportadorExcel;
import com.arielcardales.arielcardales.Util.ExportadorPDF;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;

import javax.swing.filechooser.FileSystemView;
import java.io.File;

/**
 * Controlador utilitario encargado de exportar datos a PDF o Excel.
 * Centraliza toda la lógica de exportación para que ProductoController
 * solo llame a estos métodos.
 */
public class ExportarController {

    /**
     * Obtiene la ruta del escritorio (compatible con OneDrive).
     */
    private static String getRutaEscritorio(String nombreArchivo) {
        File escritorio = FileSystemView.getFileSystemView().getHomeDirectory();
        return new File(escritorio, nombreArchivo).getAbsolutePath();
    }

    /**
     * Exporta una lista de productos a PDF.
     */
    public static void exportarPDF(ObservableList<Producto> productos) {
        try {
            String ruta = getRutaEscritorio("productos.pdf");
            ExportadorPDF.exportar(productos, ruta);
            mostrarMensaje("PDF exportado correctamente en:\n" + ruta, Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            mostrarMensaje("Error al exportar PDF:\n" + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    /**
     * Exporta una lista de productos a Excel.
     */
    public static void exportarExcel(ObservableList<Producto> productos) {
        try {
            String ruta = getRutaEscritorio("productos.xlsx");
            ExportadorExcel.exportar(productos, ruta);
            mostrarMensaje("Excel exportado correctamente en:\n" + ruta, Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            mostrarMensaje("Error al exportar Excel:\n" + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    /**
     * Muestra una alerta simple para notificar al usuario.
     */
    private static void mostrarMensaje(String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle("Exportación");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
