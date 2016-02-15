package pl.touk.widerest.api.products;

import org.broadleafcommerce.core.catalog.domain.*;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.catalog.CatalogUtils;
import pl.touk.widerest.api.catalog.dto.ProductBundleDto;
import pl.touk.widerest.api.categories.CategoryController;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class ProductConverter implements Converter<Product, ProductDto>{
    
    @Resource
    protected SkuConverter skuConverter;
    
    @Override
    public ProductDto createDto(final Product product, final boolean embed) {
        final ProductDto dto = product instanceof ProductBundle ? new ProductBundleDto() : new ProductDto();

        dto.setName(product.getName());

        // TODO: Create default SKU

        //dto.setDefaultSku(skuConverter.createDto(product.getDefaultSku(), false));

        dto.setCategoryName(Optional.ofNullable(product.getCategory()).map(Category::getName).orElse(""));

        dto.setLongDescription(Optional.ofNullable(product.getLongDescription()).orElse(""));

        dto.setDescription(Optional.ofNullable(product.getDescription()).orElse(""));
        dto.setOfferMessage(Optional.ofNullable(product.getPromoMessage()).orElse(""));
        dto.setManufacturer(Optional.ofNullable(product.getManufacturer()).orElse(""));
        dto.setModel(Optional.ofNullable(product.getModel()).orElse(""));

        Optional.ofNullable(product.getUrl()).ifPresent(dto::setUrl);

        dto.setValidFrom(Optional.ofNullable(product.getActiveStartDate()).orElse(null));
        dto.setValidTo(Optional.ofNullable(product.getActiveEndDate()).orElse(null));

		/* (Map<String, String>) */
        dto.setAttributes(product.getProductAttributes().entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> e.getValue().toString())));

        dto.setOptions(product.getProductOptionXrefs().stream().map(DtoConverters.productOptionXrefToDto).collect(toList()));


        dto.setSkus(product.getAdditionalSkus().stream().map(sku -> skuConverter.createDto(sku, false)).collect(toList()));


		/* TODO: (mst) Implement Possible Bundles */

		/*
		 * Collection<ProductBundle> possibleBundles = Lists.transform(
		 * ((VirginSkuImpl) defaultSku).getSkuBundleItems(), new
		 * Function<SkuBundleItem, ProductBundle>() {
		 *
		 * @Nullable
		 *
		 * @Override public ProductBundle apply(@Nullable SkuBundleItem input) {
		 * return input.getBundle(); } } ); possibleBundles =
		 * Collections2.filter( possibleBundles, new Predicate<ProductBundle>()
		 * {
		 *
		 * @Override public boolean apply(@Nullable ProductBundle input) {
		 * return ((VirginSku) input.getDefaultSku()).getDefaultProductBundle()
		 * == null; } } );
		 * dto.setPossibleBundles(Lists.newArrayList(Iterables.transform(
		 * possibleBundles, new Function<ProductBundle, Long>() {
		 *
		 * @Nullable
		 *
		 * @Override public Long apply(@Nullable ProductBundle input) { return
		 * input.getId(); } } )));
		 */

        if (dto instanceof ProductBundleDto) {
            ProductBundle productBundle = (ProductBundle) product;

            ((ProductBundleDto) dto).setBundleItems(productBundle.getSkuBundleItems().stream()
                    .map(DtoConverters.skuBundleItemToBundleItemDto)
                    .collect(toList()));

            ((ProductBundleDto) dto).setBundleRetailPrice(productBundle.getRetailPrice().getAmount());
            ((ProductBundleDto) dto).setBundleSalePrice(productBundle.getSalePrice().getAmount());
            ((ProductBundleDto) dto).setPotentialSavings(productBundle.getPotentialSavings());
        }

		/* HATEOAS links */
        dto.add(linkTo(methodOn(ProductController.class).readOneProductById(product.getId())).withSelfRel());

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
                }
            }
        }

        /* Links to the product's categories */
        if (product.getAllParentCategoryXrefs() != null && !product.getAllParentCategoryXrefs().isEmpty()) {
            product.getAllParentCategoryXrefs().stream()
                    .map(CategoryProductXref::getCategory)
                    .filter(CatalogUtils::archivedCategoryFilter)
                    .forEach(x -> dto.add(linkTo(methodOn(CategoryController.class).readOneCategoryById(x.getId())).withRel("category")));
        }

        dto.add(linkTo(methodOn(ProductController.class).getProductByIdAttributes(product.getId())).withRel("attributes"));

        return dto;
    }

    @Override
    public Product createEntity(final ProductDto productDto) {
        final Product product = new ProductImpl();

        final Sku defaultSku = new SkuImpl();
        product.setDefaultSku(defaultSku);

        // TODO:
        //product.setDefaultSku(skuConverter.createEntity(productDto.getDefaultSku()));

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


}
