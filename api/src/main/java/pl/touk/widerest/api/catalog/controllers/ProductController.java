package pl.touk.widerest.api.catalog.controllers;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.broadleafcommerce.core.catalog.domain.*;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.inventory.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pl.touk.widerest.api.catalog.*;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.dto.SkuDto;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/catalog/products")
@Api
public class ProductController {

    @Resource(name = "blCatalogService")
    protected CatalogService catalogService;

    @Resource(name = "blInventoryService")
    protected InventoryService inventoryService;

    /* GET /products */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(value = "Get a flat list of products", response = ProductDto.class)
    public ResponseEntity<List<ProductDto>> getProducts() {
        return new ResponseEntity<>(
                catalogService.findAllProducts().stream().map(DtoConverters.productEntityToDto).collect(Collectors.toList()),
                HttpStatus.OK);
    }

    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/", method = RequestMethod.POST)
    @ApiOperation(value = "Add a new product", response = Void.class)
    public void saveOneProduct(@RequestBody ProductDto productDto) {

        /*TODO: check if the product exists */

        if(productDto != null) {
            catalogService.saveProduct(DtoConverters.productDtoToEntity.apply(productDto));
        } else {
            //throw ???
        }

    }


    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Get a single product details", response = ProductDto.class)
    public ProductDto readOneProduct(@PathVariable(value="id") Long productId) {

        return Optional.ofNullable(catalogService.findProductById(productId))
                .map(DtoConverters.productEntityToDto)
                .orElseThrow(ResourceNotFoundException::new);


    }

    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @ApiOperation(value = "Update an existing product details", response = Void.class)
    public void changeOneProduct(@PathVariable(value="id") Long id, @RequestBody ProductDto productDto) {

        Product productToChange = catalogService.findProductById(id);

        if(productToChange!= null) {
            catalogService.saveProduct(DtoConverters.productDtoToEntity.apply(productDto));
        } else {
            throw new ResourceNotFoundException("Cannot change product with id " + id + ". Not Found");
        }

    }

    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ApiOperation(value = "Delete an existing product", response = Void.class)
    public void removeOneProduct(@PathVariable(value="id") Long id) {

        Product productToDelete = catalogService.findProductById(id);

        if(productToDelete != null) {
            catalogService.removeProduct(productToDelete);
        }

    }

    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{id}/categories", method = RequestMethod.GET)
    @ApiOperation(value = "Get a list of categories ", response = Void.class)
    public ResponseEntity<List<CategoryDto>> readCategoriesByProduct(@PathVariable (value = "id") Long productId) {

        Product product = catalogService.findProductById(productId);

        if (product == null) {
            throw new ResourceNotFoundException("Product with id: " + productId + " does not exist");

        }

        /* Deprecated */
        List<Category> productCategories = product.getAllParentCategories();

        return new ResponseEntity<>(
                productCategories.stream().map(DtoConverters.categoryEntityToDto).collect(Collectors.toList()),
                HttpStatus.OK
        );

    }

    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{id}/skus", method = RequestMethod.GET)
    public ResponseEntity<List<SkuDto>> readSkusByProduct(@PathVariable(value = "id") Long productId) {

        Product product = catalogService.findProductById(productId);

        if (product == null) {
            throw new ResourceNotFoundException("Product with id: " + productId + " does not exist");
        }

        return new ResponseEntity<>(
                product.getAllSkus().stream().map(DtoConverters.skuEntityToDto).collect(Collectors.toList()),
                HttpStatus.OK);

    }


    @RequestMapping(value = "/{id}/skus", method = RequestMethod.POST)
    @PreAuthorize("permitAll")
    public void saveOneSkuByProduct(@PathVariable (value = "id") Long productId, @RequestBody SkuDto skusDto) {

    }

}