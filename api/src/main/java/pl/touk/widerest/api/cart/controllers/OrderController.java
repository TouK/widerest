package pl.touk.widerest.api.cart.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.broadleafcommerce.common.i18n.service.ISOService;
import org.broadleafcommerce.common.locale.service.LocaleService;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.domain.ProductBundle;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.inventory.service.InventoryService;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.OrderItemService;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.order.service.call.OrderItemRequestDTO;
import org.broadleafcommerce.core.order.service.exception.AddToCartException;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.broadleafcommerce.core.pricing.service.exception.PricingException;
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
import pl.touk.widerest.api.cart.exceptions.NotShippableException;
import pl.touk.widerest.api.cart.service.FulfilmentServiceProxy;
import pl.touk.widerest.api.cart.service.OrderServiceProxy;
import pl.touk.widerest.api.cart.service.OrderValidationService;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;
import pl.touk.widerest.security.config.ResourceServerConfig;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(ResourceServerConfig.API_PATH + "/orders")
@Api(value = "orders", description = "Order management endpoint")
public class OrderController {

    @Resource(name = "blOrderService")
    protected OrderService orderService;

    @Resource(name = "blCustomerService")
    private CustomerService customerService;

    @Resource(name = "blCatalogService")
    protected CatalogService catalogService;

    @Resource(name = "blOrderItemService")
    protected OrderItemService orderItemService;

    @Resource(name = "wdOrderService")
    protected OrderServiceProxy orderServiceProxy;

    @Resource(name = "wdfulfilmentService")
    protected FulfilmentServiceProxy fulfillmentServiceProxy;

    @Resource(name = "blAddressService")
    private AddressService addressService;

    @Resource(name = "blLocaleService")
    private LocaleService localeService;

    @Resource(name = "wdOrderValidationService")
    private OrderValidationService orderValidationService;

    @Resource(name = "blInventoryService")
    private InventoryService inventoryService;

    @Resource
    private ISOService isoService;

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
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of orders list", responseContainer = "List")
    })
    public List<OrderDto> getOrders(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "Status to be used to filter orders")
            @RequestParam(value = "status", required = false) String status) {

        return Optional.ofNullable(orderServiceProxy.getOrdersByCustomer(userDetails))
                .orElse(Collections.<Order>emptyList())
                    .stream()
                    .map(DtoConverters.orderEntityToDto)
                    .filter(x -> status == null || x.getStatus().equals(status))
                    .collect(Collectors.toList());
    }

    /* GET /orders/{orderId} */
    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(
            value = "Get an order by ID",
            notes = "Gets details of a single order, specified by its ID",
            response = OrderDto.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of order details", response = OrderDto.class),
            @ApiResponse(code = 404, message = "The specified order does not exist")
    })
    public OrderDto getOrderById(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "id") Long orderId) {

        return DtoConverters.orderEntityToDto.apply(orderServiceProxy.getProperCart(userDetails, orderId));

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
            @ApiIgnore @AuthenticationPrincipal CustomerUserDetails customerUserDetails) {

        Customer currentCustomer = Optional.ofNullable(customerService.readCustomerById(customerUserDetails.getId()))
                .orElseThrow(() -> new CustomerNotFoundException("Cannot find a customer with ID: " + customerUserDetails.getId()));


        Order cart = orderService.createNewCartForCustomer(currentCustomer);
        cart.setLocale(localeService.findDefaultLocale());

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
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "orderId") Long orderId) {

        //orderService.cancelOrder(orders.get(0));
        orderService.deleteOrder(orderServiceProxy.getProperCart(userDetails, orderId));
    }


    /* POST /orders/{orderId}/items */
    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/items", method = RequestMethod.POST)
    @ApiOperation(
            value = "Add a new item",
            notes = "Adds a new item to the specified order",
            response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Specified product successfully added"),
            @ApiResponse(code = 404, message = "The specified order does not exist"),
            @ApiResponse(code = 409, message = "Only one option: skuID or productBundleId can be selected at once")

    })
    public ResponseEntity<?> addProductToOrder(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "orderId") Long orderId,
            @ApiParam(value = "Description of a new order item", required = true)
            @RequestBody OrderItemDto orderItemDto) throws PricingException, AddToCartException {


        boolean isBundleBeingAdded = false;

        if (orderItemDto.getSkuId() != null && orderItemDto.getBundleProductId() != null) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        Order cart = Optional.ofNullable(orderServiceProxy.getProperCart(userDetails, orderId))
                .orElseThrow(ResourceNotFoundException::new);

        OrderItemRequestDTO req = new OrderItemRequestDTO();

        req.setQuantity(orderItemDto.getQuantity());

        if (orderItemDto.getSkuId() != null) {
            Optional.ofNullable(catalogService.findSkuById(orderItemDto.getSkuId()))
                    .orElseThrow(() -> new ResourceNotFoundException("SKU with ID: " + orderItemDto.getSkuId() + " does not exist"));
            req.setSkuId(orderItemDto.getSkuId());
        } else if (orderItemDto.getBundleProductId() != null) {

            long bundleProductId = orderItemDto.getBundleProductId();

            Product bundleProduct = Optional.ofNullable(catalogService.findProductById(bundleProductId))
                    .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + bundleProductId + " does not exist"));

            if (!(bundleProduct instanceof ProductBundle)) {
                throw new ResourceNotFoundException("Product with ID: " + bundleProductId + " is not a bundle");
            }
            req.setProductId(bundleProductId);
            isBundleBeingAdded = true;
        }

        orderService.addItem(cart.getId(), req, true);
        // Possible improvement: calculate subtotal 'lazily' (i.e. just before checking out)
        cart.calculateSubTotal();
        cart = orderService.save(cart, false);


        HttpHeaders responseHeaders = new HttpHeaders();

        if (!isBundleBeingAdded) {
            responseHeaders.setLocation(ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(
                            (cart.getDiscreteOrderItems().stream()
                                    .filter(x -> x.getSku().getId().longValue() == orderItemDto.getSkuId())
                                    .findAny()
                                    .map(DiscreteOrderItem::getId)
                                    .orElseThrow(ResourceNotFoundException::new))
                    )
                    .toUri());
        } else {
            responseHeaders.setLocation(ServletUriComponentsBuilder.fromCurrentRequest()
                    .build()
                    .toUri());
        }

        return new ResponseEntity<>(responseHeaders, HttpStatus.CREATED);
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
            @ApiResponse(code = 201, message = "Successful retrieval of all items in a given category", responseContainer = "List"),
            @ApiResponse(code = 404, message = "The specified order does not exist")
    })
    public List<DiscreteOrderItemDto> getAllItemsInOrder(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "orderId") Long orderId) {

        return orderServiceProxy.getDiscreteOrderItemsFromProperCart(userDetails, orderId).stream()
                .map(DtoConverters.discreteOrderItemEntityToDto)
                .collect(Collectors.toList());

    }

    /* GET /orders/{orderId}/items/count */
    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{id}/items/count", method = RequestMethod.GET)
    @ApiOperation(
            value = "Count all items in the order",
            notes = "Gets a number of all items placed already in the specified order",
            response = Integer.class)
    public ResponseEntity<Integer> getItemsCountByOrderId(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "id") Long orderId) {

        final int itemsInOrderCount = Optional.ofNullable(orderServiceProxy.getProperCart(userDetails, orderId))
                .orElseThrow(ResourceNotFoundException::new)
                .getItemCount();

        return new ResponseEntity<>(itemsInOrderCount, HttpStatus.OK);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/count", method = RequestMethod.GET)
    @ApiOperation(
            value = "Count all orders",
            notes = "Get a number of all active orders",
            response = Integer.class)
    public ResponseEntity<Integer> getOrdersCount(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails) {

        final int ordersCount = Optional.ofNullable(orderServiceProxy.getOrdersByCustomer(userDetails))
            .orElseThrow(ResourceNotFoundException::new)
            .size();

        return new ResponseEntity<>(ordersCount, HttpStatus.OK);
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
    public OrderStatus getOrderStatusById(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "id") Long orderId) {

        return Optional.ofNullable(orderServiceProxy.getProperCart(userDetails, orderId))
                .orElseThrow(ResourceNotFoundException::new)
                .getStatus();

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
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "orderId") Long orderId,
            @ApiParam(value = "ID of a specific item in the order", required = true)
            @PathVariable(value = "itemId") Long itemId) {

        Order cart = Optional.ofNullable(orderServiceProxy.getProperCart(userDetails, orderId))
                .orElseThrow(ResourceNotFoundException::new);

        if (cart.getDiscreteOrderItems().stream().filter(x -> x.getId() == itemId).count() != 1) {
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
    @RequestMapping(value = "/{orderId}/items/{itemId}", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(
            value = "Get details of an item",
            notes = "Gets a description of a specified item in a given order",
            response = DiscreteOrderItemDto.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successful retrieval of item details", response = DiscreteOrderItemDto.class),
            @ApiResponse(code = 404, message = "The specified order or item does not exist")
    })
    public DiscreteOrderItemDto getOneItemFromOrder(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "orderId") Long orderId,
            @ApiParam(value = "ID of a specific item in the order", required = true)
            @PathVariable(value = "itemId") Long itemId) {


        return Optional.ofNullable(orderServiceProxy.getProperCart(userDetails, orderId))
                .orElseThrow(ResourceNotFoundException::new)
                .getDiscreteOrderItems().stream()
                .filter(x -> x.getId() == itemId).findAny()
                .map(DtoConverters.discreteOrderItemEntityToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot find the item in card with ID: " + itemId));

    }

    /* PUT /orders/items/{itemId}/quantity */
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
    public ResponseEntity<?> updateItemQuantityInOrder(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "orderId") Long orderId,
            @ApiParam(value = "ID of a specific item in the order", required = true)
            @PathVariable(value = "itemId") Long itemId,
            @ApiParam(value = "Quantity value", required = true)
            @RequestBody int quantity)
    {
        try {
            return orderServiceProxy.updateItemQuantityInOrder(quantity,userDetails,orderId,itemId);
        } catch(Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /* PUT /orders/{orderId}/fulfillment/selectedOption */
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
    public ResponseEntity<?> updateSelectedFulfillmentOption(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "orderId") Long orderId,
            @ApiParam(value = "Fulfillment Option value", required = true)
            @RequestBody long fulfillmentOptionId) throws PricingException
    {
        return orderServiceProxy.updateSelectedFulfillmentOption(userDetails, orderId, fulfillmentOptionId);
    }


    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/fulfillment", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get a fulfillment for the order",
            notes = "Returns details of a current fulfillment for the specified order",
            response = FulfillmentDto.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of fulfillment details", response = FulfillmentDto.class),
            @ApiResponse(code = 404, message = "The specified order does not exist")
    })
    public FulfillmentDto getOrderFulfilment(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "orderId") Long orderId) {

        return fulfillmentServiceProxy.createFulfillmentDto(
                 orderServiceProxy.getProperCart(userDetails, orderId));
    }


    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/fulfillment/address", method = RequestMethod.POST)
    @ApiOperation(
            value = "Create fulfillment address",
            notes = "Adds an address for fulfillment for the specified order",
            response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Fulfillment address entry successfully created"),
            @ApiResponse(code = 400, message = "Not enough data has been provided"),
            @ApiResponse(code = 404, message = "The specified order does not exist")
    })
    public ResponseEntity<?> setOrderFulfilmentAddress(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "orderId") Long orderId,
            @ApiParam(value = "Description of a fulfillment address", required = true)
            @RequestBody AddressDto addressDto) throws PricingException {

        Order order = Optional.ofNullable(orderServiceProxy.getProperCart(userDetails, orderId))
                .orElseThrow(ResourceNotFoundException::new);

        if (order.getItemCount() <= 0) {
            throw new NotShippableException("Order with ID: " + orderId + " is empty");
        }

        orderValidationService.validateAddressDto(addressDto);

        Address shippingAddress = addressService.create();
        shippingAddress.setFirstName(addressDto.getFirstName());
        shippingAddress.setLastName(addressDto.getLastName());
        shippingAddress.setCity(addressDto.getCity());
        shippingAddress.setPostalCode(addressDto.getPostalCode());
        shippingAddress.setCompanyName(addressDto.getCompanyName());
        shippingAddress.setAddressLine1(addressDto.getAddressLine1());
        shippingAddress.setAddressLine2(addressDto.getAddressLine2());
        shippingAddress.setAddressLine3(addressDto.getAddressLine3());
        shippingAddress.setCounty(addressDto.getCounty());
        shippingAddress.setIsoCountryAlpha2(isoService.findISOCountryByAlpha2Code(addressDto.getCountryAbbreviation()));

        fulfillmentServiceProxy.updateFulfillmentAddress(order, shippingAddress);

        HttpHeaders responseHeader = new HttpHeaders();

        responseHeader.setLocation(ServletUriComponentsBuilder.fromCurrentRequest().build().toUri());

        return new ResponseEntity<>(responseHeader, HttpStatus.CREATED);
    }


    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/fulfillment/address", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get an address for fulfillment",
            notes = "Returns details of a fulfillment address for the specified order",
            response = AddressDto.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of fulfillment address", response = AddressDto.class),
            @ApiResponse(code = 404, message = "The specified order does not exist")
    })
    public AddressDto getOrderFulfilmentAddress(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "orderId") Long orderId) {

        return orderServiceProxy.getOrderFulfilmentAddress(userDetails, orderId);
    }

    

    /* GET /orders/{id}/payments */
    /*
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(value = "/{id}/payments", method = RequestMethod.GET)
    @ApiOperation(value = "Get a list of available payments for an order", response = List.class)
    public List<OrderPaymentDto> getPaymentsByOrderId(
            @ApiIgnore @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
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
}
