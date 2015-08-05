package pl.touk.widerest.api.cart.exceptions;

/**
 * Created by mst on 05.08.15.
 */
public class OrderValidationException extends RuntimeException {
    public OrderValidationException() {
    }

    public OrderValidationException(String message) {
        super(message);
    }

    public OrderValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
