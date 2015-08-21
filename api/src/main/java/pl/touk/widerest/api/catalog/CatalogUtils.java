package pl.touk.widerest.api.catalog;

import org.broadleafcommerce.common.media.domain.Media;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.persistence.Status;
import org.broadleafcommerce.core.catalog.domain.*;

import org.broadleafcommerce.core.inventory.service.type.InventoryType;
import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.SkuDto;
import pl.touk.widerest.api.catalog.dto.SkuMediaDto;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;

/**
 * Created by mst on 27.07.15.
 */
public class CatalogUtils {




    public static final String EMPTY_STRING = "";

    public static boolean archivedProductFilter(Product product) {
        return ((Status) product).getArchived() == 'N';
    }

    public static boolean archivedCategoryFilter(Category category) {
        return ((Status) category).getArchived() == 'N';
    }

    public static InventoryType getInventoryTypeByAvailability(String availability) {
        final InventoryType inventoryType = InventoryType.getInstance(availability);
        return (inventoryType != null) ? inventoryType : InventoryType.ALWAYS_AVAILABLE;
    }

    public static Category updateCategoryEntityFromDto(Category categoryEntity,
                                                       CategoryDto categoryDto) {

        categoryEntity.setName(categoryDto.getName());
        categoryEntity.setDescription(categoryDto.getDescription());
        categoryEntity.setLongDescription(categoryDto.getLongDescription());


        if(categoryDto.getProductsAvailability() != null) {
            categoryEntity.setInventoryType(getInventoryTypeByAvailability(categoryDto.getProductsAvailability()));
        } else {
            /* (mst) Remove this if you don't want to have a "default" availability set */
            categoryEntity.setInventoryType(InventoryType.ALWAYS_AVAILABLE);
        }

        categoryEntity.getCategoryAttributesMap().clear();
        categoryEntity.getCategoryAttributesMap().putAll(
                Optional.ofNullable(categoryDto.getAttributes()).orElse(Collections.emptyMap()).entrySet().stream()
                        .collect(toMap(Map.Entry::getKey, e -> {
                            CategoryAttribute a = new CategoryAttributeImpl();
                            a.setName(e.getKey());
                            a.setValue(e.getValue());
                            a.setCategory(categoryEntity);
                            return a;
                        })));

        return categoryEntity;
    }

    public static Category partialUpdateCategoryEntityFromDto(
            Category categoryEntity, CategoryDto categoryDto) {

        if (categoryDto.getName() != null) {
            categoryEntity.setName(categoryDto.getName());
        }

        if (categoryDto.getDescription() != null) {
            categoryEntity.setDescription(categoryDto.getDescription());
        }

        if (categoryDto.getLongDescription() != null) {
            categoryEntity.setLongDescription(categoryDto.getLongDescription());
        }

        if(categoryDto.getProductsAvailability() != null) {
            categoryEntity.setInventoryType(getInventoryTypeByAvailability(categoryDto.getProductsAvailability()));;
        }

        if(categoryDto.getAttributes() != null) {
            categoryEntity.getCategoryAttributesMap().clear();
            categoryEntity.getCategoryAttributesMap().putAll(
                    Optional.ofNullable(categoryDto.getAttributes()).orElse(Collections.emptyMap()).entrySet().stream()
                            .collect(toMap(Map.Entry::getKey, e -> {
                                CategoryAttribute a = new CategoryAttributeImpl();
                                a.setName(e.getKey());
                                a.setValue(e.getValue());
                                a.setCategory(categoryEntity);
                                return a;
                            })));
        }

        return categoryEntity;
    }

    public static Sku updateSkuEntityFromDto(Sku skuEntity, SkuDto skuDto) {

        skuEntity.setName(skuDto.getName());
        skuEntity.setDescription(skuDto.getDescription());
        skuEntity.setSalePrice(new Money(skuDto.getSalePrice()));
        skuEntity.setQuantityAvailable(skuDto.getQuantityAvailable());
        skuEntity.setTaxCode(skuDto.getTaxCode());
        skuEntity.setActiveStartDate(skuDto.getActiveStartDate());
        skuEntity.setActiveEndDate(skuDto.getActiveEndDate());

		/*
		 * (mst) RetailPrice cannot be null, so just leave "the old" value if a
		 * new one has not been provided
		 */
        if (skuDto.getRetailPrice() != null) {
            skuEntity.setRetailPrice(new Money(skuDto.getRetailPrice()));
        } else {
            skuEntity.setRetailPrice(new Money(skuDto.getSalePrice()));
        }

        if(skuDto.getAvailability() != null && InventoryType.getInstance(skuDto.getAvailability()) != null) {
            skuEntity.setInventoryType(InventoryType.getInstance(skuDto.getAvailability()));
        } else {
            /* (mst) turn on Inventory Service by default */
            skuEntity.setInventoryType(InventoryType.ALWAYS_AVAILABLE);
        }


        skuEntity.getSkuAttributes().clear();
        skuEntity.getSkuAttributes().putAll(
                Optional.ofNullable(skuDto.getSkuAttributes()).orElse(Collections.emptyMap()).entrySet().stream()
                        .collect(toMap(Map.Entry::getKey, e -> {
                            SkuAttribute s = new SkuAttributeImpl();
                            s.setName(e.getKey());
                            s.setValue(e.getValue());
                            s.setSku(skuEntity);
                            return s;
                        })));


        if(skuDto.getSkuMedia() != null) {
            skuEntity.setSkuMediaXref(
                    skuDto.getSkuMedia().stream()
                            .collect(toMap(e -> e.getKey(), e -> {
                                SkuMediaXref newSkuMediaXref = DtoConverters.skuMediaDtoToXref.apply(e);
                                newSkuMediaXref.setSku(skuEntity);
                                newSkuMediaXref.setKey(e.getKey());
                                return newSkuMediaXref;
                            })));
        }

        return skuEntity;
    }

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

    public static Media updateMediaEntityFromDto(Media mediaEntity, SkuMediaDto skuMediaDto) {

        mediaEntity.setTitle(skuMediaDto.getTitle());
        mediaEntity.setTags(skuMediaDto.getTags());
        mediaEntity.setAltText(skuMediaDto.getAltText());
        mediaEntity.setUrl(skuMediaDto.getUrl());

        return mediaEntity;
    }

    public static Media partialUpdateMediaEntityFromDto(Media mediaEntity, SkuMediaDto skuMediaDto) {

        if(skuMediaDto.getTitle() != null) {
            mediaEntity.setTitle(skuMediaDto.getTitle());
        }

        if(skuMediaDto.getTags() != null) {
            mediaEntity.setTags(skuMediaDto.getTags());
        }

        if(skuMediaDto.getAltText() != null) {
            mediaEntity.setAltText(skuMediaDto.getAltText());
        }

        if(skuMediaDto.getUrl() != null) {
            mediaEntity.setUrl(skuMediaDto.getUrl());
        }

        return mediaEntity;
    }


}