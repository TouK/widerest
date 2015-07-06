package pl.touk.widerest.api.catalog.api.catalog.controllers;

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
import pl.touk.widerest.api.catalog.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.api.catalog.dto.SkuDto;
import pl.touk.widerest.api.catalog.api.catalog.exceptions.ResourceNotFoundException;

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
    /* TODO: ResponseEntity!!! */
    @ApiOperation("readProducts")
    @PreAuthorize("permitALL")
    @RequestMapping(value = "/", method = RequestMethod.GET)
    @Transactional
    public List<ProductDto> getProducts() {
        return catalogService.findAllProducts().stream()
                .map(DtoConverters.productEntityToDto)
                .collect(Collectors.toList());
    }

    @ApiOperation("saveOneProduct")
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public void saveOneProduct(@RequestBody ProductDto productDto) {

    }


    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @PreAuthorize("permitAll")
    @Transactional
    public ProductDto readOneProduct(@PathVariable(value="id") Long productId) {

        return DtoConverters.productEntityToDto.apply(catalogService.findProductById(productId));

    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public void changeOneProduct(@PathVariable(value="id") Long id) {

    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void removeOneProduct(@PathVariable(value="id") Long id) {

    }

    @RequestMapping(value = "/{id}/categories", method = RequestMethod.GET)
    @PreAuthorize("permitAll")
    public ResponseEntity<List<Category>> readCategoriesByProduct(@PathVariable (value = "id") Long productId) {
        return null;
    }

    @RequestMapping(value = "/{id}/skus", method = RequestMethod.GET)
    @PreAuthorize("permitAll")
    public ResponseEntity<List<SkuDto>> readSkusByProduct(@PathVariable(value = "id") Long productId) {
        return null;
    }

    @RequestMapping(value = "/{id}/skus", method = RequestMethod.POST)
    @PreAuthorize("permitAll")
    public void saveOneSkuByProduct(@PathVariable (value = "id") Long productId, @RequestBody SkuDto skusDto) {

    }

    @RequestMapping(value = "/{id}/related-products", method = RequestMethod.GET)
    @PreAuthorize("permitAll")
    public void getAllRelatedProducts(@PathVariable (value = "id") Long productId, @RequestBody SkuDto skusDto) {

    }
}