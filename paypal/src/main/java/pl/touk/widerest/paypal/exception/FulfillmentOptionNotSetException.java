package pl.touk.widerest.paypal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class FulfillmentOptionNotSetException extends RuntimeException {

    public FulfillmentOptionNotSetException() {

    }

    public FulfillmentOptionNotSetException(String message) {
        super(message);
    }

    public FulfillmentOptionNotSetException(String message, Throwable cause) {
        super(message, cause);
    }
}
