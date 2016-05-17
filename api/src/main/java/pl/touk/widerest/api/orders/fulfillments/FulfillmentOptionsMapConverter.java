package pl.touk.widerest.api.orders.fulfillments;

import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.order.domain.FulfillmentOption;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class FulfillmentOptionsMapConverter implements Converter<Map<? extends FulfillmentOption, Money>, Map<String, FulfillmentOptionDto>> {
    @Override
    public Map<String, FulfillmentOptionDto> createDto(Map<? extends FulfillmentOption, Money> moneyPerOptionMap, boolean embed, boolean link) {
        return moneyPerOptionMap.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().getName(),
                        e -> FulfillmentOptionDto.builder()
                                .description(e.getKey().getLongDescription())
                                .price(e.getValue().getAmount()).build(),
                        (a, b) -> {
                            BigDecimal priceMin = Optional.ofNullable(a.getPriceFrom()).orElse(a.getPrice())
                                    .min(Optional.ofNullable(b.getPriceFrom()).orElse(b.getPrice()));
                            BigDecimal priceMax = Optional.ofNullable(a.getPriceTo()).orElse(a.getPrice())
                                    .max(Optional.ofNullable(b.getPriceTo()).orElse(b.getPrice()));

                            FulfillmentOptionDto fulfillmentOptionDto = FulfillmentOptionDto.builder()
                                    .description(a.getDescription())
                                    .build();

                            if (priceMax.compareTo(priceMin) == 0) {
                                fulfillmentOptionDto.setPrice(priceMin);
                            } else {
                                fulfillmentOptionDto.setPriceFrom(priceMin);
                                fulfillmentOptionDto.setPriceTo(priceMax);
                            }

                            return fulfillmentOptionDto;
                        },
                        HashMap::new

                ));
    }
}
