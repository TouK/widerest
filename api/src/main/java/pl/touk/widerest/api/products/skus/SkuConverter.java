package pl.touk.widerest.api.products.skus;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.broadleafcommerce.common.currency.domain.BroadleafCurrency;
import org.broadleafcommerce.common.currency.service.BroadleafCurrencyService;
import org.broadleafcommerce.common.media.domain.Media;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.catalog.domain.SkuAttribute;
import org.broadleafcommerce.core.catalog.domain.SkuAttributeImpl;
import org.broadleafcommerce.core.catalog.domain.SkuMediaXref;
import org.broadleafcommerce.core.catalog.domain.SkuMediaXrefImpl;
import org.broadleafcommerce.core.catalog.domain.SkuProductOptionValueXref;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.inventory.service.InventoryService;
import org.broadleafcommerce.core.inventory.service.type.InventoryType;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.common.MediaConverter;
import pl.touk.widerest.api.products.ProductController;

import javax.annotation.Resource;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
@Slf4j
public class SkuConverter implements Converter<Sku, SkuDto>{

    @Resource(name="blCurrencyService")
    protected BroadleafCurrencyService blCurrencyService;

    @Resource
    protected InventoryService inventoryService;

    @Resource
    protected MediaConverter mediaConverter;

    @Resource(name = "wdDtoConverters")
    protected DtoConverters dtoConverters;

    @Resource
    protected CatalogService catalogService;
    
    @Override
    public SkuDto createDto(final Sku sku, final boolean embed, final boolean link) {
        final SkuDto dto = SkuDto.builder()
                .name(sku.getName())
                .description(sku.getDescription())
                .salePrice(Optional.ofNullable(sku.getSalePrice()).map(Money::getAmount).orElse(null))
                .retailPrice(Optional.ofNullable(sku.getRetailPrice()).map(Money::getAmount).orElse(null))
                .quantityAvailable(inventoryService.retrieveQuantityAvailable(sku))
                .availability(Optional.ofNullable(sku.getInventoryType()).map(InventoryType::getType).orElse(null))
                .isAvailable(inventoryService.isAvailable(sku,1))
                .taxCode(sku.getTaxCode())
                .validFrom(Optional.ofNullable(sku.getActiveStartDate()).map(Date::toInstant).map(instant -> instant.atZone(ZoneId.systemDefault())).orElse(null))
                .validTo(Optional.ofNullable(sku.getActiveEndDate()).map(Date::toInstant).map(instant -> instant.atZone(ZoneId.systemDefault())).orElse(null))
                .currencyCode(Optional.ofNullable(Money.toCurrency(sku.getPrice()))
                        .map(Currency::toString)
                        .orElse(null))
                .skuAttributes(sku.getSkuAttributes().entrySet().stream()
                        .collect(toMap(Map.Entry::getKey, e -> e.getValue().getName())))
                .skuProductOptionValues(sku.getProductOptionValueXrefs().stream()
                        .map(SkuProductOptionValueXref::getProductOptionValue)
                        .map(DtoConverters.productOptionValueToSkuValueDto)
                        .collect(toSet()))
                .media(sku.getSkuMediaXref().entrySet().stream()
                       .collect(toMap(Map.Entry::getKey, entry -> mediaConverter.createDto(entry.getValue().getMedia(), embed, link)))
                )
                .build();

        dto.add(ControllerLinkBuilder.linkTo(methodOn(SkuController.class).getSkuById(sku.getProduct().getId(), sku.getId(), null, null))
                .withSelfRel());

        if (link) {

            dto.add(linkTo(methodOn(ProductController.class).readOneProductById(sku.getProduct().getId(), null, null))
                    .withRel("product"));

            dto.add(linkTo(methodOn(SkuController.class).getMediaBySkuId(sku.getProduct().getId(), sku.getId()))
                    .withRel("media"));

            dto.add(linkTo(methodOn(SkuController.class).getSkuByIdAvailability(sku.getProduct().getId(), sku.getId()))
                    .withRel("availability"));

            //dto.add((linkTo(methodOn(ProductController.class).getSkusCountByProductId(sku.getProduct().getId())).withRel("count")));

            dto.add((linkTo(methodOn(SkuController.class).getSkuByIdQuantity(sku.getProduct().getId(), sku.getId())).withRel("quantity")));
        }

        return dto;
    }

    @Override
    public Sku createEntity(final SkuDto skuDto) {
        final Sku skuEntity = catalogService.createSku();

        return updateEntity(skuEntity, skuDto);
    }

    @Override
    public Sku updateEntity(final Sku sku, final SkuDto skuDto) {
        sku.setName(skuDto.getName());
        sku.setDescription(skuDto.getDescription());
        sku.setCurrency(currencyCodeToBLEntity(skuDto.getCurrencyCode()));
        sku.setRetailPrice(Optional.ofNullable(skuDto.getRetailPrice()).map((amount) -> new Money(amount, sku.getCurrency())).orElse(null));
        sku.setSalePrice(Optional.ofNullable(skuDto.getSalePrice()).map((amount) -> new Money(amount, sku.getCurrency())).orElse(null));
        sku.setQuantityAvailable(skuDto.getQuantityAvailable());
        sku.setTaxCode(skuDto.getTaxCode());

        sku.setActiveStartDate(Optional.ofNullable(skuDto.getValidFrom()).map(ZonedDateTime::toInstant).map(Date::from).orElse(null));
        sku.setActiveEndDate(Optional.ofNullable(skuDto.getValidTo()).map(ZonedDateTime::toInstant).map(Date::from).orElse(null));

        sku.setInventoryType(
                Optional.ofNullable(skuDto.getAvailability())
                        .map(InventoryType::getInstance)
                        .orElse(sku.getQuantityAvailable() == null ? InventoryType.ALWAYS_AVAILABLE : InventoryType.CHECK_QUANTITY)
        );

        sku.getSkuAttributes().clear();
        sku.getSkuAttributes().putAll(
                Optional.ofNullable(skuDto.getSkuAttributes()).orElse(Collections.emptyMap()).entrySet().stream()
                        .collect(toMap(Map.Entry::getKey, e -> {
                            SkuAttribute s = new SkuAttributeImpl();
                            s.setName(e.getKey());
                            s.setValue(e.getValue());
                            s.setSku(sku);
                            return s;
                        })));


        if(skuDto.getMedia() != null) {
            sku.setSkuMediaXref(
                    skuDto.getMedia().entrySet().stream()
                            .collect(toMap(Map.Entry::getKey, e -> {

                                final Media media = mediaConverter.createEntity(e.getValue());
                                final SkuMediaXref newSkuMediaXref = new SkuMediaXrefImpl();
                                newSkuMediaXref.setMedia(media);

//                                SkuMediaXref newSkuMediaXref = DtoConverters.skuMediaDtoToXref.apply(e.getValue());
                                newSkuMediaXref.setSku(sku);
                                newSkuMediaXref.setKey(e.getKey());
                                return newSkuMediaXref;
                            })));
        }

        return sku;
    }

    public BroadleafCurrency currencyCodeToBLEntity(String currencyCode) {
        BroadleafCurrency skuCurrency = blCurrencyService.findCurrencyByCode(
                        Optional.ofNullable(currencyCode).filter(StringUtils::isNotEmpty).orElse(Money.defaultCurrency().getCurrencyCode())
                );
        if (skuCurrency == null) {
            log.error("Invalid currency code: {}", currencyCode);
        }
        return skuCurrency;
    }


}
