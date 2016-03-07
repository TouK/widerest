package pl.touk.widerest.api.products;

import org.broadleafcommerce.common.locale.service.LocaleService;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.catalog.domain.*;
import org.broadleafcommerce.core.inventory.service.type.InventoryType;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.categories.CategoryController;
import pl.touk.widerest.api.common.CatalogUtils;
import pl.touk.widerest.api.common.MediaConverter;
import pl.touk.widerest.api.common.MediaDto;
import pl.touk.widerest.api.products.skus.SkuConverter;

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
public class ProductConverter implements Converter<Product, ProductDto>{
    
    @Resource(name = "blLocaleService")
    protected LocaleService localeService;

    @Resource
    protected SkuConverter skuConverter;

    @Resource
    protected MediaConverter mediaConverter;

    @Override
    public ProductDto createDto(final Product product, final boolean embed) {
        final ProductDto dto = product instanceof ProductBundle ? new ProductBundleDto() : new ProductDto();

        dto.setName(product.getName());

        /* (mst) This should never be null */
        final Sku productDefaultSku = product.getDefaultSku();

        dto.setRetailPrice(Optional.ofNullable(productDefaultSku.getRetailPrice()).map(Money::getAmount).orElse(null));
        dto.setSalePrice(Optional.ofNullable(productDefaultSku.getSalePrice()).map(Money::getAmount).orElse(null));
        dto.setAvailability(Optional.ofNullable(productDefaultSku.getInventoryType()).map(InventoryType::getType).orElse(null));
        dto.setQuantityAvailable(productDefaultSku.getQuantityAvailable());
        dto.setTaxCode(productDefaultSku.getTaxCode());
        dto.setCurrencyCode(Optional.ofNullable(productDefaultSku.getCurrency())
                                .orElse(localeService.findDefaultLocale().getDefaultCurrency())
                                .getCurrencyCode());

        dto.setSkuAttributes(productDefaultSku.getSkuAttributes().entrySet().stream()
                                .collect(toMap(Map.Entry::getKey, e -> e.getValue().getName())));

        dto.setSkuProductOptionValues(productDefaultSku.getProductOptionValueXrefs().stream()
                                .map(SkuProductOptionValueXref::getProductOptionValue)
                                .map(DtoConverters.productOptionValueToSkuValueDto)
                                .collect(toSet()));

        final Map<String, MediaDto> defaultSkuMedias = productDefaultSku.getSkuMediaXref().entrySet().stream()
                        .collect(toMap(Map.Entry::getKey, entry -> mediaConverter.createDto(entry.getValue().getMedia(), false)));

        dto.setMedia(defaultSkuMedias);

        dto.setCategoryName(Optional.ofNullable(product.getCategory()).map(Category::getName).orElse(""));

        dto.setLongDescription(Optional.ofNullable(product.getLongDescription()).orElse(""));

        dto.setDescription(Optional.ofNullable(product.getDescription()).orElse(""));
        dto.setOfferMessage(Optional.ofNullable(product.getPromoMessage()).orElse(""));
        dto.setManufacturer(Optional.ofNullable(product.getManufacturer()).orElse(""));
        dto.setModel(Optional.ofNullable(product.getModel()).orElse(""));

        Optional.ofNullable(product.getUrl()).ifPresent(dto::setUrl);

        dto.setValidFrom(Optional.ofNullable(product.getActiveStartDate()).orElse(null));
        dto.setValidTo(Optional.ofNullable(product.getActiveEndDate()).orElse(null));

        dto.setAttributes(product.getProductAttributes().entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> e.getValue().toString())));

        dto.setOptions(product.getProductOptionXrefs().stream().map(productOptionXrefToDto).collect(toList()));

        dto.setSkus(product.getAdditionalSkus().stream()
                .map(sku -> skuConverter.createDto(sku, false)).collect(toList()));

        if (dto instanceof ProductBundleDto) {
            ProductBundle productBundle = (ProductBundle) product;

            ((ProductBundleDto) dto).setBundleItems(productBundle.getSkuBundleItems().stream()
                    .map(DtoConverters.skuBundleItemToBundleItemDto)
                    .collect(toList()));

            ((ProductBundleDto) dto).setBundleRetailPrice(productBundle.getRetailPrice().getAmount());
            ((ProductBundleDto) dto).setBundleSalePrice(productBundle.getSalePrice().getAmount());
            ((ProductBundleDto) dto).setPotentialSavings(productBundle.getPotentialSavings());
        }

        dto.add(ControllerLinkBuilder.linkTo(methodOn(ProductController.class).readOneProductById(product.getId())).withSelfRel());

        if (product.getDefaultSku() != null) {
            dto.add(linkTo(methodOn(ProductController.class).getSkuById(product.getId(), product.getDefaultSku().getId()))
                    .withRel("default-sku"));
        }

		/* skus link does not include default SKU! */
        if (product.getAdditionalSkus() != null && !product.getAdditionalSkus().isEmpty()) {
            for (Sku additionalSku : product.getAdditionalSkus()) {
                if (!additionalSku.equals(product.getDefaultSku())) {
                    dto.add(linkTo(methodOn(ProductController.class).getSkuById(product.getId(), additionalSku.getId()))
                            .withRel("skus"));

                    //dto.add(linkTo(methodOn(ProductController.class).getMediaBySkuId(product.getId(), additionalSku.getId())).withRel("medias"));

                }
            }
        }

        /* Links to the product's categories */
        if (product.getAllParentCategoryXrefs() != null && !product.getAllParentCategoryXrefs().isEmpty()) {
            product.getAllParentCategoryXrefs().stream()
                    .map(CategoryProductXref::getCategory)
                    .filter(CatalogUtils.nonArchivedCategory)
                    .forEach(x -> dto.add(linkTo(methodOn(CategoryController.class).readOneCategoryById(x.getId())).withRel("category")));
        }

        dto.add(linkTo(methodOn(ProductController.class).getProductByIdAttributes(product.getId())).withRel("attributes"));

        dto.add(linkTo(methodOn(ProductController.class).getProductDefaultSkuMedias(product.getId())).withRel("default-medias"));

        return dto;
    }

    @Override
    public Product createEntity(final ProductDto productDto) {
        final Product product = new ProductImpl();

        final Sku defaultSku = new SkuImpl();
        product.setDefaultSku(defaultSku);

        return updateEntity(product, productDto);
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
        product.setActiveStartDate(Optional.ofNullable(productDto.getValidFrom()).orElse(product.getDefaultSku().getActiveStartDate()));
        product.setActiveEndDate(Optional.ofNullable(productDto.getValidTo()).orElse(product.getDefaultSku().getActiveEndDate()));
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
                .allowedValues(productOptionAllowedValues)
                .build();
    };

}
