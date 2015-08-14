package pl.touk.widerest.api.cart.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by mst on 03.08.15.
 */
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