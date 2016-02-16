package pl.touk.widerest.api.cart.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class NotShippableException extends RuntimeException {

    public NotShippableException() {

    }

    public NotShippableException(String message) {
        super(message);
    }

    public NotShippableException(String message, Throwable cause) {
        super(message, cause);
    }
}
