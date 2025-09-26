package com.arielcardales.arielcardales.DAO;

import com.arielcardales.arielcardales.Entidades.Producto;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {
    public static List<Producto> getAll() {
        List<Producto> productos = new ArrayList<>();
        productos.add(new Producto(1, "Mate de Algarrobo", new BigDecimal("2500"), 10));
        productos.add(new Producto(2, "Cuchillo Acero Inox", new BigDecimal("12000"), 5));
        return productos;
    }
}
