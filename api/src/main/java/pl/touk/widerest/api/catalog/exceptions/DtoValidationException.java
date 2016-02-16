package pl.touk.widerest.api.catalog.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class DtoValidationException extends RuntimeException {

    public DtoValidationException() {
        super();
    }

    public DtoValidationException(String message) {
        super(message);
    }

    public DtoValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

