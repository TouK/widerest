package pl.touk.widerest.api;

import org.apache.commons.lang.StringUtils;
import org.broadleafcommerce.common.currency.domain.BroadleafCurrency;
import org.broadleafcommerce.common.currency.service.BroadleafCurrencyService;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.catalog.domain.ProductOptionValue;
import org.broadleafcommerce.core.catalog.domain.SkuBundleItem;
import org.broadleafcommerce.core.catalog.domain.SkuBundleItemImpl;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.search.domain.SearchFacetDTO;
import org.broadleafcommerce.core.search.domain.SearchFacetResultDTO;
import org.springframework.stereotype.Service;
import pl.touk.widerest.api.common.ResourceNotFoundException;
import pl.touk.widerest.api.products.BundleItemDto;
import pl.touk.widerest.api.products.ProductDto;
import pl.touk.widerest.api.products.search.FacetDto;
import pl.touk.widerest.api.products.search.FacetValueDto;
import pl.touk.widerest.api.products.skus.SkuDto;
import pl.touk.widerest.api.products.skus.SkuProductOptionValueDto;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

@Service("wdDtoConverters")
public class DtoConverters {

    @Resource(name="blCurrencyService")
    protected BroadleafCurrencyService blCurrencyService;

    @Resource(name = "blCatalogService")
    protected CatalogService catalogService;

    public Function<String, BroadleafCurrency> currencyCodeToBLEntity = currencyCode -> {
        BroadleafCurrency skuCurrency = null;

        if(StringUtils.isEmpty(currencyCode)) {
            skuCurrency = blCurrencyService.findDefaultBroadleafCurrency();
        } else {
            skuCurrency = Optional.ofNullable(blCurrencyService.findCurrencyByCode(currencyCode))
                            .orElseThrow(() -> new ResourceNotFoundException("Invalid currency code."));
        }
        return skuCurrency;
    };

    public static Function<ProductOptionValue, SkuProductOptionValueDto> productOptionValueToSkuValueDto = entity ->
            SkuProductOptionValueDto.builder()
                    .attributeName(entity.getProductOption().getAttributeName())
                    .attributeValue(entity.getAttributeValue())
                    .build();


    public static Function<SkuBundleItem, BundleItemDto> skuBundleItemToBundleItemDto = entity -> {
        final BundleItemDto bundleItemDto = BundleItemDto.builder()
                .skuId(entity.getSku().getId())
                .quantity(entity.getQuantity())
                .salePrice(Optional.ofNullable(entity.getSalePrice()).map(Money::getAmount).orElse(null))
                .build();

        return bundleItemDto;
    };

    public Function<BundleItemDto, SkuBundleItem> bundleItemDtoToSkuBundleItem = dto -> {
        final SkuBundleItem skuBundleItem = new SkuBundleItemImpl();
        skuBundleItem.setQuantity(dto.getQuantity());
        skuBundleItem.setSalePrice(new Money(dto.getSalePrice()));
        skuBundleItem.setSku(catalogService.findSkuById(dto.getSkuId()));
        return skuBundleItem;
    };

    public static Function<SearchFacetResultDTO, FacetValueDto> searchFacetResultDTOFacetValueToDto = entity -> FacetValueDto.builder()
            .value(entity.getValue())
            .minValue(entity.getMinValue())
            .maxValue(entity.getMaxValue())
            .quantity(entity.getQuantity())
            .build();

    public static Function<SearchFacetDTO, FacetDto> searchFacetDTOFacetToDto = entity -> {
        final FacetDto facetDto = FacetDto.builder()
                .active(entity.isActive())
                .label(entity.getFacet().getLabel())
                .build();

        facetDto.setFacetOptions(Optional.ofNullable(entity.getFacetValues()).orElse(Collections.emptyList()).stream()
                .map(searchFacetResultDTOFacetValueToDto)
                .collect(toList()));

        return facetDto;
    };


    public static Function<ProductDto, SkuDto> productDtoToDefaultSkuDto = productDto ->
            SkuDto.builder()
                  .name(productDto.getName())
                  .description(productDto.getDescription())
                  .salePrice(productDto.getSalePrice())
                  .retailPrice(productDto.getRetailPrice())
                  .quantityAvailable(productDto.getQuantityAvailable())
                  .availability(productDto.getAvailability())
                  .taxCode(productDto.getTaxCode())
                  .activeStartDate(productDto.getValidFrom())
                  .activeEndDate(productDto.getValidTo())
                  .currencyCode(productDto.getCurrencyCode())
                  .media(productDto.getMedia())
                  .build();
}
