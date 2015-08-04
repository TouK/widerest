package pl.touk.widerest.api.cart.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.OrderItemService;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.order.service.call.OrderItemRequestDTO;
import org.broadleafcommerce.core.order.service.exception.AddToCartException;
import org.broadleafcommerce.core.order.service.exception.RemoveFromCartException;
import org.broadleafcommerce.core.order.service.exception.UpdateCartException;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.broadleafcommerce.core.pricing.service.exception.PricingException;
import org.broadleafcommerce.openadmin.server.security.service.AdminUserDetails;
import org.broadleafcommerce.profile.core.domain.Address;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.service.AddressService;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.cart.dto.AddressDto;
import pl.touk.widerest.api.cart.dto.DiscreteOrderItemDto;
import pl.touk.widerest.api.cart.dto.FulfillmentDto;
import pl.touk.widerest.api.cart.dto.OrderDto;
import pl.touk.widerest.api.cart.dto.OrderItemDto;
import pl.touk.widerest.api.cart.exceptions.CustomerNotFoundException;
import pl.touk.widerest.api.cart.exceptions.FulfillmentOptionNotAllowedException;
import pl.touk.widerest.api.cart.exceptions.OrderNotFoundException;
import pl.touk.widerest.api.cart.service.FulfilmentServiceProxy;
import pl.touk.widerest.api.cart.service.OrderServiceProxy;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;


/* TODO:
 * 1. Synchronization stuff
 *
 */
@RestController
@RequestMapping("/orders")
@Api(value = "orders", description = "Order management endpoint")
public class OrderController {

    @Resource(name = "blOrderService")
    protected OrderService orderService;

    @Resource(name="blCustomerService")
    private CustomerService customerService;

    @Resource(name="blCatalogService")
    protected CatalogService catalogService;

    @Resource(name = "blOrderItemService")
    protected OrderItemService orderItemService;

    @Resource(name = "wdOrderService")
    protected OrderServiceProxy orderServiceProxy;

    @Resource(name = "wdfulfilmentService")
    protected FulfilmentServiceProxy fulfillmentServiceProxy;

    @Resource(name = "blAddressService")
    private AddressService addressService;

    private final static String ANONYMOUS_CUSTOMER = "anonymous";

    /* GET /orders */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(
            value = "List all orders",
            notes = "Gets a list of all orders belonging to a currently authorized customer",
            response = OrderDto.class,
            responseContainer = "List")
    @ApiResponses(value =
    @ApiResponse(code = 200, message = "Successful retrieval of orders list")
    )
    public List<OrderDto> getOrders(@AuthenticationPrincipal UserDetails userDetails,
                                    @RequestParam(value="status", required=false) String status) {

        if(userDetails instanceof CustomerUserDetails) {
            CustomerUserDetails customerUserDetails = (CustomerUserDetails)userDetails;
            return orderServiceProxy.getOrdersByCustomer(customerUserDetails).stream()
                    .map(DtoConverters.orderEntityToDto)
                    .filter(x -> status == null || x.getStatus().equals(status))
                    .collect(Collectors.toList());
        } else if(userDetails instanceof AdminUserDetails) {
            return orderServiceProxy.getAllOrders().stream()
                    .map(DtoConverters.orderEntityToDto)
                    .collect(Collectors.toList());
        }

        // Shouldn't go there, but in case it was unauthorized return null
        return null;
    }

    /* GET /orders/{orderId} */
    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get an order by ID",
            notes = "Gets details of a single order, specified by its ID",
            response = OrderDto.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of order details", response = OrderDto.class),
            @ApiResponse(code = 404, message = "The specified order does not exist")
    })
    public OrderDto getOrderById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "id") Long orderId) {

        return DtoConverters.orderEntityToDto.apply(getProperCart(userDetails, orderId));

    }

    /* POST /orders */
    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation(
            value = "Create a new order",
            notes = "Adds a new order. A single, authorized customer can have multiple orders. " +
                    "Returns an URL to the newly created order in the Location field of the HTTP response header",
            response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A new order entry successfully created")
    })
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

        return new ResponseEntity<>(responseHeader, HttpStatus.CREATED);
    }

    /* DELETE /orders/ */
    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Delete an order",
            notes = "Removes a specific order from customer's orders list",
            response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful removal of the specified order"),
            @ApiResponse(code = 404, message = "The specified order does not exist")
    })
    public void deleteOrderForCustomer(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "orderId") Long orderId) {

        //orderService.cancelOrder(orders.get(0));
        if(userDetails instanceof CustomerUserDetails) {
            orderService.deleteOrder(getOrderForCustomerById((CustomerUserDetails)userDetails, orderId));
        } else if(userDetails instanceof AdminUserDetails) {
            orderService.deleteOrder(orderService.findOrderById(orderId));
        }
    }


    /* POST /orders/items */
    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/items", method = RequestMethod.POST)
    @ApiOperation(
            value = "Add a new item",
            notes = "Adds a new item to the specified order",
            response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Specified product successfully added"),
            @ApiResponse(code = 404, message = "The specified order does not exist")
    })
    public ResponseEntity<?> addProductToOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody OrderItemDto orderItemDto,
            @PathVariable(value = "orderId") Long orderId) throws PricingException, AddToCartException {

        Order cart = Optional.ofNullable(getProperCart(userDetails, orderId))
                .orElseThrow(ResourceNotFoundException::new);

        Sku productsSku = catalogService.findSkuById(orderItemDto.getSkuId());

        if(productsSku == null) {
            throw new ResourceNotFoundException("Invalid Sku Id: does not exist in database");
        }


        OrderItemRequestDTO req = new OrderItemRequestDTO();

        req.setQuantity(orderItemDto.getQuantity());
        req.setSkuId(orderItemDto.getSkuId());

        req.setProductId(productsSku.getProduct().getId());

        if(orderItemDto.getAttributes() != null) {

        }


        orderService.addItem(cart.getId(), req, true);
        cart.calculateSubTotal();
        cart = orderService.save(cart, false);


        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setLocation(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{orderId}/items/{id}")
                .buildAndExpand(orderId,
                        (cart.getDiscreteOrderItems().stream()
                                .filter(x -> x.getSku().getId() == orderItemDto.getSkuId())
                                .findAny()
                                .map(DiscreteOrderItem::getId)
                                .orElseThrow(ResourceNotFoundException::new))
                )
                .toUri());

        return new ResponseEntity<>(null, responseHeaders, HttpStatus.CREATED);

    }

    /* GET /orders/items/ */
    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/items", method = RequestMethod.GET)
    @ApiOperation(
            value = "List all items in an order",
            notes = "Gets a list of all items belonging to a specified order ",
            response = DiscreteOrderItemDto.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successful retrieval of all items in a given category"),
            @ApiResponse(code = 404, message = "The specified order does not exist")
    })
    public List<DiscreteOrderItemDto> getAllItemsInOrder (
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "orderId") Long orderId) {

        if(userDetails instanceof CustomerUserDetails) {
            return Optional.ofNullable(getOrderForCustomerById((CustomerUserDetails) userDetails, orderId))
                    .orElseThrow(ResourceNotFoundException::new)
                    .getDiscreteOrderItems().stream()
                    .map(DtoConverters.discreteOrderItemEntityToDto)
                    .collect(Collectors.toList());

        } else if(userDetails instanceof AdminUserDetails) {
            return Optional.ofNullable(orderService.findOrderById(orderId)).orElseThrow(ResourceNotFoundException::new)
                    .getDiscreteOrderItems().stream()
                    .map(DtoConverters.discreteOrderItemEntityToDto)
                    .collect(Collectors.toList());
        }

        return null;
    }

    /* GET /orders/{orderId}/items/count */
    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{id}/items/count", method = RequestMethod.GET)
    @ApiOperation(
            value = "Count all items in the order",
            notes = "Gets a number of all items placed already in the specified order",
            response = Integer.class)
    public Integer getItemsCountByOrderId (
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "id") Long orderId) {

        if(userDetails instanceof CustomerUserDetails) {
            return Optional.ofNullable(getOrderForCustomerById((CustomerUserDetails) userDetails, orderId))
                    .orElseThrow(ResourceNotFoundException::new)
                    .getItemCount();

        } else if(userDetails instanceof AdminUserDetails) {
            return Optional.ofNullable(orderService.findOrderById(orderId))
                    .orElseThrow(ResourceNotFoundException::new)
                    .getItemCount();

        }
        return null;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/count", method = RequestMethod.GET)
    @ApiOperation(
            value = "Count all orders",
            notes = "Get a number of all active orders",
            response = Integer.class)
    public Integer getOrdersCount(
            @AuthenticationPrincipal UserDetails userDetails) {

        if(userDetails instanceof CustomerUserDetails) {
            return Optional.ofNullable(orderServiceProxy.getOrdersByCustomer((CustomerUserDetails) userDetails))
                    .orElseThrow(ResourceNotFoundException::new)
                    .size();
        } else if(userDetails instanceof AdminUserDetails) {
            return Optional.ofNullable(orderServiceProxy.getAllOrders())
                    .orElseThrow(ResourceNotFoundException::new)
                    .size();
        }

        return null;
    }

    /* GET /orders/{orderId}/status */
    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{id}/status", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get a status of an order",
            notes = "Gets a current status of a specified order",
            response = OrderStatus.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successful retrieval of order status"),
            @ApiResponse(code = 404, message = "The specified order does not exist")
    })
    public OrderStatus getOrderStatusById (
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "id") Long orderId) {
        if(userDetails instanceof CustomerUserDetails) {
            return Optional.ofNullable(getOrderForCustomerById((CustomerUserDetails) userDetails, orderId))
                    .orElseThrow(ResourceNotFoundException::new)
                    .getStatus();
        } else if(userDetails instanceof AdminUserDetails) {
            return Optional.ofNullable(orderService.findOrderById(orderId))
                    .orElseThrow(ResourceNotFoundException::new)
                    .getStatus();
        }

        return null;
    }

    /* DELETE /orders/{orderId}/items/{itemId} */
    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/items/{itemId}", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Delete an item",
            notes = "Removes an item from a specified order",
            response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successful removal of the specified item"),
            @ApiResponse(code = 404, message = "The specified order does not exist")
    })
    public void removeItemFromOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "itemId") Long itemId,
            @PathVariable(value = "orderId") Long orderId) {

        Order cart = Optional.ofNullable(getProperCart(userDetails, orderId))
                .orElseThrow(ResourceNotFoundException::new);

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
    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/items/{itemId}", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get details of an item",
            notes = "Gets a description of a specified item in a given order",
            response = DiscreteOrderItemDto.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successful retrieval of item details", response = DiscreteOrderItemDto.class),
            @ApiResponse(code = 404, message = "The specified order or item does not exist")
    })
    public DiscreteOrderItemDto getOneItemFromOrder (
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "itemId") Long itemId,
            @PathVariable(value = "orderId") Long orderId) {


        return Optional.ofNullable(getProperCart(userDetails, orderId))
                .orElseThrow(ResourceNotFoundException::new)
                .getDiscreteOrderItems().stream()
                .filter(x -> x.getId() == itemId).findAny()
                .map(DtoConverters.discreteOrderItemEntityToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot find the item in card with ID: " + itemId));

    }

    /* PUT /orders/items/{itemId}/quantity */
    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/items/{itemId}/quantity", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Update item's quantity",
            notes = "Updates quantity of a specific item placed in order",
            response = DiscreteOrderItemDto.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Quantity number successfully updated"),
            @ApiResponse(code = 404, message = "The specified order or item does not exist"),
            @ApiResponse(code = 409, message = "Wrong quantity value")
    })
    public ResponseEntity<?> updateItemQuantityInOrder (
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "itemId") Long itemId,
            @PathVariable(value = "orderId") Long orderId,
            @RequestBody Integer quantity) throws UpdateCartException, RemoveFromCartException {


        if(quantity <= 0) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        Order cart = Optional.ofNullable(getProperCart(userDetails, orderId))
                .orElseThrow(ResourceNotFoundException::new);

        if(cart.getDiscreteOrderItems().stream().filter(x -> x.getId() == itemId).count() != 1) {
            throw new ResourceNotFoundException("Cannot find an item with ID: " + itemId);
        }


        OrderItemRequestDTO orderItemRequestDto = new OrderItemRequestDTO();
        orderItemRequestDto.setQuantity(quantity);
        orderItemRequestDto.setOrderItemId(itemId);
    	
        /* TODO: (mst) do we need to recalculate the price here? */
        orderService.updateItemQuantity(orderId, orderItemRequestDto, true);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /* PUT /orders/{orderId}/fulfillment/selectedOption */
    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/fulfillment/selectedOption", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Select fulfillment option",
            notes = "Updates the selected fulfillment option of the specified order",
            response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Fulfillment Option successfully selected/updated"),
            @ApiResponse(code = 404, message = "The specified order or item does not exist"),
            @ApiResponse(code = 409, message = "The cart is empty or selected Fulfillment Option value does not exist")
    })
    public ResponseEntity<?> updateSelectedFulfillmentOption (
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "orderId") Long orderId,
            @RequestBody Long fulfillmentOptionId) throws PricingException {

    	/* TODO: (mst) fulfillmentOptionId validation */

        Order order = Optional.ofNullable(getProperCart(userDetails, orderId))
                .orElseThrow(ResourceNotFoundException::new);

        if(order.getItemCount() <= 0) {
            throw new FulfillmentOptionNotAllowedException("Order with ID: " + orderId + " is empty");
        }

        fulfillmentServiceProxy.updateFulfillmentOption(order, fulfillmentOptionId);

        return new ResponseEntity<>(HttpStatus.OK);
    }



    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/fulfillment", method = RequestMethod.GET)
    public FulfillmentDto getOrderFulfilment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "orderId") Long orderId) {

        // DtoConverters d = new DtoConverters();

        return fulfillmentServiceProxy.createFulfillmentDto.apply(getProperCart(userDetails, orderId));


    }


    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/fulfillment/address", method = RequestMethod.POST)
    public void setOrderFulfilmentAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "orderId") Long orderId,
            @RequestBody AddressDto addressDto) throws PricingException {

        Order order = Optional.ofNullable(getProperCart(userDetails, orderId))
                .orElseThrow(ResourceNotFoundException::new);


        Address shippingAddress = addressService.create();
        shippingAddress.setFirstName(addressDto.getFirstName());
        shippingAddress.setLastName(addressDto.getLastName());
        shippingAddress.setCity(addressDto.getCity());
        shippingAddress.setPostalCode(addressDto.getPostalCode());
        shippingAddress.setCompanyName(addressDto.getCompanyName());
        shippingAddress.setAddressLine1(addressDto.getAddressLine1());
       
       /* TODO: (mst) Country! */

        fulfillmentServiceProxy.updateFulfillmentAddress(order, shippingAddress);

    }

    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/fulfillment/address", method = RequestMethod.GET)
    public AddressDto getOrderFulfilmentAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "orderId") Long orderId) {

        Order order = Optional.ofNullable(getProperCart(userDetails, orderId))
                .orElseThrow(ResourceNotFoundException::new);

        return Optional.ofNullable(fulfillmentServiceProxy.getFulfillmentAddress(order))
                .map(DtoConverters.addressEntityToDto)
                .get();
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

    private Order getProperCart(UserDetails userDetails, Long orderId) {
        Order cart = null;

        if(userDetails instanceof CustomerUserDetails) {
            cart = getOrderForCustomerById((CustomerUserDetails) userDetails, orderId);
        } else if(userDetails instanceof AdminUserDetails) {
            cart = orderService.findOrderById(orderId);
        }

        return cart;
    }

    private Order getOrderForCustomerById(CustomerUserDetails customerUserDetails, Long orderId) throws OrderNotFoundException {

        return Optional.ofNullable(orderServiceProxy.getOrdersByCustomer(customerUserDetails))
                .orElseThrow(() -> new OrderNotFoundException("Cannot find order with ID: " + orderId + " for customer with ID: " + customerUserDetails.getId()))
                .stream()
                .filter(x -> x.getId().equals(orderId))
                .findAny()
                .orElseThrow(() -> new OrderNotFoundException("Cannot find order with ID: " + orderId + " for customer with ID: " + customerUserDetails.getId()));
    }
}