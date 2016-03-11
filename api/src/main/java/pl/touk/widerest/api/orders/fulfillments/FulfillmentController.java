package pl.touk.widerest.api.orders.fulfillments;

import com.jasongoodwin.monads.Try;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationServiceProvider;
import org.broadleafcommerce.common.service.GenericEntityService;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.order.domain.FulfillmentGroup;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.service.FulfillmentGroupService;
import org.broadleafcommerce.core.order.service.OrderItemService;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.order.service.call.FulfillmentGroupItemRequest;
import org.broadleafcommerce.core.payment.service.OrderToPaymentRequestDTOService;
import org.broadleafcommerce.core.pricing.service.exception.PricingException;
import org.broadleafcommerce.profile.core.service.AddressService;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.touk.widerest.api.common.AddressConverter;
import pl.touk.widerest.api.common.CatalogUtils;
import pl.touk.widerest.api.common.ResourceNotFoundException;
import pl.touk.widerest.api.orders.DiscreteOrderItemConverter;
import pl.touk.widerest.api.orders.OrderConverter;
import pl.touk.widerest.api.orders.OrderServiceProxy;
import pl.touk.widerest.api.orders.OrderValidationService;
import pl.touk.widerest.security.authentication.AnonymousUserDetailsService;
import pl.touk.widerest.security.config.ResourceServerConfig;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@RequestMapping(value = ResourceServerConfig.API_PATH + "/orders", produces = { MediaTypes.HAL_JSON_VALUE })
@Api(value = "orders", description = "Order management endpoint", produces = MediaTypes.HAL_JSON_VALUE)
@Slf4j
public class FulfillmentController {

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

    @Resource(name = "blPaymentGatewayConfigurationServiceProvider")
    private PaymentGatewayConfigurationServiceProvider paymentGatewayConfigurationServiceProvider;

    @Resource(name = "blOrderToPaymentRequestDTOService")
    private OrderToPaymentRequestDTOService orderToPaymentRequestDTOService;

    @Resource(name = "blGenericEntityService")
    protected GenericEntityService genericEntityService;

    @Resource(name = "blFulfillmentGroupService")
    protected FulfillmentGroupService fulfillmentGroupService;

    @Resource
    private AddressConverter addressConverter;

    @Resource
    private AnonymousUserDetailsService anonymousUserDetailsService;

    @Resource
    private OrderConverter orderConverter;

    @Resource
    private DiscreteOrderItemConverter discreteOrderItemConverter;

    @Resource
    private FulfillmentConverter fulfillmentConverter;


//    /* PUT /orders/{orderId}/fulfillment/selectedOption */
//    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
//    @RequestMapping(value = "/{orderId}/fulfillment/selectedOption", method = RequestMethod.PUT)
//    @ApiOperation(
//            value = "Select fulfillment option",
//            notes = "Updates the selected fulfillment option of the specified order",
//            response = Void.class)
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "Fulfillment Option successfully selected/updated"),
//            @ApiResponse(code = 404, message = "The specified order or item does not exist"),
//            @ApiResponse(code = 409, message = "The cart is empty or selected Fulfillment Option value does not exist")
//    })
//    public ResponseEntity<?> updateSelectedFulfillmentOption(
//            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
//            @ApiParam(value = "ID of a specific order", required = true)
//            @PathVariable(value = "orderId") Long orderId,
//            @ApiParam(value = "Fulfillment Option value", required = true)
//            @RequestBody long fulfillmentOptionId) throws PricingException {
//        return orderServiceProxy.updateSelectedFulfillmentOption(userDetails, orderId, fulfillmentOptionId);
//    }


//    @Transactional
//    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
//    @RequestMapping(value = "/{orderId}/fulfillment", method = RequestMethod.GET)
//    @ApiOperation(
//            value = "Get a fulfillment for the order",
//            notes = "Returns details of a current fulfillment for the specified order",
//            response = FulfillmentDto.class)
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "Successful retrieval of fulfillment details", response = FulfillmentDto.class),
//            @ApiResponse(code = 404, message = "The specified order does not exist")
//    })
//    public FulfillmentDto getOrderFulfilment(
//            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
//            @ApiParam(value = "ID of a specific order", required = true)
//            @PathVariable(value = "orderId") Long orderId) {
//
//        return fulfillmentServiceProxy.createFulfillmentDto(orderServiceProxy.getProperCart(userDetails, orderId).orElse(null));
//    }

    // ---------------------------------------------FULFILLMENTS---------------------------------------------
    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/fulfillments", method = RequestMethod.GET)
    @ApiOperation(
            value = "List all fulfillment groups",
            notes = "Returns a list of all fulfillment groups linked to the specified order",
            response = FulfillmentDto.class,
            responseContainer = "List"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of fulfillment groups list", response = FulfillmentDto.class,
                    responseContainer = "List"),
            @ApiResponse(code = 404, message = "The specified order does not exist")
    })
    public Resources<FulfillmentDto> getOrderFulfillments(
            @ApiIgnore @AuthenticationPrincipal final UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "orderId") final Long orderId
    ) {

        final Order orderEntity = orderServiceProxy.getProperCart(userDetails, orderId)
                .orElseThrow(ResourceNotFoundException::new);

        final List<FulfillmentDto> fulfillmentGroupsDtoForOrder =  Optional.ofNullable(orderEntity.getFulfillmentGroups()).orElse(Collections.emptyList()).stream()
                .map(fulfillmentGroup -> fulfillmentConverter.createDto(fulfillmentGroup))
                .collect(toList());

        return new Resources<>(
                fulfillmentGroupsDtoForOrder,

                linkTo(methodOn(FulfillmentController.class).getOrderFulfillments(null, orderId)).withSelfRel()
        );
    }

    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/fulfillments/{fulfillmentId}", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get a fulfillment group for the order",
            notes = "Returns details of the specified fulfillment group for the specified order",
            response = FulfillmentDto.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of fulfillment group details", response = FulfillmentDto.class),
            @ApiResponse(code = 404, message = "The specified order or fulfillment group does not exist")
    })
    public FulfillmentDto getOrderFulfillmentById(
            @ApiIgnore @AuthenticationPrincipal final UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "orderId") final Long orderId,
            @ApiParam(value = "ID of a specific fulfillment group", required = true)
            @PathVariable(value = "fulfillmentId") final Long fulfillmentGroupId
    ) {

        final Order orderEntity = orderServiceProxy.getProperCart(userDetails, orderId)
                .orElseThrow(ResourceNotFoundException::new);

        return Optional.ofNullable(orderEntity.getFulfillmentGroups()).orElse(Collections.emptyList()).stream()
                .filter(fulfillmentGroup -> fulfillmentGroup.getId().longValue() == fulfillmentGroupId)
                .findFirst()
                .map(fulfillmentGroup -> fulfillmentConverter.createDto(fulfillmentGroup))
                .orElseThrow(ResourceNotFoundException::new);
    }


    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/fulfillments/{fulfillmentId}", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Update existing fulfillment group",
            notes = "Updates existing fulfillment group with new details"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Successful update of the specified fulfillment group"),
            @ApiResponse(code = 400, message = "Provided Fulfillment Group Validation Error"),
            @ApiResponse(code = 404, message = "The specified order or fulfillment group does not exist")
    })
    public ResponseEntity<Void> updateOrderFulfillmentById(
            @ApiIgnore @AuthenticationPrincipal final UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "orderId") final Long orderId,
            @ApiParam(value = "ID of a specific fulfillment group", required = true)
            @PathVariable(value = "fulfillmentId") final Long fulfillmentGroupId,
            @ApiParam(value = "Description of a new fulfillment group", required = true)
            @RequestBody @Valid final FulfillmentDto fulfillmentDto
    ) {

        final Order orderEntity = orderServiceProxy.getProperCart(userDetails, orderId)
                .orElseThrow(ResourceNotFoundException::new);

        final FulfillmentGroup fulfillmentGroupEntity = Optional.ofNullable(orderEntity.getFulfillmentGroups()).orElse(Collections.emptyList()).stream()
                .filter(fulfillmentGroup -> fulfillmentGroup.getId().longValue() == fulfillmentGroupId)
                .findFirst()
                .orElseThrow(ResourceNotFoundException::new);


        final FulfillmentGroup updatedFulfillmentGroupEntity = fulfillmentConverter.updateEntity(fulfillmentGroupEntity, fulfillmentDto);

        // TODO: Cleanup, refactor, remove duplicate code, check if Broadleaf offers a more "intelligent" way to manage
        //       fulfillment groups, cleanup exception handling mess

        Optional.ofNullable(fulfillmentDto.getItemHrefs()).orElse(Collections.emptyList())
                .forEach(itemHref -> {
                    Try.ofFailable(() -> {
                        final long orderItemId = CatalogUtils.getIdFromUrl(itemHref);
                        final OrderItem orderItemEntity = orderItemService.readOrderItemById(orderItemId);

                        if(orderItemEntity == null) {
                            throw new ResourceNotFoundException("Order item: " + orderItemId + " not found");
                        }

                        // (mst) Useless, FulfillmentGroupService::addItemToFulfillmentGroup() checks and removes items from
                        //       "old" fulfillment groups
                        //fulfillmentGroupService.removeOrderItemFromFullfillmentGroups(orderEntity, orderItemEntity);

                        final FulfillmentGroupItemRequest fulfillmentGroupItemRequest = new FulfillmentGroupItemRequest();

                        fulfillmentGroupItemRequest.setFulfillmentGroup(updatedFulfillmentGroupEntity);
                        fulfillmentGroupItemRequest.setOrder(orderEntity);
                        fulfillmentGroupItemRequest.setQuantity(orderEntity.getItemCount());
                        fulfillmentGroupItemRequest.setOrderItem(orderItemEntity);

                        return fulfillmentGroupService.addItemToFulfillmentGroup(fulfillmentGroupItemRequest, true);
                    }).onFailure((e) -> {throw new ResourceNotFoundException();});
                });

        fulfillmentGroupService.save(updatedFulfillmentGroupEntity);
        Try.ofFailable(() -> orderService.save(orderEntity, true)).onFailure(ex -> log.error("Error saving object", ex));

        return ResponseEntity.noContent().build();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
    @RequestMapping(value = "/{orderId}/fulfillments", method = RequestMethod.POST)
    @ApiOperation(
            value = "Create a new fulfillment group",
            notes = "Allows to create a new fulfillment group with a specified address and order items"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A new fulfillment group has been successfully created"),
            @ApiResponse(code = 400, message = "Provided Fulfillment Group Validation Error"),
            @ApiResponse(code = 404, message = "The specified order does not exist"),
            @ApiResponse(code = 409, message = "One of the provided order item does not exist")
    })
    public ResponseEntity<Void> addOrderFulfillment (
            @ApiIgnore @AuthenticationPrincipal final UserDetails userDetails,
            @ApiParam(value = "ID of a specific order", required = true)
            @PathVariable(value = "orderId") final Long orderId,
            @ApiParam(value = "Description of a new fulfillment group", required = true)
            @RequestBody @Valid final FulfillmentDto fulfillmentDto
    ) throws PricingException, MalformedURLException {

        // TODO: Handle "exceptional" situations...order item does not exist, FG duplicates etc

        final Order orderEntity = orderServiceProxy.getProperCart(userDetails, orderId)
                .orElseThrow(ResourceNotFoundException::new);

        // TODO: Cleanup, refactor, remove duplicate code, check if Broadleaf offers a more "intelligent" way to manage
        //       fulfillment groups, cleanup exception handling mess

        FulfillmentGroup fulfillmentGroup = null;
        for (String itemHref : fulfillmentDto.getItemHrefs()) {
            OrderItem orderItem = Optional.ofNullable(orderItemService.readOrderItemById(CatalogUtils.getIdFromUrl(itemHref)))
                    .orElseThrow(ResourceNotFoundException::new);
            final FulfillmentGroupItemRequest fulfillmentGroupItemRequest = new FulfillmentGroupItemRequest();
            fulfillmentGroupItemRequest.setFulfillmentGroup(fulfillmentGroup);
            fulfillmentGroupItemRequest.setOrder(orderEntity);
            fulfillmentGroupItemRequest.setOrderItem(orderItem);
            fulfillmentGroup = fulfillmentGroupService.addItemToFulfillmentGroup(fulfillmentGroupItemRequest, false);
        }

        fulfillmentGroup.setAddress(Optional.ofNullable(fulfillmentDto.getAddress()).map(addressConverter::createEntity).orElse(null));
        fulfillmentGroupService.save(fulfillmentGroup);
        orderService.save(orderEntity, true);

        ResponseEntity<Void> build = ResponseEntity.created(
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{fulfillmentId}")
                        .buildAndExpand(fulfillmentGroup.getId())
                        .toUri()
        ).build();

        return build;
    }


    // ---------------------------------------------FULFILLMENTS---------------------------------------------

//    @Transactional
//    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
//    @RequestMapping(value = "/{orderId}/fulfillment/address", method = RequestMethod.POST)
//    @ApiOperation(
//            value = "Create fulfillment address",
//            notes = "Adds an address for fulfillment for the specified order",
//            response = ResponseEntity.class)
//    @ApiResponses(value = {
//            @ApiResponse(code = 201, message = "Fulfillment address entry successfully created"),
//            @ApiResponse(code = 400, message = "Not enough data has been provided"),
//            @ApiResponse(code = 404, message = "The specified order does not exist")
//    })
//    public ResponseEntity<?> setOrderFulfilmentAddress(
//            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
//            @ApiParam(value = "ID of a specific order", required = true)
//            @PathVariable(value = "orderId") final Long orderId,
//            @ApiParam(value = "Description of a fulfillment address", required = true)
//            @RequestBody final AddressDto addressDto) throws PricingException {
//
//        final Order order = orderServiceProxy.getProperCart(userDetails, orderId).orElseThrow(ResourceNotFoundException::new);
//
//        if (order.getItemCount() <= 0) {
//            throw new NotShippableException("Order with ID: " + orderId + " is empty");
//        }
//
//        orderValidationService.validateAddressDto(addressDto);
//
//        Address shippingAddress = addressService.create();
//        addressConverter.updateEntity(shippingAddress, addressDto);
//
//
//        fulfillmentServiceProxy.updateFulfillmentAddress(order, shippingAddress);
//
//        return ResponseEntity.created(
//                ServletUriComponentsBuilder.fromCurrentRequest()
//                        .build()
//                        .toUri()
//        ).build();
//    }
//
//
//    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ORDER', 'ROLE_USER')")
//    @RequestMapping(value = "/{orderId}/fulfillment/address", method = RequestMethod.GET)
//    @ApiOperation(
//            value = "Get an address for fulfillment",
//            notes = "Returns details of a fulfillment address for the specified order",
//            response = AddressDto.class)
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "Successful retrieval of fulfillment address", response = AddressDto.class),
//            @ApiResponse(code = 404, message = "The specified order does not exist")
//    })
//    public AddressDto getOrderFulfilmentAddress(
//            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
//            @ApiParam(value = "ID of a specific order", required = true)
//            @PathVariable(value = "orderId") Long orderId) {
//
//        return orderServiceProxy.getOrderFulfilmentAddress(userDetails, orderId);
//    }


}
