package pl.touk.widerest.api.cart.service;

import org.broadleafcommerce.common.locale.service.LocaleService;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.catalog.domain.*;
import org.springframework.stereotype.Service;
import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.catalog.controllers.CategoryController;
import pl.touk.widerest.api.catalog.controllers.ProductController;
import pl.touk.widerest.api.catalog.dto.BundleDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.dto.SkuDto;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Service("wdSkuService")
public class SkuServiceProxy {

    @Resource(name = "blLocaleService")
    private LocaleService localeService;

    public Function<Sku, SkuDto> skuEntityToDto = entity -> {
        // Na przyszlosc: jesli dostanie sie wartosc z errCoda to znaczy
        // ze dana wartosc nie ustawiona => admin widzi objekt, klient nie
        Money errCode = new Money(BigDecimal.valueOf(-1337));

        SkuDto dto = SkuDto.builder()
                .skuId(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .salePrice(Optional.ofNullable(entity.getPrice()).orElse(errCode).getAmount())
                .quantityAvailable(entity.getQuantityAvailable()).taxCode(entity.getTaxCode())
                .activeStartDate(entity.getActiveStartDate()).activeEndDate(entity.getActiveEndDate())
                .currencyCode(Optional.ofNullable(entity.getCurrency())
                        .orElse(localeService.findDefaultLocale().getDefaultCurrency())
                        .getCurrencyCode())
                .skuAttributes(entity.getSkuAttributes().entrySet().stream()
                        .collect(toMap(Map.Entry::getKey, e -> {
                            return e.getValue().getName();
                        })))
                .productOptionValues(entity.getProductOptionValueXrefs().stream()
                        .map(SkuProductOptionValueXref::getProductOptionValue)
                        .map(DtoConverters.productOptionValueEntityToDto)
                        .collect(toSet()))
                .build();

        // selection wysylany jest tylko od klienta
        dto.add(linkTo(methodOn(ProductController.class).getSkuById(entity.getProduct().getId(), entity.getId()))
                .withSelfRel());
        return dto;
    };


    public Function<Product, ProductDto> productEntityToDto = entity -> {

        ProductDto dto = null;

        if (entity instanceof ProductBundle) {
            dto = new BundleDto();
        } else {
            dto = new ProductDto();
        }

		/* (mst) Do we really need ProductID? */
        dto.setProductId(entity.getId());

        dto.setName(entity.getName());

		/*
		 * TODO: (mst) Do we need the entire CategoryDto here or Category name +
		 * HATEAOS link will do the job?
		 *
		 * if(entity.getDefaultCategory() != null) {
		 * dto.setCategory(categoryEntityToDto.apply(entity.getDefaultCategory()
		 * )); }
		 */

        // if(entity.getCategory() != null)
        // dto.setCategoryName(Optional.ofNullable(entity.getCategory().getName()).orElse(""));

		/*
		 * TODO: REMOVE if(entity.getLongDescription() != null &&
		 * !entity.getLongDescription().isEmpty()) {
		 * dto.setLongDescription(entity.getLongDescription()); }
		 */

        dto.setLongDescription(Optional.ofNullable(entity.getLongDescription()).orElse(""));

		/*
		 * TODO: REMOVE if(entity.getPromoMessage() != null &&
		 * !entity.getPromoMessage().isEmpty()) {
		 * dto.setOfferMessage(entity.getPromoMessage()); }
		 */

        dto.setDescription(Optional.ofNullable(entity.getDescription()).orElse(""));
        dto.setOfferMessage(Optional.ofNullable(entity.getPromoMessage()).orElse(""));
        dto.setManufacturer(Optional.ofNullable(entity.getManufacturer()).orElse(""));
        dto.setModel(Optional.ofNullable(entity.getModel()).orElse(""));

        dto.setValidFrom(Optional.ofNullable(entity.getActiveStartDate()).orElse(null));
        dto.setValidTo(Optional.ofNullable(entity.getActiveEndDate()).orElse(null));

		/* (Map<String, String>) */
        dto.setAttributes(entity.getProductAttributes().entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> e.getValue().toString())));

        dto.setOptions(entity.getProductOptionXrefs().stream().map(DtoConverters.productOptionXrefToDto).collect(toList()));

        dto.setDefaultSku(skuEntityToDto.apply(entity.getDefaultSku()));

		/* (mst) As far as I know, this DOES include Default SKU */
        dto.setSkus(entity.getAllSkus().stream().map(skuEntityToDto).collect(toList()));

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

        if (dto instanceof BundleDto) {
            ProductBundle productBundle = (ProductBundle) entity;

            ((BundleDto) dto).setBundleItems(
                    productBundle.getSkuBundleItems().stream().map(DtoConverters.skuBundleItemToBundleItemDto).collect(toList()));

            ((BundleDto) dto).setBundlePrice(productBundle.getSalePrice().getAmount());
            ((BundleDto) dto).setPotentialSavings(productBundle.getPotentialSavings());
        }

		/* HATEOAS links */
        dto.add(linkTo(methodOn(ProductController.class).readOneProductById(entity.getId())).withSelfRel());

        if (entity.getDefaultSku() != null) {
            dto.add(linkTo(methodOn(ProductController.class).getSkuById(entity.getId(), entity.getDefaultSku().getId()))
                    .withRel("default-sku"));
        }

		/* skus link does not include default SKU! */
        if (entity.getAdditionalSkus() != null && !entity.getAdditionalSkus().isEmpty()) {
            for (Sku additionalSku : entity.getAdditionalSkus()) {
                if (!additionalSku.equals(entity.getDefaultSku())) {
                    dto.add(linkTo(methodOn(ProductController.class).getSkuById(entity.getId(), additionalSku.getId()))
                            .withRel("skus"));
                }
            }
        }

		/*
		 * TODO: (mst) REMOVE because AllParentCategoryRefs() already has the
		 * DefaultCategory if(entity.getCategory() != null) {
		 * dto.add(linkTo(methodOn(CategoryController.class).readOneCategoryById
		 * (entity.getCategory().getId())).withRel("category")); }
		 */

		/* Links to the product's categories */
        if (entity.getAllParentCategoryXrefs() != null && !entity.getAllParentCategoryXrefs().isEmpty()) {
            for (CategoryProductXref parentCategoryXrefs : entity.getAllParentCategoryXrefs()) {
                dto.add(linkTo(methodOn(CategoryController.class)
                        .readOneCategoryById(parentCategoryXrefs.getCategory().getId())).withRel("category"));
            }
        }

        return dto;
    };
}
