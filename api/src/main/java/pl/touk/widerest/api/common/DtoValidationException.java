package pl.touk.widerest.api.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
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

