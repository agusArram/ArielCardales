package SORT_PROYECTS.AppInventario.DAO;

/**
 * Excepción lanzada cuando se intenta autenticar con una cuenta expirada
 */
public class AccountExpiredException extends RuntimeException {

    public AccountExpiredException(String message) {
        super(message);
    }

    public AccountExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
