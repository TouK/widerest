package pl.touk.widerest.api.orders.fulfillments;

import pl.touk.widerest.api.orders.OrderValidationException;

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
