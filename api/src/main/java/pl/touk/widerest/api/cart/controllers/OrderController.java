package pl.touk.widerest.api.cart.controllers;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.broadleafcommerce.profile.core.service.UserDetailsServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pl.touk.widerest.api.cart.dto.OrderDto;
import pl.touk.widerest.api.cart.dto.OrderItemDto;
import pl.touk.widerest.api.cart.exceptions.CustomerNotFoundException;
import pl.touk.widerest.api.catalog.DtoConverters;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by mst on 07.07.15.
 */

//TODO: Aquire and release locks?!
@RestController
@RequestMapping("/orders")
@Api(value = "/orders", description = "Order management")
public class OrderController /*extends AbstractCartController */{

    @Resource(name = "blOrderService")
    protected OrderService orderService;

    @Resource(name="blCustomerService")
    private CustomerService customerService;

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<OrderDto>> getOrders(@AuthenticationPrincipal CustomerUserDetails customerUserDetails) {

        /* If the current user has admin rights, list all the orders */
        if(customerUserDetails.getAuthorities().contains(new SimpleGrantedAuthority("'ROLE_ADMIN"))) {

        } else {
            Customer currentCustomer = customerService.readCustomerById(customerUserDetails.getId());

            if (currentCustomer == null) {
                throw new CustomerNotFoundException();
            }

            return new ResponseEntity<>(
                    orderService.findOrdersForCustomer(currentCustomer).stream().map(DtoConverters.orderEntityToDto).collect(Collectors.toList()),
                    HttpStatus.OK);
        }

        // TMP!!!
        return new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
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

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
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

        // delete cart also?
        orderService.deleteOrder(order);
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation(value = "Create a new order", response = OrderDto.class)
    public ResponseEntity<OrderDto> createNewOrder(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @RequestBody OrderDto orderDto) {


        System.out.println("Post!");
        if(customerUserDetails == null) {
            throw new CustomerNotFoundException();
        }

        // Administrator ???
        Customer currentCustomer = customerService.readCustomerById(customerUserDetails.getId());

        if(currentCustomer == null) {
            throw new CustomerNotFoundException();
        }

        Order cart = orderService.findCartForCustomer(currentCustomer);

        if(cart == null) {
            cart = orderService.createNewCartForCustomer(currentCustomer);
        }

        return new ResponseEntity<>(DtoConverters.orderEntityToDto.apply(cart), HttpStatus.OK);

    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @ApiOperation(value = "Add a new item to the cart", response = Void.class)
    public void addProductToOrder(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @RequestBody OrderItemDto orderItemDto,
            @PathVariable(value = "id") Long orderId) {


        Customer currentCustomer = customerService.readCustomerById(customerUserDetails.getId());

        if(currentCustomer == null) {
            throw new CustomerNotFoundException();
        }


        Order cart = orderService.findCartForCustomer(currentCustomer);

        if(cart == null || cart.getId() != orderId) {
            throw new ResourceNotFoundException("Cannot find an order with ID: " + orderId);
        }

        cart.addOrderItem(DtoConverters.orderDtoToEntity.apply(orderItemDto));
    }


    }
