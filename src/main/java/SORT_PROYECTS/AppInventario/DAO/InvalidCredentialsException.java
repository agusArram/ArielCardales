package SORT_PROYECTS.AppInventario.DAO;

/**
 * Excepción lanzada cuando las credenciales son incorrectas
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
