package pl.touk.widerest.api.orders;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javaslang.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationServiceProvider;
import org.broadleafcommerce.common.service.GenericEntityService;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.domain.ProductBundle;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderAttribute;
import org.broadleafcommerce.core.order.domain.OrderAttributeImpl;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.service.FulfillmentGroupService;
import org.broadleafcommerce.core.order.service.OrderItemService;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.order.service.call.OrderItemRequestDTO;
import org.broadleafcommerce.core.order.service.exception.AddToCartException;
import org.broadleafcommerce.core.order.service.exception.RemoveFromCartException;
import org.broadleafcommerce.core.order.service.exception.UpdateCartException;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.broadleafcommerce.core.pricing.service.exception.PricingException;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import pl.touk.widerest.api.RequestUtils;
import pl.touk.widerest.api.common.CatalogUtils;
import pl.touk.widerest.api.common.ResourceNotFoundException;
import pl.touk.widerest.security.authentication.AnonymousUserDetailsService;
import pl.touk.widerest.security.oauth2.ResourceServerConfig;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.Resource;
import javax.validation.constraints.Min;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/*
    TODO: (mst) Refactor, clean up, make exceptions more "expressive"
 */

@RestController
@RequestMapping(value = ResourceServerConfig.API_PATH + "/orders", produces = { MediaTypes.HAL_JSON_VALUE })
@Api(value = "orders", description = "Order management endpoint", produces = MediaTypes.HAL_JSON_VALUE)
@Slf4j
public class OrderController {

    @Resource(name = "blOrderService")
    protected OrderService orderService;

    @Resource(name = "blCustomerService")
    private CustomerService customerService;

    @Resource(name = "blCatalogService")
    protected CatalogService catalogService;

    @Resource(name = "blOrderItemService")
    protected OrderItemService orderItemService;

    @Resource
    protected OrderServiceProxy orderServiceProxy;

    @Resource(name = "blPaymentGatewayConfigurationServiceProvider")
    private PaymentGatewayConfigurationServiceProvider paymentGatewayConfigurationServiceProvider;

    @Resource(name = "blGenericEntityService")
    protected GenericEntityService genericEntityService;

    @Resource(name = "blFulfillmentGroupService")
    protected FulfillmentGroupService fulfillmentGroupService;

    @Resource
    private AnonymousUserDetailsService anonymousUserDetailsService;

    @Resource
    private OrderConverter orderConverter;

    @Resource
    private DiscreteOrderItemConverter discreteOrderItemConverter;

    @Value("${automatically.merge.like.items}")
    protected boolean automaticallyMergeLikeItems;

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
    public Resources<OrderDto> getOrders(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "Status to be used to filter orders")
            @RequestParam(value = "link", defaultValue = "true") Boolean link,
            @RequestParam(value = "status", required = false) String status
    ) {

        return new Resources<>(
                orderServiceProxy.getOrdersByCustomer(userDetails).stream()
                        .map(order -> orderConverter.createDto(order, false, link))
                        .filter(x -> status == null || x.getStatus().equals(status))
                        .collect(toList()),

                linkTo(methodOn(getClass()).getOrders(null, null, status)).withSelfRel()
        );
    }

    /* GET /orders/{orderId} */
    @Transactional
    @PreAuthorize("hasAnyAuthority('PERMISSION_ALL_ORDER', 'ROLE_USER')")
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
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "id") Long orderId,
            @RequestParam(value = "embed", defaultValue = "false") Boolean embed,
            @RequestParam(value = "link", defaultValue = "true") Boolean link
    ) {

        return orderConverter.createDto(orderServiceProxy.getProperCart(userDetails, orderId).orElse(null), embed, link);
        //return DtoConverters.orderEntityToDto.apply(orderServiceProxy.getProperCart(userDetails, orderId).orElse(null));
    }

    /* POST /orders */
    @Transactional
    @PreAuthorize("hasAnyAuthority('PERMISSION_ALL_ORDER', 'ROLE_USER')")
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
            @ApiIgnore @AuthenticationPrincipal CustomerUserDetails customerUserDetails) throws PricingException {

        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();

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

        orderService.save(cart, true);

        return ResponseEntity.created(
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(cart.getId())
                        .toUri()
        ).build();
    }

    /* DELETE /orders/ */
    @Transactional
    @PreAuthorize("hasAnyAuthority('PERMISSION_ALL_ORDER', 'ROLE_USER')")
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
    @PreAuthorize("hasAnyAuthority('PERMISSION_ALL_ORDER', 'ROLE_USER')")
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
    public ResponseEntity<?> addProductToOrderByProductOptions(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "orderId") Long orderId,
            @ApiParam(value = "Description of a new order item", required = true)
            @RequestBody OrderItemDto orderItemDto) throws PricingException, AddToCartException {

        Order cart = orderServiceProxy.getProperCart(userDetails, orderId).orElseThrow(ResourceNotFoundException::new);

        long hrefProductId;

        hrefProductId = CatalogUtils.getIdFromUrl(orderItemDto.getProductHref());

        final OrderItemRequestDTO orderItemRequestDTO = new OrderItemRequestDTO();
        orderItemRequestDTO.setQuantity(orderItemDto.getQuantity());
        orderItemRequestDTO.setProductId(hrefProductId);

        if(orderItemDto.getSelectedOptions() != null) {
            orderItemRequestDTO.getItemAttributes().putAll(removeNullValues(orderItemDto.getSelectedOptions()));
        }

        final List<DiscreteOrderItem> currentDiscreteItems = cart.getDiscreteOrderItems();

        orderService.addItem(cart.getId(), orderItemRequestDTO, true);
        cart.calculateSubTotal();
        cart = orderService.save(cart, false);

        // (mst) Figure out added item's ID
        DiscreteOrderItem addedDiscreteOrderItem = null;

        if(automaticallyMergeLikeItems) {
            addedDiscreteOrderItem = cart.getDiscreteOrderItems().stream()
                    .filter(x -> x.getSku().getId().longValue() == orderItemRequestDTO.getSkuId())
                    .findAny()
                    .orElseThrow(ResourceNotFoundException::new);
        } else {
            final List<DiscreteOrderItem> newDiscreteItemsIds = cart.getDiscreteOrderItems().stream()
                    .filter(item -> !currentDiscreteItems.contains(item))
                    .collect(Collectors.toList());

            if(newDiscreteItemsIds.size() == 1) {
                addedDiscreteOrderItem = newDiscreteItemsIds.get(0);
            }
        }

        if(addedDiscreteOrderItem != null) {
            return ResponseEntity.created(
                    ServletUriComponentsBuilder.fromCurrentRequest()
                            .path("/{id}")
                            .buildAndExpand(addedDiscreteOrderItem.getId())
                            .toUri()
            ).body(discreteOrderItemConverter.createDto(addedDiscreteOrderItem, false, true));
        } else {
            return ResponseEntity.ok().build();
        }
    }

    private Map<String,String> removeNullValues(final Map<String, String> selectedOptions) {
        return selectedOptions.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /* POST /orders/{orderId}/items */
    @Transactional
    @PreAuthorize("hasAnyAuthority('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/items-old", method = RequestMethod.POST)
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

            skuId = CatalogUtils.getIdFromUrl(orderItemDto.getSkuHref());
            req.setSkuId(skuId);
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

        final List<DiscreteOrderItem> currentDiscreteItems = cart.getDiscreteOrderItems();

        orderService.addItem(cart.getId(), req, true);
        // Possible improvement: calculate subtotal 'lazily' (i.e. just before checking out)
        cart.calculateSubTotal();
        cart = orderService.save(cart, false);

        // (mst) Figure out added item's ID
        long addedItemId;

        if(automaticallyMergeLikeItems) {
            addedItemId = cart.getDiscreteOrderItems().stream()
                                .filter(x -> x.getSku().getId().longValue() == req.getSkuId())
                                .findAny()
                                .map(DiscreteOrderItem::getId)
                                .orElseThrow(ResourceNotFoundException::new);
        } else {
            final List<DiscreteOrderItem> newDiscreteItemsIds = cart.getDiscreteOrderItems().stream()
                    .filter(item -> !currentDiscreteItems.contains(item))
                    .collect(Collectors.toList());

            if(newDiscreteItemsIds.size() != 1) {
                throw new ResourceNotFoundException();
            }

            addedItemId = newDiscreteItemsIds.get(0).getId();
        }

        UriComponentsBuilder uriComponentsBuilder =
                ServletUriComponentsBuilder.fromCurrentRequestUri().replacePath(
                        ServletUriComponentsBuilder.fromCurrentRequest().build().getPath().replace("-old","")
                );

        UriComponents uriComponents = isBundleBeingAdded ?
                uriComponentsBuilder.build()
                : uriComponentsBuilder
                .path("/{id}")
                .buildAndExpand(addedItemId);

        return ResponseEntity.created(uriComponents.toUri()).build();
    }

    /* GET /orders/items/ */
    @Transactional
    @PreAuthorize("hasAnyAuthority('PERMISSION_ALL_ORDER', 'ROLE_USER')")
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
    public Resources<DiscreteOrderItemDto> getAllItemsInOrder(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "orderId") Long orderId,
            @RequestParam(value = "embed", defaultValue = "false") Boolean embed,
            @RequestParam(value = "link", defaultValue = "true") Boolean link
    ) {

        return new Resources<>(
                orderServiceProxy.getDiscreteOrderItemsFromProperCart(userDetails, orderId).stream()
                .map(discreteOrderItem -> discreteOrderItemConverter.createDto(discreteOrderItem, embed, link))
                .collect(toList())
        );

    }

    /* GET /orders/{orderId}/items/count */
    @Transactional
    @PreAuthorize("hasAnyAuthority('PERMISSION_ALL_ORDER', 'ROLE_USER')")
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
    @PreAuthorize("hasAnyAuthority('PERMISSION_ALL_ORDER', 'ROLE_USER')")
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
    @PreAuthorize("hasAnyAuthority('PERMISSION_ALL_ORDER', 'ROLE_USER')")
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
    @PreAuthorize("hasAnyAuthority('PERMISSION_ALL_ORDER', 'ROLE_USER')")
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

        Try.of(() -> orderService.save(orderService.removeItem(cart.getId(), itemId, true), true))
                .getOrElseThrow(() -> new ResourceNotFoundException("Error while removing item with ID: " + itemId));
    }

    /* GET /orders/items/{itemId} */
    @Transactional
    @PreAuthorize("hasAnyAuthority('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/items/{itemId}", method = RequestMethod.GET)
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
            @PathVariable(value = "itemId") Long itemId,
            @RequestParam(value = "embed", defaultValue = "false") Boolean embed,
            @RequestParam(value = "link", defaultValue = "true") Boolean link
    ) {
        return orderServiceProxy.getProperCart(userDetails, orderId).orElseThrow(ResourceNotFoundException::new)
                .getDiscreteOrderItems().stream()
                .filter(x -> Objects.equals(x.getId(), itemId)).findAny()
                .map(discreteOrderItem -> discreteOrderItemConverter.createDto(discreteOrderItem, embed, link))
                .orElseThrow(() -> new ResourceNotFoundException("Cannot find the item in card with ID: " + itemId));

    }

    /* PUT /orders/items/{itemId}/quantity */
    @PreAuthorize("hasAnyAuthority('PERMISSION_ALL_ORDER', 'ROLE_USER')")
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
    public void updateItemQuantityInOrder(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "orderId") Long orderId,
            @ApiParam(value = "ID of a specific item in the order", required = true)
            @PathVariable(value = "itemId") Long itemId,
            @ApiParam(value = "Quantity value", required = true)
            @RequestBody @Min(0) int quantity) throws RemoveFromCartException, UpdateCartException {
        orderServiceProxy.updateItemQuantityInOrder(quantity,userDetails,orderId,itemId);
    }


//    //private static boolean notYetSubmitted(Order order) {
//        return !order.getStatus().equals(OrderStatus.SUBMITTED);
//    }

    private OrderItem getOrderItemByHref(final String orderItemHref, final Order order) throws MalformedURLException, ResourceNotFoundException {
        final long orderItemId = CatalogUtils.getIdFromUrl(orderItemHref);

        final OrderItem orderItemEntity = orderItemService.readOrderItemById(orderItemId);

        if(orderItemEntity != null && orderItemEntity.getOrder().equals(order)) {
            return orderItemEntity;
        } else {
            throw new ResourceNotFoundException("Order Item: " + orderItemId + " does not exist or is not related to order!");
        }
    }

}
