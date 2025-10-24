package SORT_PROYECTS.AppInventario.controller;

import SORT_PROYECTS.AppInventario.Entidades.ItemInventario;
import SORT_PROYECTS.AppInventario.Entidades.Producto;
import SORT_PROYECTS.AppInventario.Util.ExportadorExcel;
import SORT_PROYECTS.AppInventario.Util.ExportadorPDF;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.List;

public class ExportarController {

    private static File escritorio() {
        return FileSystemView.getFileSystemView().getHomeDirectory();
    }

    private static void ok(String m){ alerta(m, Alert.AlertType.INFORMATION); }
    private static void error(String m){ alerta(m, Alert.AlertType.ERROR); }
    private static void alerta(String m, Alert.AlertType t){
        Alert a = new Alert(t);
        a.setTitle("Exportación");
        a.setHeaderText(null);
        a.setContentText(m);
        a.showAndWait();
    }

    // --------- Lista plana de Producto (compat) ---------
    public static void exportarProductosPDF(ObservableList<Producto> productos, Window owner) {
        File f = fileChooser(owner, "PDF", "*.pdf", new File(escritorio(), "productos.pdf"));
        if (f == null) return;
        try {
            ExportadorPDF.exportarProductos(productos, f.getAbsolutePath());
            ok("PDF exportado en:\n" + f.getAbsolutePath());
        } catch (Exception e) { error("Error al exportar PDF:\n" + e.getMessage()); }
    }

    public static void exportarProductosExcel(ObservableList<Producto> productos, Window owner) {
        File f = fileChooser(owner, "Excel", "*.xlsx", new File(escritorio(), "productos.xlsx"));
        if (f == null) return;
        try {
            ExportadorExcel.exportarProductos(productos, f.getAbsolutePath());
            ok("Excel exportado en:\n" + f.getAbsolutePath());
        } catch (Exception e) { error("Error al exportar Excel:\n" + e.getMessage()); }
    }

    // --------- Árbol visible (ItemInventario) ---------
    public static void exportarInventarioTreePDF(List<ItemInventario> flat, Window owner) {
        File f = fileChooser(owner, "PDF", "*.pdf", new File(escritorio(), "inventario.pdf"));
        if (f == null) return;
        try {
            ItemInventario root = flat.isEmpty() ? null : flat.get(0);
            ExportadorPDF.exportarInventarioTree(root, flat, f.getAbsolutePath());
            ok("PDF exportado en:\n" + f.getAbsolutePath());
        } catch (Exception e) { error("Error al exportar PDF:\n" + e.getMessage()); }
    }

    public static void exportarInventarioTreeExcel(List<ItemInventario> flat, Window owner) {
        File f = fileChooser(owner, "Excel", "*.xlsx", new File(escritorio(), "inventario.xlsx"));
        if (f == null) return;
        try {
            ExportadorExcel.exportarInventarioTree(flat, f.getAbsolutePath());
            ok("Excel exportado en:\n" + f.getAbsolutePath());
        } catch (Exception e) { error("Error al exportar Excel:\n" + e.getMessage()); }
    }

    private static File fileChooser(Window owner, String desc, String pattern, File suggested) {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(escritorio());
        fc.setInitialFileName(suggested.getName());
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(desc, pattern));
        return fc.showSaveDialog(owner);
    }
}
