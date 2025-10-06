package com.arielcardales.arielcardales.DAO;

import java.util.List;
import java.util.Optional;

//interfaz genérica que define las operaciones básicas de un DAO (Data Access Object).
public interface CrudDAO<T, ID> {
    List<T> findAll();               // Leer todos
    Optional<T> findById(ID id);     // Leer uno por ID
    ID insert(T entity);             // Insertar un registro
    boolean update(T entity);        // Actualizar registro
    boolean deleteById(ID id);       // Eliminar por ID
}

