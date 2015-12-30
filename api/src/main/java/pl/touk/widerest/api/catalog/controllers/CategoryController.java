package pl.touk.widerest.api.catalog.controllers;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.tuple.Pair;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.CategoryProductXref;
import org.broadleafcommerce.core.catalog.domain.CategoryProductXrefImpl;
import org.broadleafcommerce.core.catalog.domain.CategoryXref;
import org.broadleafcommerce.core.catalog.domain.CategoryXrefImpl;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.inventory.service.type.InventoryType;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.core.EmbeddedWrapper;
import org.springframework.hateoas.core.EmbeddedWrappers;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.catalog.CatalogUtils;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.HalTestDto;
import pl.touk.widerest.api.catalog.dto.HalTestResource;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.exceptions.DtoValidationException;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;
import pl.touk.widerest.security.config.ResourceServerConfig;

import java.net.MalformedURLException;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@RestController
@RequestMapping(value = ResourceServerConfig.API_PATH, produces = { MediaType.APPLICATION_JSON_VALUE, "application/hal+json" })
//@ExposesResourceFor(CategoryDto.class)
@Api(value = "categories", description = "Category catalog endpoint")
public class CategoryController {

    @javax.annotation.Resource(name="blCatalogService")
    protected CatalogService catalogService;

    @javax.annotation.Resource(name = "wdDtoConverters")
    protected DtoConverters dtoConverters;

    /* GET /categories */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/categories", method = RequestMethod.GET)
    @ApiOperation(
            value = "List all categories",
            notes = "Gets a list of all available categories in the catalog",
            response = CategoryDto.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of categories list", response = CategoryDto.class, responseContainer = "List")
    })
    public List<CategoryDto> readAllCategories(
            @ApiParam(value = "Level in the categories hierarchy tree")
            @RequestParam(value = "depth", required = false) Integer depth,
            @ApiParam(value = "Amount of categories to be returned")
            @RequestParam(value = "limit", required = false) Integer limit,
            @ApiParam(value = "Offset which to start returning categories from")
            @RequestParam(value = "offset", required = false) Integer offset) {

        List<Category> categoriesToReturn;

        if(depth == null || depth < 0) {

            categoriesToReturn = catalogService.findAllCategories(limit != null ? limit : 0, offset != null ? offset : 0).stream()
                    .filter(CatalogUtils::archivedCategoryFilter)
                    .collect(Collectors.toList());
        } else {

            final List<Category> globalParentCategories = catalogService.findAllParentCategories().stream()
                    .filter(CatalogUtils::archivedCategoryFilter)
                    .filter(category -> category.getAllParentCategoryXrefs().size() == 0)
                    .collect(Collectors.toList());

            categoriesToReturn = CatalogUtils.getSublistForOffset(getCategoriesAtLevel(globalParentCategories, depth),
                                        offset != null ? offset : 0, limit != null ? limit : 0);

        }

        return categoriesToReturn.stream()
                .map(DtoConverters.categoryEntityToDto)
                .collect(Collectors.toList());
    }

    /* GET /categories */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/categories2", method = RequestMethod.GET)
    @ApiOperation(
            value = "List all categories",
            notes = "Gets a list of all available categories in the catalog",
            response = CategoryDto.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of categories list", response = CategoryDto.class, responseContainer = "List")
    })
    public Resources<CategoryDto> readAllCategories2(
            @ApiParam(value = "Level in the categories hierarchy tree")
            @RequestParam(value = "depth", required = false) Integer depth,
            @ApiParam(value = "Amount of categories to be returned")
            @RequestParam(value = "limit", required = false) Integer limit,
            @ApiParam(value = "Offset which to start returning categories from")
            @RequestParam(value = "offset", required = false) Integer offset) {

        final List<CategoryDto> categoriesToReturn = new ArrayList<>();

        final List<Category> globalParentCategories = catalogService.findAllParentCategories().stream()
                    .filter(CatalogUtils::archivedCategoryFilter)
                    .filter(category -> category.getAllParentCategoryXrefs().size() == 0)
                    .collect(Collectors.toList());

        /* TODO (mst): Figure out how to do this without a secondary queue */
        final Queue<Category> subcategoriesQueue = new LinkedList<>();
        final Queue<CategoryDto> subcategoriesDtoQueue = new LinkedList<>();

        int currentDepth;

        for(Category rootCategory : globalParentCategories) {

            subcategoriesQueue.add(rootCategory);
            subcategoriesDtoQueue.add(DtoConverters.categoryEntityToDto.apply(rootCategory));
            currentDepth = 0;

            while (!subcategoriesQueue.isEmpty()) {

                final Category currentRootCategory = subcategoriesQueue.remove();
                final CategoryDto currentRootCategoryDto = subcategoriesDtoQueue.remove();

                if(currentRootCategory.equals(rootCategory)) {
                    categoriesToReturn.add(currentRootCategoryDto);
                }

                /* (mst) We are done with processing currently selected category when:
                         * we reach the specified depth,
                         * it does not contain any more subcategories
                 */
                if((depth != null && currentDepth >= depth) ||
                   (currentRootCategory.getAllChildCategoryXrefs() == null && currentRootCategory.getAllChildCategoryXrefs().isEmpty())) {
                    break;
                }

                /* (mst) If the queue is empty here, it means that all categories from an entire level have been processed */
                if(subcategoriesQueue.isEmpty()) {
                    currentDepth++;
                }


                final List<CategoryDto> subcategories = new ArrayList<>();

                for (CategoryXref categoryXref : currentRootCategory.getAllChildCategoryXrefs()) {
                    final Category currentSubcategory = categoryXref.getSubCategory();
                    final CategoryDto currentSubcategoryDto = DtoConverters.categoryEntityToDto.apply(currentSubcategory);

                    subcategoriesQueue.add(currentSubcategory);
                    subcategoriesDtoQueue.add(currentSubcategoryDto);
                    subcategories.add(currentSubcategoryDto);
                }
                currentRootCategoryDto.setSubcategories(Resources.wrap(subcategories));
            }
        }
        return new Resources<>(categoriesToReturn);
    }

    /* GET /categoriesTest */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/categoriesTest", method = RequestMethod.GET)
    @ApiOperation(
            value = "List all categories",
            notes = "Gets a list of all available categories in the catalog",
            response = HalTestDto.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of categories list", response = HalTestDto.class)
    })
    public Resources<Resource> readHalTestCategory() {

        final HalTestDto halTestDto = HalTestDto.builder()
                .name("TestName1")
                .build();

        final HalTestDto halTestDto2 = HalTestDto.builder()
                .name("TestName2")
                .build();

        final HalTestResource halTestResource = HalTestResource.builder()
                .name("TestResource1")
                .build();

        final HalTestResource halTestResource2 = HalTestResource.builder()
                .name("TestResource2")
                .build();

        final HalTestResource halTestSubResource = HalTestResource.builder()
                .name("TestSubResource1")
                .build();


        final Link link = linkTo(CategoryController.class).withSelfRel();

        halTestDto.add(linkTo(CategoryController.class).withSelfRel());
        halTestDto2.add(linkTo(CategoryController.class).withSelfRel());

        halTestResource.setSubResource(new Resources(Arrays.asList(halTestSubResource, link)));

//        return (Arrays.asList(
//                new Resource(halTestResource, link),
//                new Resource(halTestResource2, link)
//        ));
        return new Resources(Arrays.asList(
                new Resource(halTestResource, link),
                new Resource(halTestResource2, link)
        ));
    }

    /* GET /{categoryId}/subcategories */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/categories/{categoryId}/subcategories", method = RequestMethod.GET)
    @ApiOperation(
            value = "List all subcategories of a specified parent category",
            notes = "Gets a list of all available subcategories of a given category in the catalog",
            response = CategoryDto.class,
            responseContainer = "List"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of category's products availability", response = String.class),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public List<CategoryDto> getSubcategoriesByCategoryId(
            @ApiParam(value = "ID of a specific category", required = true)
            @PathVariable(value = "categoryId") Long categoryId,
            @ApiParam(value = "Amount of subcategories to be returned")
            @RequestParam(value = "limit", required = false) Integer limit,
            @ApiParam(value = "Offset which to start returning subcategories from")
            @RequestParam(value = "offset", required = false) Integer offset) {

        final Category category = Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils::archivedCategoryFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));

        return catalogService.findAllSubCategories(category, limit != null ? limit : 0, offset != null ? offset : 0).stream()
                .filter(CatalogUtils::archivedCategoryFilter)
                .map(DtoConverters.categoryEntityToDto)
                .collect(Collectors.toList());
    }

    /* POST /{categoryId}/subcategories */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/categories/{categoryId}/subcategories", method = RequestMethod.POST)
    @ApiOperation(
            value = "Link an existing category to its new parent category",
            notes = "Adds an existing category to another category as its subcategory"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A new subcategory has been successfully created"),
            @ApiResponse(code = 400, message = "Not enough data has been provided (missing category link)"),
            @ApiResponse(code = 404, message = "The specified category does not exist"),
            @ApiResponse(code = 409, message = "Category is already a subcategory of a specified category")
    })
    public ResponseEntity<?> addSubcategoryToParent(
            @ApiParam(value = "ID of a specific category", required = true)
            @PathVariable(value = "categoryId") Long categoryId,
            @ApiParam(value = "Link to the subcategory")
            @RequestParam(value = "href", required = true) String href) {

        if(href == null || href.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        long hrefCategoryId;

        try {
            hrefCategoryId = CatalogUtils.getIdFromUrl(href);
        } catch (MalformedURLException | NumberFormatException | DtoValidationException e) {
            return ResponseEntity.badRequest().build();
        }

        if(hrefCategoryId == categoryId) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        final Category hrefCategory = Optional.ofNullable(catalogService.findCategoryById(hrefCategoryId))
                .filter(CatalogUtils::archivedCategoryFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + hrefCategoryId + " does not exist"));

        final Category parentCategory = Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils::archivedCategoryFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));

        final CategoryXref parentChildCategoryXref = new CategoryXrefImpl();
        parentChildCategoryXref.setCategory(parentCategory);
        parentChildCategoryXref.setSubCategory(hrefCategory);

        if(!parentCategory.getAllChildCategoryXrefs().contains(parentChildCategoryXref)) {
            parentCategory.getAllChildCategoryXrefs().add(parentChildCategoryXref);
            catalogService.saveCategory(parentCategory);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /* DELETE /{categoryId}/subcategories */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/categories/{categoryId}/subcategories", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Remove a link to an existing subcategory from its parent category",
            notes = "Removes an existing link to a specified subcategory from its parent category"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Specified subcategory has been successfully removed from its parent category"),
            @ApiResponse(code = 404, message = "The specified category does not exist or is not a proper subcategory"),
            @ApiResponse(code = 409, message = "Subcategory and its parent cannot point to the same category")
    })
    public ResponseEntity<?> removeSubcategoryFromParent(
            @ApiParam(value = "ID of a specific category", required = true)
            @PathVariable(value = "categoryId") Long categoryId,
            @ApiParam(value = "Link to the subcategory")
            @RequestParam(value = "href", required = true) String href) {

        if(href == null || href.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        long hrefCategoryId;

        try {
            hrefCategoryId = CatalogUtils.getIdFromUrl(href);
        } catch (MalformedURLException | NumberFormatException | DtoValidationException e) {
            return ResponseEntity.badRequest().build();
        }

        if(hrefCategoryId == categoryId) {
            ResponseEntity.status(HttpStatus.CONFLICT).build();
        }


        Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils::archivedCategoryFilter)
                .map(e -> {
                    final CategoryXref catXref = Optional.ofNullable(e.getAllChildCategoryXrefs()).orElse(Collections.emptyList()).stream()
                            .filter(x -> Optional.ofNullable(x.getSubCategory()).map(Category::getId).orElse(-1L) == hrefCategoryId)
                            .findAny()
                            .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + hrefCategoryId + " does not exist or is not a subcategory of a category with ID: " + categoryId));

                    return Pair.of(e, catXref);
                })
                .map(e -> {
                    e.getKey().getAllChildCategoryXrefs().remove(e.getValue());
                    return catalogService.saveCategory(e.getKey());
                })
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));

        return ResponseEntity.noContent().build();
    }

    /* POST /categories */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/categories", method = RequestMethod.POST)
    @ApiOperation(
            value = "Add a new category",
            notes = "Adds a new category to the catalog. It does take duplicates (same NAME) into account. " +
                    "Returns an URL to the newly added category in the Location field of the HTTP response header"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A new category entry successfully created"),
            @ApiResponse(code = 400, message = "Not enough data has been provided"),
            @ApiResponse(code = 409, message = "Category already exists")
    })
    public ResponseEntity<?> addOneCategory(
            @ApiParam(value = "Description of a new category", required = true)
            @RequestBody CategoryDto categoryDto) {

    	/* (mst) CategoryDto has to have at least a Name! */
        if(categoryDto.getName() == null || categoryDto.getName().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
    	
    	/* (mst) Providing that both Description() and LongDescription() can be null, which...is OK, this one
    	 *       should do the job "better" IMO
    	 */
//        final long duplicatesCount = catalogService.findCategoriesByName(categoryDto.getName()).stream()
//                .filter(CatalogUtils::archivedCategoryFilter)
//                .count();
//
//        if(duplicatesCount > 0) {
//            return ResponseEntity.status(HttpStatus.CONFLICT).build();
//        }

        final Category createdCategoryEntity = catalogService.saveCategory(DtoConverters.categoryDtoToEntity.apply(categoryDto));

        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdCategoryEntity.getId())
                .toUri())
                .build();//contentType(MediaType.APPLICATION_JSON).body("");
    }

    /* GET /categories/count */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/categories/count", method = RequestMethod.GET)
    @ApiOperation(
            value = "Count all categories",
            notes = "Gets a number of all categories available in the catalog",
            response = Long.class
    )
    public ResponseEntity<Long> getAllCategoriesCount() {

        final long allCategoriesCount = catalogService.findAllCategories().stream()
                .filter(CatalogUtils::archivedCategoryFilter)
                .count();

        return ResponseEntity.ok(allCategoriesCount);
    }

    /* GET /categories/{id} */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/categories/{categoryId}", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get a single category details",
            notes = "Gets details of a single category specified by its ID",
            response = CategoryDto.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of category details", response = CategoryDto.class),
            @ApiResponse(code = 404, message = "The specified category does not exist or is marked as archived")
    })
    public ResponseEntity<CategoryDto> readOneCategoryById(
            @ApiParam(value = "ID of a specific category", required = true)
            @PathVariable(value="categoryId") Long categoryId) {

        final CategoryDto categoryToReturnDto = Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils::archivedCategoryFilter)
                .map(DtoConverters.categoryEntityToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));

        return ResponseEntity.ok(categoryToReturnDto);
    }

    /* DELETE /categories/{categoryId} */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/categories/{categoryId}", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Delete a category",
            notes = "Removes an existing category from catalog by marking it (internally) as archived",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 204, message = "Successful removal of the specified category"),
            @ApiResponse(code = 404, message = "The specified category does not exist or is already marked as archived")
    })
    public ResponseEntity<?> removeOneCategoryById(
            @ApiParam(value = "ID of a specific category")
            @PathVariable(value="categoryId") Long categoryId) {

        Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils::archivedCategoryFilter)
                .map(e -> {
                    catalogService.removeCategory(e);
                    return e;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Cannot delete category with ID: " + categoryId + ". Category does not exist"));

        return ResponseEntity.noContent().build();
    }

    /* PUT /categories/{id} */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/categories/{categoryId}", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Update an existing category",
            notes = "Updates an existing category with new details. If the category does not exist, it does NOT create it!",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful update of the specified category"),
            @ApiResponse(code = 400, message = "Not enough data has been provided"),
            @ApiResponse(code = 404, message = "The specified category does not exist"),
            @ApiResponse(code = 409, message = "Category with that name already exists")
    })
    public ResponseEntity<?> updateOneCategory(
            @ApiParam(value = "ID of a specific category", required = true)
            @PathVariable(value = "categoryId") Long categoryId,
            @ApiParam(value = "(Full) Description of an updated category", required = true)
            @RequestBody CategoryDto categoryDto) {

    	/* (mst) CategoryDto has to have at least a Name! */
        if(categoryDto.getName() == null || categoryDto.getName().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

//        final long duplicatesCount = catalogService.findCategoriesByName(categoryDto.getName()).stream()
//                .filter(CatalogUtils::archivedCategoryFilter)
//                .count();
//
//        if(duplicatesCount > 0) {
//            return ResponseEntity.status(HttpStatus.CONFLICT).build();
//        }

        Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils::archivedCategoryFilter)
                .map(e -> CatalogUtils.updateCategoryEntityFromDto(e, categoryDto))
                .map(catalogService::saveCategory)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));


        return ResponseEntity.ok().build();
    }

    /* PATCH /categories/{id} */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/categories/{categoryId}", method = RequestMethod.PATCH)
    @ApiOperation(
            value = "Partially update an existing category",
            notes = "Partially updates an existing category with new details. It does not follow the format specified in RFC yet though",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful update of the specified category"),
            @ApiResponse(code = 404, message = "The specified category does not exist"),
            @ApiResponse(code = 409, message = "Category with that name already exists")
    })
    public ResponseEntity<?> partialUpdateOneCategory(
            @ApiParam(value = "ID of a specific category", required = true)
            @PathVariable(value = "categoryId") Long categoryId,
            @ApiParam(value = "(Partial) Description of an updated category", required = true)
            @RequestBody CategoryDto categoryDto) {
        
        /* (mst) Here...we don't need to have Name set BUT in case we do, we also check for duplicates! */
//        if(categoryDto.getName() != null) {
//
//            final long duplicatesCount = catalogService.findCategoriesByName(categoryDto.getName()).stream()
//                    .filter(CatalogUtils::archivedCategoryFilter)
//                    .count();
//
//            if(duplicatesCount > 0) {
//                return ResponseEntity.status(HttpStatus.CONFLICT).build();
//            }
//        }

        Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils::archivedCategoryFilter)
                .map(e -> CatalogUtils.partialUpdateCategoryEntityFromDto(e, categoryDto))
                .map(catalogService::saveCategory)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));

        return ResponseEntity.ok().build();
    }


    /* GET /{categoryId}/availability */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/categories/{categoryId}/availability", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get category's products availability",
            notes = "Gets an availability of all the products in this category",
            response = String.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of category's products availability", response = String.class),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public ResponseEntity<String> getCategoryByIdAvailability(
            @ApiParam(value = "ID of a specific category", required = true)
            @PathVariable(value = "categoryId") Long categoryId) {

        final Category category = Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils::archivedCategoryFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));

        final String categoryAvailability = Optional.ofNullable(category.getInventoryType())
                .map(InventoryType::getType)
                .orElse(CatalogUtils.EMPTY_STRING);

        return ResponseEntity.ok(categoryAvailability);
    }

    /* PUT /{categoryId}/availability */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/categories/{categoryId}/availability", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Update category's products availability",
            notes = "Update an availability of all the products in this category",
            response = Void.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of category's products availability"),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public ResponseEntity<?> updateCategoryByIdAvailability(
            @ApiParam(value = "ID of a specific category", required = true)
            @PathVariable(value = "categoryId") Long categoryId,
            @ApiParam(value = "Inventory type: ALWAYS_AVAILABLE, UNAVAILABLE, CHECK_QUANTITY")
            @RequestBody String availability) {

        Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils::archivedCategoryFilter)
                .map(e -> {
                    e.setInventoryType(Optional.ofNullable(InventoryType.getInstance(availability))
                            .orElseThrow(() -> new ResourceNotFoundException("The specified Inventory Type does not exist")));
                    return e;
                })
                .map(catalogService::saveCategory)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));

        return ResponseEntity.ok().build();
    }

    /* GET /categories/{id}/products */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/categories/{categoryId}/products", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get all products in a category",
            notes = "Gets a list of all products belonging to a specified category",
            response = ProductDto.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of all products in a given category", responseContainer = "List"),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public List<ProductDto> readProductsFromCategory(
            @ApiParam(value = "ID of a specific category", required = true)
            @PathVariable(value="categoryId") Long categoryId) {

        return getProductsFromCategoryId(categoryId).stream()
                .filter(CatalogUtils::archivedProductFilter)
                .map(dtoConverters.productEntityToDto)
                .collect(Collectors.toList());
    }

    /* PATCH /categories/{id}/products/{productId} */
    /* (mst) This endpoint inserts only a reference to the product (so it has to exist)
     *            into Category's product list. In case the product does not exist, use
     *            ProductController's POST methods first
     */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/categories/{categoryId}/products", method = RequestMethod.POST)
    @ApiOperation(
            value = "Insert existing product into category",
            notes = "Inserts existing product into category. It actually only updates few references therefore" +
                    " to insert a completely new product refer to ProductControllers' POST methods first",
            response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Product successfully inserted into specified category"),
            @ApiResponse(code = 400, message = "Not enough data has been provided (missing product link)"),
            @ApiResponse(code = 404, message = "The specified category or product does not exist"),
            @ApiResponse(code = 409, message = "Category already contains the specified product")
    })
    public ResponseEntity<?> insertOneProductIntoCategory(
            @ApiParam(value = "ID of a specific category", required = true)
            @PathVariable(value="categoryId") Long categoryId,
            @ApiParam(value = "Link to the product")
            @RequestParam(value = "href", required = true) String href) {

        if(href == null || href.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        long hrefProductId;

        try {
            hrefProductId = CatalogUtils.getIdFromUrl(href);
        } catch (MalformedURLException | NumberFormatException | DtoValidationException e) {
            return ResponseEntity.badRequest().build();
        }

        final Category categoryEntity = Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils::archivedCategoryFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));

        /* (mst) ProductController's POST methods guarantee there will be no duplicates therefore... */
        final Product productToAdd = Optional.ofNullable(catalogService.findProductById(hrefProductId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + hrefProductId + " does not exist"));

        final CategoryProductXref productToAddXref = new CategoryProductXrefImpl();
        productToAddXref.setProduct(productToAdd);
        productToAddXref.setCategory(categoryEntity);

        if(!categoryEntity.getAllProductXrefs().contains(productToAddXref)) {
            categoryEntity.getAllProductXrefs().add(productToAddXref);
            catalogService.saveCategory(categoryEntity);
            /* TODO: (mst) add URI */
            return new ResponseEntity<>(HttpStatus.CREATED);
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /* DELETE /categories/{categoryId}/products/{productId} */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/categories/{categoryId}/products", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Remove a product from a category",
            notes = "Removes a product from a specific category. It DOES NOT delete the product completely from catalog, just removes its " +
                    "references to the specified category",
            response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Specified product successfully removed from a category"),
            @ApiResponse(code = 400, message = "Not enough data has been provided (missing product link)"),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public ResponseEntity<?> removeOneProductFromCategory(
            @ApiParam(value = "ID of a specific category", required = true)
            @PathVariable(value="categoryId") Long categoryId,
            @ApiParam(value = "Link to the product")
            @RequestParam(value = "href", required = true) String href) {

        if(href == null || href.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        long hrefProductId;

        try {
            hrefProductId = CatalogUtils.getIdFromUrl(href);
        } catch (MalformedURLException | NumberFormatException | DtoValidationException e) {
            return ResponseEntity.badRequest().build();
        }

    	/* (mst) Ok, here we do NOT remove the product completely from catalog -> this is the job of the ProductController! */
        getProductsFromCategoryId(categoryId).stream()
                .filter(CatalogUtils::archivedProductFilter)
                .filter(x -> x.getId().longValue() == hrefProductId)
                .findAny()
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + hrefProductId + " does not exist in category with ID: " + categoryId));


        Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils::archivedCategoryFilter)
                .map(e -> {
                    CategoryProductXref xref = e.getAllProductXrefs().stream()
                            .filter(x -> x.getProduct().getId().longValue() == hrefProductId)
                            .findAny()
                            .orElseThrow(() -> new ResourceNotFoundException("(Internal) Product with ID: " + hrefProductId + " not found on the list of references for category with ID: " + categoryId));
                    return Pair.of(e, xref);
                })
                .map(e -> {
                    e.getKey().getAllProductXrefs().remove(e.getValue());
                    return e.getKey();
                })
                .map(catalogService::saveCategory)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));


        return ResponseEntity.noContent().build();

    }


    /* GET /categories/{categoryId}/products/count */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/categories/{categoryId}/products/count", method = RequestMethod.GET)
    @ApiOperation(
            value = "Count all products in a specific category",
            notes = "Gets a number of all products belonging to a specified category",
            response = Long.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of products count"),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public ResponseEntity<Long> getAllProductsInCategoryCount(
            @ApiParam(value = "ID of a specific category", required = true)
            @PathVariable(value = "categoryId") Long categoryId) {

        final long allProductsInCategoryCount = getProductsFromCategoryId(categoryId).stream()
                .filter(CatalogUtils::archivedProductFilter)
                .count();

        return ResponseEntity.ok(allProductsInCategoryCount);
    }

    /* ------------------------------- HELPER METHODS ------------------------------- */

    private List<Product> getProductsFromCategoryId(Long categoryId) throws ResourceNotFoundException {

        return Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils::archivedCategoryFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"))
                .getAllProductXrefs().stream()
                .map(CategoryProductXref::getProduct)
                .collect(Collectors.toList());
    }

    public static List<Category> getCategoriesAtLevel(final List<Category> parentCategories, final int level) {

        if(level < 0 || parentCategories.size() <= 0) {
            return Collections.emptyList();
        }

        if(level == 0) {
            return parentCategories;
        }

        final List<Category> levelCategories = new ArrayList<>();

        /* (mst) BFS-type search. We maintain two queues though,
                 to be able to break easily if we reach the
                 required level.
         */

        Queue<Category> activeCategories = new LinkedList<>();
        Queue<Category> inactiveCategories = new LinkedList<>();
        int currentDepth;

        for(Category rootCategory : parentCategories) {

            currentDepth = 0;

            activeCategories.add(rootCategory);

            while(!activeCategories.isEmpty()) {
                while(!activeCategories.isEmpty()) {
                    final Category c = activeCategories.remove();

                    for (CategoryXref children : c.getAllChildCategoryXrefs()) {
                        inactiveCategories.add(children.getSubCategory());
                    }
                }

                currentDepth++;

                if(currentDepth >= level) {
                    levelCategories.addAll(inactiveCategories);
                    activeCategories.clear();
                } else {
                    activeCategories.clear();
                    activeCategories.addAll(inactiveCategories);
                }

                inactiveCategories.clear();
            }
        }

        return levelCategories;
    }



}


   /* (mst) TO BE REMOVED!!! */
    /* GET /categories/{id}/products/{productId} */
//    @Transactional
//    @PreAuthorize("permitAll")
//    @RequestMapping(value = "/categories/{categoryId}/products/{productId}", method = RequestMethod.GET)
//    @ApiOperation(
//            value = "Get details of a product from a category",
//            notes = "Gets details of a specific product in a given category",
//            response = ProductDto.class)
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "Successful retrieval of product details", response = ProductDto.class),
//            @ApiResponse(code = 404, message = "The specified category does not exist")
//    })
//    public ProductDto readOneProductFromCategory(
//            @ApiParam(value = "ID of a specific category", required = true)
//            @PathVariable(value="categoryId") Long categoryId,
//            @ApiParam(value = "ID of a product belonging to the specified category", required = true)
//            @PathVariable(value = "productId") Long productId) {
//
//        return this.getProductsFromCategoryId(categoryId).stream()
//                .filter(CatalogUtils::archivedProductFilter)
//                .filter(x -> x.getId().longValue() == productId)
//                .findAny()
//                .map(dtoConverters.productEntityToDto)
//                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist in category with ID: " + categoryId));
//    }


    /* (mst) TO BE REMOVED!!! */
    /* PUT /categories/{id}/products/{productId} */
    /* (mst) This endpoint inserts only a reference to the product (so it has to exist)
     *            into Category's product list. In case the product does not exist, use
     *            ProductController's POST methods first
     */
//    @Transactional
//    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
//    @RequestMapping(value = "/categories/{categoryId}/products/{productId}", method = RequestMethod.PUT)
//    @ApiOperation(
//            value = "Insert existing product into category",
//            notes = "Inserts existing product into category. It actually only updates few references therefore" +
//                    " to insert a completely new product refer to ProductControllers' POST methods first",
//            response = Void.class)
//    @ApiResponses(value = {
//            @ApiResponse(code = 201, message = "Product successfully inserted into specified category"),
//            @ApiResponse(code = 404, message = "The specified category or product does not exist"),
//            @ApiResponse(code = 409, message = "Category already contains the specified product")
//    })
//    public ResponseEntity<?> insertOneProductIntoCategory(
//            @ApiParam(value = "ID of a specific category", required = true)
//            @PathVariable(value="categoryId") Long categoryId,
//            @ApiParam(value = "ID of a product belonging to the specified category", required = true)
//            @PathVariable(value = "productId") Long productId) {
//
//        final Category categoryEntity = Optional.ofNullable(catalogService.findCategoryById(categoryId))
//                .filter(CatalogUtils::archivedCategoryFilter)
//                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));
//
//        /* (mst) ProductController's POST methods guarantee there will be no duplicates therefore... */
//        final Product productToAdd = Optional.ofNullable(catalogService.findProductById(productId))
//                .filter(CatalogUtils::archivedProductFilter)
//                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));
//
//        final CategoryProductXref productToAddXref = new CategoryProductXrefImpl();
//        productToAddXref.setProduct(productToAdd);
//        productToAddXref.setCategory(categoryEntity);
//
//        if(!categoryEntity.getAllProductXrefs().contains(productToAddXref)) {
//            categoryEntity.getAllProductXrefs().add(productToAddXref);
//            catalogService.saveCategory(categoryEntity);
//            /* TODO: (mst) add URI */
//            return new ResponseEntity<>(HttpStatus.CREATED);
//        } else {
//            return ResponseEntity.status(HttpStatus.CONFLICT).build();
//        }
//    }

     /* (mst) TO BE REMOVED!!! */
//    /* DELETE /categories/{categoryId}/products/{productId} */
//    @Transactional
//    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
//    @RequestMapping(value = "/categories/{categoryId}/products/{productId}", method = RequestMethod.DELETE)
//    @ApiOperation(
//            value = "Remove a product from a category",
//            notes = "Removes a product from a specific category. It DOES NOT delete the product completely from catalog, just removes its " +
//                    "references to the specified category",
//            response = Void.class)
//    @ApiResponses(value = {
//            @ApiResponse(code = 204, message = "Specified product successfully removed from a category"),
//            @ApiResponse(code = 400, message = "Not enough data has been provided (missing product link)"),
//            @ApiResponse(code = 404, message = "The specified category does not exist")
//    })
//    public ResponseEntity<?> removeOneProductFromCategory(
//            @ApiParam(value = "ID of a specific category", required = true)
//            @PathVariable(value="categoryId") Long categoryId,
//            @ApiParam(value = "ID of a product belonging to the specified category", required = true)
//            @PathVariable(value = "productId") Long productId) {
//
//    	/* (mst) Ok, here we do NOT remove the product completely from catalog -> this is the job of the ProductController! */
//        getProductsFromCategoryId(categoryId).stream()
//                .filter(CatalogUtils::archivedProductFilter)
//                .filter(x -> x.getId().longValue() == productId)
//                .findAny()
//                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist in category with ID: " + categoryId));
//
//
//        Optional.ofNullable(catalogService.findCategoryById(categoryId))
//                .filter(CatalogUtils::archivedCategoryFilter)
//                .map(e -> {
//                    CategoryProductXref xref = e.getAllProductXrefs().stream()
//                            .filter(x -> x.getProduct().getId().longValue() == productId)
//                            .findAny()
//                            .orElseThrow(() -> new ResourceNotFoundException("(Internal) Product with ID: " + productId + " not found on the list of references for category with ID: " + categoryId));
//                    return Pair.of(e, xref);
//                })
//                .map(e -> {
//                    e.getKey().getAllProductXrefs().remove(e.getValue());
//                    return e.getKey();
//                })
//                .map(catalogService::saveCategory)
//                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));
//
//
//        return ResponseEntity.noContent().build();
//
//    }