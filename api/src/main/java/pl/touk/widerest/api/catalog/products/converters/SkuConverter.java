package pl.touk.widerest.api.catalog.products.converters;

import org.broadleafcommerce.common.currency.domain.BroadleafCurrency;
import org.broadleafcommerce.common.currency.service.BroadleafCurrencyService;
import org.broadleafcommerce.common.locale.service.LocaleService;
import org.broadleafcommerce.common.media.domain.Media;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.catalog.domain.*;
import org.broadleafcommerce.core.inventory.service.type.InventoryType;
import org.springframework.hateoas.EmbeddedResource;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.catalog.dto.MediaDto;
import pl.touk.widerest.api.catalog.products.dto.SkuDto;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;
import pl.touk.widerest.api.catalog.products.ProductController;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class SkuConverter implements Converter<Sku, SkuDto>{

    @Resource(name = "blLocaleService")
    protected LocaleService localeService;

    @Resource(name="blCurrencyService")
    protected BroadleafCurrencyService blCurrencyService;

    @Resource
    protected MediaConverter mediaConverter;
    
    @Override
    public SkuDto createDto(final Sku sku, final boolean embed) {
        final SkuDto dto = SkuDto.builder()
                .name(sku.getName())
                .description(sku.getDescription())
                .salePrice(Optional.ofNullable(sku.getSalePrice()).map(Money::getAmount).orElse(null))
                .retailPrice(Optional.ofNullable(sku.getRetailPrice()).map(Money::getAmount).orElse(null))
                .quantityAvailable(sku.getQuantityAvailable())
                .availability(Optional.ofNullable(sku.getInventoryType()).map(InventoryType::getType).orElse(null))
                .taxCode(sku.getTaxCode())
                .activeStartDate(sku.getActiveStartDate())
                .activeEndDate(sku.getActiveEndDate())
                .currencyCode(Optional.ofNullable(sku.getCurrency())
                        .orElse(localeService.findDefaultLocale().getDefaultCurrency())
                        .getCurrencyCode())
                .skuAttributes(sku.getSkuAttributes().entrySet().stream()
                        .collect(toMap(Map.Entry::getKey, e -> e.getValue().getName())))
                .skuProductOptionValues(sku.getProductOptionValueXrefs().stream()
                        .map(SkuProductOptionValueXref::getProductOptionValue)
                        .map(DtoConverters.productOptionValueToSkuValueDto)
                        .collect(toSet()))
                .skuMedia(sku.getSkuMediaXref().entrySet().stream()
                       .collect(toMap(Map.Entry::getKey, entry -> mediaConverter.createDto(entry.getValue().getMedia(), false)))
                )
                .build();

        dto.add(ControllerLinkBuilder.linkTo(methodOn(ProductController.class).getSkuById(sku.getProduct().getId(), sku.getId()))
                .withSelfRel());

        dto.add(linkTo(methodOn(ProductController.class).readOneProductById(sku.getProduct().getId()))
                .withRel("product"));

        dto.add(linkTo(methodOn(ProductController.class).getMediaBySkuId(sku.getProduct().getId(), sku.getId()))
                .withRel("media"));

        dto.add(linkTo(methodOn(ProductController.class).getSkuByIdAvailability(sku.getProduct().getId(), sku.getId()))
                .withRel("availability"));

        //dto.add((linkTo(methodOn(ProductController.class).getSkusCountByProductId(sku.getProduct().getId())).withRel("count")));

        dto.add((linkTo(methodOn(ProductController.class).getSkuByIdQuantity(sku.getProduct().getId(), sku.getId())).withRel("quantity")));

        return dto;
    }

    @Override
    public Sku createEntity(final SkuDto skuDto) {
        final Sku skuEntity = new SkuImpl();

        skuEntity.setCurrency(currencyCodeToBLEntity.apply(skuDto.getCurrencyCode()));

        return updateEntity(skuEntity, skuDto);
    }

    @Override
    public Sku updateEntity(final Sku sku, final SkuDto skuDto) {
        sku.setName(skuDto.getName());
        sku.setDescription(skuDto.getDescription());
        sku.setSalePrice(new Money(skuDto.getSalePrice()));
        sku.setQuantityAvailable(skuDto.getQuantityAvailable());
        sku.setTaxCode(skuDto.getTaxCode());
        sku.setActiveStartDate(skuDto.getActiveStartDate());
        sku.setActiveEndDate(skuDto.getActiveEndDate());

		/*
		 * (mst) RetailPrice cannot be null, so just leave "the old" value if a
		 * new one has not been provided
		 */
        if (skuDto.getRetailPrice() != null) {
            sku.setRetailPrice(new Money(skuDto.getRetailPrice()));
        } else {
            sku.setRetailPrice(new Money(skuDto.getSalePrice()));
        }

        if(skuDto.getAvailability() != null && InventoryType.getInstance(skuDto.getAvailability()) != null) {
            sku.setInventoryType(InventoryType.getInstance(skuDto.getAvailability()));
        } else {
            /* (mst) turn on Inventory Service by default */
            sku.setInventoryType(InventoryType.ALWAYS_AVAILABLE);
        }


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


        if(skuDto.getSkuMedia() != null) {
            sku.setSkuMediaXref(
                    skuDto.getSkuMedia().entrySet().stream()
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

    @Override
    public Sku partialUpdateEntity(final Sku sku, final SkuDto skuDto) {
        throw new UnsupportedOperationException();
    }

    private Function<String, BroadleafCurrency> currencyCodeToBLEntity = currencyCode -> {
        BroadleafCurrency skuCurrency = null;

        if(currencyCode == null || currencyCode.isEmpty()) {
            skuCurrency = blCurrencyService.findDefaultBroadleafCurrency();
        } else {
            skuCurrency = blCurrencyService.findCurrencyByCode(currencyCode);

            if (skuCurrency == null) {
//                BroadleafCurrency newBLCurrency = new BroadleafCurrencyImpl();
//                newBLCurrency.setCurrencyCode(currencyCode);
//                skuCurrency = blCurrencyService.save(newBLCurrency);
                throw new ResourceNotFoundException("Invalid currency code.");
            }

        }
        return skuCurrency;
    };
}
