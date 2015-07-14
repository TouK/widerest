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

import lombok.extern.log4j.Log4j;
import org.broadleafcommerce.core.order.dao.OrderDao;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderImpl;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.order.service.call.OrderItemRequestDTO;
import org.broadleafcommerce.core.order.service.exception.AddToCartException;
import org.broadleafcommerce.core.order.service.exception.RemoveFromCartException;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.broadleafcommerce.core.pricing.service.exception.PricingException;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.domain.CustomerImpl;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
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
import pl.touk.widerest.api.cart.dto.DiscreteOrderItemDto;
import pl.touk.widerest.api.cart.dto.OrderDto;
import pl.touk.widerest.api.cart.dto.OrderItemDto;
import pl.touk.widerest.api.cart.dto.OrderPaymentDto;
import pl.touk.widerest.api.cart.exceptions.CustomerNotFoundException;
import pl.touk.widerest.api.cart.exceptions.OrderAlreadyInUseException;
import pl.touk.widerest.api.catalog.DtoConverters;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;

/**
 * Created by mst on 07.07.15.
 */

//TODO: Aquire and release locks?!
@RestController
@RequestMapping("/catalog/orders")
@Api(value = "/catalog/orders", description = "Order management")
public class OrderController {

    @Resource(name = "blOrderService")
    protected OrderService orderService;

    @Resource(name="blCustomerService")
    private CustomerService customerService;

    @PersistenceContext(unitName = "blPU")
    protected EntityManager em;

    final static Logger logger = Logger.getLogger("OrderController");

    //  @Resource(name = "blUpdateCartService")
    //  private UpdateCartService updateCartService;

    private List<Order> getOrders() {
        CriteriaBuilder builder = this.em.getCriteriaBuilder();
        CriteriaQuery criteria = builder.createQuery(Order.class);
        Root order = criteria.from(OrderImpl.class);
        criteria.select(order);
        TypedQuery query = this.em.createQuery(criteria);
        query.setHint("org.hibernate.cacheable", Boolean.valueOf(true));
        query.setHint("org.hibernate.cacheRegion", "query.Order");
        return query.getResultList();
    }

    private final static String ANONYMOUS_CUSTOMER = "anonymous";

    /* GET /orders */
    //@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(value = "Get a list of customers' orders", response = List.class)
    @Transactional
    public ResponseEntity<List<OrderDto>> getOrders(@AuthenticationPrincipal CustomerUserDetails customerUserDetails) {

        // If the current user has admin rights, list all the orders
        if(customerUserDetails.getAuthorities().contains(new SimpleGrantedAuthority("'ROLE_ADMIN"))) {
            return new ResponseEntity<>(getOrders().stream().map(DtoConverters.orderEntityToDto).collect(Collectors.toList()), HttpStatus.OK);
        } else {
            Customer currentCustomer = customerService.readCustomerById(customerUserDetails.getId());

            if (currentCustomer == null) {
                throw new CustomerNotFoundException("Cannot find a customer with ID: " + customerUserDetails.getId());
            }

            return new ResponseEntity<>(
                    orderService.findOrdersForCustomer(currentCustomer).stream().map(DtoConverters.orderEntityToDto).collect(Collectors.toList()),
                    HttpStatus.OK);

        }
}

    /* GET /orders/{orderId} */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Get a specific order by its ID", response = OrderDto.class)
    public ResponseEntity<OrderDto> getOrderById(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @PathVariable(value = "id") Long orderId) {

        Order order = orderService.findOrderById(orderId);

        if(order == null) {
            throw new ResourceNotFoundException("Order with ID: " + orderId + " does not exist");
        }

        return new ResponseEntity<>(DtoConverters.orderEntityToDto.apply(order), HttpStatus.OK);
    }

    /* POST /orders */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "", method = RequestMethod.POST)
    @ApiOperation(value = "Create a new order", response = ResponseEntity.class)
    public ResponseEntity<?> createNewOrder(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails) {
        //TODO: sprawdzenie czy nie utworzy sie wiecej niz jeden koszyk na uzytkownika
        Customer currentCustomer = customerService.readCustomerById(customerUserDetails.getId());

        /*
		 * We have a new Anonymous customer, he got his Token and Customer
		 * ID but he is not in the database yet
		 */
        if (currentCustomer == null && customerUserDetails.getUsername().equals(ANONYMOUS_CUSTOMER)) {
            Customer newAnonymousCustomer = new CustomerImpl();
            newAnonymousCustomer.setId(customerUserDetails.getId());
            newAnonymousCustomer.setAnonymous(true);
            newAnonymousCustomer.setRegistered(false);
            newAnonymousCustomer.setUsername(customerUserDetails.getUsername());
            newAnonymousCustomer.setPassword(customerUserDetails.getPassword());
            currentCustomer = customerService.saveCustomer(newAnonymousCustomer);
        } else {
			/* shouldnt it be a fatal exception ??? */
            throw new CustomerNotFoundException("Cannot find a customer with ID: " + customerUserDetails.getId());
        }

        Order cart = orderService.findCartForCustomer(currentCustomer);

        /* The cart should be null provided that the current customer hasnt created an order yet! */
        if(cart != null) {
            throw new OrderAlreadyInUseException("Cannot create a new order for customer with ID: " + customerUserDetails.getId() + ". Order has already been created (ID: " + cart.getId() + ")");
        }

        cart = orderService.createNewCartForCustomer(currentCustomer);

        try {
            orderService.save(cart, false);
        } catch (PricingException e) {
			/* Order is empty - there should not be any PricingException situations */
            e.printStackTrace();
        }

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /* DELETE /orders/ */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "/", method = RequestMethod.DELETE)
    @ApiOperation(value = "Delete the own order", response = Void.class)
    public void deleteOrderForCustomer(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails) {

        Customer currentCustomer = customerService.readCustomerById(customerUserDetails.getId());

        if (currentCustomer == null) {
            throw new CustomerNotFoundException("Cannot find a customer with ID: " + customerUserDetails.getId());
        }

        Order order = Optional.ofNullable(orderService.findCartForCustomer(currentCustomer))
                              .orElseThrow(ResourceNotFoundException::new);

        orderService.cancelOrder(order);
        orderService.deleteOrder(order);
    }

    /* DELETE /orders/{orderId} */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
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

        // delete cart as well?
        orderService.cancelOrder(order);
        orderService.deleteOrder(order);
    }



    /* POST /orders/items */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "/items", method = RequestMethod.POST)
    @ApiOperation(value = "Add a new item to the cart", response = Void.class)
    @Transactional
    public void addProductToOrder(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @RequestBody OrderItemDto orderItemDto) throws PricingException, AddToCartException {


        Customer currentCustomer = customerService.readCustomerById(customerUserDetails.getId());

        if(currentCustomer == null) {
            throw new CustomerNotFoundException();
        }

        Order cart = orderService.findCartForCustomer(currentCustomer);

        if(cart == null) {
            throw new ResourceNotFoundException("Cannot find an order for a customer ID: " + customerUserDetails.getId());
        }

        //updateCartService.updateAndValidateCart(cart);

        /* cart = orderService.addItem(orderId, , false); */
        OrderItemRequestDTO req = new OrderItemRequestDTO();
        req.setQuantity(orderItemDto.getQuantity());
        req.setSkuId(orderItemDto.getSkuId());
        //req.setProductId(orderItemDto.getProductId());
        //req.setItemAttributes(orderItemDto.getAttributes());
        
        //cart.addOrderItem(DtoConverters.orderItemDtoToEntity.apply(orderItemDto));
        orderService.addItem(cart.getId(), req, false);
        orderService.save(cart, false);
    }

    /* GET /orders/items/ */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "/items", method = RequestMethod.GET)
    @ApiOperation(value = "Get a list of items in an order", response = List.class)
    @Transactional
    public List<DiscreteOrderItemDto> getAllItemsInOrder (
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails) {

        Customer currentCustomer = customerService.readCustomerById(customerUserDetails.getId());

        if(currentCustomer == null) {
            throw new CustomerNotFoundException();
        }

        Order cart = orderService.findCartForCustomer(currentCustomer);


        if(cart == null) {
            throw new ResourceNotFoundException("Cannot find an order for a customer ID: " + customerUserDetails.getId());
        }

        return Optional.ofNullable(cart.getDiscreteOrderItems().stream().map(DtoConverters.discreteOrderItemEntityToDto).collect(Collectors.toList()))
                .orElseThrow(ResourceNotFoundException::new);
    }

    /* GET /orders/{orderId}/items/count */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "/{id}/items/count", method = RequestMethod.GET)
    @ApiOperation(value = "Get a number of items in the order", response = Integer.class)
    public Integer getItemsCountByOrderId (
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @PathVariable(value = "id") Long orderId) {

        Order order = orderService.findOrderById(orderId);

        if(customerUserDetails.getId() != order.getCustomer().getId() &&
                !(customerUserDetails.getAuthorities().contains(new SimpleGrantedAuthority("'ROLE_ADMIN")))) {
            throw new AccessDeniedException("");
        }

        return order.getItemCount();
    }

    /* GET /orders/{orderId}/items/status */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "/{id}/items/status", method = RequestMethod.GET)
    @ApiOperation(value = "Get a status of an order", response = OrderStatus.class)
    public OrderStatus getOrderStatusById (
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @PathVariable(value = "id") Long orderId) {

        Order order = orderService.findOrderById(orderId);

        if(customerUserDetails.getId() != order.getCustomer().getId() &&
                !(customerUserDetails.getAuthorities().contains(new SimpleGrantedAuthority("'ROLE_ADMIN")))) {
            throw new AccessDeniedException("");
        }

        return order.getStatus();
    }

    /* DELETE /orders/{orderId}/items/{itemId} */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "/{id}/items/{pId}", method = RequestMethod.DELETE)
    @ApiOperation(value = "Remove an item from an order", response = Void.class)
    public void removeItemFromOrder(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @PathVariable(value = "id") Long orderId,
            @PathVariable(value = "pId") Long productId) {

        Customer currentCustomer = customerService.readCustomerById(customerUserDetails.getId());

        if(currentCustomer == null) {
            throw new CustomerNotFoundException();
        }


        Order cart = orderService.findCartForCustomer(currentCustomer);

        if(cart == null || cart.getId() != orderId) {
            throw new ResourceNotFoundException("Cannot find an order with ID: " + orderId);
        }

        try {
            /* price order?! */
            Order updatedOrder = orderService.removeItem(cart.getId(), productId, true);
            orderService.save(updatedOrder, true);
        } catch (RemoveFromCartException e) {
            // TODO:
        } catch (PricingException e) {
            // TODO:
        }

    }

    /* GET /orders/{orderId}/items/{itemId} */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "/{id}/items/{pId}", method = RequestMethod.GET)
    @ApiOperation(value = "Get a description of an item in an Order", response = OrderItemDto.class)
    public OrderItemDto getOneItemFromOrder(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @PathVariable(value = "id") Long orderId,
            @PathVariable(value = "pId") Long productId) {

        Customer currentCustomer = customerService.readCustomerById(customerUserDetails.getId());

        if(currentCustomer == null) {
            throw new CustomerNotFoundException();
        }

        Order cart = orderService.findCartForCustomer(currentCustomer);

        if(cart == null || cart.getId() != orderId) {
            throw new ResourceNotFoundException("Cannot find an order with ID: " + orderId);
        }


        OrderItemDto orderItemDto = cart.getOrderItems().stream().
                filter(x -> x.getId() == productId).limit(2).map(DtoConverters.orderItemEntityToDto)
                .collect(Collectors.toList()).get(0);

        return orderItemDto;
    }

    /* PUT /orders/items/{itemId} */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "/items/{pId}", method = RequestMethod.PUT)
    @ApiOperation(value = "Update an item in a cart", response = Void.class)
    public void updateItemInOrder(
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @PathVariable(value = "id") Long orderId,
            @PathVariable(value = "pId") Long productId,
            @RequestBody OrderItemDto orderItemDto) {

        Customer currentCustomer = customerService.readCustomerById(customerUserDetails.getId());

        if(currentCustomer == null) {
            throw new CustomerNotFoundException();
        }


        Order cart = orderService.findCartForCustomer(currentCustomer);

        if(cart == null || cart.getId() != orderId) {
            throw new ResourceNotFoundException("Cannot find an order with ID: " + orderId);
        }

        OrderItem orderItemEntityToUpdate = cart.getOrderItems().stream().
                filter(x -> x.getId() == productId).limit(2).collect(Collectors.toList()).get(0);

        if(orderItemEntityToUpdate == null) {
            throw new ResourceNotFoundException("Cannot find an item with ID: " + productId + " in order ID: " + orderId);
        }


        // TODO!

    }


    /* GET /orders/{id}/payments */
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
}