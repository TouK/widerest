package pl.touk.widerest.api.catalog;


import org.broadleafcommerce.core.catalog.domain.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class DtoConverters {
    public static Function<ProductAttribute, String> getProductAttributeName = input -> {
        return input.getValue();
    };

    public static Function<org.broadleafcommerce.core.catalog.domain.ProductOptionValue, String> getProductOptionValueName = input -> {
        return input.getAttributeValue();
    };

    public static Function<ProductOptionXref, ProductOption> productOptionXrefToDto = input -> {
        org.broadleafcommerce.core.catalog.domain.ProductOption productOption = input.getProductOption();
        //dto.setName(productOption.getAttributeName());
        List<ProductOptionValue> productOptionValues = productOption.getAllowedValues();
        List<String> collectAllowedValues = productOptionValues.stream().map(getProductOptionValueName).collect(toList());
        //dto.setAllowedValues(collectAllowedValues);
        return new ProductOption(productOption.getAttributeName(), collectAllowedValues);
    };

    public static Function<Sku, SkuDto> skuToDto = input -> {
        SkuDto dto = SkuDto.builder()
                .id(input.getId())
                .description(input.getDescription())
                .price(input.getPrice().getAmount())
                .quantityAvailable(input.getQuantityAvailable())
                .code(input.getTaxCode()).build();
        //TODO: selection
        return dto;
    };

    public static Function<org.broadleafcommerce.core.catalog.domain.Product, Product> productEntityToDto
            = entity -> {
        Product dto = new Product();
        if(entity.getDefaultCategory() != null) {
            dto.setCategory(entity.getDefaultCategory().getName());
        }
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setId(entity.getId());
        if(entity.getLongDescription() != null && !entity.getLongDescription().isEmpty()) {
            dto.setLongDescription(entity.getLongDescription());
        }

        dto.setValidFrom(entity.getActiveStartDate());
        dto.setValidTo(entity.getActiveEndDate());
        // TODO: Mozliwe ze offer message nie istnieje? Czy trzeba wyrzucic wyjatek?
        dto.setOfferMessage(entity.getPromoMessage());

        
        Map<String, String> productAttributesCollect = entity.getProductAttributes().entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> e.getValue().toString()));
        dto.setAttributes(productAttributesCollect);

        List<ProductOption> productOptionsCollect = entity.getProductOptionXrefs().stream().map(productOptionXrefToDto)
                .collect(toList());
        dto.setOptions(productOptionsCollect);

        // TODO: czy potrzeba cos inaczej ustawic, jako ze jest defaultCategory? - TAK?, na poczatek listy domyslny
        List<SkuDto> skuDtosCollect = entity.getAllSkus().stream()
                .map(skuToDto).collect(toList());
        dto.setSkus(skuDtosCollect);

        //SkuDto defaultSku = skuToDto.apply(entity.getDefaultSku());
        /*Collection<ProductBundle> possibleBundles = Lists.transform(
                ((VirginSkuImpl) defaultSku).getSkuBundleItems(),
                new Function<SkuBundleItem, ProductBundle>() {
                    @Nullable
                    @Override
                    public ProductBundle apply(@Nullable SkuBundleItem input) {
                        return input.getBundle();
                    }
                }
        );
        possibleBundles = Collections2.filter(
                possibleBundles,
                new Predicate<ProductBundle>() {
                    @Override
                    public boolean apply(@Nullable ProductBundle input) {
                        return ((SkuDto) input.getDefaultSku()).getDefaultProductBundle() == null;
                    }
                }
        );
        dto.setPossibleBundles(Lists.newArrayList(Iterables.transform(
                possibleBundles,
                new Function<ProductBundle, Long>() {
                    @Nullable
                    @Override
                    public Long apply(@Nullable ProductBundle input) {
                        return input.getId();
                    }
                }
        )));
*/

        return dto;
    };
}
