package pl.touk.widerest.api.cart.exceptions;

public class NoFulfillmentOptionException extends OrderValidationException  {
    public NoFulfillmentOptionException() {
    }

    public NoFulfillmentOptionException(String message) {
        super(message);
    }

    public NoFulfillmentOptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
