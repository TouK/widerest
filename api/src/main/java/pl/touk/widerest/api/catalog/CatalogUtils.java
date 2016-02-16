package pl.touk.widerest.api.catalog;

import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.persistence.Status;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.CategoryAttribute;
import org.broadleafcommerce.core.catalog.domain.CategoryAttributeImpl;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.inventory.service.type.InventoryType;

import pl.touk.widerest.api.catalog.products.dto.SkuDto;
import pl.touk.widerest.api.catalog.exceptions.DtoValidationException;
import pl.touk.widerest.api.catalog.products.dto.ProductDto;

public class CatalogUtils {

    public static final String EMPTY_STRING = "";

    public static boolean archivedProductFilter(Product product) {
        return ((Status) product).getArchived() == 'N';
    }

    public static boolean archivedCategoryFilter(Category category) {
        return ((Status) category).getArchived() == 'N';
    }

    public static InventoryType getInventoryTypeByAvailability(final String availability) {
        final InventoryType inventoryType = InventoryType.getInstance(availability);
        return (inventoryType != null) ? inventoryType : InventoryType.ALWAYS_AVAILABLE;
    }


//    public static Category partialUpdateCategoryEntityFromDto(
//            Category categoryEntity, CategoryDto categoryDto) {
//
//        if (categoryDto.getName() != null) {
//            categoryEntity.setName(categoryDto.getName());
//        }
//
//        if (categoryDto.getDescription() != null) {
//            categoryEntity.setDescription(categoryDto.getDescription());
//        }
//
//        if (categoryDto.getLongDescription() != null) {
//            categoryEntity.setLongDescription(categoryDto.getLongDescription());
//        }
//
//        if(categoryDto.getProductsAvailability() != null) {
//            categoryEntity.setInventoryType(getInventoryTypeByAvailability(categoryDto.getProductsAvailability()));;
//        }
//
//        if(categoryDto.getAttributes() != null) {
//            categoryEntity.getCategoryAttributesMap().clear();
//            categoryEntity.getCategoryAttributesMap().putAll(
//                    Optional.ofNullable(categoryDto.getAttributes()).orElse(Collections.emptyMap()).entrySet().stream()
//                            .collect(toMap(Map.Entry::getKey, valueExtractor(categoryEntity))));
//        }
//
//        return categoryEntity;
//    }

    public static Function<Map.Entry<String, String>, CategoryAttribute> valueExtractor(Category categoryEntity) {
        return e -> {
            CategoryAttribute a = new CategoryAttributeImpl();
            a.setName(e.getKey());
            a.setValue(e.getValue());
            a.setCategory(categoryEntity);
            return a;
        };
    }

//    public static Sku updateSkuEntityFromDto(Sku skuEntity, SkuDto skuDto) {
//
//        skuEntity.setName(skuDto.getName());
//        skuEntity.setDescription(skuDto.getDescription());
//        skuEntity.setSalePrice(new Money(skuDto.getSalePrice()));
//        skuEntity.setQuantityAvailable(skuDto.getQuantityAvailable());
//        skuEntity.setTaxCode(skuDto.getTaxCode());
//        skuEntity.setActiveStartDate(skuDto.getActiveStartDate());
//        skuEntity.setActiveEndDate(skuDto.getActiveEndDate());
//
//		/*
//		 * (mst) RetailPrice cannot be null, so just leave "the old" value if a
//		 * new one has not been provided
//		 */
//        if (skuDto.getRetailPrice() != null) {
//            skuEntity.setRetailPrice(new Money(skuDto.getRetailPrice()));
//        } else {
//            skuEntity.setRetailPrice(new Money(skuDto.getSalePrice()));
//        }
//
//        if(skuDto.getAvailability() != null && InventoryType.getInstance(skuDto.getAvailability()) != null) {
//            skuEntity.setInventoryType(InventoryType.getInstance(skuDto.getAvailability()));
//        } else {
//            /* (mst) turn on Inventory Service by default */
//            skuEntity.setInventoryType(InventoryType.ALWAYS_AVAILABLE);
//        }
//
//
//        skuEntity.getSkuAttributes().clear();
//        skuEntity.getSkuAttributes().putAll(
//                Optional.ofNullable(skuDto.getSkuAttributes()).orElse(Collections.emptyMap()).entrySet().stream()
//                        .collect(toMap(Map.Entry::getKey, e -> {
//                            SkuAttribute s = new SkuAttributeImpl();
//                            s.setName(e.getKey());
//                            s.setValue(e.getValue());
//                            s.setSku(skuEntity);
//                            return s;
//                        })));
//
//
//        if(skuDto.getSkuMedia() != null) {
//            skuEntity.setSkuMediaXref(
//                    skuDto.getSkuMedia().entrySet().stream()
//                            .collect(toMap(Map.Entry::getKey, e -> {
//                                SkuMediaXref newSkuMediaXref = DtoConverters.skuMediaDtoToXref.apply(e.getValue());
//                                newSkuMediaXref.setSku(skuEntity);
//                                newSkuMediaXref.setKey(e.getKey());
//                                return newSkuMediaXref;
//                            })));
//        }
//
//        return skuEntity;
//    }

    public static Sku partialUpdateSkuEntityFromDto(Sku skuEntity, SkuDto skuDto) {

        if (skuDto.getName() != null) {
            skuEntity.setName(skuDto.getName());
        }

        if (skuDto.getDescription() != null) {
            skuEntity.setDescription(skuDto.getDescription());
        }
        if (skuDto.getSalePrice() != null) {
            skuEntity.setSalePrice(new Money(skuDto.getSalePrice()));
        }
        if (skuDto.getQuantityAvailable() != null) {
            skuEntity.setQuantityAvailable(skuDto.getQuantityAvailable());
        }
        if (skuDto.getTaxCode() != null) {
            skuEntity.setTaxCode(skuDto.getTaxCode());
        }
        if (skuDto.getActiveStartDate() != null) {
            skuEntity.setActiveStartDate(skuDto.getActiveStartDate());
        }
        if (skuDto.getActiveEndDate() != null) {
            skuEntity.setActiveEndDate(skuDto.getActiveEndDate());
        }

        if (skuDto.getRetailPrice() != null) {
            skuEntity.setRetailPrice(new Money(skuDto.getRetailPrice()));
        }



        return skuEntity;
    }

//    public static Media updateMediaEntityFromDto(Media mediaEntity, MediaDto mediaDto) {
//
//        mediaEntity.setTitle(mediaDto.getTitle());
//        mediaEntity.setTags(mediaDto.getTags());
//        mediaEntity.setAltText(mediaDto.getAltText());
//        mediaEntity.setUrl(mediaDto.getUrl());
//
//        return mediaEntity;
//    }

//    public static Media partialUpdateMediaEntityFromDto(Media mediaEntity, MediaDto mediaDto) {
//
//        if(mediaDto.getTitle() != null) {
//            mediaEntity.setTitle(mediaDto.getTitle());
//        }
//
//        if(mediaDto.getTags() != null) {
//            mediaEntity.setTags(mediaDto.getTags());
//        }
//
//        if(mediaDto.getAltText() != null) {
//            mediaEntity.setAltText(mediaDto.getAltText());
//        }
//
//        if(mediaDto.getUrl() != null) {
//            mediaEntity.setUrl(mediaDto.getUrl());
//        }
//
//        return mediaEntity;
//    }

//    public static Product updateProductEntityFromDto(Product productEntity, ProductDto productDto) {
//        productEntity.setName(productDto.getName());
//        productEntity.setDescription(productDto.getDescription());
//        productEntity.setLongDescription(productDto.getLongDescription());
//        productEntity.setPromoMessage(productDto.getOfferMessage());
//        productEntity.setActiveStartDate(Optional.ofNullable(productDto.getValidFrom()).orElse(productEntity.getDefaultSku().getActiveStartDate()));
//        productEntity.setActiveEndDate(Optional.ofNullable(productDto.getValidTo()).orElse(productEntity.getDefaultSku().getActiveEndDate()));
//        productEntity.setModel(productDto.getModel());
//        productEntity.setManufacturer(productDto.getManufacturer());
//        productEntity.setUrl(productDto.getUrl());
//
//       /* List<Sku> s;
//
//        if (productDto.getSkus() != null && !productDto.getSkus().isEmpty()) {
//            s = productDto.getSkus().stream()
//                    .map(skuDtoToEntity)
//                    .collect(toList());
//
//            product.setAdditionalSkus(s);
//        }*/
//
//
//        if (productDto.getAttributes() != null) {
//
//            productEntity.setProductAttributes(
//                    productDto.getAttributes().entrySet().stream().collect(toMap(Map.Entry::getKey, e -> {
//                        ProductAttribute p = new ProductAttributeImpl();
//                        p.setName(e.getKey());
//                        p.setValue(e.getValue());
//                        p.setProduct(productEntity);
//                        return p;
//                    })));
//        }
//
//        return productEntity;
//    }

    public static void validateSkuPrices(final BigDecimal salePrice, final BigDecimal retailPrice) throws DtoValidationException {

        if(salePrice == null && retailPrice == null) {
            throw new DtoValidationException("Product's SKU has to have a price");
        }

        if((salePrice != null && salePrice.longValue() < 0) ||
                (retailPrice != null && retailPrice.longValue() < 0)) {
            throw new DtoValidationException("Sku's prices cannot be negative");
        }
    }

    public static void validateProductDto(final ProductDto productDto) throws DtoValidationException {

        validateSkuPrices(productDto.getSalePrice(), productDto.getRetailPrice());

        if(productDto.getName() == null || productDto.getName().isEmpty()) {
            throw new DtoValidationException("Product has to have a name");
        }
    }

    public static long getIdFromUrl(final String categoryPathUrl) throws MalformedURLException, DtoValidationException, NumberFormatException {
        final URL categoryPathURL = new URL(categoryPathUrl);

        final String categoryPath = categoryPathURL.getPath();

        final int lastSlashIndex = categoryPath.lastIndexOf('/');

        if(lastSlashIndex < 0 || (lastSlashIndex + 1) >= categoryPath.length()) {
            throw new DtoValidationException();
        }

        return Long.parseLong(categoryPath.substring(lastSlashIndex + 1));
    }

    /*
        (mst)

        Limits:
                0 : unlimited

     */
    public static <T> List<T> getSublistForOffset(final List<T> list, final int offset, final int limit) {

        final int listSize = list.size();

        if(listSize == 0) {
            return list;
        }

        /* (mst) Rather than just throwing an exception, we'll set some 'default' values instead */

//        if(offset < 0 || limit < 0) {
//            throw new IllegalArgumentException("Offset/Limit must be >= 0");
//        }

        int offsetParam = offset;
        int limitParam  = limit;

        if(offset < 0) {
            offsetParam = 0;
        }

        if(limit < 0) {
            limitParam = 0;
        }

        if(offsetParam > 0) {

            if(offsetParam >= listSize) {
                return list.subList(0, 0);
            }

            if(limitParam > 0) {
                return list.subList(offsetParam, Math.min(offsetParam + limitParam, listSize));
            } else {
                return list.subList(offsetParam, list.size());
            }
        } else if(limitParam > 0) {
            return list.subList(0, Math.min(limitParam, listSize));
        } else {
            return list.subList(0, listSize);
        }
    }

}