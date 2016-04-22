package pl.touk.widerest.api.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import javaslang.control.Try;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.FulfillmentGroup;
import org.broadleafcommerce.core.order.domain.OrderItemAttribute;
import org.springframework.hateoas.EmbeddedResource;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.orders.fulfillments.FulfillmentController;
import pl.touk.widerest.api.orders.fulfillments.FulfillmentConverter;
import pl.touk.widerest.api.products.ProductController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class DiscreteOrderItemConverter implements Converter<DiscreteOrderItem, DiscreteOrderItemDto> {

    public static final String FULFILLMENT_REL = "fulfillment";

    @Resource
    protected ObjectMapper objectMapper;

    @Resource
    protected FulfillmentConverter fulfillmentConverter;

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
                .attributes(
                        Optional.ofNullable(discreteOrderItem.getOrderItemAttributes())
                                .map(Map::values)
                                .map(Collection::stream)
                                .map(stream -> stream.collect(toMap(
                                        OrderItemAttribute::getName,
                                        orderItemAttribute -> Try.of(() -> objectMapper.readValue(orderItemAttribute.getValue(), Object.class)).getOrElse(Optional.ofNullable(orderItemAttribute.getValue()).orElse(null))
                                ))).orElse(null)
                )
                .build();

        orderItemDto.add(linkTo(methodOn(OrderController.class).getOneItemFromOrder(null, discreteOrderItem.getOrder().getId(), discreteOrderItem.getId(), null, null)).withSelfRel());

        Optional<FulfillmentGroup> fullfillmentGroup = findFullfillmentGroup(discreteOrderItem);

        if (link) {
            orderItemDto.add(linkTo(methodOn(ProductController.class).readOneProductById(productId)).withRel("product"));
            fullfillmentGroup.ifPresent(fulfillmentGroup -> {
                orderItemDto.add(linkTo(methodOn(FulfillmentController.class).getOrderFulfillmentById(null, discreteOrderItem.getOrder().getId(), fulfillmentGroup.getId())).withRel(FULFILLMENT_REL));
            });
        }

        if (embed) {
            fullfillmentGroup.ifPresent(fulfillmentGroup -> {
                orderItemDto.add(new EmbeddedResource(FULFILLMENT_REL, fulfillmentConverter.createDto(fulfillmentGroup)));
            });
        }

        return orderItemDto;
    }

    private Optional<FulfillmentGroup> findFullfillmentGroup(final DiscreteOrderItem discreteOrderItem) {
        return discreteOrderItem.getOrder().getFulfillmentGroups()
                .stream()
                .filter(group -> group.getDiscreteOrderItems().contains(discreteOrderItem))
                .findFirst();
    }

}
