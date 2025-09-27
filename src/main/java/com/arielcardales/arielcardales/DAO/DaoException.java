package com.arielcardales.arielcardales.DAO;

public class DaoException extends RuntimeException {
    public DaoException(String msg, Throwable cause) { super(msg, cause); }
    public DaoException(String msg) { super(msg); }
}
