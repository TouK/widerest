package pl.touk.widerest.api.catalog.controllers;


import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.broadleafcommerce.common.persistence.Status;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.CategoryProductXref;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import pl.touk.widerest.api.catalog.DtoConverters;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;

@RestController
@RequestMapping("/catalog/categories")
@Api(value = "categories", description = "Category catalog endpoint")
public class CategoryController {

    @Resource(name="blCatalogService")
    protected CatalogService catalogService;

    /* GET /categories */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(
            value = "List all categories",
            notes = "Gets a list of all available categories in the catalog",
            response = CategoryDto.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of categories list", response = CategoryDto.class)
    })
    public ResponseEntity<List<CategoryDto>> readAllCategories() {
        return new ResponseEntity<>(
                catalogService.findAllCategories().stream()
                        .filter(e -> validateCategoryEntity(e))
                        .map(DtoConverters.categoryEntityToDto)
                        .collect(Collectors.toList()),
                HttpStatus.OK);
    }

    /* POST /categories */
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation(
            value = "Add a new category",
            notes = "Adds a new category to the catalog. It does take duplicates (same name and description) into account. " +
                    "Returns an URL to the newly added category in the Location field of the HTTP response header",
            response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A new category entry successfully created"),
            @ApiResponse(code = 409, message = "Category already exists")
    })
    public ResponseEntity<?> saveOneCategory(@RequestBody CategoryDto categoryDto) {

        long duplicatesCount = catalogService.findCategoriesByName(categoryDto.getName()).stream()
                .filter(x -> x.getDescription().equals(categoryDto.getDescription()))
                .filter(e -> validateCategoryEntity(e))
                .count();

        if(duplicatesCount > 0) {
            return new ResponseEntity<>(null, HttpStatus.CONFLICT);
        }

        Category createdCategoryEntity = catalogService.saveCategory(DtoConverters.categoryDtoToEntity.apply(categoryDto));

        HttpHeaders responseHeader = new HttpHeaders();

        responseHeader.setLocation(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdCategoryEntity.getId())
                .toUri());

        return new ResponseEntity<>(null, responseHeader, HttpStatus.CREATED);
    }

    /* GET /categories/count */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/count", method = RequestMethod.GET)
    @ApiOperation(
            value = "Count all categories",
            notes = "Gets a number of all, non-archived categories available in the catalog",
            response = Long.class
    )
    public Long getAllCategoriesCount() {
        return catalogService.findAllCategories().stream()
                .filter(e -> validateCategoryEntity(e))
                .count();
    }


    /* GET /categories/{id} */
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{categoryId}", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get a single category details",
            notes = "Gets details of a single non-archived category specified by its ID",
            response = CategoryDto.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of category details", response = CategoryDto.class),
            @ApiResponse(code = 404, message = "The specified category does not exist or is marked as archived")
    })
    public CategoryDto readOneCategoryById(@PathVariable(value="categoryId") Long categoryId) {

        Category categoryEntity = Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .orElseThrow(() -> new ResourceNotFoundException("Cannot find category with ID: " + categoryId));

        if(((Status)categoryEntity).getArchived() == 'Y') {
            throw new ResourceNotFoundException("Cannot find category with ID: " + categoryId + ". Category marked as archived");
        }

        return DtoConverters.categoryEntityToDto.apply(categoryEntity);
    }

    /* DELETE /categories/id */
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Delete an existing category",
            notes = "Removes an existing category from catalog by marking it as archived",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful removal of the specified category"),
            @ApiResponse(code = 404, message = "The specified category does not exist or is already marked as archived")
    })
    public void removeOneCategoryById(@PathVariable(value = "id") Long id) {

        Optional.ofNullable(catalogService.findCategoryById(id))
                .map(e -> {
                    catalogService.removeCategory(e);
                    return e;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Cannot delete category with ID: " + id + ". Category does not exist"));
    }

    /* PUT /categories/{id} */
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Update an existing category",
            notes = "Updates an existing category with new details",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful update of the specified category"),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public void changeOneCategory(@PathVariable(value = "id") Long id, @RequestBody CategoryDto categoryDto) {

        Optional.ofNullable(catalogService.findCategoryById(id))
                .filter(x -> validateCategoryEntity(x))
                .map(x -> {
                    categoryDto.setCategoryId(x.getId());
                    catalogService.saveCategory(DtoConverters.categoryDtoToEntity.apply(categoryDto));
                    return x;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Cannot change category with ID " + id + ". Category not found"));

    }

    /* GET /categories/{id}/products */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{id}/products", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get products in a category",
            notes = "Gets a list of all products belonging to a specified category",
            response = ProductDto.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of all products in a given category"),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public List<ProductDto> readProductsFromCategory(@PathVariable(value="id") Long id) {

        return Optional.ofNullable(catalogService.findCategoryById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Cannot find category with ID: " + id))
                .getAllProductXrefs().stream()
                .map(e -> e.getProduct())
                .filter(entity -> validateProductEntity(entity))
                .map(DtoConverters.productEntityToDto)
                .collect(Collectors.toList());

    }

    /* POST /categories/{id}/products */
    /*
     * TODO: What if the product has defaultSKU set but no entry in allSkus list? (= copy?)
     */
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @Transactional
    @RequestMapping(value = "/{id}/products", method = RequestMethod.POST)
    @ApiOperation(
            value = "Add a product to the category",
            notes = "Adds a product to the specified category and returns" +
                    " an URL to it in the Location field of the HTTP response header",
            response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Specified product successfully added"),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public ResponseEntity<?> saveOneProductInCategory(@PathVariable(value="id") Long categoryId, @RequestBody ProductDto productDto) {
        Category category = catalogService.findCategoryById(categoryId);

        if (!validateCategoryEntity(category)) {
            throw new ResourceNotFoundException("Cannot find category with id: " + categoryId);
        }

        /* TODO: check if the product already exists */
        category.getAllProducts().add(DtoConverters.productDtoToEntity.apply(productDto));

        Category updatedCategory = catalogService.saveCategory(category);

        HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.setLocation(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{catId}/products/{id}")
                .buildAndExpand(categoryId, updatedCategory.getId())
                .toUri());

        return new ResponseEntity<>(null, httpHeaders, HttpStatus.CREATED);
    }


    /* GET /categories/{id}/products/{productId} */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{id}/products/{productId}", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get details of a product from a category",
            notes = "Gets details of a specific product in a given category",
            response = ProductDto.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of product details", response = ProductDto.class),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public ProductDto readOneProductFromCategory(@PathVariable(value="id") Long categoryId,
                                                 @PathVariable(value = "productId") Long productId) {

        return this.getProductsFromCategoryId(categoryId).stream()
                .filter(entity -> validateProductEntity(entity))
                .filter(x -> x.getId() == productId)
                .findAny()
                .map(DtoConverters.productEntityToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot find product with id: " + productId + " in category: " + categoryId));

    }

    // TODO: test if it actually works
    /* PUT /categories/{id}/products/{productId} */
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/{id}/products/{productId}", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Update details of a single product in a category",
            notes = "Updates details of the specific product in a given category",
            response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successful update of product details"),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public void changeOneProductFromCategory(@PathVariable(value="id") Long categoryId,
                                             @PathVariable(value = "productId") Long productId,
                                             @RequestBody ProductDto productDto) {

        this.getProductsFromCategoryId(categoryId).stream()
                .filter(entity -> validateProductEntity(entity))
                .filter(x -> x.getId() == productId)
                .findAny()
                .map(e -> {
                    /* TODO:  Check if products category and categoryId match */
                    catalogService.saveProduct(DtoConverters.productDtoToEntity.apply(productDto));
                    return e;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Cannot find product with id: " + categoryId + " in category: " + categoryId));

    }

    /* DELETE /categories/{id}/products/{productId} */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/{id}/products/{productId}", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Remove a product from a category",
            notes = "Removes a product from a specific category",
            response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Specified product successfully removed from a category"),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public void removeOneProductFromCategory(@PathVariable(value="id") Long categoryId,
                                             @PathVariable(value = "productId") Long productId) {

        Category category = Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .map(e -> {
                    if (!validateCategoryEntity(e)) {
                        return null;
                    } else return e;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Cannot find category with id: " + categoryId));

        CategoryProductXref productXref = category
                .getAllProductXrefs().stream()
                .filter(e -> validateProductEntity(e.getProduct()))
                .filter(x -> x.getId() == productId)
                .findAny()
                .orElseThrow(() -> new ResourceNotFoundException("Cannot find product with ID: " + categoryId + " in category ID: " + categoryId));

        List<CategoryProductXref> list = category.getAllProductXrefs();
        list.remove(productXref);

        category.setAllProductXrefs(list);

        catalogService.saveCategory(category);
    }

    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{id}/products/count", method = RequestMethod.GET)
    @ApiOperation(
            value = "Count all products in a specific category",
            notes = "Gets a number of all products belonging to a specified category",
            response = Long.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of products count"),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public Long getAllProductsInCategoryCount(@PathVariable(value = "id") Long categoryId) {

        return getProductsFromCategoryId(categoryId).stream().count();
    }

    private List<Product> getProductsFromCategoryId(Long categoryId) throws ResourceNotFoundException {
        Category category = catalogService.findCategoryById(categoryId);

        if (!validateCategoryEntity(category)) {
            throw new ResourceNotFoundException("Cannot find category with id: " + categoryId);
        } else {
            return category.getAllProductXrefs().stream().map(e -> e.getProduct()).collect(Collectors.toList());
        }
    }

    private Boolean validateCategoryEntity(Category categoryToValidate) {
        if(categoryToValidate == null || ((Status)categoryToValidate).getArchived() == 'Y') {
            return false;
        } else {
            return true;
        }
    }

    private boolean validateProductEntity(Product product) {
        return ((Status) product).getArchived() == 'N';
    }

}