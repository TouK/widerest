package pl.touk.widerest.api.cart.controllers;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import pl.touk.widerest.api.cart.dto.OrderDto;
import pl.touk.widerest.api.catalog.DtoConverters;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;

import javax.annotation.Resource;
import java.util.List;

/**
 * Created by mst on 07.07.15.
 */

//TODO: Aquire and release blocks?!
@RestController
@RequestMapping("/orders")
@Api(value = "/orders", description = "Order management")
public class OrderController /*extends AbstractCartController */{

    @Resource(name = "blOrderService")
    protected OrderService orderService;

    @PostAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(method = RequestMethod.GET)
    public String getOrders(@AuthenticationPrincipal CustomerUserDetails customerUserDetails) {

        //TODO: Administrator: wszystkie, klienci: tylko swoje



        return "Authenticated: " + customerUserDetails.getUsername() + " id: " + customerUserDetails.getId();

    }

    @PostAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Get a specific order by its ID", response = OrderDto.class)
    public ResponseEntity<OrderDto> getOrderById(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @PathVariable(value = "id") Long orderId) {

        Order order = orderService.findOrderById(orderId);

        if(order == null) {
            throw new ResourceNotFoundException("Order with ID: " + orderId + " does not exist");
        }

        if(customerUserDetails.getId() != order.getCustomer().getId() &&
                !(customerUserDetails.getAuthorities().contains(new SimpleGrantedAuthority("'ROLE_ADMIN")))) {
            throw new AccessDeniedException("");
        }

        return new ResponseEntity<>(DtoConverters.orderEntityToDto.apply(order), HttpStatus.OK);
    }

    @PostAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ApiOperation(value = "Delete an order", response = Void.class)
    public void deleteOrderById(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @PathVariable(value = "id") Long orderId) {

        Order order = orderService.findOrderById(orderId);

        if(customerUserDetails.getId() != order.getCustomer().getId() &&
                !(customerUserDetails.getAuthorities().contains(new SimpleGrantedAuthority("'ROLE_ADMIN")))) {
            throw new AccessDeniedException("");
        }

        orderService.deleteOrder(order);
    }


}
