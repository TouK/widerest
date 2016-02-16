package pl.touk.widerest.api.cart.orders;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.i18n.service.ISOService;
import org.broadleafcommerce.common.payment.PaymentGatewayType;
import org.broadleafcommerce.common.payment.dto.PaymentRequestDTO;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationServiceProvider;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.domain.ProductBundle;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.inventory.service.InventoryService;
import org.broadleafcommerce.core.order.domain.BundleOrderItem;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderAttribute;
import org.broadleafcommerce.core.order.domain.OrderAttributeImpl;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.service.OrderItemService;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.order.service.call.OrderItemRequestDTO;
import org.broadleafcommerce.core.order.service.exception.AddToCartException;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.broadleafcommerce.core.payment.service.OrderToPaymentRequestDTOService;
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

import com.jasongoodwin.monads.Try;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javaslang.control.Match;
import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.RequestUtils;
import pl.touk.widerest.api.cart.customers.dto.AddressDto;
import pl.touk.widerest.api.cart.dto.DiscreteOrderItemDto;
import pl.touk.widerest.api.cart.dto.FulfillmentDto;
import pl.touk.widerest.api.cart.orders.dto.OrderDto;
import pl.touk.widerest.api.cart.dto.OrderItemDto;
import pl.touk.widerest.api.cart.dto.OrderItemOptionDto;
import pl.touk.widerest.api.cart.dto.PaymentDto;
import pl.touk.widerest.api.cart.exceptions.NotShippableException;
import pl.touk.widerest.api.cart.orders.converters.OrderConverter;
import pl.touk.widerest.api.cart.service.FulfilmentServiceProxy;
import pl.touk.widerest.api.cart.service.OrderServiceProxy;
import pl.touk.widerest.api.cart.service.OrderValidationService;
import pl.touk.widerest.api.catalog.CatalogUtils;
import pl.touk.widerest.api.catalog.exceptions.DtoValidationException;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;
import pl.touk.widerest.security.authentication.AnonymousUserDetailsService;
import pl.touk.widerest.security.config.ResourceServerConfig;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping(ResourceServerConfig.API_PATH + "/orders")
@Api(value = "orders", description = "Order management endpoint")
public class OrderController {

    private static final Log LOG = LogFactory.getLog(OrderController.class);

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

    @Resource(name = "wdOrderValidationService")
    private OrderValidationService orderValidationService;

    @Resource(name = "blInventoryService")
    private InventoryService inventoryService;

    @Resource(name = "blPaymentGatewayConfigurationServiceProvider")
    private PaymentGatewayConfigurationServiceProvider paymentGatewayConfigurationServiceProvider;

    @Resource(name = "blOrderToPaymentRequestDTOService")
    private OrderToPaymentRequestDTOService orderToPaymentRequestDTOService;

    @Resource
    private ISOService isoService;

    @Resource
    private AnonymousUserDetailsService anonymousUserDetailsService;

    @Resource
    private OrderConverter orderConverter;

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

        return orderServiceProxy.getOrdersByCustomer(userDetails).stream()
                .map(order -> orderConverter.createDto(order, false))
                .filter(x -> status == null || x.getStatus().equals(status))
                .collect(toList());
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

        return orderConverter.createDto(orderServiceProxy.getProperCart(userDetails, orderId).orElse(null), false);
        //return DtoConverters.orderEntityToDto.apply(orderServiceProxy.getProperCart(userDetails, orderId).orElse(null));
    }

    /* POST /orders */
    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
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

        final Customer currentCustomer = Optional.ofNullable(customerUserDetails)
                .map(CustomerUserDetails::getId)
                .map(customerService::readCustomerById)
                .orElse(anonymousUserDetailsService.createAnonymousCustomer());

        final Order cart = orderService.createNewCartForCustomer(currentCustomer);

        String channel = RequestUtils.getRequestChannel();
        if (StringUtils.isNotEmpty(channel)) {
            OrderAttribute channelAttribute = new OrderAttributeImpl();
            channelAttribute.setName("channel");
            channelAttribute.setValue(channel);
            channelAttribute.setOrder(cart);
            cart.getOrderAttributes().put(channelAttribute.getName(), channelAttribute);
            cart.setName(channel);
        }

        Try.ofFailable(() -> orderService.save(cart, true)).onFailure(LOG::error);

        return ResponseEntity.created(
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(cart.getId())
                        .toUri()
        ).build();
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
        orderService.deleteOrder(orderServiceProxy.getProperCart(userDetails, orderId).orElse(null));
    }

    /* POST /orders/{orderId}/items */
    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/itemsp", method = RequestMethod.POST)
    @ApiOperation(
            value = "Add a new item",
            notes = "Adds a new item to the specified order",
            response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Specified product successfully added"),
            @ApiResponse(code = 404, message = "The specified order does not exist"),
            @ApiResponse(code = 409, message = "Only one option: skuID or productBundleId can be selected at once")

    })
    public ResponseEntity<?> addProductToOrderByProductOptions(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "orderId") Long orderId,
            @ApiParam(value = "Description of a new order item", required = true)
            @RequestBody OrderItemDto orderItemDto) throws PricingException, AddToCartException {

        if(orderItemDto.getQuantity() == null || orderItemDto.getProductHref() == null || orderItemDto.getSelectedProductOptions() == null ||
                orderItemDto.getSelectedProductOptions().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Order cart = orderServiceProxy.getProperCart(userDetails, orderId).orElseThrow(ResourceNotFoundException::new);

        long hrefProductId;

        try {
            hrefProductId = CatalogUtils.getIdFromUrl(orderItemDto.getProductHref());
        } catch (MalformedURLException | NumberFormatException | DtoValidationException e) {
            return ResponseEntity.badRequest().build();
        }

        final OrderItemRequestDTO orderItemRequestDTO = new OrderItemRequestDTO();
        orderItemRequestDTO.setQuantity(orderItemDto.getQuantity());
        orderItemRequestDTO.setProductId(hrefProductId);

        if(orderItemDto.getSelectedProductOptions() != null && !orderItemDto.getSelectedProductOptions().isEmpty()) {
            orderItemRequestDTO.getItemAttributes().putAll(
                    orderItemDto.getSelectedProductOptions().stream()
                        .collect(toMap(OrderItemOptionDto::getOptionName, OrderItemOptionDto::getOptionValue)));
        }

        orderService.addItem(cart.getId(), orderItemRequestDTO, true);
        cart.calculateSubTotal();
        cart = orderService.save(cart, false);

        return ResponseEntity.created(
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(
                                (cart.getDiscreteOrderItems().stream()
                                        .filter(x -> x.getProduct().getId().longValue() == orderItemRequestDTO.getProductId())
                                        .findAny()
                                        .map(DiscreteOrderItem::getId)
                                        .orElseThrow(ResourceNotFoundException::new))
                        )
                        .toUri()
        ).build();
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

        if (orderItemDto.getSkuId() != null && orderItemDto.getBundleProductId() != null || orderItemDto.getQuantity() <= 0) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }


        Order cart = orderServiceProxy.getProperCart(userDetails, orderId)
                .orElseThrow(ResourceNotFoundException::new);

        final OrderItemRequestDTO req = new OrderItemRequestDTO();
        req.setQuantity(orderItemDto.getQuantity());


        if(orderItemDto.getSkuHref() != null && !orderItemDto.getSkuHref().isEmpty()) {

            long skuId;

            try {
                skuId = CatalogUtils.getIdFromUrl(orderItemDto.getSkuHref());
                req.setSkuId(skuId);
            } catch (MalformedURLException | NumberFormatException | DtoValidationException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        else if (orderItemDto.getSkuId() != null) {
            Optional.ofNullable(catalogService.findSkuById(orderItemDto.getSkuId()))
                    .orElseThrow(() -> new ResourceNotFoundException("SKU with ID: " + orderItemDto.getSkuId() + " does not exist"));

            req.setSkuId(orderItemDto.getSkuId());
        } else if (orderItemDto.getBundleProductId() != null) {

            long bundleProductId = orderItemDto.getBundleProductId();

            final Product bundleProduct = Optional.ofNullable(catalogService.findProductById(bundleProductId))
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


        if (!isBundleBeingAdded) {
            return ResponseEntity.created(
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(
                                (cart.getDiscreteOrderItems().stream()
                                        .filter(x -> x.getSku().getId().longValue() == req.getSkuId())
                                        .findAny()
                                        .map(DiscreteOrderItem::getId)
                                        .orElseThrow(ResourceNotFoundException::new))
                        )
                        .toUri()
            ).build();
        } else {
            return ResponseEntity.created(
                ServletUriComponentsBuilder.fromCurrentRequest()
                    .build()
                    .toUri()
            ).build();
        }
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
                .collect(toList());

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

        final int itemsInOrderCount = orderServiceProxy.getProperCart(userDetails, orderId)
                .orElseThrow(ResourceNotFoundException::new)
                .getItemCount();

        return ResponseEntity.ok(itemsInOrderCount);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/count", method = RequestMethod.GET)
    @ApiOperation(
            value = "Count all orders",
            notes = "Get a number of all active orders",
            response = Integer.class)
    public ResponseEntity<String> getOrdersCount(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails) {

        final String ordersCount = Long.toString(orderServiceProxy.getOrdersByCustomer(userDetails).stream().count());

        return ResponseEntity.ok(ordersCount);
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

        return orderServiceProxy.getProperCart(userDetails, orderId)
                .map(Order::getStatus)
                .orElseThrow(ResourceNotFoundException::new);

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

        final Order cart = orderServiceProxy.getProperCart(userDetails, orderId).orElseThrow(ResourceNotFoundException::new);

        if (cart.getDiscreteOrderItems().stream().filter(x -> Objects.equals(x.getId(), itemId)).count() != 1) {
            throw new ResourceNotFoundException("Cannot find an item with ID: " + itemId);
        }

        Try.ofFailable(() -> orderService.save(orderService.removeItem(cart.getId(), itemId, true), true))
                .toOptional()
                .orElseThrow(() -> new ResourceNotFoundException("Error while removing item with ID: " + itemId));
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


        return orderServiceProxy.getProperCart(userDetails, orderId).orElseThrow(ResourceNotFoundException::new)
                .getDiscreteOrderItems().stream()
                .filter(x -> Objects.equals(x.getId(), itemId)).findAny()
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
        if(quantity <= 0) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

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
            @RequestBody long fulfillmentOptionId) throws PricingException {
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

        return fulfillmentServiceProxy.createFulfillmentDto(orderServiceProxy.getProperCart(userDetails, orderId).orElse(null));
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

        final Order order = orderServiceProxy.getProperCart(userDetails, orderId).orElseThrow(ResourceNotFoundException::new);

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
        shippingAddress.setIsoCountryAlpha2(isoService.findISOCountryByAlpha2Code(addressDto.getCountryCode()));
        shippingAddress.setIsoCountrySubdivision(addressDto.getCountrySubdivisionCode());

        fulfillmentServiceProxy.updateFulfillmentAddress(order, shippingAddress);

        return ResponseEntity.created(
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .build()
                        .toUri()
        ).build();
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

    @PreAuthorize("hasRole('ROLE_USER')")
    @RequestMapping(value = "/{orderId}/payment", method = RequestMethod.POST)
    @ApiOperation(
            value = "Initiate order payment execution using the given payment provider",
            notes = "Initiates for one chosen order",
            response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Redirects to checkout website"),
            @ApiResponse(code = 403, message = "Access denied to given order")
            // and throws
    })
    @Transactional
    public ResponseEntity initiatePayment(
            @RequestBody PaymentDto paymentDto,
            @ApiIgnore @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @PathVariable(value = "orderId") Long orderId
    ) throws PaymentException {

        final Order order = Optional.ofNullable(orderService.findOrderById(orderId))
                .filter(OrderController::notYetSubmitted)
                .orElseThrow(() -> new org.apache.velocity.exception.ResourceNotFoundException(""));

        if(!order.getCustomer().getId().equals(customerUserDetails.getId())) {
            throw new IllegalAccessError("Access Denied");
        }

        orderValidationService.validateOrderBeforeCheckout(order);

        PaymentRequestDTO paymentRequestDTO =
                orderToPaymentRequestDTOService.translateOrder(order)
                        .additionalField("SUCCESS_URL", paymentDto.getSuccessUrl())
                        .additionalField("CANCEL_URL", paymentDto.getCancelUrl())
                        .additionalField("FAILURE_URL", paymentDto.getFailureUrl());

        paymentRequestDTO = populateLineItemsAndSubscriptions(order, paymentRequestDTO);

        final PaymentGatewayConfigurationService configurationService =
                paymentGatewayConfigurationServiceProvider.getGatewayConfigurationService(
                        PaymentGatewayType.getInstance(String.valueOf(paymentDto.getProvider()))
                );

        final PaymentResponseDTO paymentResponse = configurationService.getHostedService().requestHostedEndpoint
                (paymentRequestDTO);

        //return redirect URI from the paymentResponse
        final String redirectURI = Optional.ofNullable(paymentResponse.getResponseMap().get("REDIRECT_URL"))
                .orElseThrow(() -> new org.apache.velocity.exception.ResourceNotFoundException(""));

        return ResponseEntity.created(URI.create(redirectURI)).build();
    }

    private PaymentRequestDTO populateLineItemsAndSubscriptions(final Order order, PaymentRequestDTO
            paymentRequest) {
        for (OrderItem item : order.getOrderItems()) {

            /* (mst) Previously, there was SKU's Description used here to set item's name
                    but because it is not required in our implementation, I chose to use SKU's Name instead */

            final String name = Match.of(item)
                    .whenType(BundleOrderItem.class).then(it -> it.getSku().getName())
                    .whenType(DiscreteOrderItem.class).then(it -> it.getSku().getName())
                    .otherwise(OrderItem::getName).get();

            final String category = Optional.ofNullable(item.getCategory())
                    .map(Category::getName)
                    .orElse(null);

            paymentRequest = paymentRequest.lineItem()
                    .name(name)
                    .amount(String.valueOf(item.getAveragePrice()))
                    .category(category)
                    .quantity(String.valueOf(item.getQuantity()))
                    .total(order.getTotal().toString())
                    .done();
        }

        return paymentRequest;
    }


    private static boolean notYetSubmitted(Order order) {
        return !order.getStatus().equals(OrderStatus.SUBMITTED);
    }
}
