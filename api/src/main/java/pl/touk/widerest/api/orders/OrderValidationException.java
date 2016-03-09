package pl.touk.widerest.api.orders;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class OrderValidationException extends RuntimeException {
    public OrderValidationException() {
    }

    public OrderValidationException(String message) {
        super(message);
    }

    public OrderValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
