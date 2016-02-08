package pl.touk.widerest.api.cart.exceptions;

public class NoShippingAddressException extends OrderValidationException {
    public NoShippingAddressException() {
    }

    public NoShippingAddressException(String message) {
        super(message);
    }

    public NoShippingAddressException(String message, Throwable cause) {
        super(message, cause);
    }
}
