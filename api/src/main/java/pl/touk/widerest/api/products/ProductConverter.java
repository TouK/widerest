package pl.touk.widerest.api.products;

import javaslang.control.Try;
import org.broadleafcommerce.common.locale.service.LocaleService;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.catalog.domain.*;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.catalog.service.type.ProductOptionType;
import org.broadleafcommerce.core.catalog.service.type.ProductOptionValidationStrategyType;
import org.broadleafcommerce.core.catalog.service.type.ProductType;
import org.broadleafcommerce.core.inventory.service.InventoryService;
import org.broadleafcommerce.core.inventory.service.type.InventoryType;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.categories.CategoryController;
import pl.touk.widerest.api.common.CatalogUtils;
import pl.touk.widerest.api.common.MediaConverter;
import pl.touk.widerest.api.common.MediaDto;
import pl.touk.widerest.api.common.ResourceNotFoundException;
import pl.touk.widerest.api.orders.fulfillments.FulfillmentOptionsMapConverter;
import pl.touk.widerest.api.orders.fulfillments.FulfilmentServiceProxy;
import pl.touk.widerest.api.products.skus.SkuController;
import pl.touk.widerest.api.products.skus.SkuConverter;
import pl.touk.widerest.api.products.skus.SkuDto;
import pl.touk.widerest.api.products.skus.SkuProductOptionValueDto;
import pl.touk.widerest.hal.EmbeddedResource;

import javax.annotation.Resource;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.empty;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class ProductConverter implements Converter<Product, ProductDto>{
    
    @Resource(name = "blLocaleService")
    protected LocaleService localeService;

    @Resource
    protected SkuConverter skuConverter;

    @Resource
    protected MediaConverter mediaConverter;

    @Resource
    protected CatalogService catalogService;

    @Resource
    protected InventoryService inventoryService;

    @Resource
    protected FulfilmentServiceProxy fulfilmentServiceProxy;

    @Resource
    protected FulfillmentOptionsMapConverter fulfillmentOptionsMapConverter;

    @Override
    public ProductDto createDto(final Product product, final boolean embed, final boolean link) {
        final ProductDto dto = product instanceof ProductBundle ? new ProductBundleDto() : new ProductDto();

        dto.setName(product.getName());

        /* (mst) This should never be null */
        final Sku productDefaultSku = product.getDefaultSku();

        dto.setRetailPrice(Optional.ofNullable(productDefaultSku.getRetailPrice()).map(Money::getAmount).orElse(null));
        dto.setSalePrice(Optional.ofNullable(productDefaultSku.getSalePrice()).map(Money::getAmount).orElse(null));
        dto.setQuantityAvailable(inventoryService.retrieveQuantityAvailable(productDefaultSku));
        dto.setAvailability(Optional.ofNullable(productDefaultSku.getInventoryType()).map(InventoryType::getType).orElse(null));
        dto.setIsAvailable(inventoryService.isAvailable(productDefaultSku,1));
        dto.setTaxCode(productDefaultSku.getTaxCode());
        dto.setCurrencyCode(Optional.ofNullable(productDefaultSku.getCurrency())
                                .orElse(localeService.findDefaultLocale().getDefaultCurrency())
                                .getCurrencyCode());

        final Map<String, MediaDto> defaultSkuMedias = productDefaultSku.getSkuMediaXref().entrySet().stream()
                        .collect(toMap(Map.Entry::getKey, entry -> mediaConverter.createDto(entry.getValue().getMedia(), embed, link)));

        dto.setMedia(defaultSkuMedias);

        dto.setCategoryName(Optional.ofNullable(product.getCategory()).map(Category::getName).orElse(""));

        dto.setLongDescription(Optional.ofNullable(product.getLongDescription()).orElse(""));

        dto.setDescription(Optional.ofNullable(product.getDescription()).orElse(""));
        dto.setOfferMessage(Optional.ofNullable(product.getPromoMessage()).orElse(""));
        dto.setManufacturer(Optional.ofNullable(product.getManufacturer()).orElse(""));
        dto.setModel(Optional.ofNullable(product.getModel()).orElse(""));

        Optional.ofNullable(product.getUrl()).ifPresent(dto::setUrl);

        dto.setValidFrom(Optional.ofNullable(product.getActiveStartDate()).map(Date::toInstant).map(instant -> instant.atZone(ZoneId.systemDefault())).orElse(null));
        dto.setValidTo(Optional.ofNullable(product.getActiveEndDate()).map(Date::toInstant).map(instant -> instant.atZone(ZoneId.systemDefault())).orElse(null));

        dto.setAttributes(product.getProductAttributes().entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> e.getValue().toString())));

        dto.setOptions(product.getProductOptionXrefs().stream().map(productOptionXrefToDto).collect(toList()));

        dto.setSkus(product.getAdditionalSkus().stream()
                .map(sku -> skuConverter.createDto(sku, embed, link)).collect(toList()));

        if (dto instanceof ProductBundleDto) {
            ProductBundle productBundle = (ProductBundle) product;

            ((ProductBundleDto) dto).setBundleItems(productBundle.getSkuBundleItems().stream()
                    .map(DtoConverters.skuBundleItemToBundleItemDto)
                    .collect(toList()));

            ((ProductBundleDto) dto).setBundleRetailPrice(productBundle.getRetailPrice().getAmount());
            ((ProductBundleDto) dto).setBundleSalePrice(productBundle.getSalePrice().getAmount());
            ((ProductBundleDto) dto).setPotentialSavings(productBundle.getPotentialSavings());
        }

        dto.add(ControllerLinkBuilder.linkTo(methodOn(ProductController.class).readOneProductById(product.getId(), null, null)).withSelfRel());

        if (link) {

            if (product.getDefaultSku() != null) {
                dto.add(linkTo(methodOn(SkuController.class).getSkuById(product.getId(), product.getDefaultSku().getId(), null, null))
                        .withRel("default-sku"));
            }

		/* skus link does not include default SKU! */
            if (product.getAdditionalSkus() != null && !product.getAdditionalSkus().isEmpty()) {
                for (Sku additionalSku : product.getAdditionalSkus()) {
                    if (!additionalSku.equals(product.getDefaultSku())) {
                        dto.add(linkTo(methodOn(SkuController.class).getSkuById(product.getId(), additionalSku.getId(), null, null))
                                .withRel("skus"));

                        //dto.add(linkTo(methodOn(ProductController.class).getMediaBySkuId(product.getId(), additionalSku.getId())).withRel("medias"));

                    }
                }
            }

        /* Links to the product's categories */
            if (product.getAllParentCategoryXrefs() != null && !product.getAllParentCategoryXrefs().isEmpty()) {
                product.getAllParentCategoryXrefs().stream()
                        .map(CategoryProductXref::getCategory)
                        .filter(CatalogUtils.shouldCategoryBeVisible)
                        .forEach(x -> dto.add(linkTo(methodOn(CategoryController.class).readOneCategoryById(null, x.getId(), null, null)).withRel("category")));
            }

            dto.add(linkTo(methodOn(ProductController.class).getProductDefaultSkuMedias(product.getId())).withRel("default-medias"));
        }

        if (embed) {
            dto.add(new EmbeddedResource(
                    "fulfillmentOptions",
                    Try.of(() -> fulfilmentServiceProxy.readFulfillmentOptionsWithPricesAvailableForProduct(product))
                            .map(fulfillmentOptionsMapConverter::createDto)
                            .get()
            ));
        }

        return dto;
    }

    @Override
    public Product createEntity(final ProductDto productDto) {
        final Product product = catalogService.createProduct(ProductType.PRODUCT);

        final Sku defaultSku = catalogService.createSku();
        product.setDefaultSku(defaultSku);

        updateEntity(product, productDto);

                /* (mst) if there is a default category set, try to find it and connect it with the product.
                 Otherwise just ignore it.
         */
        setCategoryIfPresent(productDto, product);

        setProductOptionXrefs(productDto, product, product);

        Optional.ofNullable(productDto.getSkus())
                .map(additionalSkus -> setAdditionalSkus(additionalSkus, product));

        return product;
    }

    @Override
    public Product updateEntity(final Product product, final ProductDto productDto) {

        if(product.getDefaultSku() == null) {
            throw new RuntimeException("Product Entity does not have any default SKU set!");
        }

        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setLongDescription(productDto.getLongDescription());
        product.setPromoMessage(productDto.getOfferMessage());
        product.setActiveStartDate(
                Optional.ofNullable(productDto.getValidFrom())
                        .map(ZonedDateTime::toInstant)
                        .map(Date::from)
                        .orElse(product.getDefaultSku().getActiveStartDate())
        );
        product.setActiveEndDate(
                Optional.ofNullable(productDto.getValidTo())
                        .map(ZonedDateTime::toInstant)
                        .map(Date::from)
                        .orElse(product.getDefaultSku().getActiveEndDate())
        );
        product.setModel(productDto.getModel());
        product.setManufacturer(productDto.getManufacturer());
        product.setUrl(productDto.getUrl());

        skuConverter.updateEntity(product.getDefaultSku(), DtoConverters.productDtoToDefaultSkuDto.apply(productDto));


       /* List<Sku> s;

        if (productDto.getSkus() != null && !productDto.getSkus().isEmpty()) {
            s = productDto.getSkus().stream()
                    .map(skuDtoToEntity)
                    .collect(toList());

            product.setAdditionalSkus(s);
        }*/


        if (productDto.getAttributes() != null) {

            product.setProductAttributes(
                    productDto.getAttributes().entrySet().stream().collect(toMap(Map.Entry::getKey, e -> {
                        ProductAttribute p = new ProductAttributeImpl();
                        p.setName(e.getKey());
                        p.setValue(e.getValue());
                        p.setProduct(product);
                        return p;
                    })));
        }

        return product;
    }

    private static Function<ProductOptionXref, ProductOptionDto> productOptionXrefToDto = input -> {
        final ProductOption productOption = input.getProductOption();

        final List<String> productOptionAllowedValues = Optional.ofNullable(productOption.getAllowedValues()).orElse(Collections.emptyList()).stream()
                .map(ProductOptionValue::getAttributeValue)
                .collect(toList());

        return ProductOptionDto.builder()
                .name(productOption.getAttributeName())
                .type(Optional.ofNullable(productOption.getType()).map(ProductOptionType::getType).orElse(null))
                .required(productOption.getRequired())
                .allowedValues(productOptionAllowedValues)
                .build();
    };


    private void setCategoryIfPresent(ProductDto productDto, Product newProduct) {
        Optional.ofNullable(productDto.getCategoryName())
                .filter(name -> !isNullOrEmpty(name))
                .map(name -> catalogService.findCategoriesByName(name))
                .map(Collection::stream).orElse(empty())
                .filter(CatalogUtils.shouldCategoryBeVisible)
                .findAny()
                .ifPresent(newProduct::setCategory);
    }

    private Product setAdditionalSkus(final List<SkuDto> additionalSkus, final Product newProduct) {

        final List<Sku> savedSkus = new ArrayList<>();
        savedSkus.addAll(newProduct.getAllSkus());

        for (SkuDto additionalSkuDto : additionalSkus) {

            Sku additionalSkuEntity = skuConverter.createEntity(additionalSkuDto);

            final Sku tempSkuEntityParam = additionalSkuEntity;

            additionalSkuEntity.setProductOptionValueXrefs(
                    Optional.ofNullable(additionalSkuDto.getSkuProductOptionValues()).orElse(Collections.emptySet()).stream()
                            .map(e -> generateXref(e, tempSkuEntityParam, newProduct))
                            .collect(toSet())
            );


            additionalSkuEntity.setProduct(newProduct);
//            additionalSkuEntity = catalogService.saveSku(additionalSkuEntity);
            savedSkus.add(additionalSkuEntity);
        }

        newProduct.setAdditionalSkus(savedSkus);
        return newProduct;
    }

    public SkuProductOptionValueXref generateXref(SkuProductOptionValueDto skuProductOption, Sku sku, Product product) {
        final ProductOption currentProductOption = Optional.ofNullable(getProductOptionByNameForProduct(
                skuProductOption.getAttributeName(),
                product))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product option: " + skuProductOption.getAttributeName() + " does not exist in product with ID: " + product.getId()
                ));

        final ProductOptionValue productOptionValue = Optional.ofNullable(getProductOptionValueByNameForProduct(
                        currentProductOption,
                        skuProductOption.getAttributeValue()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "'" + skuProductOption.getAttributeValue() + "'" + " is not an allowed value for option: " +
                                skuProductOption.getAttributeName() + " for product with ID: " + product.getId()
                ));


        return new SkuProductOptionValueXrefImpl(sku, productOptionValue);
    }

    private ProductOptionXref generateProductXref(ProductOptionDto productOptionDto, Product product) {
        final ProductOption p = catalogService.saveProductOption(productOptionDtoToEntity(productOptionDto));
        p.getAllowedValues().forEach(x -> x.setProductOption(p));
        p.setProductOptionValidationStrategyType(ProductOptionValidationStrategyType.ADD_ITEM);
        p.setRequired(true);

        final ProductOptionXref productOptionXref = new ProductOptionXrefImpl();
        productOptionXref.setProduct(product);
        productOptionXref.setProductOption(p);

        return productOptionXref;
    }

    private void setProductOptionXrefs(ProductDto productDto, Product newProduct, Product productParam) {
        final List<ProductOptionXref> productOptionXrefs = Optional.ofNullable(productDto.getOptions())
                .filter(e -> !e.isEmpty())
                .map(List::stream)
                .map(e -> e.map(x -> generateProductXref(x, productParam)))
                .map(e -> e.collect(toList()))
                .orElse(newProduct.getProductOptionXrefs());

        newProduct.setProductOptionXrefs(productOptionXrefs);
    }

    public ProductOption productOptionDtoToEntity(ProductOptionDto dto) {
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



}
