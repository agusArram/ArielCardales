package com.arielcardales.arielcardales.DAO;

import java.util.List;
import java.util.Optional;

public interface CrudDAO<T, ID> {
    List<T> findAll();
    Optional<T> findById(ID id);
    ID insert(T entity);
    boolean update(T entity);
    boolean deleteById(ID id);
}
