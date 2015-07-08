package pl.touk.widerest.api.cart.exceptions;

/**
 * Created by mst on 08.07.15.
 */
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
