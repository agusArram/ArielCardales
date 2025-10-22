package com.arielcardales.arielcardales.DAO;

import com.arielcardales.arielcardales.Licencia.Licencia;
import com.arielcardales.arielcardales.Util.Mapper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para gestión de licencias en base de datos
 * Implementa el patrón DAO estándar con CrudDAO
 */
public class LicenciaDAO implements CrudDAO<Licencia, String> {

    /**
     * Busca una licencia por DNI
     * Este es el método principal para validación de licencias
     *
     * @param dni DNI del cliente
     * @return Optional con la licencia si existe
     */
    @Override
    public Optional<Licencia> findById(String dni) {
        String sql = """
            SELECT id, cliente_id, nombre, email, estado::text as estado,
                   plan::text as plan, fecha_expiracion, notas,
                   createdAt, updatedAt
            FROM licencia
            WHERE cliente_id = ?
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, dni);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(Mapper.getLicencia(rs));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DaoException("Error buscando licencia por DNI: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene todas las licencias (para administración)
     */
    @Override
    public List<Licencia> findAll() {
        String sql = """
            SELECT id, dni, nombre, email, estado::text as estado,
                   plan::text as plan, fecha_expiracion, notas,
                   createdAt, updatedAt
            FROM licencia
            ORDER BY nombre ASC
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Licencia> licencias = new ArrayList<>();
            while (rs.next()) {
                licencias.add(Mapper.getLicencia(rs));
            }
            return licencias;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DaoException("Error obteniendo licencias: " + e.getMessage(), e);
        }
    }

    /**
     * Inserta una nueva licencia
     *
     * @param licencia Licencia a insertar
     * @return DNI de la licencia insertada
     */
    @Override
    public String insert(Licencia licencia) {
        String sql = """
            INSERT INTO licencia (dni, nombre, email, estado, plan, fecha_expiracion, notas)
            VALUES (?, ?, ?, ?::estado_licencia, ?::plan_licencia, ?, ?)
            RETURNING dni
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, licencia.getClienteId());
            ps.setString(2, licencia.getNombre());
            ps.setString(3, licencia.getEmail());
            ps.setString(4, licencia.getEstado().name());
            ps.setString(5, licencia.getPlan().name());
            ps.setDate(6, Date.valueOf(licencia.getFechaExpiracion()));
            ps.setString(7, null); // notas

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
                throw new DaoException("No se pudo insertar la licencia");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            if (e.getSQLState().equals("23505")) { // unique_violation
                throw new DaoException("Ya existe una licencia para el DNI: " + licencia.getClienteId(), e);
            }
            throw new DaoException("Error insertando licencia: " + e.getMessage(), e);
        }
    }

    /**
     * Actualiza una licencia existente
     *
     * @param licencia Licencia con datos actualizados
     * @return true si se actualizó correctamente
     */
    @Override
    public boolean update(Licencia licencia) {
        String sql = """
            UPDATE licencia
            SET nombre = ?,
                email = ?,
                estado = ?::estado_licencia,
                plan = ?::plan_licencia,
                fecha_expiracion = ?
            WHERE dni = ?
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, licencia.getNombre());
            ps.setString(2, licencia.getEmail());
            ps.setString(3, licencia.getEstado().name());
            ps.setString(4, licencia.getPlan().name());
            ps.setDate(5, Date.valueOf(licencia.getFechaExpiracion()));
            ps.setString(6, licencia.getClienteId());

            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DaoException("Error actualizando licencia: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina una licencia por DNI
     * NOTA: En producción considerar soft delete en vez de hard delete
     *
     * @param dni DNI de la licencia a eliminar
     * @return true si se eliminó correctamente
     */
    @Override
    public boolean deleteById(String dni) {
        String sql = "DELETE FROM licencia WHERE dni = ?";

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, dni);
            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DaoException("Error eliminando licencia: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica si existe una licencia para un DNI
     *
     * @param dni DNI a verificar
     * @return true si existe
     */
    public boolean existsByDni(String dni) {
        String sql = "SELECT 1 FROM licencia WHERE dni = ?";

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new DaoException("Error verificando DNI", e);
        }
    }

    /**
     * Busca licencias que están por expirar en los próximos días
     *
     * @param dias Cantidad de días hacia adelante
     * @return Lista de licencias por expirar
     */
    public List<Licencia> findPorExpirar(int dias) {
        String sql = """
            SELECT id, dni, nombre, email, estado::text as estado,
                   plan::text as plan, fecha_expiracion, notas,
                   createdAt, updatedAt
            FROM licencia
            WHERE estado = 'ACTIVO'
            AND fecha_expiracion BETWEEN CURRENT_DATE AND (CURRENT_DATE + ? * INTERVAL '1 day')
            ORDER BY fecha_expiracion ASC
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, dias);

            try (ResultSet rs = ps.executeQuery()) {
                List<Licencia> licencias = new ArrayList<>();
                while (rs.next()) {
                    licencias.add(Mapper.getLicencia(rs));
                }
                return licencias;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DaoException("Error buscando licencias por expirar: " + e.getMessage(), e);
        }
    }
}
