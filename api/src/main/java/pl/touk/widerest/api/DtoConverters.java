package pl.touk.widerest.api;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Resource;

import org.broadleafcommerce.common.currency.domain.BroadleafCurrency;
import org.broadleafcommerce.common.currency.service.BroadleafCurrencyService;
import org.broadleafcommerce.common.i18n.service.ISOService;
import org.broadleafcommerce.common.media.domain.Media;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.value.ValueAssignable;
import org.broadleafcommerce.core.catalog.domain.*;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.OrderAttribute;
import org.broadleafcommerce.core.order.domain.OrderAttributeImpl;
import org.broadleafcommerce.core.search.domain.SearchFacetDTO;
import org.broadleafcommerce.core.search.domain.SearchFacetResultDTO;
import org.springframework.stereotype.Service;

import pl.touk.widerest.api.cart.orders.OrderController;
import pl.touk.widerest.api.cart.dto.CartAttributeDto;
import pl.touk.widerest.api.cart.dto.DiscreteOrderItemDto;
import pl.touk.widerest.api.catalog.dto.*;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;
import pl.touk.widerest.api.catalog.products.dto.ProductDto;
import pl.touk.widerest.api.catalog.products.dto.SkuDto;

@Service("wdDtoConverters")
public class DtoConverters {

    @Resource(name="blCurrencyService")
    protected BroadleafCurrencyService blCurrencyService;

    @Resource(name = "blCatalogService")
    protected CatalogService catalogService;

    @Resource
    protected ISOService isoService;

    private static Function<ProductAttribute, String> getProductAttributeName = ValueAssignable::getValue;

    private static Function<ProductOptionValue, String> getProductOptionValueName = ProductOptionValue::getAttributeValue;

    /******************************** Currency ********************************/

    public Function<String, BroadleafCurrency> currencyCodeToBLEntity = currencyCode -> {
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

    /******************************** Currency ********************************/

    /******************************** SKU ********************************/


    /******************************** SKU ********************************/

    public ProductOption getProductOptionByNameForProduct(String productOptionName, Product product) {
        return Optional.ofNullable(product.getProductOptionXrefs())
                .orElse(Collections.emptyList()).stream()
                    .map(ProductOptionXref::getProductOption)
                    .filter(x -> x.getAttributeName().equals(productOptionName))
                    .findAny()
                    .orElse(null);
    }

    public ProductOptionValue getProductOptionValueByNameForProduct(ProductOption productOption,
                                                                    String productOptionValue) {
        return productOption.getAllowedValues().stream()
                .filter(x -> x.getAttributeValue().equals(productOptionValue))
                .findAny()
                .orElse(null);
    }



    /******************************** Product ********************************/


    /******************************** Product ********************************/

    /******************************** Product Option ********************************/

    public static Function <ProductOption, ProductOptionDto> productOptionEntityToDto = entity -> {

        return ProductOptionDto.builder()
                .name(entity.getAttributeName())
                .allowedValues(entity.getAllowedValues().stream()
                        .map(DtoConverters.getProductOptionValueName)
                        .collect(toList()))
                .build();
    };


    public static Function<ProductOptionDto, ProductOption> productOptionDtoToEntity = dto -> {
        ProductOption productOption = new ProductOptionImpl();
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

    /******************************** Product Option ********************************/

    /******************************** Product Option Value ********************************/

    public static Function<ProductOptionValue, ProductOptionValueDto> productOptionValueEntityToDto = entity -> {
        return ProductOptionValueDto.builder()
                .attributeValue(entity.getAttributeValue())
                .productOption(DtoConverters.productOptionEntityToDto.apply(entity.getProductOption()))
                .build();
    };


    public static Function<ProductOptionValueDto, ProductOptionValue> productOptionValueDtoToEntity = dto -> {
        ProductOptionValue productOptionValue = new ProductOptionValueImpl();

        productOptionValue.setAttributeValue(dto.getAttributeValue());
        productOptionValue.setProductOption(DtoConverters.productOptionDtoToEntity.apply(dto.getProductOption()));
        return productOptionValue;
    };

    public static Function<ProductOptionValue, SkuProductOptionValueDto> productOptionValueToSkuValueDto = entity -> {
        return SkuProductOptionValueDto.builder()
                .attributeName(entity.getProductOption().getAttributeName())
                .attributeValue(entity.getAttributeValue())
                .build();
    };

    /******************************** Product Option Value ********************************/

    /******************************** CATEGORY ********************************/

    /******************************** CATEGORY ********************************/

    /******************************** Sku Media  ********************************/

    public static Function<CategoryMediaXref, MediaDto> categoryMediaXrefToDto = xref -> {

        final Media entity = xref.getMedia();

        return MediaDto.builder()
                .title(entity.getTitle())
                .url(entity.getUrl())
                .altText(entity.getAltText())
                .tags(entity.getTags())
//                .key(xref.getKey())
                .build();
    };

//    public static CategoryMediaXref mediaDtoToCategoryMediaXref(MediaDto dto) {
//        CategoryMediaXref categoryMediaXref = new CategoryMediaXrefImpl();
//        Media media = new MediaImpl();
//
//        media = CatalogUtils.updateMediaEntityFromDto(media, dto);
//
//        categoryMediaXref.setMedia(media);
//        return categoryMediaXref;
//    };

//    public static Function<SkuMediaXref, MediaDto> skuMediaXrefToDto = xref -> {
//
//        final Media entity = xref.getMedia();
//
//        return MediaDto.builder()
//                .title(entity.getTitle())
//                .url(entity.getUrl())
//                .altText(entity.getAltText())
//                .tags(entity.getTags())
////                .key(xref.getKey())
//                .build();
//    };

    /* (mst) Remember to set SKU after this one */
//    public static Function<MediaDto, SkuMediaXref> skuMediaDtoToXref = dto -> {
//        SkuMediaXref skuMediaXref = new SkuMediaXrefImpl();
//        Media skuMedia = new MediaImpl();
//
//        skuMedia = CatalogUtils.updateMediaEntityFromDto(skuMedia, dto);
//
//        skuMediaXref.setMedia(skuMedia);
//        return skuMediaXref;
//    };

    /******************************** Sku Media  ********************************/

    /******************************** SKU BUNDLE ITEMS ********************************/

    public static Function<SkuBundleItem, BundleItemDto> skuBundleItemToBundleItemDto = entity -> {
        final BundleItemDto bundleItemDto = BundleItemDto.builder()
                .skuId(entity.getSku().getId())
                .quantity(entity.getQuantity())
                .salePrice(Optional.ofNullable(entity.getSalePrice()).map(Money::getAmount).orElse(null))
                .build();

        final Product associatedProduct = entity.getSku().getProduct();

//        bundleItemDto.add(linkTo(methodOn(ProductController.class).getSkuById(
//                associatedProduct.getId(),
//                entity.getSku().getId())).withSelfRel());


        return bundleItemDto;
    };

    public Function<BundleItemDto, SkuBundleItem> bundleItemDtoToSkuBundleItem = dto -> {
        SkuBundleItem skuBundleItem = new SkuBundleItemImpl();
        skuBundleItem.setQuantity(dto.getQuantity());
        skuBundleItem.setSalePrice(new Money(dto.getSalePrice()));
        skuBundleItem.setSku(catalogService.findSkuById(dto.getSkuId()));
        return skuBundleItem;
    };

    /******************************** SKU BUNDLE ITEMS ********************************/

    /******************************** Product Option Xref ********************************/

    public static Function<ProductOptionXref, ProductOptionDto> productOptionXrefToDto = input -> {
        org.broadleafcommerce.core.catalog.domain.ProductOption productOption = input.getProductOption();

        List<ProductOptionValue> productOptionValues = productOption.getAllowedValues();
        List<String> collectAllowedValues = productOptionValues.stream()
                .map(getProductOptionValueName)
                .collect(toList());
        return new ProductOptionDto(productOption.getAttributeName(), collectAllowedValues);
    };

    // experimental
    public static Function<ProductOptionDto, ProductOptionXref> productOptionDtoToXRef = input -> {
        ProductOptionXref productOptionXref = new ProductOptionXrefImpl();
        ProductOption productOption = new ProductOptionImpl();

        productOption.setAttributeName(input.getName());
        productOption.setAllowedValues(input.getAllowedValues().stream()
                .map(e -> {
                    ProductOptionValue v = new ProductOptionValueImpl();
                    v.setAttributeValue(e);
                    return v;
                }).collect(toList()));

        productOptionXref.setProductOption(productOption);
        return productOptionXref;
    };

    public static Function<OrderAttribute, CartAttributeDto> orderAttributeEntityToDto = entity -> {
        return CartAttributeDto.builder()
                .name(entity.getName())
                .value(entity.getValue())
                .build();
    };

    public static Function<CartAttributeDto, OrderAttribute> orderAttributeDtoToEntity = dto -> {
        final OrderAttribute order = new OrderAttributeImpl();
        order.setName(dto.getName());
        order.setValue(dto.getValue());
        return order;
    };


    /******************************** DISCRETEORDERITEM ********************************/
    public static Function<DiscreteOrderItem, DiscreteOrderItemDto> discreteOrderItemEntityToDto = entity -> {
        final Money errCode = new Money(BigDecimal.valueOf(-1337));
        final Sku sku = entity.getSku();

        final long productId = sku.getProduct().getId();

        final DiscreteOrderItemDto orderItemDto = DiscreteOrderItemDto.builder()
                .itemId(entity.getId())
                .salePrice(entity.getSalePrice())
                .retailPrice(entity.getRetailPrice())
                .quantity(entity.getQuantity())
                .productName(entity.getName())
                .productId(productId)
                .skuId(sku.getId())
                .description(sku.getDescription())
                .price(Optional.ofNullable(entity.getTotalPrice()).orElse(errCode).getAmount())
                .build();

        orderItemDto.add(linkTo(methodOn(OrderController.class).getOneItemFromOrder(null, entity.getId(), entity.getOrder().getId())).withSelfRel());
//        orderItemDto.add(linkTo(methodOn(ProductController.class).readOneProductById(productId)).withRel("product"));

        return orderItemDto;
    };
    /******************************** DISCRETEORDERITEM ********************************/
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


    public static Function<ProductDto, SkuDto> productDtoToDefaultSkuDto = productDto -> {
          return SkuDto.builder()
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
                  .skuMedia(productDto.getSkuMedia())
                  .build();
    };
}