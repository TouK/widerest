package pl.touk.widerest.api.orders.fulfillments;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class FulfillmentOptionNotAllowedException extends RuntimeException {
    public FulfillmentOptionNotAllowedException() {
    }

    public FulfillmentOptionNotAllowedException(String message) {
        super(message);
    }

    public FulfillmentOptionNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }
}
