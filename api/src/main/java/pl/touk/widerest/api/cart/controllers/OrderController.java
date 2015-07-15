package pl.touk.widerest.api.cart.controllers;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderImpl;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.service.OrderItemService;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.order.service.call.OrderItemRequestDTO;
import org.broadleafcommerce.core.order.service.exception.AddToCartException;
import org.broadleafcommerce.core.order.service.exception.RemoveFromCartException;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.broadleafcommerce.core.pricing.service.exception.PricingException;
import org.broadleafcommerce.core.web.service.UpdateCartService;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.domain.CustomerImpl;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.touk.widerest.api.cart.dto.DiscreteOrderItemDto;
import pl.touk.widerest.api.cart.dto.OrderDto;
import pl.touk.widerest.api.cart.dto.OrderItemDto;
import pl.touk.widerest.api.cart.dto.OrderPaymentDto;
import pl.touk.widerest.api.cart.exceptions.CustomerNotFoundException;
import pl.touk.widerest.api.cart.exceptions.OrderNotFoundException;
import pl.touk.widerest.api.cart.service.OrderServiceProxy;
import pl.touk.widerest.api.catalog.DtoConverters;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;

/**
 * Created by mst on 07.07.15.
 */

//TODO: Aquire and release locks?!
@RestController
@RequestMapping("/catalog/orders")
@Api(value = "orders", description = "Order management endpoint")
public class OrderController {

    @Resource(name = "blOrderService")
    protected OrderService orderService;

    @Resource(name="blCustomerService")
    private CustomerService customerService;

    @Resource(name = "blOrderItemService")
    protected OrderItemService orderItemService;

    @Resource(name = "wdOrderService")
    protected OrderServiceProxy orderServiceProxy;

    private final static String ANONYMOUS_CUSTOMER = "anonymous";

    /* GET /orders */
    @Transactional
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(value = "Get a list of customers' orders", response = List.class)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public List<OrderDto> getOrders(@AuthenticationPrincipal CustomerUserDetails customerUserDetails) {


        return orderServiceProxy.getOrdersByCustomer(customerUserDetails).stream()
                .map(DtoConverters.orderEntityToDto)
                .collect(Collectors.toList());
}

    /* GET /orders/{orderId} */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Get a specific order by its ID", response = OrderDto.class)
    @Transactional
    public OrderDto getOrderById(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @PathVariable(value = "id") Long orderId) {

        return DtoConverters.orderEntityToDto.apply(getOrderForCustomerById(customerUserDetails, orderId));

    }

    /* POST /orders */
    @Transactional
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "", method = RequestMethod.POST)
    @ApiOperation(value = "Create a new order", response = ResponseEntity.class)
    public ResponseEntity<?> createNewOrder(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails) {

        Customer currentCustomer = customerService.readCustomerById(customerUserDetails.getId());

        if (currentCustomer == null && customerUserDetails.getUsername().equals(ANONYMOUS_CUSTOMER)) {
            // ???
            throw new CustomerNotFoundException("Cannot find a customer with ID: " + customerUserDetails.getId());
        }

        Order cart = orderService.createNewCartForCustomer(currentCustomer);

        HttpHeaders responseHeader = new HttpHeaders();

        responseHeader.setLocation(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(cart.getId())
                .toUri());

        try {
            orderService.save(cart, true);
        } catch (PricingException e) {
			/* Order is empty - there should not be any PricingException situations */
            e.printStackTrace();
        }

        return new ResponseEntity<>(null, responseHeader, HttpStatus.CREATED);
    }

    /* DELETE /orders/ */
    @Transactional
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}", method = RequestMethod.DELETE)
    @ApiOperation(value = "Delete the own order", response = Void.class)
    public void deleteOrderForCustomer(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @PathVariable(value = "orderId") Long orderId) {

        //orderService.cancelOrder(orders.get(0));
        orderService.deleteOrder(getOrderForCustomerById(customerUserDetails, orderId));
    }


    /* POST /orders/items */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/items", method = RequestMethod.POST)
    @ApiOperation(value = "Add a new item to the cart", response = Void.class)
    @Transactional
    public void addProductToOrder(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @RequestBody OrderItemDto orderItemDto,
            @PathVariable(value = "orderId") Long orderId) throws PricingException, AddToCartException {


       Order cart = getOrderForCustomerById(customerUserDetails, orderId);

        orderService.addItem(cart.getId(), DtoConverters.orderItemDtoToRequest.apply(orderItemDto), false);
        orderService.save(cart, false);
    }

    /* GET /orders/items/ */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/items", method = RequestMethod.GET)
    @ApiOperation(value = "Get a list of items in an order", response = List.class)
    @Transactional
    public List<DiscreteOrderItemDto> getAllItemsInOrder (
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @PathVariable(value = "orderId") Long orderId) {

        Order order = getOrderForCustomerById(customerUserDetails, orderId);

        return Optional.ofNullable(order.getDiscreteOrderItems().stream().map(DtoConverters.discreteOrderItemEntityToDto).collect(Collectors.toList()))
                .orElseThrow(ResourceNotFoundException::new);
    }

    /* GET /orders/{orderId}/items/count */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "/{id}/items/count", method = RequestMethod.GET)
    @ApiOperation(value = "Get a number of items in the order", response = Integer.class)
    @Transactional
    public Integer getItemsCountByOrderId (
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @PathVariable(value = "id") Long orderId) {

        Order order = getOrderForCustomerById(customerUserDetails, orderId);

        return order.getItemCount();
    }

    /* GET /orders/{orderId}/status */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "/{id}/items/status", method = RequestMethod.GET)
    @ApiOperation(value = "Get a status of an order", response = OrderStatus.class)
    public OrderStatus getOrderStatusById (
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @PathVariable(value = "id") Long orderId) {

        Order order = getOrderForCustomerById(customerUserDetails, orderId);
        return order.getStatus();
    }

    /* DELETE /orders/{orderId}/items/{itemId} */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/items/{itemId}", method = RequestMethod.DELETE)
    @ApiOperation(value = "Remove an item from an order", response = Void.class)
    @Transactional
    public void removeItemFromOrder(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @PathVariable(value = "itemId") Long itemId,
            @PathVariable(value = "orderId") Long orderId) {

        Order cart = getOrderForCustomerById(customerUserDetails, orderId);

        if(cart.getDiscreteOrderItems().stream().filter(x -> x.getId() == itemId).count() != 1) {
            throw new ResourceNotFoundException("Cannot find an item with ID: " + itemId);
        }

        try {
            /* price order?! */
            Order updatedOrder = orderService.removeItem(cart.getId(), itemId, true);
            orderService.save(updatedOrder, true);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Error while removing item with ID: " + itemId);
        }

    }

    /* GET /orders/items/{itemId} */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/items/{itemId}", method = RequestMethod.GET)
    @ApiOperation(value = "Get a description of an item in an Order", response = OrderItemDto.class)
    @Transactional
    public DiscreteOrderItemDto getOneItemFromOrder(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @PathVariable(value = "itemId") Long itemId,
            @PathVariable(value = "orderId") Long orderId) {

        Order cart = getOrderForCustomerById(customerUserDetails, orderId);

        List<DiscreteOrderItemDto> list =  cart.getDiscreteOrderItems().stream().
               filter(x -> x.getId() == itemId).limit(2).map(DtoConverters.discreteOrderItemEntityToDto)
               .collect(Collectors.toList());

        if(list.isEmpty()) {
            throw new ResourceNotFoundException("Cannot find the item in card with ID: " + itemId);
        }

        return list.get(0);
    }



    /* GET /orders/{id}/payments */
    /*
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "/{id}/payments", method = RequestMethod.GET)
    @ApiOperation(value = "Get a list of available payments for an order", response = List.class)
    public List<OrderPaymentDto> getPaymentsByOrderId(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @PathVariable(value = "id") Long orderId) {

        Customer currentCustomer = customerService.readCustomerById(customerUserDetails.getId());

        if(currentCustomer == null) {
            throw new CustomerNotFoundException();
        }

        Order cart = orderService.findCartForCustomer(currentCustomer);

        if(cart == null || cart.getId() != orderId) {
            throw new ResourceNotFoundException("Cannot find an order with ID: " + orderId);
        }

        return cart.getPayments().stream().map(DtoConverters.orderPaymentEntityToDto).collect(Collectors.toList());
    }
    */

    private Order getOrderForCustomerById(CustomerUserDetails customerUserDetails, Long orderId) throws OrderNotFoundException {

        List<Order> orders = orderServiceProxy.getOrdersByCustomer(customerUserDetails).stream()
                .filter(x -> x.getId() == orderId).limit(2)
                .collect(Collectors.toList());

        if(orders == null || orders.isEmpty()) {
            throw new OrderNotFoundException("Cannot find order with ID: " + orderId + " for customer with ID: " + customerUserDetails.getId());
        }

        Order order = orders.get(0);

        if(order == null) {
            throw new OrderNotFoundException("Cannot find order with ID: " + orderId + " for customer with ID: " + customerUserDetails.getId());
        }

        return order;
    }
}