package pl.touk.widerest.api.orders.fulfillments;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class UnknownFulfillmentOptionException extends RuntimeException {
}
