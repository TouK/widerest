package pl.touk.widerest.paypal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class CredentialNotSetException extends RuntimeException {

    public CredentialNotSetException() {

    }

    public CredentialNotSetException(String message) {
        super(message);
    }

    public CredentialNotSetException(String message, Throwable cause) {
        super(message, cause);
    }
}
