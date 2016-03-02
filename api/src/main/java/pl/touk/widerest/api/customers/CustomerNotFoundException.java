package pl.touk.widerest.api.customers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException () {

    }
    public CustomerNotFoundException (String message) {
        super(message);
    }

    public CustomerNotFoundException (String message, Throwable cause) {
        super(message, cause);
    }
}
