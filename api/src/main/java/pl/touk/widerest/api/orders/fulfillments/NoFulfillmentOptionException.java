package pl.touk.widerest.api.orders.fulfillments;

import pl.touk.widerest.api.orders.OrderValidationException;

public class NoFulfillmentOptionException extends OrderValidationException {
    public NoFulfillmentOptionException() {
    }

    public NoFulfillmentOptionException(String message) {
        super(message);
    }

    public NoFulfillmentOptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
