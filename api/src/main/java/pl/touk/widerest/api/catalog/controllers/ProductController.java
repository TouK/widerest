package pl.touk.widerest.api.catalog.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.broadleafcommerce.common.currency.dao.BroadleafCurrencyDao;
import org.broadleafcommerce.common.currency.service.BroadleafCurrencyService;
import org.broadleafcommerce.core.catalog.domain.*;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.inventory.service.InventoryService;
import org.broadleafcommerce.core.rating.service.RatingService;
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

import pl.touk.widerest.api.catalog.CatalogUtils;
import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.catalog.dto.*;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;


@RestController
@RequestMapping("/catalog/products")
@Api(value = "products", description = "Product catalog endpoint")
public class ProductController {

    @Resource(name = "blCatalogService")
    protected CatalogService catalogService;

    @Resource(name = "blInventoryService")
    protected InventoryService inventoryService;

    @Resource(name = "blRatingService")
    protected RatingService ratingService;

    @Resource(name = "wdDtoConverters")
    protected DtoConverters dtoConverters;

    /* GET /products */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(
            value = "List all products",
            notes = "Gets a list of all available products in the catalog",
            response = ProductDto.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of products list", response = ProductDto.class)
    })
    public List<ProductDto> getAllProducts() {
        return catalogService.findAllProducts().stream()
                .filter(CatalogUtils::archivedProductFilter)
                .map(dtoConverters.productEntityToDto)
                .collect(Collectors.toList());
    }

    /* POST /products */
    /* TODO: (mst) Merging existing products SKUs instead of blindly refusing to add existing product */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation(
            value = "Add a new product",
            notes = "Adds a new product to the catalog. If the provided category does not exist," +
                    " it is simply ignored. Returns an URL to the newly added " +
                    "product in the Location field of the HTTP response header",
            response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A new product successfully created"),
            @ApiResponse(code = 400, message = "Not enough data has been provided"),
            @ApiResponse(code = 409, message = "Product already exists")
    })
    public ResponseEntity<?> addOneProduct(@RequestBody ProductDto productDto) {

        /* (mst) every new Product has to have at least a DefaultSKU and a name */
        if(productDto.getName() == null || productDto.getName().isEmpty() || productDto.getDefaultSku() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if(!productDto.getName().equals(productDto.getDefaultSku().getName())) {
            productDto.getDefaultSku().setName(productDto.getName());
        }

        /* TODO: (mst) modify matching rules (currently only "by name") + refactor to separate method */
        long duplicatesCount = catalogService.findProductsByName(productDto.getName()).stream()
                .filter(CatalogUtils::archivedProductFilter)
                .count();

        /* (mst) Old "duplicate matching" code */
        /*
        long duplicatesCount = catalogService.findProductsByName(productDto.getName()).stream()
                .filter(x -> x.getDescription().equals(productDto.getDescription()) || x.getLongDescription().equals(productDto.getLongDescription()))
                .filter(CatalogUtils::archivedProductFilter)
                .count();
                */

        if(duplicatesCount > 0) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }


        Product newProduct = dtoConverters.productDtoToEntity.apply(productDto);

        Sku defaultSku = dtoConverters.skuDtoToEntity.apply(productDto.getDefaultSku());

        /* (mst) if there is a default category set, try to find it and connect it with the product.
                 Otherwise just ignore it.

                 TODO: Refactor this one using Lambdas
         */
        if(productDto.getCategoryName() != null && !productDto.getCategoryName().isEmpty()) {
            Optional<Category> categoryToReference = catalogService.findCategoriesByName(productDto.getCategoryName()).stream()
                    .filter(CatalogUtils::archivedCategoryFilter)
                    .findAny();

            if(categoryToReference.isPresent()) {
                newProduct.setCategory(categoryToReference.get());
            }
        }

        newProduct.setDefaultSku(defaultSku);


        /* TODO: (mst) creating Product Bundles */
        //Product newProduct = catalogService.createProduct(ProductType.PRODUCT);



        newProduct = catalogService.saveProduct(newProduct);

        HttpHeaders responseHeader = new HttpHeaders();

        responseHeader.setLocation(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newProduct.getId())
                .toUri());

        return new ResponseEntity<>(null, responseHeader, HttpStatus.CREATED);
    }

    /* GET /products/count */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/count", method = RequestMethod.GET)
    @ApiOperation(
            value = "Count all products",
            notes = "Gets a number of all available products",
            response = Long.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of products count")
    })
    public Long getAllProductsCount() {
        return catalogService.findAllProducts().stream()
                .filter(CatalogUtils::archivedProductFilter)
                .count();
    }

    /* GET /products/{id} */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{productId}", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get a single product details",
            notes = "Gets details of a single product specified by its ID",
            response = ProductDto.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of product details", response = ProductDto.class),
            @ApiResponse(code = 404, message = "The specified product does not exist or is marked as archived")
    })
    public ProductDto readOneProductById(@PathVariable(value = "productId") Long productId) {

        return Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .map(dtoConverters.productEntityToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));
    }

    /* PUT /products/{id} */
    /* TODO: (mst) Implementation */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Update an existing product",
            notes = "Updates an existing product with new details. If the category does not exist, it does NOT create it!",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful update of the specified product"),
            @ApiResponse(code = 400, message = "Not enough data has been provided"),
            @ApiResponse(code = 404, message = "The specified product does not exist"),
            @ApiResponse(code = 409, message = "Product with that name already exists")
    })
    public ResponseEntity<?> updateOneProduct(@PathVariable(value = "productId") Long productId,
                                              @RequestBody ProductDto productDto) {

        if(productDto.getName() == null || productDto.getName().isEmpty() || productDto.getDefaultSku() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if(!productDto.getName().equals(productDto.getDefaultSku().getName())) {
            productDto.getDefaultSku().setName(productDto.getName());
        }

        long duplicatesCount = catalogService.findProductsByName(productDto.getName()).stream()
                .filter(CatalogUtils::archivedProductFilter)
                .count();

        if(duplicatesCount > 0) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }



        Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .map(p -> {
                    Product productEntity = dtoConverters.productDtoToEntity.apply(productDto);
                    productEntity.setId(productId);
                    catalogService.saveProduct(productEntity);
                    return p;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Cannot change product with id " + productId + ". Not Found"));

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /* PATCH /products/{id} */
    /* TODO (mst) Implement? :) */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}", method = RequestMethod.PATCH)
    @ApiOperation(
            value = "Partially update an existing product",
            notes = "Partially updates an existing category with new details. It does not follow the format specified in RFC yet though",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful update of the specified product"),
            @ApiResponse(code = 404, message = "The specified product does not exist"),
            @ApiResponse(code = 409, message = "Product with that name already exists")
    })
    public ResponseEntity<?> partialUpdateOneProduct(@PathVariable(value = "productId") Long productId, @RequestBody ProductDto productDto) {
        throw new ResourceNotFoundException("To be implemented!");
    }

    /* DELETE /products/{id} */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Delete a product",
            notes = "Removes an existing product along with its SKUs from catalog",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 204, message = "Successful removal of the specified product"),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public ResponseEntity<?> removeOneProductById(@PathVariable(value = "productId") Long productId) {
        Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .map(e -> {
                    catalogService.removeProduct(e);
                    return e;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Cannot delete product with ID: " + productId + ". Product does not exist"));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /* GET /products/{id}/categories */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{productId}/categories", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get product's categories",
            notes = "Gets a list of all categories belonging to a specified product",
            response = CategoryDto.class,
            responseContainer = "List")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of product's categories"),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public List<CategoryDto> readCategoriesByProduct(@PathVariable(value = "productId") Long productId) {

        return Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllParentCategoryXrefs().stream()
                .map(CategoryProductXref::getCategory)
                .filter(CatalogUtils::archivedCategoryFilter)
                .map(DtoConverters.categoryEntityToDto)
                .collect(Collectors.toList());
    }

    /* GET /products/{id}/categories */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{productId}/categories/count", method = RequestMethod.GET)
    @ApiOperation(
            value = "Count product's categories",
            notes = "Gets a number of categories, specified product belongs to",
            response = Long.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of product's categories"),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public Long getCategoriesByProductCount(@PathVariable(value = "productId") Long productId) {

        return Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllParentCategoryXrefs().stream()
                .map(CategoryProductXref::getCategory)
                .filter(CatalogUtils::archivedCategoryFilter)
                .count();
    }


    /* GET /products/{id}/skus */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{productId}/skus", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get product's SKUs",
            notes = "Gets a list of all SKUs available for a specified product",
            response = SkuDto.class,
            responseContainer = "List")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of all available SKUs"),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public List<SkuDto> readSkusByProduct(@PathVariable(value = "productId") Long productId) {

        return Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllSkus().stream()
                .map(dtoConverters.skuEntityToDto)
                .collect(Collectors.toList());

    }

    /* POST /products/{id}/skus */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}/skus", method = RequestMethod.POST)
    @ApiOperation(
            value = "Add a SKU to the product",
            notes = "Adds a SKU to the existing product",
            response = SkuDto.class,
            responseContainer = "List")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Specified SKU successfully added"),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public ResponseEntity<?> saveOneSkuByProduct(@PathVariable(value = "productId") Long productId,
                                                 @RequestBody SkuDto skuDto) {

        Product product = Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));

        Sku newSkuEntity = dtoConverters.skuDtoToEntity.apply(skuDto);

        newSkuEntity.setProduct(product);


        if(skuDto.getSkuProductOptionValues() != null) {

            Set<SkuProductOptionValueXref> skuProductOptionValueXrefs = new HashSet<>();

            /*
            Set<ProductOption> allProductOptions = skuDto.getProductOptionValues().stream()
                    .map(ProductOptionValueDto::getProductOption)
                    .map(DtoConverters.productOptionDtoToEntity)
                    .collect(Collectors.toSet());
*/

            /*
            Set<ProductOptionValue> skusProductOptionValues = skuDto.getProductOptionValues().stream()
                    .map(DtoConverters.productOptionValueDtoToEntity)
                    .collect(Collectors.toSet());
*/
            for (SkuProductOptionValueDto skuProductOption: skuDto.getSkuProductOptionValues()) {

                ProductOptionValue productOptionValue = new ProductOptionValueImpl();
                productOptionValue.setProductOption(dtoConverters.getProductOptionByNameForProduct(skuProductOption.getAttributeName(), product));
                productOptionValue.setAttributeValue(skuProductOption.getAttributeValue());

                SkuProductOptionValueXrefImpl skuProductOptionValueXref = new SkuProductOptionValueXrefImpl(newSkuEntity, productOptionValue);

                skuProductOptionValueXrefs.add(skuProductOptionValueXref);
            }

            newSkuEntity.setProductOptionValueXrefs(skuProductOptionValueXrefs);

        }



        /*
        skuDto.getProductOptionValues().stream()
                .map(ProductOptionValueDto::getProductOption)
                .map(DtoConverters.productOptionDtoToEntity)
                .forEach(catalogService::saveProductOption);

        */

        newSkuEntity = catalogService.saveSku(newSkuEntity);

        /*
        for(ProductOption productOption : allProductOptions) {

            ProductOptionValue productOptionValue = new ProductOptionValueImpl();
            productOptionValue.setProductOption(catalogService.saveProductOption(productOption));
            productOptionValue.setAttributeValue(productOption.getAttributeName());


            SkuProductOptionValueXrefImpl skuProductOptionValueXref = new SkuProductOptionValueXrefImpl(newSkuEntity, productOptionValue);
            skuProductOptionValueXref.setId(productOptionValue.getId());

            skuProductOptionValueXrefs.add(skuProductOptionValueXref);
        }
*/


        //newSkuEntity.setProductOptionValueXrefs(skuProductOptionValueXrefs);

        //newSkuEntity.setProductOptionValueXrefs(null);

        //newSkuEntity = catalogService.saveSku(newSkuEntity);

        List<Sku> allProductsSkus = new ArrayList<>();
        allProductsSkus.addAll(product.getAllSkus());
        allProductsSkus.add(newSkuEntity);

        //product.getAllSkus().add(newSkuEntity);
        product.setAdditionalSkus(allProductsSkus);
        catalogService.saveProduct(product);

        HttpHeaders responseHeader = new HttpHeaders();

        responseHeader.setLocation(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/products/{productId}/skus/{skuId}")
                .buildAndExpand(productId,newSkuEntity.getId())
                .toUri());

        return new ResponseEntity<>(responseHeader, HttpStatus.CREATED);
    }

    /* GET /products/{productId}/skus/{skuId} */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{productId}/skus/{skuId}", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get a single SKU details",
            notes = "Gets details of a single SKU, specified by its ID",
            response = SkuDto.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of SKU details"),
            @ApiResponse(code = 404, message = "The specified SKU or product does not exist")
    })
    public SkuDto getSkuById(@PathVariable(value = "productId") Long productId,
            @PathVariable(value = "skuId") Long skuId) {

        return Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllSkus().stream()
                .filter(x -> x.getId().longValue() == skuId)
                .findAny()
                .map(dtoConverters.skuEntityToDto)
                .orElseThrow(() -> new ResourceNotFoundException("SKU with ID: " + skuId + " does not exist or is not related to product with ID: " + productId));
    }

    /* GET /products/{productId}/skus/default */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{productId}/skus/default", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get default SKU details",
            notes = "Gets details of a default SKU belonging to a specified product",
            response = SkuDto.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of SKU details"),
            @ApiResponse(code = 404, message = "The specified SKU or product does not exist")
    })
    public SkuDto getDefaultSkuByProductId(
            @PathVariable(value = "productId") Long productId) {

        return Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .map(Product::getDefaultSku)
                .map(dtoConverters.skuEntityToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));
    }

    /* POST /products/{productId}/skus/default */
    /* (mst) Experimental */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}/skus/default", method = RequestMethod.POST)
    @ApiOperation(
            value = "Replace default SKU",
            notes = "Replaces an existing default SKU with a new one",
            response = SkuDto.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successful replace of default SKU details"),
            @ApiResponse(code = 404, message = "The specified Product does not exist")
    })
    public ResponseEntity<?> changeDefaultSkuByProductId(
            @PathVariable(value = "productId") Long productId,
            @RequestBody SkuDto defaultSkuDto) {

        Product product = Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));

        Sku currentDefaultSKU = product.getDefaultSku();

        Sku newSkuEntity = dtoConverters.skuDtoToEntity.apply(defaultSkuDto);

        newSkuEntity.setProduct(product);
        newSkuEntity = catalogService.saveSku(newSkuEntity);

        product.setDefaultSku(newSkuEntity);
        catalogService.saveProduct(product);

        /* (mst) Removing a default SKU should technically remove the entire product...*/
        //catalogService.removeSku(currentDefaultSKU);

        HttpHeaders responseHeader = new HttpHeaders();

        responseHeader.setLocation(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/products/{productId}/skus/{skuId}")
                .buildAndExpand(productId,newSkuEntity.getId())
                .toUri());

        return new ResponseEntity<>(responseHeader, HttpStatus.CREATED);
    }


    /* GET /skus/count */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{productId}/skus/count", method = RequestMethod.GET)
    @ApiOperation(
            value = "Count all SKUs",
            notes = "Gets a number of all SKUs related to the specific product",
            response = Long.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of SKU count"),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public Long getAllSkusCount(@PathVariable(value = "productId") Long productId) {

        return Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllSkus().stream()
                .count();
    }

    /* DELETE /products/{productId}/skus/{id} */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}/skus/{skuId}", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Delete an existing SKU",
            notes = "Removes an existing SKU from product",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 204, message = "Successful removal of the specified SKU"),
            @ApiResponse(code = 404, message = "The specified SKU or product does not exist")
    })
    public ResponseEntity<?> deleteOneSkuById(@PathVariable(value = "productId") Long productId,
            @PathVariable(value = "skuId") Long skuId) {

        Product product = Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));

        if(product.getDefaultSku().getId().longValue() == skuId) {
            throw new RuntimeException("Cannot delete SKU with ID: " + skuId + " of product with ID: " + productId + " - default SKU");
        }

        Optional<Sku> skuToDelete = product.getAllSkus().stream()
                .filter(x -> x.getId().longValue() == skuId)
                .findFirst();

        if(!skuToDelete.isPresent()) {
            throw new ResourceNotFoundException("Cannot delete SKU with ID: " + skuId + ". SKU is not related to product with ID: " + productId + " or does not exist");
        }

        Sku skuToDeleteEntity = skuToDelete.get();

        List<Sku> newProductSkus = product.getAllSkus().stream()
                .filter(x -> x.getId().longValue() != skuId)
                .collect(Collectors.toList());

        catalogService.removeSku(skuToDeleteEntity);
        product.setAdditionalSkus(newProductSkus);

        catalogService.saveProduct(product);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /* PUT /products/{productId}/skus/{skuId} */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}/skus/{skuId}", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Update an existing SKU",
            notes = "Updates an exising SKU with new details. If the SKU does not exist, it does NOT create it!",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful update of the specified SKU"),
            @ApiResponse(code = 400, message = "Not enough data has been provided"),
            @ApiResponse(code = 404, message = "The specified product or SKU does not exist"),
            @ApiResponse(code = 409, message = "SKU with that name already exists")
    })
    public ResponseEntity<?> updateOneSkuByProductId(
            @PathVariable(value = "productId") Long productId,
            @PathVariable(value = "skuId") Long skuId,
            @RequestBody SkuDto skuDto) {

        if(skuDto.getName() == null || skuDto.getName().isEmpty() || skuDto.getSalePrice() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Product product = Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));


        Optional<Sku> skuToUpdate = product.getAllSkus().stream()
                .filter(x -> x.getId().longValue() == skuId)
                .findFirst();

        if(!skuToUpdate.isPresent()) {
            throw new ResourceNotFoundException("Cannot update SKU with ID: " + skuId + ". SKU is not related to product with ID: " + productId + " or does not exist");
        }

        Sku skuToUpdateEntity = skuToUpdate.get();

        skuToUpdateEntity = CatalogUtils.updateSkuEntityFromDto(skuToUpdateEntity, skuDto);

        catalogService.saveSku(skuToUpdateEntity);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /* PATCH /products/{productId}/skus/{skuId} */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}/skus/{skuId}", method = RequestMethod.PATCH)
    @ApiOperation(
            value = "Partially update an existing SKU",
            notes = "Partially updates an existing SKU with new details. It does not follow the format specified in RFC yet though",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful update of the specified SKU"),
            @ApiResponse(code = 404, message = "The specified product or SKU does not exist")
    })
    public ResponseEntity<?> partialUpdateOneSkuByProductId(
            @PathVariable(value = "productId") Long productId,
            @PathVariable(value = "skuId") Long skuId,
            @RequestBody SkuDto skuDto) {

        Product product = Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));


        Optional<Sku> skuToUpdate = product.getAllSkus().stream()
                .filter(x -> x.getId().longValue() == skuId)
                .findFirst();

        if(!skuToUpdate.isPresent()) {
            throw new ResourceNotFoundException("Cannot update SKU with ID: " + skuId + ". SKU is not related to product with ID: " + productId + " or does not exist");
        }

        Sku skuToUpdateEntity = skuToUpdate.get();

        skuToUpdateEntity = CatalogUtils.partialUpdateSkuEntityFromDto(skuToUpdateEntity, skuDto);

        catalogService.saveSku(skuToUpdateEntity);

        return new ResponseEntity<>(HttpStatus.OK);
    }


    /* GET /products/{id}/reviews *//*
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{id}/reviews", method = RequestMethod.GET)
    @ApiOperation(
            value = "List all reviews for the product",
            notes = "Gets a list of all reviews written for the specified product",
            response = ReviewDto.class,
            responseContainer = "List")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of available reviews"),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public List<ReviewDto> getReviewForProduct(@PathVariable(value = "id") Long productId) {

        return Optional.ofNullable(ratingService.readRatingSummary(productId.toString(), RatingType.PRODUCT))
                .map(r -> {
                    return r.getReviews().stream()
                            .map(DtoConverters.reviewEntityToDto)
                            .collect(Collectors.toList());
                })
                .orElse(Collections.emptyList());

    }

    *//* POST /products/{id}/reviews *//*
    @PreAuthorize("hasAnyRole('PERMISSION_ALL_ADMIN_ROLES', 'ROLE_USER')")
    @RequestMapping(value = "/{id}/reviews", method = RequestMethod.POST)
    @ApiOperation(
            value = "Add a review for a the product",
            notes = "Creates and adds a new review for the specified product",
            response = ResponseEntity.class)
    @ApiResponses({
            @ApiResponse(code = 201, message = "A new review for the product has been successfull added"),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public ResponseEntity<?> saveReviewForProduct(
            @PathVariable(value = "id") Long productId,
            @RequestBody ReviewDto reviewDto,
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails) {

        Product product = catalogService.findProductById(productId);

        if (product == null) {
            throw new ResourceNotFoundException("Cannot find product with ID: " + productId);
        }

        RatingSummary ratingSummary = ratingService.readRatingSummary(productId.toString(), RatingType.PRODUCT);

        if (ratingSummary == null) {
            *//* TODO: Do we create a new one ?! *//*
        }

        // TODO: Customer verification

        ratingSummary.getReviews().add(DtoConverters.reviewDtoToEntity.apply(reviewDto));

        ratingService.saveRatingSummary(ratingSummary);

        return new ResponseEntity<>(null, HttpStatus.CREATED);
    }

    *//* GET /products/{id}/reviews/count *//*

    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{id}/reviews/count", method = RequestMethod.GET)
    @ApiOperation(
            value = "Count all review for the product",
            notes = "Gets a number of all reviews for the specific product",
            response = Integer.class
    )
    public Integer getReviewCountForProduct(@PathVariable(value = "id") Long productId) {
        Integer reviewCount = 0;
        RatingSummary ratingSummary = ratingService.readRatingSummary(productId.toString(), RatingType.PRODUCT);

        if (ratingSummary != null) {
            reviewCount = ratingSummary.getNumberOfReviews();
        }

        return reviewCount;
    }

    *//* GET /products/{id}/reviews/count *//*
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{id}/ratings/count", method = RequestMethod.GET)
    @ApiOperation(
            value = "Count all ratings for the product",
            notes = "Gets a number of all ratings for the specific product",
            response = Integer.class
    )
    public Integer getRatingsCountForProduct(@PathVariable(value = "id") Long productId) {
        Integer ratingsCount = 0;
        RatingSummary ratingSummary = ratingService.readRatingSummary(productId.toString(), RatingType.PRODUCT);

        if (ratingSummary != null) {
            ratingsCount = ratingSummary.getNumberOfRatings();
        }

        return ratingsCount;
    }

    *//* GET /products/{id}/ratings/avg *//*
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{id}/ratings/avg", method = RequestMethod.GET)
    @ApiOperation(
            value = "Average ratings for the product",
            notes = "Gets an average rating for the specific product",
            response = Double.class
    )
    public Double getAverageRatingForProduct(@PathVariable(value = "id") Long productId) {
        Double avgRating = 0.0;
        RatingSummary ratingSummary = ratingService.readRatingSummary(productId.toString(), RatingType.PRODUCT);

        if (ratingSummary != null) {
            avgRating = ratingSummary.getAverageRating();
        }

        return avgRating;
    }

    *//* GET /products/{id}/ratings *//*
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{id}/ratings", method = RequestMethod.GET)
    @ApiOperation(
            value = "List all ratings for the product",
            notes = "Gets a list of all available ratings for the specified product",
            response = RatingDto.class,
            responseContainer = "List"
    )
    public List<RatingDto> getRatingsForProduct(@PathVariable(value = "id") Long productId) {
        RatingSummary ratingSummary = ratingService.readRatingSummary(productId.toString(), RatingType.PRODUCT);

        if (ratingSummary != null) {
            return ratingSummary.getRatings().stream().map(DtoConverters.ratingEntityToDto).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }*/
}


    /* PUT /skus/{id} */
    /*
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Update an existing SKU",
            notes = "Updates an exising SKU with new details",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful update of the specified SKU"),
            @ApiResponse(code = 404, message = "The specified category does not exist")
    })
    public void updateOneSkuById(
            @PathVariable (value = "id") Long skuId,
            @RequestBody SkuDto skuDto) {

        Sku sku = catalogService.findSkuById(skuId);

        if(sku == null) {
            throw new ResourceNotFoundException("Cannot find SKU with ID: " + skuId);
        }

        skuDto.setSkuId(skuId);

        catalogService.saveSku(DtoConverters.skuDtoToEntity.apply(skuDto));
    }
    */

/* ------------------------------- ARCHIVE CODE ------------------------------- */


 /*   @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}/categories", method = RequestMethod.POST)
    @ApiOperation(
            value = "Assign category to the product",
            notes = "Assigns an existing category to the product. In case the category does not exist, it gets created",
            response = Void.class
    )
    @ApiResponses({
            @ApiResponse(code = 201, message = "Category successfully added to the product"),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public void addCategoryToTheProduct(
            @PathVariable(value = "productId") Long productId,
            @RequestBody CategoryDto categoryDto) {

        Product product = Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));

        Optional<Category> category = catalogService.findCategoriesByName(categoryDto.getName()).stream()
                .filter(x -> x.getDescription().equals(categoryDto.getDescription())
                        && Optional.ofNullable(x.getLongDescription()).map(e -> e.equals(categoryDto.getLongDescription())).orElse(Boolean.TRUE))
                .filter(CatalogUtils::archivedCategoryFilter).findAny();

        Category categoryEntity = null;

        if(!category.isPresent()) {
            categoryEntity = catalogService.saveCategory(DtoConverters.categoryDtoToEntity.apply(categoryDto));
        } else {
            categoryEntity = category.get();
        }

        List<CategoryProductXref> allParentCategories = new ArrayList<>(product.getAllParentCategoryXrefs());

        *//*TODO (mst) Account for duplicates *//*

        if(!allParentCategories.contains(categoryEntity)) {
            CategoryProductXref categoryProductXref = new CategoryProductXrefImpl();
            categoryProductXref.setCategory(categoryEntity);
            categoryProductXref.setProduct(product);
            allParentCategories.add(categoryProductXref);
        }

        product.setAllParentCategoryXrefs(allParentCategories);

        Product d = catalogService.saveProduct(product);
    }
*/