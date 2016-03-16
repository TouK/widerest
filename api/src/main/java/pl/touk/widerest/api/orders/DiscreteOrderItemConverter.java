package pl.touk.widerest.api.orders;

import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.FulfillmentGroup;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.orders.fulfillments.FulfillmentController;
import pl.touk.widerest.api.products.ProductController;

import java.math.BigDecimal;
import java.util.Optional;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class DiscreteOrderItemConverter implements Converter<DiscreteOrderItem, DiscreteOrderItemDto> {

    public static final String FULFILLMENT_REL = "fulfillment";

    @Override
    public DiscreteOrderItemDto createDto(final DiscreteOrderItem discreteOrderItem, final boolean embed, final boolean link) {
        final Money errCode = new Money(BigDecimal.valueOf(-1337));
        final Sku sku = discreteOrderItem.getSku();
        final long productId = sku.getProduct().getId();

        final DiscreteOrderItemDto orderItemDto = DiscreteOrderItemDto.builder()
                .externalId(sku.getExternalId())
                .salePrice(discreteOrderItem.getSalePrice())
                .retailPrice(discreteOrderItem.getRetailPrice())
                .quantity(discreteOrderItem.getQuantity())
                .productName(discreteOrderItem.getName())
                .productHref(linkTo(methodOn(ProductController.class)
                        .readOneProductById(productId)).toUri().toASCIIString()
                )
                .description(sku.getDescription())
                .price(Optional.ofNullable(discreteOrderItem.getTotalPrice()).orElse(errCode).getAmount())
                .build();

        orderItemDto.add(linkTo(methodOn(OrderController.class).getOneItemFromOrder(null, discreteOrderItem.getOrder().getId(), discreteOrderItem.getId(), null, null)).withSelfRel());

        if (link) {
            orderItemDto.add(linkTo(methodOn(ProductController.class).readOneProductById(productId)).withRel("product"));
            orderItemDto.add(linkTo(methodOn(FulfillmentController.class).getOrderFulfillmentById(null, discreteOrderItem.getOrder().getId(), findFullfillmentGroupId(discreteOrderItem))).withRel(FULFILLMENT_REL));
        }

        return orderItemDto;
    }

    private Long findFullfillmentGroupId(final DiscreteOrderItem discreteOrderItem) {
        return discreteOrderItem.getOrder().getFulfillmentGroups()
                .stream()
                .filter(group -> group.getDiscreteOrderItems().contains(discreteOrderItem))
                .map(FulfillmentGroup::getId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find fulfillmentgroup for this item"));
    }

}
