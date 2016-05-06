package pl.touk.widerest.api.orders.fulfillments;

import org.broadleafcommerce.common.vendor.service.exception.FulfillmentPriceException;
import org.broadleafcommerce.core.order.domain.FulfillmentGroup;
import org.broadleafcommerce.core.order.domain.FulfillmentOption;
import org.broadleafcommerce.core.order.service.FulfillmentGroupService;
import org.broadleafcommerce.core.order.service.OrderItemService;
import org.broadleafcommerce.core.order.service.type.FulfillmentType;
import org.broadleafcommerce.core.pricing.service.FulfillmentPricingService;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.common.AddressConverter;
import pl.touk.widerest.api.orders.OrderController;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class FulfillmentConverter implements Converter<FulfillmentGroup, FulfillmentDto> {

    @Resource
    private AddressConverter addressConverter;

    @Resource(name = "blOrderItemService")
    protected OrderItemService orderItemService;

    @Resource
    private FulfilmentServiceProxy fulfillmentServiceProxy;

    @Resource(name = "blFulfillmentGroupService")
    private FulfillmentGroupService fulfillmentGroupService;

    @Resource(name = "blFulfillmentPricingService")
    private FulfillmentPricingService fulfillmentPricingService;

    @Override
    public FulfillmentDto createDto(final FulfillmentGroup fulfillmentGroup, final boolean embed, final boolean link) {

        final FulfillmentDto fulfillmentDto = new FulfillmentDto();

        Optional.ofNullable(fulfillmentGroup.getAddress()).ifPresent(address -> {
            fulfillmentDto.setAddress(addressConverter.createDto(address, embed, link));
        });

        fulfillmentDto.setItemHrefs(
                Optional.ofNullable(fulfillmentGroup.getFulfillmentGroupItems()).orElse(Collections.emptyList()).stream()
                        .map(fulfillmentGroupItem -> linkTo(methodOn(OrderController.class)
                                .getOneItemFromOrder(null, fulfillmentGroup.getOrder().getId(), fulfillmentGroupItem.getOrderItem().getId(), null, null)).toUri().toASCIIString())
                        .collect(Collectors.toList())
        );

        fulfillmentDto.setType(Optional.ofNullable(fulfillmentGroup.getType()).map(FulfillmentType::getFriendlyType).orElse(null));

        Optional.ofNullable(fulfillmentGroup.getFulfillmentOption())
                .map(selectedOption -> selectedOption.getName())
                .ifPresent(fulfillmentDto::setSelectedFulfillmentOption);

        try {
            Optional.ofNullable(fulfillmentServiceProxy.readFulfillmentOptionsWithPricesAvailableForProductsInFulfillmentGroup(fulfillmentGroup))
                    .ifPresent(options -> {
                        fulfillmentDto.setFulfillmentOptions(options.entrySet().stream()
                                .collect(Collectors.toMap(
                                        e -> e.getKey().getName(),
                                        e -> FulfillmentOptionDto.builder()
                                                .description(e.getKey().getLongDescription())
                                                .price(e.getValue().getAmount()).build())
                                ));
                    });
        } catch (FulfillmentPriceException e) {
            throw new RuntimeException(e);
        }

        /* HATEOAS links */

        fulfillmentDto.add(linkTo(methodOn(FulfillmentController.class).getOrderFulfillmentById(null, fulfillmentGroup.getOrder().getId(), fulfillmentGroup.getId())).withSelfRel());
        return fulfillmentDto;
    }

    @Override
    public FulfillmentGroup createEntity(final FulfillmentDto fulfillmentDto) {
        return updateEntity(fulfillmentGroupService.createEmptyFulfillmentGroup(), fulfillmentDto);
    }

    @Override
    public FulfillmentGroup updateEntity(final FulfillmentGroup fulfillmentGroup, final FulfillmentDto fulfillmentDto) {

        fulfillmentGroup.setAddress(Optional.ofNullable(fulfillmentDto.getAddress()).map(addressConverter::createEntity).orElse(null));

        Set<FulfillmentOption> availableFulfillmentOptions = fulfillmentServiceProxy.findFulfillmentOptionsForProductsInFulfillmentGroup(fulfillmentGroup);

        Optional<FulfillmentOption> matchOption = availableFulfillmentOptions.stream()
                .filter(o -> o.getName().equals(fulfillmentDto.getSelectedFulfillmentOption()))
                .findFirst();

        if(matchOption.isPresent()) {
            fulfillmentGroup.setFulfillmentOption(matchOption.get());
        } //TODO nullcheck because someone migh want to order first, then see prices of postage, should it be like this?
        else if(fulfillmentDto.getSelectedFulfillmentOption() != null) {
            throw new NoFulfillmentOptionException(String.format("No such fulfillment: %s", fulfillmentDto.getSelectedFulfillmentOption()));
        }

        return fulfillmentGroup;
    }

}
