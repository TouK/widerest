package pl.touk.widerest.api.categories;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.tuple.Pair;
import org.broadleafcommerce.common.service.GenericEntityService;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.CategoryProductXref;
import org.broadleafcommerce.core.catalog.domain.CategoryProductXrefImpl;
import org.broadleafcommerce.core.catalog.domain.CategoryXref;
import org.broadleafcommerce.core.catalog.domain.CategoryXrefImpl;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.touk.widerest.api.common.CatalogUtils;
import pl.touk.widerest.api.common.ResourceNotFoundException;
import pl.touk.widerest.api.products.ProductConverter;
import pl.touk.widerest.api.products.ProductDto;
import pl.touk.widerest.security.oauth2.ResourceServerConfig;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

@RestController
@Validated
@RequestMapping(value = ResourceServerConfig.API_PATH, produces = { MediaTypes.HAL_JSON_VALUE})
@Api(value = "categories", description = "Category catalog endpoint")
public class CategoryController {

    private static final ResponseEntity<Void> NO_CONTENT = ResponseEntity.noContent().build();
    private static final ResponseEntity<Void> OK = ResponseEntity.ok().build();
    private static final ResponseEntity<Void> CONFLICT = ResponseEntity.status(HttpStatus.CONFLICT).build();
    private static final ResponseEntity<Void> CREATED = ResponseEntity.status(HttpStatus.CREATED).build();

    @Resource(name="blCatalogService")
    protected CatalogService catalogService;

    @Resource(name = "blGenericEntityService")
    protected GenericEntityService genericEntityService;

    @Resource
    private CategoryConverter categoryConverter;

    @Resource
    protected ProductConverter productConverter;

    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/categories", method = RequestMethod.GET)
    @ApiOperation(
            value = "List all categories",
            notes = "Gets a list of all available categories in the catalog")
    @ApiResponses(value = {
            @ApiResponse(
                    code = 200, message = "Successful retrieval of categories list",
                    response = CategoryDto.class, responseContainer = "List")
    })
    public Resources<CategoryDto> readAllCategories(
            @ApiParam(value = "Level in the categories hierarchy tree", defaultValue = "false")
            @RequestParam(value = "flat", required = false, defaultValue = "false") boolean flat
    ) {

        final List<CategoryDto> categoriesToReturn =
                (flat ? catalogService.findAllCategories() : catalogService.findAllParentCategories()).stream()
                        .filter(CatalogUtils.nonArchivedCategory)
                        .filter(category -> flat || category.getAllParentCategoryXrefs().size() == 0)
                        .map(category -> categoryConverter.createDto(category, !flat, true))
                        .collect(Collectors.toList());

        return new Resources<>(categoriesToReturn);
    }

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
                @Valid @RequestBody final CategoryDto categoryDto
    ) {
        final Category createdCategoryEntity = catalogService.saveCategory(categoryConverter.createEntity(categoryDto));

        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdCategoryEntity.getId())
                .toUri())
                .build();
    }

    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/categories/{categoryId}", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get a single category details",
            notes = "Gets details of a single category specified by its ID"
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of category details", response = CategoryDto.class),
            @ApiResponse(code = 404, message = "The specified category does not exist or is marked as archived")
    })
    public ResponseEntity<CategoryDto> readOneCategoryById(
            @ApiParam(value = "ID of a specific category", required = true)
            @PathVariable(value="categoryId") final Long categoryId
    ) {

        final CategoryDto categoryToReturnDto = Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils.nonArchivedCategory)
                .map(category -> categoryConverter.createDto(category))
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));

        return ResponseEntity.ok(categoryToReturnDto);
    }

    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/categories/{categoryId}", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Delete a category",
            notes = "Removes an existing category from catalog by marking it (internally) as archived")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Successful removal of the specified category"),
            @ApiResponse(code = 404, message = "The specified category does not exist or is already marked as archived")
    })
    public ResponseEntity<?> removeOneCategoryById(
            @ApiParam(value = "ID of a specific category")
            @PathVariable(value="categoryId") final Long categoryId) {

        Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils.nonArchivedCategory)
                .map(e -> {
                    catalogService.removeCategory(e);
                    return e;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Cannot delete category with ID: " + categoryId + ". Category does not exist"));

        return NO_CONTENT;
    }

    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/categories/{categoryId}", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Update an existing category",
            notes = "Updates an existing category with new details. If the category does not exist, it does NOT create it!"
    )
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
                @Valid @RequestBody final CategoryDto categoryDto) {

        Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils.nonArchivedCategory)
                .map(e -> categoryConverter.updateEntity(e, categoryDto))
                .map(catalogService::saveCategory)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));

        return OK;
    }

    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/categories/{categoryId}/subcategories", method = RequestMethod.GET)
    @ApiOperation(
            value = "List all subcategories of a specified parent category",
            notes = "Gets a list of all available subcategories of a given category in the catalog"
    )
    @ApiResponses({
            @ApiResponse(
                    code = 200, message = "Successful retrieval of category's products availability",
                    response = CategoryDto.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public Resources<CategoryDto> getSubcategoriesByCategoryId(
            @ApiParam(value = "ID of a specific category", required = true)
            @PathVariable(value = "categoryId") Long categoryId,
            @ApiParam(value = "Amount of subcategories to be returned")
            @RequestParam(value = "limit", required = false) Integer limit,
            @ApiParam(value = "Offset which to start returning subcategories from")
            @RequestParam(value = "offset", required = false) Integer offset) {

        final Category category = Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils.nonArchivedCategory)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));

        List<CategoryDto> subcategoriesDtos = catalogService.findAllSubCategories(category, limit != null ? limit : 0, offset != null ? offset : 0).stream()
                .filter(CatalogUtils.nonArchivedCategory)
                .map(subcategory -> categoryConverter.createDto(subcategory))
                .collect(Collectors.toList());

        return new Resources(subcategoriesDtos);
    }

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
            @RequestParam(value = "href", required = true) @NotEmpty @URL String href) {


        long hrefCategoryId;

        hrefCategoryId = CatalogUtils.getIdFromUrl(href);

        if(hrefCategoryId == categoryId) {
            return CONFLICT;
        }

        final Category hrefCategory = Optional.ofNullable(catalogService.findCategoryById(hrefCategoryId))
                .filter(CatalogUtils.nonArchivedCategory)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + hrefCategoryId + " does not exist"));

        final Category parentCategory = Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils.nonArchivedCategory)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));

        final CategoryXref parentChildCategoryXref = new CategoryXrefImpl();
        parentChildCategoryXref.setCategory(parentCategory);
        parentChildCategoryXref.setSubCategory(hrefCategory);

        if(!parentCategory.getAllChildCategoryXrefs().contains(parentChildCategoryXref)) {
            parentCategory.getAllChildCategoryXrefs().add(parentChildCategoryXref);
            catalogService.saveCategory(parentCategory);
            return CREATED;
        } else {
            return CONFLICT;
        }
    }

    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/categories/{categoryId}/subcategories", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Remove a link to an existing subcategory from its parent category",
            notes = "Removes an existing link to a specified subcategory from its parent category"
    )
    @ApiResponses({
            @ApiResponse(code = 204, message = "Specified subcategory has been successfully removed from its parent category"),
            @ApiResponse(code = 404, message = "The specified category does not exist or is not a proper subcategory"),
            @ApiResponse(code = 409, message = "Subcategory and its parent cannot point to the same category")
    })
    public ResponseEntity<?> removeSubcategoryFromParent(
            @ApiParam(value = "ID of a specific category", required = true)
                @PathVariable(value = "categoryId") final Long categoryId,
            @ApiParam(value = "Link to the subcategory")
                @RequestParam(value = "href", required = true) @NotEmpty @URL final String href) {

        long hrefCategoryId;

        hrefCategoryId = CatalogUtils.getIdFromUrl(href);

        if(hrefCategoryId == categoryId) {
            return CONFLICT;
        }

            Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils.nonArchivedCategory)
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

        return NO_CONTENT;
    }

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
    public Resources<ProductDto> readProductsFromCategory(
            @ApiParam(value = "ID of a specific category", required = true)
            @PathVariable(value="categoryId") final Long categoryId) {

        return new Resources<>(
                getProductsFromCategoryId(categoryId).stream()
                .filter(CatalogUtils.nonArchivedProduct)
                .map(product -> productConverter.createDto(product))
                .collect(Collectors.toList())
        );
    }

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
            @RequestParam(value = "href", required = true) @NotEmpty @URL String href) {

        long hrefProductId = CatalogUtils.getIdFromUrl(href);

        final Category categoryEntity = Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils.nonArchivedCategory)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));

        /* (mst) ProductController's POST methods guarantee there will be no duplicates therefore... */
        final Product productToAdd = Optional.ofNullable(catalogService.findProductById(hrefProductId))
                .filter(CatalogUtils.nonArchivedProduct)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + hrefProductId + " does not exist"));

        final CategoryProductXref productToAddXref = new CategoryProductXrefImpl();
        productToAddXref.setProduct(productToAdd);
        productToAddXref.setCategory(categoryEntity);

        if(!categoryEntity.getAllProductXrefs().contains(productToAddXref)) {
            categoryEntity.getAllProductXrefs().add(productToAddXref);
            catalogService.saveCategory(categoryEntity);
            /* TODO: (mst) add URI */
            return CREATED;
        } else {
            return CONFLICT;
        }
    }

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
            @RequestParam(value = "href", required = true) @NotEmpty @URL String href) {

        long hrefProductId = CatalogUtils.getIdFromUrl(href);

    	/* (mst) Ok, here we do NOT remove the product completely from catalog -> this is the job of the ProductController! */
        getProductsFromCategoryId(categoryId).stream()
                .filter(CatalogUtils.nonArchivedProduct)
                .filter(x -> x.getId() == hrefProductId)
                .findAny()
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + hrefProductId + " does not exist in category with ID: " + categoryId));


        Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils.nonArchivedCategory)
                .map(e -> {
                    CategoryProductXref xref = e.getAllProductXrefs().stream()
                            .filter(x -> x.getProduct().getId() == hrefProductId)
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


        return NO_CONTENT;

    }

    /* ------------------------------- HELPER METHODS ------------------------------- */

    private List<Product> getProductsFromCategoryId(final long categoryId) throws ResourceNotFoundException {

        return Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils.nonArchivedCategory)
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
