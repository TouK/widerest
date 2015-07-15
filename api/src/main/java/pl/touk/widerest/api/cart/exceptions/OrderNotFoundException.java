package pl.touk.widerest.api.cart.exceptions;

import org.broadleafcommerce.core.order.domain.Order;

/**
 * Created by mst on 15.07.15.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException() {
    }

    public OrderNotFoundException(String message) {
        super(message);
    }

    public OrderNotFoundException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
