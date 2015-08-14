package pl.touk.widerest.api.catalog.controllers;


import io.swagger.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import javafx.util.Pair;
import org.broadleafcommerce.core.catalog.domain.*;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.inventory.service.type.InventoryType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import pl.touk.widerest.api.catalog.CatalogUtils;
import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;

@RestController
@RequestMapping("/catalog/categories")
@Api(value = "categories", description = "Category catalog endpoint")
public class CategoryController {

    @Resource(name="blCatalogService")
    protected CatalogService catalogService;

    @Resource(name = "wdDtoConverters")
    protected DtoConverters dtoConverters;

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
    public List<CategoryDto> readAllCategories(
            @ApiParam(value = "Amount of categories to be returned")
                @RequestParam(value = "limit", required = false) Integer limit,
            @ApiParam(value = "Offset which to start returning categories from")
                @RequestParam(value = "offset", required = false) Integer offset) {

        return catalogService.findAllCategories(limit != null ? limit : 0, offset != null ? offset : 0).stream()
                .filter(CatalogUtils::archivedCategoryFilter)
                .map(DtoConverters.categoryEntityToDto)
                .collect(Collectors.toList());
    }

    /* POST /categories */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation(
            value = "Add a new category",
            notes = "Adds a new category to the catalog. It does take duplicates (same NAME) into account. " +
                    "Returns an URL to the newly added category in the Location field of the HTTP response header",
            response = ResponseEntity.class)
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
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    	
    	/* (mst) Providing that both Description() and LongDescription() can be null, which...is OK, this one
    	 *       should do the job "better" IMO
    	 */
        long duplicatesCount = catalogService.findCategoriesByName(categoryDto.getName()).stream()
                .filter(CatalogUtils::archivedCategoryFilter)
                .count();

        if(duplicatesCount > 0) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }


        Category createdCategoryEntity = catalogService.saveCategory(DtoConverters.categoryDtoToEntity.apply(categoryDto));

        HttpHeaders responseHeader = new HttpHeaders();

        responseHeader.setLocation(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdCategoryEntity.getId())
                .toUri());

        return new ResponseEntity<>(responseHeader, HttpStatus.CREATED);
    }

    /* GET /categories/count */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/count", method = RequestMethod.GET)
    @ApiOperation(
            value = "Count all categories",
            notes = "Gets a number of all categories available in the catalog",
            response = Long.class
    )
    public Long getAllCategoriesCount() {
        return catalogService.findAllCategories().stream()
                .filter(CatalogUtils::archivedCategoryFilter)
                .count();
    }

    /* GET /categories/{id} */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{categoryId}", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get a single category details",
            notes = "Gets details of a single category specified by its ID",
            response = CategoryDto.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of category details", response = CategoryDto.class),
            @ApiResponse(code = 404, message = "The specified category does not exist or is marked as archived")
    })
    public CategoryDto readOneCategoryById(
            @ApiParam(value = "ID of a specific category", required = true)
                @PathVariable(value="categoryId") Long categoryId) {

        return Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils::archivedCategoryFilter)
                .map(DtoConverters.categoryEntityToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));

    }

    /* DELETE /categories/{categoryId} */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/{categoryId}", method = RequestMethod.DELETE)
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

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /* PUT /categories/{id} */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/{categoryId}", method = RequestMethod.PUT)
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
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        long duplicatesCount = catalogService.findCategoriesByName(categoryDto.getName()).stream()
                .filter(CatalogUtils::archivedCategoryFilter)
                .count();

        if(duplicatesCount > 0) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils::archivedCategoryFilter)
                .map(e -> CatalogUtils.updateCategoryEntityFromDto(e, categoryDto))
                .map(catalogService::saveCategory)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));


        return new ResponseEntity<>(HttpStatus.OK);
    }

    /* PATCH /categories/{id} */
    /* TODO: (mst) more "efficient" partial update */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/{categoryId}", method = RequestMethod.PATCH)
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
        if(categoryDto.getName() != null) {

            long duplicatesCount = catalogService.findCategoriesByName(categoryDto.getName()).stream()
                    .filter(CatalogUtils::archivedCategoryFilter)
                    .count();

            if(duplicatesCount > 0) {
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            }
        }

        Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils::archivedCategoryFilter)
                .map(e -> CatalogUtils.partialUpdateCategoryEntityFromDto(e, categoryDto))
                .map(catalogService::saveCategory)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));

        return new ResponseEntity<>(HttpStatus.OK);
    }


    /* GET /{categoryId}/availability */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{categoryId}/availability", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get category's products availability",
            notes = "Gets an availability of all the products in this category",
            response = Void.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of category's products availability"),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public String getCategoryByIdAvailability(
            @ApiParam(value = "ID of a specific category", required = true)
                @PathVariable(value = "categoryId") Long categoryId) {

        Category category = Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils::archivedCategoryFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));

        return Optional.ofNullable(category.getInventoryType()).map(InventoryType::getType).orElse(CatalogUtils.EMPTY_STRING);

    }

    /* PUT /{categoryId}/availability */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/{categoryId}/availability", method = RequestMethod.PUT)
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

        return new ResponseEntity<>(HttpStatus.OK);
    }




    /* GET /categories/{id}/products */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{categoryId}/products", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get all products in a category",
            notes = "Gets a list of all products belonging to a specified category",
            response = ProductDto.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of all products in a given category"),
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


    /* GET /categories/{id}/products/{productId} */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{categoryId}/products/{productId}", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get details of a product from a category",
            notes = "Gets details of a specific product in a given category",
            response = ProductDto.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of product details", response = ProductDto.class),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public ProductDto readOneProductFromCategory(
            @ApiParam(value = "ID of a specific category", required = true)
                @PathVariable(value="categoryId") Long categoryId,
            @ApiParam(value = "ID of a product belonging to the specified category", required = true)
                @PathVariable(value = "productId") Long productId) {

        return this.getProductsFromCategoryId(categoryId).stream()
                .filter(CatalogUtils::archivedProductFilter)
                .filter(x -> x.getId().longValue() == productId)
                .findAny()
                .map(dtoConverters.productEntityToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist in category with ID: " + categoryId));
    }



    /* PUT /categories/{id}/products/{productId} */
    /* (mst) This endpoint inserts only a reference to the product (so it has to exist)
     *            into Category's product list. In case the product does not exist, use
     *            ProductController's POST methods first
     */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/{categoryId}/products/{productId}", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Insert existing product into category",
            notes = "Inserts existing product into category. It actually only updates few references therefore" +
                    " to insert a completely new product refer to ProductControllers' POST methods first",
            response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Product successfully inserted into specified category"),
            @ApiResponse(code = 404, message = "The specified category or product does not exist"),
            @ApiResponse(code = 409, message = "Category already contains the specified product")
    })
    public ResponseEntity<?> insertOneProductIntoCategory(
            @ApiParam(value = "ID of a specific category", required = true)
                @PathVariable(value="categoryId") Long categoryId,
            @ApiParam(value = "ID of a product belonging to the specified category", required = true)
                @PathVariable(value = "productId") Long productId) {

        Category categoryEntity = Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils::archivedCategoryFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));

        /* (mst) ProductController's POST methods guarantee there will be no duplicates therefore... */
        Product productToAdd = Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));

        CategoryProductXref productToAddXref = new CategoryProductXrefImpl();
        productToAddXref.setProduct(productToAdd);
        productToAddXref.setCategory(categoryEntity);

        if(!categoryEntity.getAllProductXrefs().contains(productToAddXref)) {
            categoryEntity.getAllProductXrefs().add(productToAddXref);
            catalogService.saveCategory(categoryEntity);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }

    /* DELETE /categories/{categoryId}/products/{productId} */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_CATEGORY')")
    @RequestMapping(value = "/{categoryId}/products/{productId}", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Remove a product from a category",
            notes = "Removes a product from a specific category. It DOES NOT delete the product completely from catalog, just removes its " +
                    "references to the specified category",
            response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Specified product successfully removed from a category"),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public ResponseEntity<?> removeOneProductFromCategory(
            @ApiParam(value = "ID of a specific category", required = true)
                @PathVariable(value="categoryId") Long categoryId,
            @ApiParam(value = "ID of a product belonging to the specified category", required = true)
                @PathVariable(value = "productId") Long productId) {
    	
    	/* (mst) Ok, here we do NOT remove the product completely from catalog -> this is the job of the ProductController! */

        getProductsFromCategoryId(categoryId).stream()
                .filter(CatalogUtils::archivedProductFilter)
                .filter(x -> x.getId().longValue() == productId)
                .findAny()
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist in category with ID: " + categoryId));


        Optional.ofNullable(catalogService.findCategoryById(categoryId))
                .filter(CatalogUtils::archivedCategoryFilter)
                .map(e -> {
                    CategoryProductXref xref = e.getAllProductXrefs().stream()
                        .filter(x -> x.getProduct().getId().longValue() == productId)
                        .findAny()
                        .orElseThrow(() -> new ResourceNotFoundException("(Internal) Product with ID: " + productId + " not found on the list of references for category with ID: " + categoryId));
                    return new Pair<>(e,xref);
                })
                .map(e -> {
                    e.getKey().getAllProductXrefs().remove(e.getValue());
                    return e.getKey();
                })
                .map(catalogService::saveCategory)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID: " + categoryId + " does not exist"));


        return new ResponseEntity<>(HttpStatus.NO_CONTENT);




    }
    /* GET /categories/{categoryId}/products/count */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{categoryId}/products/count", method = RequestMethod.GET)
    @ApiOperation(
            value = "Count all products in a specific category",
            notes = "Gets a number of all products belonging to a specified category",
            response = Long.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of products count"),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public Long getAllProductsInCategoryCount(
            @ApiParam(value = "ID of a specific category", required = true)
                @PathVariable(value = "categoryId") Long categoryId) {
        return getProductsFromCategoryId(categoryId).stream()
                .filter(CatalogUtils::archivedProductFilter)
                .count();
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

}
