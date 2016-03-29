package pl.touk.widerest.api.products.skus;

import org.broadleafcommerce.common.currency.service.BroadleafCurrencyService;
import org.broadleafcommerce.common.locale.service.LocaleService;
import org.broadleafcommerce.common.media.domain.Media;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.catalog.domain.SkuAttribute;
import org.broadleafcommerce.core.catalog.domain.SkuAttributeImpl;
import org.broadleafcommerce.core.catalog.domain.SkuMediaXref;
import org.broadleafcommerce.core.catalog.domain.SkuMediaXrefImpl;
import org.broadleafcommerce.core.catalog.domain.SkuProductOptionValueXref;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.inventory.service.type.InventoryType;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.common.MediaConverter;
import pl.touk.widerest.api.products.ProductController;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

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
                .media(sku.getSkuMediaXref().entrySet().stream()
                       .collect(toMap(Map.Entry::getKey, entry -> mediaConverter.createDto(entry.getValue().getMedia(), embed, link)))
                )
                .build();

        dto.add(ControllerLinkBuilder.linkTo(methodOn(SkuController.class).getSkuById(sku.getProduct().getId(), sku.getId()))
                .withSelfRel());

        if (link) {

            dto.add(linkTo(methodOn(ProductController.class).readOneProductById(sku.getProduct().getId()))
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

        skuEntity.setCurrency(dtoConverters.currencyCodeToBLEntity.apply(skuDto.getCurrencyCode()));

        return updateEntity(skuEntity, skuDto);
    }

    @Override
    public Sku updateEntity(final Sku sku, final SkuDto skuDto) {
        sku.setName(skuDto.getName());
        sku.setDescription(skuDto.getDescription());
        sku.setCurrency(dtoConverters.currencyCodeToBLEntity.apply(skuDto.getCurrencyCode()));
        sku.setRetailPrice(Optional.ofNullable(skuDto.getRetailPrice()).map((amount) -> new Money(amount, sku.getCurrency())).orElse(null));
        sku.setSalePrice(Optional.ofNullable(skuDto.getSalePrice()).map((amount) -> new Money(amount, sku.getCurrency())).orElse(null));
        sku.setQuantityAvailable(skuDto.getQuantityAvailable());
        sku.setTaxCode(skuDto.getTaxCode());
        sku.setActiveStartDate(skuDto.getActiveStartDate());
        sku.setActiveEndDate(skuDto.getActiveEndDate());

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
}
