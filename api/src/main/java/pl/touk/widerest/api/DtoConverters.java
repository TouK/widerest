package pl.touk.widerest.api;

import org.apache.commons.lang.StringUtils;
import org.broadleafcommerce.common.currency.domain.BroadleafCurrency;
import org.broadleafcommerce.common.currency.service.BroadleafCurrencyService;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.value.ValueAssignable;
import org.broadleafcommerce.core.catalog.domain.*;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.order.domain.OrderAttribute;
import org.broadleafcommerce.core.order.domain.OrderAttributeImpl;
import org.broadleafcommerce.core.search.domain.SearchFacetDTO;
import org.broadleafcommerce.core.search.domain.SearchFacetResultDTO;
import org.springframework.stereotype.Service;
import pl.touk.widerest.api.common.ResourceNotFoundException;
import pl.touk.widerest.api.orders.CartAttributeDto;
import pl.touk.widerest.api.products.BundleItemDto;
import pl.touk.widerest.api.products.ProductDto;
import pl.touk.widerest.api.products.ProductOptionDto;
import pl.touk.widerest.api.products.ProductOptionValueDto;
import pl.touk.widerest.api.products.search.FacetDto;
import pl.touk.widerest.api.products.search.FacetValueDto;
import pl.touk.widerest.api.products.skus.SkuDto;
import pl.touk.widerest.api.products.skus.SkuProductOptionValueDto;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
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

    public ProductOption getProductOptionByNameForProduct(final String productOptionName, final Product product) {
        return Optional.ofNullable(product.getProductOptionXrefs())
                .orElse(Collections.emptyList()).stream()
                    .map(ProductOptionXref::getProductOption)
                    .filter(x -> x.getAttributeName().equals(productOptionName))
                    .findAny()
                    .orElse(null);
    }

    public ProductOptionValue getProductOptionValueByNameForProduct(final ProductOption productOption,
                                                                    final String productOptionValue) {
        return productOption.getAllowedValues().stream()
                .filter(x -> x.getAttributeValue().equals(productOptionValue))
                .findAny()
                .orElse(null);
    }

    public static Function <ProductOption, ProductOptionDto> productOptionEntityToDto = entity ->
                ProductOptionDto.builder().name(entity.getAttributeName()).allowedValues(entity.getAllowedValues().stream()
                        .map(ProductOptionValue::getAttributeValue)
                        .collect(toList()))
                        .build();


    public static Function<ProductOptionDto, ProductOption> productOptionDtoToEntity = dto -> {
        final ProductOption productOption = new ProductOptionImpl();
        productOption.setAttributeName(dto.getName());
        productOption.setAllowedValues(dto.getAllowedValues().stream()
                .map(e -> {
                    ProductOptionValue p = new ProductOptionValueImpl();
                    p.setAttributeValue(e);
                    return p;
                })
                .collect(toList()));

        return productOption;
    };


    public static Function<ProductOptionValue, ProductOptionValueDto> productOptionValueEntityToDto = entity ->
            ProductOptionValueDto.builder().attributeValue(entity.getAttributeValue())
                .productOption(DtoConverters.productOptionEntityToDto.apply(entity.getProductOption())).build();


    public static Function<ProductOptionValueDto, ProductOptionValue> productOptionValueDtoToEntity = dto -> {
        final ProductOptionValue productOptionValue = new ProductOptionValueImpl();

        productOptionValue.setAttributeValue(dto.getAttributeValue());
        productOptionValue.setProductOption(DtoConverters.productOptionDtoToEntity.apply(dto.getProductOption()));
        return productOptionValue;
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
                  .skuAttributes(productDto.getSkuAttributes())
                  .skuProductOptionValues(productDto.getSkuProductOptionValues())
                  .media(productDto.getMedia())
                  .build();
}
