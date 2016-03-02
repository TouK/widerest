package pl.touk.widerest.api.categories;

import org.apache.commons.lang3.tuple.Pair;
import org.broadleafcommerce.common.media.domain.Media;
import org.broadleafcommerce.common.media.domain.MediaImpl;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.CategoryImpl;
import org.broadleafcommerce.core.catalog.domain.CategoryMediaXref;
import org.broadleafcommerce.core.catalog.domain.CategoryMediaXrefImpl;
import org.broadleafcommerce.core.catalog.domain.CategoryXref;
import org.broadleafcommerce.core.inventory.service.type.InventoryType;
import org.springframework.hateoas.EmbeddedResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import pl.touk.widerest.api.Converter;
import pl.touk.widerest.api.common.CatalogUtils;
import pl.touk.widerest.api.common.MediaConverter;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;
import static pl.touk.widerest.api.common.CatalogUtils.valueExtractor;

@Component
public class CategoryConverter implements Converter<Category, CategoryDto> {

    @Resource
    protected MediaConverter mediaConverter;


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
                .media(entity.getCategoryMediaXref().entrySet().stream().collect(toMap(Map.Entry::getKey, e -> mediaConverter.createDto(e.getValue().getMedia(), false))))
                .build();


        dto.add(ControllerLinkBuilder.linkTo(methodOn(CategoryController.class).readOneCategoryById(entity.getId())).withSelfRel());

        dto.add(linkTo(methodOn(CategoryController.class).readProductsFromCategory(entity.getId())).withRel("products"));

//        dto.add(linkTo(methodOn(CategoryController.class).getCategoryByIdAvailability(entity.getId())).withRel("availability"));

//        dto.add(linkTo(methodOn(CategoryController.class).getAllProductsInCategoryCount(entity.getId())).withRel("products-count"));

//        dto.add(linkTo(methodOn(CategoryController.class).getAllCategoriesCount(null)).withRel("categories-count"));

        final List<Link> subcategoriesLinks = Optional.ofNullable(entity.getAllChildCategoryXrefs())
                .orElse(Collections.emptyList()).stream()
                .map(CategoryXref::getSubCategory)
                .map(x -> linkTo(methodOn(CategoryController.class).readOneCategoryById(x.getId())).withRel("subcategories"))
                .collect(toList());

        dto.add(subcategoriesLinks);

        final List<Link> parentCategoriesLinks = Optional.ofNullable(entity.getAllParentCategoryXrefs())
                .orElse(Collections.emptyList()).stream()
                .map(CategoryXref::getCategory)
                .map(x -> linkTo(methodOn(CategoryController.class).readOneCategoryById(x.getId())).withRel("parentcategories"))
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
                .map(Map::entrySet).orElse(Collections.emptySet()).stream()
                .map(e -> Pair.of(e.getKey(), valueExtractor(categoryEntity).apply(e)))
                .forEach(o -> categoryEntity.getCategoryAttributesMap().put(o.getKey(), o.getValue()));

        categoryEntity.getCategoryMediaXref().clear();

        final Map<String, CategoryMediaXrefImpl> mediaXrefs = Optional.ofNullable(categoryDto.getMedia())
                .map(Map::entrySet).orElse(Collections.emptySet()).stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        mediaDtoEntry -> {
                            final CategoryMediaXrefImpl categoryMediaXref = new CategoryMediaXrefImpl();
                            categoryMediaXref.setCategory(categoryEntity);
                            categoryMediaXref.setKey(mediaDtoEntry.getKey());

                            final Media categoryMedia = new MediaImpl();
                            mediaConverter.updateEntity(categoryMedia, mediaDtoEntry.getValue());

                            categoryMediaXref.setMedia(categoryMedia);

//                            CatalogUtils.updateMediaEntityFromDto(categoryMediaXref, mediaDtoEntry.getValue());
                            return categoryMediaXref;
                        }
                ));

        categoryEntity.getCategoryMediaXref().putAll(mediaXrefs);

        return categoryEntity;
    }

    @Override
    public Category partialUpdateEntity(final Category category, final CategoryDto categoryDto) {

        if (categoryDto.getName() != null) {
            category.setName(categoryDto.getName());
        }

        if (categoryDto.getDescription() != null) {
            category.setDescription(categoryDto.getDescription());
        }

        if (categoryDto.getLongDescription() != null) {
            category.setLongDescription(categoryDto.getLongDescription());
        }

        if(categoryDto.getProductsAvailability() != null) {
            category.setInventoryType(CatalogUtils.getInventoryTypeByAvailability(categoryDto.getProductsAvailability()));;
        }

        if(categoryDto.getAttributes() != null) {
            category.getCategoryAttributesMap().clear();
            category.getCategoryAttributesMap().putAll(
                    Optional.ofNullable(categoryDto.getAttributes()).orElse(Collections.emptyMap()).entrySet().stream()
                            .collect(toMap(Map.Entry::getKey, valueExtractor(category))));
        }

        if(categoryDto.getMedia() != null) {
            category.getCategoryMediaXref().clear();
            category.getCategoryMediaXref().putAll(
                    categoryDto.getMedia().entrySet().stream()
                        .collect(toMap(Map.Entry::getKey, mediaDtoEntry -> {
                            final CategoryMediaXref categoryMediaXref = new CategoryMediaXrefImpl();
                            categoryMediaXref.setCategory(category);
                            categoryMediaXref.setKey(mediaDtoEntry.getKey());

                            final Media categoryMedia = new MediaImpl();
                            mediaConverter.updateEntity(categoryMedia, mediaDtoEntry.getValue());

                            categoryMediaXref.setMedia(categoryMedia);

                            return categoryMediaXref;
                        }))
            );
        }

        return category;
    }

}
