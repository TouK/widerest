package pl.touk.widerest.api.catalog.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import pl.touk.widerest.api.catalog.DtoConverters;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/catalog/categories")
@Api(value = "/catalog/categories", description = "Category catalog")
public class CategoryController {

    @Resource(name="blCatalogService")
    protected CatalogService catalogService;


    /* GET /categories */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(value = "Get a list of all categories", response = List.class)
    public ResponseEntity<List<CategoryDto>> readAllCategories() {
        return new ResponseEntity<>(
                catalogService.findAllCategories().stream().map(DtoConverters.categoryEntityToDto).collect(Collectors.toList()),
                HttpStatus.OK);
    }

    /* POST /categories */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation(value = "Add a new category", response = Void.class)
    public void saveOneCategory(@RequestBody CategoryDto categoryDto) {

        if(categoryDto != null) {

            catalogService.saveCategory(DtoConverters.categoryDtoToEntity.apply(categoryDto));
        }
    }

    /* GET /categories/{id} */
    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Get a single category details", response = CategoryDto.class)
    public CategoryDto readOneCategoryById(@PathVariable(value="id") Long id) {
        return Optional.ofNullable(catalogService.findCategoryById(id))
                .map(DtoConverters.categoryEntityToDto)
                .orElseThrow(ResourceNotFoundException::new);
    }

    /* DELETE /categories/id */
    /* TODO! */
    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE) @ApiOperation(value = "Delete an existing category", response = Void.class)
    public void removeOneCategoryById(@PathVariable(value = "id") Long id) {

        Category categoryToDelete = catalogService.findCategoryById(id);


        if(categoryToDelete != null) {
            catalogService.removeCategory(categoryToDelete);
        }
    }

    /* PUT /categories/{id} */
    /* TODO! */
    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @ApiOperation(value = "Update an existing category details", response = Void.class)
    public void changeOneCategory(@PathVariable(value = "id") Long id, @RequestBody CategoryDto categoryDto) {
        Category categoryToChange = catalogService.findCategoryById(id);

        if(categoryToChange != null) {
            catalogService.saveCategory(DtoConverters.categoryDtoToEntity.apply(categoryDto));
        } else {
            throw new ResourceNotFoundException("Cannot change category with id " + id + ". Not Found");
        }
    }

    /* GET /categories/{id}/products */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{id}/products", method = RequestMethod.GET)
    @ApiOperation(value = "Get a list of products from a category", response = List.class)
    public ResponseEntity<List<ProductDto>> readProductsFromCategory(@PathVariable(value="id") Long id) {
        Category category = catalogService.findCategoryById(id);

        if(category == null) {
            throw new ResourceNotFoundException("Cannot find category with id: " + id);
        }

        /* TODO: TEMPORARY! */
        return new ResponseEntity<>(
                category.getAllProducts().stream().map(DtoConverters.productEntityToDto).collect(Collectors.toList()),
                HttpStatus.OK);

    }

    /* POST /categories/{id}/products */
    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/{id}/products", method = RequestMethod.POST)
    @ApiOperation(value = "Add a new product in a category", response = Void.class)
    public void saveOneProductInCategory(@PathVariable(value="id") Long categoryId, @RequestBody ProductDto productDto) {
        Category category = catalogService.findCategoryById(categoryId);

        if (category == null) {
            throw new ResourceNotFoundException("Cannot find category with id: " + categoryId);
        }

        /* TODO: check if a product already exists */
        category.getAllProducts().add(DtoConverters.productDtoToEntity.apply(productDto));

        /* Updates??!!!! */
        catalogService.saveCategory(category);
    }


    /* GET /categories/{id}/products/{productId} */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{id}/products/{productId}", method = RequestMethod.GET)
    @ApiOperation(value = "Get a single product details from a category", response = ProductDto.class)
    public ProductDto readOneProductFromCategory(@PathVariable(value="id") Long categoryId, @PathVariable(value = "productId") Long productId) {

        Product product = this.getProductsFromCategoryId(categoryId).stream()
                .filter(x -> x.getId() == productId)
                .limit(2)
                .collect(Collectors.toList()).get(0);

        if(product == null) {
            throw new ResourceNotFoundException("Cannot find product with id: " + categoryId + " in category: " + categoryId);
        } else {
            return DtoConverters.productEntityToDto.apply(product);
        }
    }

    /* PUT /categories/{id}/products/{productId} */
    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/{id}/products/{productId}", method = RequestMethod.PUT)
    @ApiOperation(value = "Update details of a single product in a category", response = Void.class)
    public void changeOneProductFromCategory(@PathVariable(value="id") Long categoryId,
                                             @PathVariable(value = "productId") Long productId,
                                             @RequestBody ProductDto productDto) {

        Product product = this.getProductsFromCategoryId(categoryId).stream()
                .filter(x -> x.getId() == productId)
                .limit(2)
                .collect(Collectors.toList()).get(0);

        if(product == null) {
            throw new ResourceNotFoundException("Cannot find product with id: " + categoryId + " in category: " + categoryId);
        } else {
            /* TODO:  Check if products category and categoryId match */
            catalogService.saveProduct(DtoConverters.productDtoToEntity.apply(productDto));
        }
    }

    /* DELETE /categories/{id}/products/{productId} */
    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/{id}/products/{productId}", method = RequestMethod.DELETE)
    @ApiOperation(value = "Delete a product from a category", response = Void.class)
    public void removeOneProductFromCategory(@PathVariable(value="id") Long categoryId, @PathVariable(value = "productId") Long productId) {

        Product product = this.getProductsFromCategoryId(categoryId).stream()
                .filter(x -> x.getId() == productId)
                .limit(2)
                .collect(Collectors.toList()).get(0);

        if(product == null) {
            throw new ResourceNotFoundException("Cannot find product with id: " + categoryId + " in category: " + categoryId);
        }

        catalogService.removeProduct(product);

    }

    private List<Product> getProductsFromCategoryId(Long categoryId) throws ResourceNotFoundException {
        Category category = catalogService.findCategoryById(categoryId);

        if (category == null) {
            throw new ResourceNotFoundException("Cannot find category with id: " + categoryId);
        } else {
            return category.getAllProducts();
        }
    }


}