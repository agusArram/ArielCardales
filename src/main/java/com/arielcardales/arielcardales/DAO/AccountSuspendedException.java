package com.arielcardales.arielcardales.DAO;

/**
 * Excepción lanzada cuando se intenta autenticar con una cuenta suspendida
 */
public class AccountSuspendedException extends RuntimeException {

    public AccountSuspendedException(String message) {
        super(message);
    }

    public AccountSuspendedException(String message, Throwable cause) {
        super(message, cause);
    }
}
