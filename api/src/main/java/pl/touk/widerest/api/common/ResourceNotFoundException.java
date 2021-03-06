package pl.touk.widerest.api.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private Long resourceId;

    public ResourceNotFoundException() {

    }

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(Long resourceId) { this.resourceId = resourceId; }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public Long getResourceId() { return this.resourceId; }
}
