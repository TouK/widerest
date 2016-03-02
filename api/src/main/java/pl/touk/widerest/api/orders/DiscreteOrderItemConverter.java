package pl.touk.widerest.api.orders;

import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItemImpl;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.products.ProductController;

import java.math.BigDecimal;
import java.util.Optional;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class DiscreteOrderItemConverter implements Converter<DiscreteOrderItem, DiscreteOrderItemDto> {
    @Override
    public DiscreteOrderItemDto createDto(final DiscreteOrderItem discreteOrderItem, final boolean embed) {
        final Money errCode = new Money(BigDecimal.valueOf(-1337));
        final Sku sku = discreteOrderItem.getSku();
        final long productId = sku.getProduct().getId();

        final DiscreteOrderItemDto orderItemDto = DiscreteOrderItemDto.builder()
                .itemId(discreteOrderItem.getId())
                .salePrice(discreteOrderItem.getSalePrice())
                .retailPrice(discreteOrderItem.getRetailPrice())
                .quantity(discreteOrderItem.getQuantity())
                .productName(discreteOrderItem.getName())
                .productId(productId)
                .skuId(sku.getId())
                .description(sku.getDescription())
                .price(Optional.ofNullable(discreteOrderItem.getTotalPrice()).orElse(errCode).getAmount())
                .build();

        orderItemDto.add(linkTo(methodOn(OrderController.class).getOneItemFromOrder(null, discreteOrderItem.getId(), discreteOrderItem.getOrder().getId())).withSelfRel());
        orderItemDto.add(linkTo(methodOn(ProductController.class).readOneProductById(productId)).withRel("product"));

        return orderItemDto;
    }

    @Override
    public DiscreteOrderItem createEntity(final DiscreteOrderItemDto discreteOrderItemDto) {
        return updateEntity(new DiscreteOrderItemImpl(), discreteOrderItemDto);
    }

    @Override
    public DiscreteOrderItem updateEntity(final DiscreteOrderItem discreteOrderItem, final DiscreteOrderItemDto discreteOrderItemDto) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DiscreteOrderItem partialUpdateEntity(final DiscreteOrderItem discreteOrderItem, final DiscreteOrderItemDto discreteOrderItemDto) {
        throw new UnsupportedOperationException();
    }
}
