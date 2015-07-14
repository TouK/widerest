package pl.touk.widerest.api.cart.exceptions;

/**
 * Created by mst on 14.07.15.
 */
public class OrderAlreadyInUseException extends RuntimeException {
    public OrderAlreadyInUseException() {
        super();
    }

    public OrderAlreadyInUseException(String message) {
        super(message);
    }

    public OrderAlreadyInUseException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
