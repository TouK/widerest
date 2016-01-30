package pl.touk.widerest.api.categories;

import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.CategoryAttribute;
import org.broadleafcommerce.core.catalog.domain.CategoryAttributeImpl;
import org.broadleafcommerce.core.catalog.domain.CategoryImpl;
import org.broadleafcommerce.core.catalog.domain.CategoryMediaXrefImpl;
import org.broadleafcommerce.core.catalog.domain.CategoryXref;
import org.broadleafcommerce.core.inventory.service.type.InventoryType;
import org.springframework.hateoas.EmbeddedResource;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.catalog.CatalogUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class CategoryConverter implements Converter<Category, CategoryDto> {

    @Override
    public CategoryDto createDto(Category entity, boolean embed) {
        final CategoryDto dto = CategoryDto.builder()
                .name(entity.getName())
                .description(entity.getDescription())
                .longDescription(entity.getLongDescription())
                .productsAvailability(Optional.ofNullable(entity.getInventoryType())
                        .map(InventoryType::getType)
                        .orElse(null))
                .attributes(entity.getCategoryAttributesMap().entrySet().stream()
                        .collect(toMap(Map.Entry::getKey, e -> e.getValue().toString())))
                .media(entity.getCategoryMediaXref().entrySet().stream().collect(toMap(Map.Entry::getKey, e -> DtoConverters.categoryMediaXrefToDto.apply(e.getValue()))))
                .build();


        dto.add(linkTo(methodOn(CategoryController.class).readOneCategoryById(entity.getId())).withSelfRel());

        dto.add(linkTo(methodOn(CategoryController.class).readProductsFromCategory(entity.getId())).withRel("products"));

//        dto.add(linkTo(methodOn(CategoryController.class).getCategoryByIdAvailability(entity.getId())).withRel("availability"));

//        dto.add(linkTo(methodOn(CategoryController.class).getAllProductsInCategoryCount(entity.getId())).withRel("products-count"));

//        dto.add(linkTo(methodOn(CategoryController.class).getAllCategoriesCount(null)).withRel("categories-count"));

        final List<Link> subcategoriesLinks = Optional.ofNullable(entity.getAllChildCategoryXrefs())
                .orElse(Collections.emptyList()).stream()
                .map(CategoryXref::getSubCategory)
                .map(x -> {
                    return linkTo(methodOn(CategoryController.class).readOneCategoryById(x.getId())).withRel("subcategories");
                })
                .collect(toList());

        dto.add(subcategoriesLinks);

        final List<Link> parentCategoriesLinks = Optional.ofNullable(entity.getAllParentCategoryXrefs())
                .orElse(Collections.emptyList()).stream()
                .map(CategoryXref::getCategory)
                .map(x -> {
                    return linkTo(methodOn(CategoryController.class).readOneCategoryById(x.getId())).withRel("parentcategories");
                })
                .collect(toList());

        dto.add(parentCategoriesLinks);

        if (embed) {
            List<CategoryDto> subcategoryDtos = entity.getAllChildCategoryXrefs().stream()
                    .map(CategoryXref::getSubCategory)
                    .map(subcategory -> createDto(subcategory, true))
                    .collect(Collectors.toList());

            if (!CollectionUtils.isEmpty(subcategoryDtos)) {
                dto.add(new EmbeddedResource("subcategories", subcategoryDtos));
            }

        }

        return dto;
    }

    @Override
    public Category createEntity(CategoryDto dto) {
        final Category categoryEntity = new CategoryImpl();

        return updateEntity(categoryEntity, dto);
    }

    @Override
    public Category updateEntity(Category categoryEntity, CategoryDto categoryDto) {

        categoryEntity.setName(categoryDto.getName());
        categoryEntity.setDescription(categoryDto.getDescription());
        categoryEntity.setLongDescription(categoryDto.getLongDescription());

        if(categoryDto.getProductsAvailability() != null) {
            categoryEntity.setInventoryType(CatalogUtils.getInventoryTypeByAvailability(categoryDto.getProductsAvailability()));
        } else {
            /* (mst) Remove this if you don't want to have a "default" availability set */
            categoryEntity.setInventoryType(InventoryType.ALWAYS_AVAILABLE);
        }

        categoryEntity.getCategoryAttributesMap().clear();
        Optional.ofNullable(categoryDto.getAttributes())
                .map(Map::entrySet)
                .map(Collection::stream)
                .map(stream -> stream.collect(toMap(
                        Map.Entry::getKey,
                        attributeEntry -> {
                            CategoryAttribute categoryAttribute = new CategoryAttributeImpl();
                            categoryAttribute.setName(attributeEntry.getKey());
                            categoryAttribute.setValue(attributeEntry.getValue());
                            categoryAttribute.setCategory(categoryEntity);
                            return categoryAttribute;
                        }
                )))
                .ifPresent(attributesMap -> categoryEntity.getCategoryAttributesMap().putAll(attributesMap));

        categoryEntity.getCategoryMediaXref().clear();
        Optional.ofNullable(categoryDto.getMedia())
                .map(Map::entrySet)
                .map(Collection::stream)
                .map(stream -> stream.collect(toMap(
                        Map.Entry::getKey,
                        mediaDtoEntry -> {
                            CategoryMediaXrefImpl categoryMediaXref = new CategoryMediaXrefImpl();
                            categoryMediaXref.setCategory(categoryEntity);
                            categoryMediaXref.setKey(mediaDtoEntry.getKey());
                            CatalogUtils.updateMediaEntityFromDto(categoryMediaXref, mediaDtoEntry.getValue());
                            return categoryMediaXref;
                        }
                )))
                .ifPresent(media -> categoryEntity.getCategoryMediaXref().putAll(media));

        return categoryEntity;
    }

}
