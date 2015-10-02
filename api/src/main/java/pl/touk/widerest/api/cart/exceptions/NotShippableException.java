package pl.touk.widerest.api.cart.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by mst on 03.08.15.
 */
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
