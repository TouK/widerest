package pl.touk.widerest.api.cart.exceptions;

public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException () {

    }

    public CustomerNotFoundException (String message) {
        super(message);
    }

    public CustomerNotFoundException (String message, Throwable cause) {
        super(message, cause);
    }
}
