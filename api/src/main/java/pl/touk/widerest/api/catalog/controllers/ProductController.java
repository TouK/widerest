package pl.touk.widerest.api.catalog.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.broadleafcommerce.core.catalog.domain.*;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.catalog.service.type.ProductBundlePricingModelType;
import org.broadleafcommerce.core.inventory.service.InventoryService;
import org.broadleafcommerce.core.inventory.service.type.InventoryType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.cart.CartUtils;
import pl.touk.widerest.api.catalog.CatalogUtils;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.ProductAttributeDto;
import pl.touk.widerest.api.catalog.dto.ProductBundleDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.dto.ProductOptionDto;
import pl.touk.widerest.api.catalog.dto.SkuDto;
import pl.touk.widerest.api.catalog.dto.SkuMediaDto;
import pl.touk.widerest.api.catalog.dto.SkuProductOptionValueDto;
import pl.touk.widerest.api.catalog.exceptions.DtoValidationException;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


@RestController
@RequestMapping(value = "/catalog/products", produces = "application/json")
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
            @ApiResponse(code = 200, message = "Successful retrieval of products list", response = ProductDto.class, responseContainer = "List")
    })
    public List<ProductDto> getAllProducts(
            @ApiParam(value = "Amount of products to be returned")
            @RequestParam(value = "limit", required = false) Integer limit,
            @ApiParam(value = "Offset which to  start returning products from")
            @RequestParam(value = "offset", required = false) Integer offset) {

        return catalogService.findAllProducts(limit != null ? limit : 0, offset != null ? offset : 0)
                .stream()
                .filter(CatalogUtils::archivedProductFilter)
                .map(dtoConverters.productEntityToDto)
                .collect(Collectors.toList());
    }

    /* GET /products/bundles */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/bundles", method = RequestMethod.GET)
    @ApiOperation(
            value = "List all bundles",
            notes = "Gets a list of all available product bundles in the catalog",
            response = ProductBundleDto.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of products list", response = ProductBundleDto.class, responseContainer = "List")
    })
    public List<ProductDto> getAllBundlesProducts() {
        return catalogService.findAllProducts().stream()
                .filter(CatalogUtils::archivedProductFilter)
                .filter(e -> e instanceof ProductBundle)
                .map(dtoConverters.productEntityToDto)
                .collect(Collectors.toList());
    }


    /* GET /products/bundles/{bundleId} */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/bundles/{bundleId}", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(
            value = "Get a single bundle details",
            notes = "Gets details of a single bundle specified by its ID",
            response = ProductBundleDto.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of bundle details", response = ProductBundleDto.class),
            @ApiResponse(code = 404, message = "The specified bundle does not exist")
    })
    public ProductDto readOneBundleById(
            @ApiParam(value = "ID of a specific bundle", required = true)
            @PathVariable(value = "bundleId") Long bundleId) {

        return Optional.ofNullable(catalogService.findProductById(bundleId))
                .filter(CatalogUtils::archivedProductFilter)
                .filter(e -> e instanceof ProductBundle)
                .map(dtoConverters.productEntityToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Bundle with ID: " + bundleId + " does not exist"));
    }


    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/bundles", method = RequestMethod.POST)
    @ApiOperation(
            value = "Add a new bundle",
            notes = "Adds a new bundle to the catalog. Returns an URL to the newly created " +
                    "bundle in the Location field of the HTTP response header",
            response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A new bundle successfully created"),
            @ApiResponse(code = 400, message = "Not enough data has been provided"),
            @ApiResponse(code = 409, message = "Bundle already exists or provided SKUs do not exist")
    })
    public ResponseEntity<?> createNewBundle(
            @ApiParam(value = "Description of a new bundle", required = true)
            @RequestBody ProductBundleDto productBundleDto) {


        if (hasDuplicates(productBundleDto.getName())) {
            throw new DtoValidationException("Provided bundle already exists");
//            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        CatalogUtils.validateSkuPrices(productBundleDto.getDefaultSku());

        Product product = new ProductBundleImpl();

        product.setDefaultSku(dtoConverters.skuDtoToEntity.apply(productBundleDto.getDefaultSku()));

        product = CatalogUtils.updateProductEntityFromDto(product, productBundleDto);

        ((ProductBundle) product).setPricingModel(ProductBundlePricingModelType.BUNDLE);

        product.getDefaultSku().setProduct(product);

        final ProductBundle p = (ProductBundle) product;

        ((ProductBundle) product).setSkuBundleItems(productBundleDto.getBundleItems().stream()
                .filter(x -> catalogService.findSkuById(x.getSkuId()) != null)
                .map(dtoConverters.bundleItemDtoToSkuBundleItem)
                .map(e -> {
                    e.setBundle(p);
                    return e;
                })
                .collect(toList()));


        if (((ProductBundle) product).getSkuBundleItems().size() != productBundleDto.getBundleItems().size()) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }


        product = catalogService.saveProduct(product);


        HttpHeaders responseHeader = new HttpHeaders();

        responseHeader.setLocation(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(product.getId())
                .toUri());

        return new ResponseEntity<>(responseHeader, HttpStatus.CREATED);


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
    public ResponseEntity<?> addOneProduct(
            @ApiParam(value = "Description of a new product", required = true)
            @RequestBody ProductDto productDto) {

        /* (mst) every new Product has to have at least a DefaultSKU and a name */
        if (productDto.getName() == null || productDto.getName().isEmpty() || productDto.getDefaultSku() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if (!productDto.getName().equals(productDto.getDefaultSku().getName())) {
            productDto.getDefaultSku().setName(productDto.getName());
        }

        if (hasDuplicates(productDto.getName())) {
            throw new DtoValidationException("Provided bundle already exists");
        }

        CatalogUtils.validateSkuPrices(productDto.getDefaultSku());

        Product newProduct = dtoConverters.productDtoToEntity.apply(productDto);

        newProduct.getDefaultSku().setInventoryType(InventoryType.ALWAYS_AVAILABLE);

        //newProduct.getDefaultSku().setInventoryType(InventoryType.getInstance(productDto.getDefaultSku().getAvailability()));


        /* (mst) if there is a default category set, try to find it and connect it with the product.
                 Otherwise just ignore it.

         */
        if (productDto.getCategoryName() != null && !productDto.getCategoryName().isEmpty()) {
            Optional<Category> categoryToReference = catalogService.findCategoriesByName(productDto.getCategoryName()).stream()
                    .filter(CatalogUtils::archivedCategoryFilter)
                    .findAny();

            if (categoryToReference.isPresent()) {
                newProduct.setCategory(categoryToReference.get());
            }
        }

        final Product productParam = newProduct;

        newProduct.setProductOptionXrefs(
                Optional.ofNullable(productDto.getOptions())
                        .filter(e -> !e.isEmpty())
                        .map(List::stream)
                        .map(e -> e.map(x -> generateProductXref(x, productParam)))
                        .map(e -> e.collect(Collectors.toList()))
                        .orElse(newProduct.getProductOptionXrefs())
        );

        newProduct = catalogService.saveProduct(newProduct);


        if (productDto.getSkus() != null && !productDto.getSkus().isEmpty()) {

            List<Sku> savedSkus = new ArrayList<>();
            savedSkus.addAll(newProduct.getAllSkus());

            for (SkuDto skuDto : productDto.getSkus()) {

                try {
                    CatalogUtils.validateSkuPrices(skuDto);
                } catch(DtoValidationException ex) {
                    /* (mst) Since this is not a default SKU, we'll just skip it */
                    continue;
                }

                Sku s = dtoConverters.skuDtoToEntity.apply(skuDto);

                final Sku skuParam = s;
                final Product p = newProduct;

                if (skuDto.getSkuProductOptionValues() != null && !skuDto.getSkuProductOptionValues().isEmpty()) {
                    s.setProductOptionValueXrefs(
                            skuDto.getSkuProductOptionValues().stream()
                                    .map(e -> generateXref(e, skuParam, p))
                                    .collect(Collectors.toSet())
                    );
                }

                s.setProduct(newProduct);
                s = catalogService.saveSku(s);
                savedSkus.add(s);
            }

            newProduct.setAdditionalSkus(savedSkus);
            newProduct = catalogService.saveProduct(newProduct);
        }


        HttpHeaders responseHeader = new HttpHeaders();

        responseHeader.setLocation(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newProduct.getId())
                .toUri());

        return new ResponseEntity<>(responseHeader, HttpStatus.CREATED);
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
    @RequestMapping(value = "/{productId}", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(
            value = "Get a single product details",
            notes = "Gets details of a single product specified by its ID",
            response = ProductDto.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of product details", response = ProductDto.class),
            @ApiResponse(code = 404, message = "The specified product does not exist or is marked as archived")
    })
    public ProductDto readOneProductById(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId) {

        return Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .map(dtoConverters.productEntityToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));
    }

    /* PUT /products/{id} */
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
    public ResponseEntity<?> updateOneProduct(
            @ApiParam(value = "ID of a specific category", required = true)
            @PathVariable(value = "productId") Long productId,
            @ApiParam(value = "(Full) Description of an updated product", required = true)
            @RequestBody ProductDto productDto) {

        if (productDto.getName() == null || productDto.getName().isEmpty() || productDto.getDefaultSku() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if (hasDuplicates(productDto.getName())) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        CatalogUtils.validateSkuPrices(productDto.getDefaultSku());

        if (!productDto.getName().equals(productDto.getDefaultSku().getName())) {
            productDto.getDefaultSku().setName(productDto.getName());
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
    public ResponseEntity<?> partialUpdateOneProduct(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId,
            @ApiParam(value = "(Partial) Description of an updated product", required = true)
            @RequestBody ProductDto productDto) {
        throw new ResourceNotFoundException("Implement me, dammit!");
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
    public ResponseEntity<?> removeOneProductById(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId) {

        //product.getAdditionalSkus().stream().forEach(catalogService::removeSku);

        Product product = Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .map(e -> {
                    catalogService.removeProduct(e);
                    return e;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Cannot delete product with ID: " + productId + ". Product does not exist"));


        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /* ---------------------------- Product Attributes ENDPOINTS ---------------------------- */

    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{productId}/attributes", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get a single product attributes",
            notes = "Gets a map of attributes of a single product specified by its ID",
            response = Map.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of product attributes", response = Map.class),
            @ApiResponse(code = 404, message = "The specified product does not exist or is marked as archived")
    })
    public Map<String, String> getProductByIdAttributes(
            @ApiParam(value = "ID of a specific product", required = true)
                @PathVariable(value = "productId") Long productId) {

        final Product product = Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));

        return Optional.ofNullable(product.getProductAttributes())
                .orElse(Collections.emptyMap())
                .entrySet().stream()
                    .collect(toMap(Map.Entry::getKey, e -> e.getValue().toString()));
    }


    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}/attributes", method = RequestMethod.POST)
    @ApiOperation(
            value = "Add/Update product attribute",
            notes = "Adds a new product attribute or updates an existing one in the catalog. If an attribute with that" +
                    "name already exists, it overwrites its value",
            response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A new attribute successfully created"),
            @ApiResponse(code = 400, message = "Not enough data has been provided")
    })
    public ResponseEntity<?> addOneProductByIdAttribute(
            @ApiParam(value = "ID of a specific product", required = true)
                @PathVariable(value = "productId") Long productId,
            @ApiParam(value = "Description of a new attribute", required = true)
                @RequestBody ProductAttributeDto productAttributeDto) {


        if(productAttributeDto.getAttributeName() == null || productAttributeDto.getAttributeValue() == null ||
                productAttributeDto.getAttributeName().isEmpty() || productAttributeDto.getAttributeValue().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        final Product product = Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));

        final ProductAttribute productAttribute = new ProductAttributeImpl();
        productAttribute.setProduct(product);
        productAttribute.setName(productAttributeDto.getAttributeName());
        productAttribute.setValue(productAttributeDto.getAttributeValue());

        product.getProductAttributes().put(productAttributeDto.getAttributeName(), productAttribute);

        catalogService.saveProduct(product);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}/attributes/{attributeName}", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Delete a product attribute",
            notes = "Removes a product attribute associated with an existing product",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 204, message = "Successful removal of the specified attribute"),
            @ApiResponse(code = 404, message = "The specified product or attribute does not exist")
    })
    public ResponseEntity<?> removeOneProductByIdAttribute(
            @ApiParam(value = "ID of a specific product", required = true)
                @PathVariable(value = "productId") Long productId,
            @ApiParam(value = "Name of the attribute", required = true)
                @PathVariable(value = "attributeName") String attributeName){

        final Product product = Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));


        Optional.ofNullable(product.getProductAttributes().get(attributeName))
                .orElseThrow(() -> new ResourceNotFoundException("Attribute of name: " + attributeName + " does not exist or is not related to product with ID: " + productId));

        product.getProductAttributes().remove(attributeName);
        catalogService.saveProduct(product);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }



    /* ---------------------------- Product Attributes ENDPOINTS ---------------------------- */


    /* ---------------------------- CATEGORIES ENDPOINTS ---------------------------- */

    /* GET /products/{id}/categories */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{productId}/categories", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(
            value = "Get product's categories",
            notes = "Gets a list of all categories belonging to a specified product",
            response = CategoryDto.class,
            responseContainer = "List")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of product's categories", responseContainer = "List"),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public List<CategoryDto> readCategoriesByProduct(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId) {

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
    public Long getCategoriesByProductCount(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId) {

        return Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllParentCategoryXrefs().stream()
                .map(CategoryProductXref::getCategory)
                .filter(CatalogUtils::archivedCategoryFilter)
                .count();
    }
    /* ---------------------------- CATEGORIES ENDPOINTS ---------------------------- */

    /* ---------------------------- SKUs ENDPOINTS ---------------------------- */


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
            @ApiResponse(code = 200, message = "Successful retrieval of all available SKUs", responseContainer = "List"),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public List<SkuDto> readSkusByProduct(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId) {

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
            @ApiResponse(code = 400, message = "Not enough data has been provided"),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public ResponseEntity<?> saveOneSkuByProduct(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId,
            @ApiParam(value = "Description of a new SKU", required = true)
            @RequestBody SkuDto skuDto) {


        if (skuDto.getName() == null || skuDto.getName().isEmpty() || skuDto.getQuantityAvailable() == null
                || skuDto.getActiveStartDate() == null || skuDto.getSalePrice() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        CatalogUtils.validateSkuPrices(skuDto);

        Product product = Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));

        /* (mst) Basically, when you're adding a new SKU to a product which has options, you have to
                provide them.
         */
        if (product.getProductOptionXrefs() != null && !product.getProductOptionXrefs().isEmpty()) {
            if (skuDto.getSkuProductOptionValues() == null || skuDto.getSkuProductOptionValues().isEmpty() ||
                    skuDto.getSkuProductOptionValues().size() != product.getProductOptionXrefs().size()) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }

        Sku newSkuEntity = dtoConverters.skuDtoToEntity.apply(skuDto);
        newSkuEntity.setProduct(product);


        /* (mst) TODO: Merge with SKU with the same Product Options set if it already exists in catalog */
        final Sku skuParam = newSkuEntity;

        if (skuDto.getSkuProductOptionValues() != null && !skuDto.getSkuProductOptionValues().isEmpty()) {
            newSkuEntity.setProductOptionValueXrefs(
                    skuDto.getSkuProductOptionValues().stream()
                            .map(e -> generateXref(e, skuParam, product))
                            .collect(Collectors.toSet())
            );
        }

        newSkuEntity = catalogService.saveSku(newSkuEntity);

        List<Sku> allProductsSkus = new ArrayList<>();
        allProductsSkus.addAll(product.getAllSkus());
        allProductsSkus.add(newSkuEntity);

        //product.getAllSkus().add(newSkuEntity);
        product.setAdditionalSkus(allProductsSkus);
        catalogService.saveProduct(product);

        HttpHeaders responseHeader = new HttpHeaders();

        responseHeader.setLocation(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/products/{productId}/skus/{skuId}")
                .buildAndExpand(productId, newSkuEntity.getId())
                .toUri());

        return new ResponseEntity<>(responseHeader, HttpStatus.CREATED);
    }

    /* GET /products/{productId}/skus/{skuId} */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{productId}/skus/{skuId}", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(
            value = "Get a single SKU details",
            notes = "Gets details of a single SKU, specified by its ID",
            response = SkuDto.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of SKU details"),
            @ApiResponse(code = 404, message = "The specified SKU or product does not exist")
    })
    public SkuDto getSkuById(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
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
    @RequestMapping(value = "/{productId}/skus/default", method = RequestMethod.GET, produces = "application/json")
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
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId) {

        return Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .map(Product::getDefaultSku)
                .map(dtoConverters.skuEntityToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));
    }

    /* PUT /products/{productId}/skus/default */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}/skus/default", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Update default SKU",
            notes = "Updates an existing default SKU with new details",
            response = SkuDto.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successful replace of default SKU details"),
            @ApiResponse(code = 404, message = "The specified Product does not exist"),
            @ApiResponse(code = 409, message = "Sku validation error")
    })
    public ResponseEntity<?> changeDefaultSkuByProductId(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId,
            @ApiParam(value = "Description of a new default SKU", required = true)
            @RequestBody SkuDto defaultSkuDto) {

        CatalogUtils.validateSkuPrices(defaultSkuDto);

        Product product = Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));

        Sku defaultSKU = CatalogUtils.updateSkuEntityFromDto(product.getDefaultSku(), defaultSkuDto);

        defaultSKU.setCurrency(dtoConverters.currencyCodeToBLEntity.apply(defaultSkuDto.getCurrencyCode()));

        //defaultSKU.setProduct(product);
        defaultSKU = catalogService.saveSku(defaultSKU);

        //product.setDefaultSku(newSkuEntity);
        //catalogService.saveProduct(product);


        HttpHeaders responseHeader = new HttpHeaders();

        responseHeader.setLocation(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/products/{productId}/skus/{skuId}")
                .buildAndExpand(productId, defaultSKU.getId())
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
    public ResponseEntity<Long> getSkusCountByProductId(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId) {


        final long skusCount = Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllSkus().stream()
                .count();

        return new ResponseEntity<>(skusCount, HttpStatus.OK);
    }

    /* GET /products/{productId}/skus/{skuId}/quantity */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{productId}/skus/{skuId}/quantity", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get SKU's quantity",
            notes = "Gets a quantity of all available SKUs",
            response = Integer.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of SKU's quantity"),
            @ApiResponse(code = 404, message = "The specified SKU or product does not exist")
    })
    public ResponseEntity<Integer> getSkuByIdQuantity(
            @ApiParam(value = "ID of a specific product", required = true)
                @PathVariable(value = "productId") Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
                @PathVariable(value = "skuId") Long skuId) {


        final int skuQuantity = Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllSkus().stream()
                    .filter(x -> x.getId().longValue() == skuId)
                    .findAny()
                    .orElseThrow(() -> new ResourceNotFoundException("SKU with ID: " + skuId + " does not exist or is not related to product with ID: " + productId))
                    .getQuantityAvailable();

        return new ResponseEntity<>(skuQuantity, HttpStatus.OK);
    }

    /* PUT /products/{productId}/skus/{skuId}/quantity */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}/skus/{skuId}/quantity", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Update SKU's quantity",
            notes = "Update a quantity of the specified SKUs",
            response = Void.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful update of SKU's quantity"),
            @ApiResponse(code = 404, message = "The specified SKU or product does not exist")
    })
    public ResponseEntity<?> updateSkuByIdQuantity(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") Long skuId,
            @ApiParam(value = "Quantity of a specific SKU")
            @RequestBody int quantity)
    {
         /* TODO: (mst) Inventory Service??? */

        Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllSkus().stream()
                .filter(x -> x.getId().longValue() == skuId)
                .findAny()
                .map(e -> {
                    e.setQuantityAvailable(quantity);
                    return e;
                })
                .map(catalogService::saveSku)
                .orElseThrow(() -> new ResourceNotFoundException("SKU with ID: " + skuId + " does not exist or is not related to product with ID: " + productId));

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /* PUT /products/{productId}/skus/{skuId}/availability */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}/skus/{skuId}/availability", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Update SKU's availability",
            notes = "Update an availability of the specified SKUs",
            response = Void.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful update of SKU's availability"),
            @ApiResponse(code = 404, message = "The specified SKU or product does not exist")
    })
    public ResponseEntity<?> updateSkuByIdAvailability(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") Long skuId,
            @ApiParam(value = "Inventory type: ALWAYS_AVAILABLE, UNAVAILABLE, CHECK_QUANTITY")
            @RequestBody String availability) {


        Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllSkus().stream()
                .filter(x -> x.getId().longValue() == skuId)
                .findAny()
                .map(e -> {
                    e.setInventoryType(Optional.ofNullable(InventoryType.getInstance(availability))
                            .orElseThrow(() -> new ResourceNotFoundException("The specified Inventory Type does not exist")));
                    return e;
                })
                .map(catalogService::saveSku)
                .orElseThrow(() -> new ResourceNotFoundException("SKU with ID: " + skuId + " does not exist or is not related to product with ID: " + productId));

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /* GET /products/{productId}/skus/{skuId}/availability */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{productId}/skus/{skuId}/availability", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get SKU's availability",
            notes = "Gets an availability of the specified SKUs",
            response = String.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of SKU's availability"),
            @ApiResponse(code = 404, message = "The specified SKU or product does not exist")
    })
    public ResponseEntity<String> getSkuByIdAvailability(
            @ApiParam(value = "ID of a specific product", required = true)
                @PathVariable(value = "productId") Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
                @PathVariable(value = "skuId") Long skuId) {

        final Sku sku = Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllSkus().stream()
                .filter(x -> x.getId().longValue() == skuId)
                .findAny()
                .orElseThrow(() -> new ResourceNotFoundException("SKU with ID: " + skuId + " does not exist or is not related to product with ID: " + productId));


        final String skuAvailability = Optional.ofNullable(sku.getInventoryType())
                .map(InventoryType::getType)
                .orElse(CatalogUtils.EMPTY_STRING);

        return new ResponseEntity<>(skuAvailability, HttpStatus.OK);

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
            @ApiResponse(code = 404, message = "The specified SKU or product does not exist"),
            @ApiResponse(code = 409, message = "Cannot delete Default SKU. If you need to change it, use PUT endpoint")
    })
    public ResponseEntity<?> deleteOneSkuById(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") Long skuId) {

        Product product = Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));

        if (product.getDefaultSku().getId().longValue() == skuId) {
            return new ResponseEntity(HttpStatus.CONFLICT);
        }

        product.getAllSkus().stream()
                .filter(x -> x.getId().longValue() == skuId)
                .findFirst()
                .map(e -> {
                    catalogService.removeSku(e);
                    return e;
                })
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cannot delete SKU with ID: " + skuId + ". SKU is not related to product with ID: " + productId + " or does not exist"
                ));


        product.setAdditionalSkus(
                product.getAllSkus().stream()
                        .filter(x -> x.getId().longValue() != skuId)
                        .collect(Collectors.toList())
        );

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
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") Long skuId,
            @ApiParam(value = "(Full) Description of an updated SKU", required = true)
            @RequestBody SkuDto skuDto) {

        if (skuDto.getName() == null || skuDto.getName().isEmpty() || skuDto.getSalePrice() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        CatalogUtils.validateSkuPrices(skuDto);

        Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllSkus().stream()
                .filter(x -> x.getId().longValue() == skuId)
                .findFirst()
                .map(e -> CatalogUtils.updateSkuEntityFromDto(e, skuDto))
                .map(e -> {
                    e.setCurrency(dtoConverters.currencyCodeToBLEntity.apply(skuDto.getCurrencyCode()));
                    return e;
                })
                .map(catalogService::saveSku)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cannot update SKU with ID: " + skuId + ". SKU is not related to product with ID: " + productId + " or does not exist"
                ));

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
            @ApiResponse(code = 404, message = "The specified product or SKU does not exist"),
            @ApiResponse(code = 409, message = "Sku valdation error")
    })
    public ResponseEntity<?> partialUpdateOneSkuByProductId(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") Long skuId,
            @ApiParam(value = "(Partial) Description of an updated SKU", required = true)
            @RequestBody SkuDto skuDto) {

        CatalogUtils.validateSkuPrices(skuDto);

        Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllSkus().stream()
                .filter(x -> x.getId().longValue() == skuId)
                .findFirst()
                .map(e -> CatalogUtils.partialUpdateSkuEntityFromDto(e, skuDto))
                .map(e -> {
                    if (skuDto.getCurrencyCode() != null)
                        e.setCurrency(dtoConverters.currencyCodeToBLEntity.apply(skuDto.getCurrencyCode()));
                    return e;
                })
                .map(catalogService::saveSku)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cannot update SKU with ID: " + skuId + ". SKU is not related to product with ID: " + productId + " or does not exist"
                ));

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /* ---------------------------- SKUs ENDPOINTS ---------------------------- */

    /* ---------------------------- MEDIA ENDPOINTS ---------------------------- */

    /* GET /products/{productId}/skus/{skuId}/media */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{productId}/skus/{skuId}/media", method = RequestMethod.GET)
    @ApiOperation(
            value = "List all SKU's media",
            notes = "Gets a list of all medias belonging to a specified SKU",
            response = SkuMediaDto.class,
            responseContainer = "List"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of media details", responseContainer = "List"),
            @ApiResponse(code = 404, message = "The specified SKU or product does not exist")
    })
    public List<SkuMediaDto> getMediaBySkuId(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") Long skuId) {


        return Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllSkus().stream()
                .filter(x -> x.getId().longValue() == skuId)
                .findAny()
                .orElseThrow(() -> new ResourceNotFoundException("SKU with ID: " + skuId + " does not exist or is not related to product with ID: " + productId))
                .getSkuMediaXref().entrySet().stream()
                .map(Map.Entry::getValue)
                .map(DtoConverters.skuMediaXrefToDto)
                .collect(Collectors.toList());
    }

    /* GET /products/{productId}/skus/{skuId}/media/{mediaId} */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{productId}/skus/{skuId}/media/{mediaId}", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(
            value = "Get a single media details",
            notes = "Gets details of a particular media belonging to a specified SKU",
            response = SkuMediaDto.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of media details"),
            @ApiResponse(code = 404, message = "The specified SKU or product does not exist")
    })
    public SkuMediaDto getMediaByIdForSku(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") Long skuId,
            @ApiParam(value = "ID of a specific media", required = true)
            @PathVariable(value = "mediaId") Long mediaId) {

        /* (mst) Here is the deal: if the specified SKU does not contain any medias
         *       BL's service will return medias associated with Default SKU instead.,
         */


        return Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllSkus().stream()
                .filter(x -> x.getId().longValue() == skuId)
                .findAny()
                .orElseThrow(() -> new ResourceNotFoundException("SKU with ID: " + skuId + " does not exist or is not related to product with ID: " + productId))
                .getSkuMediaXref().entrySet().stream()
                .map(Map.Entry::getValue)
                .filter(x -> x.getMedia().getId().longValue() == mediaId)
                .map(DtoConverters.skuMediaXrefToDto)
                .findAny()
                .orElseThrow(() -> new ResourceNotFoundException("Media with ID: " + mediaId + " does not exist or is not related to SKU with ID: " + skuId + " of product with ID: " + productId));


    }

    /* DELETE /products/{productId}/skus/{id}/media/{mediaId} */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}/skus/{skuId}/media/{mediaId}", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Delete an existing media",
            notes = "Removes a specific media related to the specified SKU",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 204, message = "Successful removal of the specified media"),
            @ApiResponse(code = 404, message = "The specified media, SKU or product does not exist")
    })
    public ResponseEntity<?> deleteOneMediaForSkuById(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") Long skuId,
            @ApiParam(value = "ID of a specific media", required = true)
            @PathVariable(value = "mediaId") Long mediaId) {

        Sku mediaSkuEntity = Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllSkus().stream()
                .filter(x -> x.getId().longValue() == skuId)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SKU with ID: " + skuId + " does not exist or is not related to product with ID: " + productId
                ));


        long currentSkuMediaSize = mediaSkuEntity.getSkuMediaXref().size();


        Map<String, SkuMediaXref> newSkuMediaXref = mediaSkuEntity.getSkuMediaXref().entrySet().stream()
                .filter(x -> x.getValue().getMedia().getId().longValue() != mediaId)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (currentSkuMediaSize == newSkuMediaXref.size()) {
            throw new ResourceNotFoundException("Media with ID: " + mediaId + " does not exist or is not related to SKU with ID: " + skuId);
        }

        mediaSkuEntity.getSkuMediaXref().clear();
        mediaSkuEntity.getSkuMediaXref().putAll(newSkuMediaXref);

        catalogService.saveSku(mediaSkuEntity);

        /* TODO: (mst) This might be a bit incomplete since we do nothing with the specified media... */

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    /* POST /{productId}/skus/{skuId}/media */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}/skus/{skuId}/media", method = RequestMethod.POST)
    @ApiOperation(
            value = "Add media to the product",
            notes = "Adds a new media to the existing SKU",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 201, message = "Specified media successfully added"),
            @ApiResponse(code = 400, message = "Not enough data has been provided"),
            @ApiResponse(code = 404, message = "The specified product does not exist"),
            @ApiResponse(code = 409, message = "Media with that key already exists")
    })
    public ResponseEntity<?> saveOneMediaForSku(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") Long skuId,
            @ApiParam(value = "Description of a new media")
            @RequestBody SkuMediaDto skuMediaDto) {

        List<String> allowableKeys = Arrays.asList("primary", "alt1", "alt2", "alt3", "alt4", "alt5", "alt6", "alt7", "alt8", "alt9");

        if (skuMediaDto.getKey() == null || !allowableKeys.contains(skuMediaDto.getKey()) || skuMediaDto.getUrl() == null || skuMediaDto.getUrl().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Sku mediaSkuEntity = Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllSkus().stream()
                .filter(x -> x.getId().longValue() == skuId)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("SKU with ID: " + skuId + " does not exist or is not related to product with ID: " + productId));

        if (mediaSkuEntity.getSkuMediaXref().get(skuMediaDto.getKey()) != null) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        SkuMediaXref newSkuMediaXref = DtoConverters.skuMediaDtoToXref.apply(skuMediaDto);

        newSkuMediaXref.setSku(mediaSkuEntity);
        newSkuMediaXref.setKey(skuMediaDto.getKey());

        mediaSkuEntity.getSkuMediaXref().put(skuMediaDto.getKey(), newSkuMediaXref);

        /* (mst) Here is the deal: if the specified SKU does not contain any medias,
         *       BL's service will return medias associated with Default SKU instead. (from GET endpoint)
         *
         *       Therefore, when we add a media to a SKU, that does not contain any, all those
         *       previously visible (eg: through GET endpoints) medias WILL "disappear".
         */

        Sku alreadySavedSku = catalogService.saveSku(mediaSkuEntity);

        HttpHeaders responseHeader = new HttpHeaders();

        responseHeader.setLocation(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/products/{productId}/skus/{skuId}/media/{mediaId}")
                .buildAndExpand(
                        productId,
                        skuId,
                        alreadySavedSku.getSkuMediaXref().get(newSkuMediaXref.getKey()).getMedia().getId())
                .toUri());

        return new ResponseEntity<>(responseHeader, HttpStatus.CREATED);
    }

    /* PUT /{productId}/skus/{skuId}/media/{mediaId} */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}/skus/{skuId}/media/{mediaId}", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Update an existing media",
            notes = "Updates an existing media with new details. If the media does not exist, it does NOT create it!",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful update of the specified media"),
            @ApiResponse(code = 400, message = "Not enough data has been provided"),
            @ApiResponse(code = 404, message = "The specified product or SKU does not exist"),
    })
    public ResponseEntity<?> updateOneMediaForSkuById(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") Long skuId,
            @ApiParam(value = "ID of a specific media", required = true)
            @PathVariable(value = "mediaId") Long mediaId,
            @ApiParam(value = "(Full) Description of an updated media")
            @RequestBody SkuMediaDto skuMediaDto) {

        if (skuMediaDto.getUrl() == null || skuMediaDto.getUrl().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Sku skuMediaEntity = Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllSkus().stream()
                .filter(x -> x.getId().longValue() == skuId)
                .findAny()
                .orElseThrow(() -> new ResourceNotFoundException("SKU with ID: " + skuId + " does not exist or is not related to product with ID: " + productId));

        /* (mst) Do NOT use skuMediaDto.getKey() EVER, even if it is available! */

        SkuMediaXref skuMediaXref = skuMediaEntity.getSkuMediaXref().entrySet().stream()
                .filter(x -> x.getValue().getMedia().getId().longValue() == mediaId)
                .map(Map.Entry::getValue)
                .findAny()
                .orElseThrow(() -> new ResourceNotFoundException("Media with ID: " + mediaId + " does not exist or is not related to SKU with ID: " + skuId));

        CatalogUtils.updateMediaEntityFromDto(skuMediaXref.getMedia(), skuMediaDto);

        catalogService.saveSku(skuMediaEntity);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /* PATCH /products/{productId}/skus/{skuId}/media/{mediaId} */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}/skus/{skuId}/media/{mediaId}", method = RequestMethod.PATCH)
    @ApiOperation(
            value = "Partially update an existing media",
            notes = "Partially updates an existing media with new details. It does not follow the format specified in RFC yet though",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful update of the specified media"),
            @ApiResponse(code = 404, message = "The specified product or SKU does not exist")
    })
    public ResponseEntity<?> partialUpdateOneMediaForSkuById(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") Long skuId,
            @ApiParam(value = "ID of a specific media", required = true)
            @PathVariable(value = "mediaId") Long mediaId,
            @ApiParam(value = "(Partial) Description of an updated media")
            @RequestBody SkuMediaDto skuMediaDto) {

        Sku skuMediaEntity = Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllSkus().stream()
                .filter(x -> x.getId().longValue() == skuId)
                .findAny()
                .orElseThrow(() -> new ResourceNotFoundException("SKU with ID: " + skuId + " does not exist or is not related to product with ID: " + productId));

        /* (mst) Do NOT use skuMediaDto.getKey() EVER, even if it is available! */

        SkuMediaXref skuMediaXref = skuMediaEntity.getSkuMediaXref().entrySet().stream()
                .filter(x -> x.getValue().getMedia().getId().longValue() == mediaId)
                .map(Map.Entry::getValue)
                .findAny()
                .orElseThrow(() -> new ResourceNotFoundException("Media with ID: " + mediaId + " does not exist or is not related to SKU with ID: " + skuId));

        CatalogUtils.partialUpdateMediaEntityFromDto(skuMediaXref.getMedia(), skuMediaDto);

        catalogService.saveSku(skuMediaEntity);

        return new ResponseEntity<>(HttpStatus.OK);
    }

/* ---------------------------- MEDIA ENDPOINTS ---------------------------- */

    private SkuProductOptionValueXref generateXref(SkuProductOptionValueDto skuProductOption, Sku sku, Product product) {
        ProductOption currentProductOption = Optional.ofNullable(dtoConverters.getProductOptionByNameForProduct(
                skuProductOption.getAttributeName(),
                product))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product option: " + skuProductOption.getAttributeName() + " does not exist in product with ID: " + product.getId()
                ));

        ProductOptionValue productOptionValue = Optional.ofNullable(dtoConverters.getProductOptionValueByNameForProduct(
                currentProductOption,
                skuProductOption.getAttributeValue()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "'" + skuProductOption.getAttributeValue() + "'" + " is not an allowed value for option: " +
                                skuProductOption.getAttributeName() + " for product with ID: " + product.getId()
                ));


        return new SkuProductOptionValueXrefImpl(sku, productOptionValue);
    }

    private ProductOptionXref generateProductXref(ProductOptionDto productOptionDto, Product product) {
        ProductOption p = catalogService.saveProductOption(DtoConverters.productOptionDtoToEntity.apply(productOptionDto));
        p.getAllowedValues().forEach(x -> x.setProductOption(p));

        ProductOptionXref productOptionXref = new ProductOptionXrefImpl();
        productOptionXref.setProduct(product);
        productOptionXref.setProductOption(p);
        return productOptionXref;
    }

    // ---------------------------- UTILS ---------------------------- //

    private Boolean hasDuplicates(String productName) {
        return catalogService.findProductsByName(productName).stream()
                .filter(CatalogUtils::archivedProductFilter)
                .count() > 0;
    }
}