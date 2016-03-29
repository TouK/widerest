package pl.touk.widerest.api.products;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang.StringUtils;
import org.broadleafcommerce.common.exception.ServiceException;
import org.broadleafcommerce.common.media.domain.Media;
import org.broadleafcommerce.common.security.service.ExploitProtectionService;
import org.broadleafcommerce.common.service.GenericEntityService;
import org.broadleafcommerce.common.util.BLCSystemProperty;
import org.broadleafcommerce.core.catalog.domain.CategoryProductXref;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.domain.ProductBundle;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.catalog.domain.SkuBundleItem;
import org.broadleafcommerce.core.catalog.domain.SkuMediaXref;
import org.broadleafcommerce.core.catalog.domain.SkuMediaXrefImpl;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.inventory.service.InventoryService;
import org.broadleafcommerce.core.search.domain.SearchCriteria;
import org.broadleafcommerce.core.search.domain.SearchResult;
import org.broadleafcommerce.core.search.service.SearchService;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
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
import pl.touk.widerest.api.categories.CategoryConverter;
import pl.touk.widerest.api.categories.CategoryDto;
import pl.touk.widerest.api.common.CatalogUtils;
import pl.touk.widerest.api.common.MediaConverter;
import pl.touk.widerest.api.common.MediaDto;
import pl.touk.widerest.api.common.ResourceNotFoundException;
import pl.touk.widerest.security.oauth2.ResourceServerConfig;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@RequestMapping(value = ResourceServerConfig.API_PATH + "/products", produces = { MediaTypes.HAL_JSON_VALUE })
@Api(value = "products", description = "Product catalog endpoint", produces = MediaTypes.HAL_JSON_VALUE)
public class ProductController {

    @Resource(name = "blCatalogService")
    protected CatalogService catalogService;

    @Resource(name = "blInventoryService")
    protected InventoryService inventoryService;

    @Resource(name = "blSearchService")
    protected SearchService searchService;

    /* (mst) For filtering input search query */
    @Resource(name = "blExploitProtectionService")
    protected ExploitProtectionService exploitProtectionService;

    @Resource(name = "blGenericEntityService")
    protected GenericEntityService genericEntityService;

    @Resource
    protected CategoryConverter categoryConverter;

    @Resource
    protected ProductConverter productConverter;

    @Resource
    protected MediaConverter mediaConverter;

    /* GET /products */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(
            value = "List all products",
            notes = "Gets a list of all available products in the catalog",
            response = ProductDto.class,
            responseContainer = "List"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of products list", response = ProductDto.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Invalid query text")
    })
    public ResponseEntity<Resources<ProductDto>> getAllProducts(
            @ApiParam(value = "Amount of products to be returned")
                @RequestParam(value = "limit", required = false, defaultValue = "100") final Integer limit,
            @ApiParam(value = "Offset which to  start returning products from")
                @RequestParam(value = "offset", required = false, defaultValue = "0") final Integer offset,
            @ApiParam(value = "Search query text")
                @RequestParam(value = "q", required = false) final String q,
            @ApiParam(value = "Amount of items per page (applies only to searching)")
                @RequestParam(value = "pageSize", defaultValue = "15") final Integer pageSize,
            @ApiParam(value = "Page number to return (applies only to searching)")
                @RequestParam(value = "page", defaultValue = "1") final Integer page
    ) throws ServiceException {

        List<Product> productsToReturn;

        if(StringUtils.isNotEmpty(q)) {

            String cleanedUpQuery;

            cleanedUpQuery = StringUtils.trim(q);
            cleanedUpQuery = exploitProtectionService.cleanString(cleanedUpQuery);

            final SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.setPage((page <= 0) ? 1 : page);

            final int maxAllowedPageSize = BLCSystemProperty.resolveIntSystemProperty("web.maxPageSize");
            final int defaultPageSize = BLCSystemProperty.resolveIntSystemProperty("web.defaultPageSize");
            searchCriteria.setPageSize((pageSize <= 0) ? defaultPageSize : Math.min(pageSize, maxAllowedPageSize));

            final Map<String, String[]> searchFilterCriteria = new HashMap<>();
            /* (nst)
                 'defaultSku.name' -> 'name'
             */
            final String[] nameFilters = { };
            searchFilterCriteria.put("name", nameFilters);

            searchCriteria.setFilterCriteria(searchFilterCriteria);


            final SearchResult searchResult = searchService.findSearchResultsByQuery(cleanedUpQuery, searchCriteria);
            productsToReturn = Optional.ofNullable(searchResult.getProducts()).orElse(Collections.emptyList());
        } else {
            productsToReturn = catalogService.findAllProducts(limit != null ? limit : 0, offset != null ? offset : 0);
        }

        return ResponseEntity.ok(
                new Resources<>(
                        productsToReturn.stream()
                                .filter(CatalogUtils.nonArchivedProduct)
                                .map(product -> productConverter.createDto(product))
                                .collect(toList()),

                        linkTo(methodOn(getClass()).getAllProducts(limit, offset, q, pageSize, page)).withSelfRel()
                )
        );
    }

    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/url", method = RequestMethod.GET, params = "url")
    @ApiOperation(
            value = "Get product by URL",
            notes = "Gets a single product details",
            response = ProductDto.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of product", response = ProductDto.class)
    })
    public ProductDto getProductByUrl(
            @ApiParam @RequestParam(value = "url", required = true) final String url) {

        return Optional.ofNullable(catalogService.findProductByURI(url))
                .filter(CatalogUtils.nonArchivedProduct)
                .map(product -> productConverter.createDto(product))
                .orElseThrow(() -> new ResourceNotFoundException("Product with URL: " + url + " does not exist"));
    }

    /* GET /products/bundles/{bundleId} */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/bundles/{bundleId}", method = RequestMethod.GET)
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
            @PathVariable(value = "bundleId") final Long bundleId) {

        return Optional.ofNullable(catalogService.findProductById(bundleId))
                .filter(CatalogUtils.nonArchivedProduct)
                .filter(e -> e instanceof ProductBundle)
                .map(product -> productConverter.createDto(product))
                .orElseThrow(() -> new ResourceNotFoundException("Bundle with ID: " + bundleId + " does not exist"));
    }


//    @Transactional
//    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
//    @RequestMapping(value = "/bundles", method = RequestMethod.POST)
//    @ApiOperation(
//            value = "Add a new bundle",
//            notes = "Adds a new bundle to the catalog. Returns an URL to the newly created " +
//                    "bundle in the Location field of the HTTP response header",
//            response = Void.class)
//    @ApiResponses(value = {
//            @ApiResponse(code = 201, message = "A new bundle successfully created"),
//            @ApiResponse(code = 400, message = "Not enough data has been provided"),
//            @ApiResponse(code = 409, message = "Bundle already exists or provided SKUs do not exist")
//    })
//    public ResponseEntity<?> createNewBundle(
//            @ApiParam(value = "Description of a new bundle", required = true)
//            @RequestBody ProductBundleDto productBundleDto) {
//
//
//        if (hasDuplicates(productBundleDto.getName())) {
//            throw new DtoValidationException("Provided bundle already exists");
//        }
//
//        CatalogUtils.validateSkuPrices(productBundleDto.getBundleSalePrice(), productBundleDto.getBundleRetailPrice());
//
//        Product product = new ProductBundleImpl();
//
//        //product.setDefaultSku(dtoConverters.skuDtoToEntity.apply(productBundleDto.getDefaultSku()));
//
//        final Sku bundleDefaultSku = new SkuImpl();
//        product.setDefaultSku(bundleDefaultSku);
//
//        // TODO:
////        product.setDefaultSku(skuConverter.createEntity(productBundleDto.getDefaultSku()));
//
//        product = productConverter.updateEntity(product, productBundleDto);
//
//        ((ProductBundle) product).setPricingModel(ProductBundlePricingModelType.BUNDLE);
//
//        product.getDefaultSku().setProduct(product);
//
//        final ProductBundle productBundle = (ProductBundle) product;
//
//        ((ProductBundle) product).setSkuBundleItems(productBundleDto.getBundleItems().stream()
//                .filter(x -> catalogService.findSkuById(x.getSkuId()) != null)
//                .map(dtoConverters.bundleItemDtoToSkuBundleItem)
//                .map(toSkuWithBundle(productBundle))
//                .collect(toList()));
//
//
//        if (((ProductBundle) product).getSkuBundleItems().size() != productBundleDto.getBundleItems().size()) {
//            return ResponseEntity.status(HttpStatus.CONFLICT).build();
//        }
//
//
//        product = catalogService.saveProduct(product);
//
//        return ResponseEntity.created(
//                ServletUriComponentsBuilder.fromCurrentRequest()
//                        .path("/{id}")
//                        .buildAndExpand(product.getId())
//                        .toUri()
//        ).build();
//
//    }

    private static Function<SkuBundleItem, SkuBundleItem> toSkuWithBundle(ProductBundle p) {
        return e -> {
            e.setBundle(p);
            return e;
        };
    }


    /* POST /products */
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
            @ApiResponse(code = 400, message = "Not enough data has been provided - validation exception"),
            @ApiResponse(code = 409, message = "Product already exists")
    })
    public ResponseEntity<?> addOneProduct(
            @ApiParam(value = "Description of a new product", required = true)
                @Valid @RequestBody final ProductDto receivedProductDto) {

        Product product = catalogService.saveProduct(
                productConverter.createEntity(receivedProductDto)
        );

        return ResponseEntity.created(
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(product.getId())
                        .toUri()
        ).build();
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
                .filter(CatalogUtils.nonArchivedProduct)
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
    public ProductDto readOneProductById(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") final Long productId) {

        return Optional.of(getProductById(productId))
                .map(product -> productConverter.createDto(product))
                .get();
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
    public void updateOneProduct(
            @ApiParam(value = "ID of a specific category", required = true)
            @PathVariable(value = "productId") final Long productId,
            @ApiParam(value = "(Full) Description of an updated product", required = true)
                @Valid @RequestBody final ProductDto productDto) {

        Optional.ofNullable(getProductById(productId))
                .ifPresent(oldProductEntity -> {
                    final Product newProductEntity = productConverter.createEntity(productDto);
                    newProductEntity.setId(productId);
                    catalogService.saveProduct(newProductEntity);
                });
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
    public void removeOneProductById(
            @ApiParam(value = "ID of a specific product", required = true)
                @PathVariable(value = "productId") final Long productId) {

        Optional.of(getProductById(productId)).ifPresent(catalogService::removeProduct);
    }

    /* ---------------------------- CATEGORIES ENDPOINTS ---------------------------- */

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
            @ApiResponse(code = 200, message = "Successful retrieval of product's categories", responseContainer = "List"),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public Resources<CategoryDto> readCategoriesByProduct(
            @ApiParam(value = "ID of a specific product", required = true)
                @PathVariable(value = "productId") final Long productId) {

        final List<CategoryDto> productCategories = getProductById(productId).getAllParentCategoryXrefs().stream()
                .map(CategoryProductXref::getCategory)
                .filter(CatalogUtils.nonArchivedCategory)
                .map(category -> categoryConverter.createDto(category, true, true))
                .collect(toList());

        return new Resources<>(
                productCategories,
                linkTo(methodOn(getClass()).readCategoriesByProduct(productId)).withSelfRel()
        );
    }

    /* ---------------------------- CATEGORIES ENDPOINTS ---------------------------- */

    /* ---------------------------- SKUs ENDPOINTS ---------------------------- */


    /* GET /products/{productId}/media */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{productId}/media", method = RequestMethod.GET)
    @ApiOperation(
            value = "List default SKU's media",
            notes = "Gets a list of all medias belonging to a specified product's default SKU",
            response = MediaDto.class,
            responseContainer = "List"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of media details", responseContainer = "List"),
            @ApiResponse(code = 404, message = "The specified SKU or product does not exist")
    })
    public Resources<MediaDto> getProductDefaultSkuMedias(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") final Long productId
    ) {

        return new Resources<>(
                getDefaultSkuForProductById(productId).getSkuMediaXref().entrySet().stream()
                    .map(Map.Entry::getValue)
                    .map(skuMediaXref -> {
                        final MediaDto mediaDto = mediaConverter.createDto(skuMediaXref.getMedia(), true, true);
                        mediaDto.add(linkTo(methodOn(ProductController.class).getProductDefaultSkuMedia(productId, skuMediaXref.getKey())).withSelfRel());
                        return mediaDto;
                    })
                    .collect(toList()),

                linkTo(methodOn(getClass()).getProductDefaultSkuMedias(productId)).withSelfRel()
        );
    }

    /* GET /products/{productId}/media/{key} */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{productId}/media/{key}", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get a single media details",
            notes = "Gets details of a particular media belonging to a specified product's default SKU",
            response = MediaDto.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of media details"),
            @ApiResponse(code = 404, message = "The specified SKU or product does not exist")
    })
    public MediaDto getProductDefaultSkuMedia(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") final Long productId,
            @ApiParam(value = "ID of a specific media", required = true)
            @PathVariable(value = "key") final String key
    ) {

        final Sku productDefaultSku = getDefaultSkuForProductById(productId);

        return Optional.ofNullable(productDefaultSku.getSkuMediaXref().get(key))
                .map(SkuMediaXref::getMedia)
                .map(media -> {
                    final MediaDto mediaDto = mediaConverter.createDto(media);
                    mediaDto.add(linkTo(methodOn(ProductController.class).getProductDefaultSkuMedia(productId, key)).withSelfRel());
                    return mediaDto;
                })
                .orElseThrow(() -> new ResourceNotFoundException("No media with key " + key + " exists for this product"));
    }

    /* DELETE /products/{productId}/media/{key} */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}/media/key", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Delete an existing media",
            notes = "Removes a specific media related to the specified SKU",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 204, message = "Successful removal of the specified media"),
            @ApiResponse(code = 404, message = "The specified media, SKU or product does not exist")
    })
    public void deleteOneMediaForSkuById(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") final Long productId,
            @ApiParam(value = "ID of a specific media", required = true)
            @PathVariable(value = "key") final String key
    ) {

        final Sku productDefaultSku = getDefaultSkuForProductById(productId);

        final SkuMediaXref skuMediaXrefToBeRemoved = Optional.ofNullable(productDefaultSku.getSkuMediaXref().remove(key))
                .orElseThrow(() -> new ResourceNotFoundException("No media with key " + key + "for this sku"));

        genericEntityService.remove(skuMediaXrefToBeRemoved);
        catalogService.saveSku(productDefaultSku);
    }

    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}/media/{key}", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Create new or update existing media",
            notes = "Updates an existing media with new details. If the media does not exist, it creates it!",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful update of the specified media"),
            @ApiResponse(code = 400, message = "Not enough data has been provided"),
            @ApiResponse(code = 404, message = "The specified product or SKU does not exist"),
    })
    public void updateOneMediaForSkuById(
            @ApiParam(value = "ID of a specific product", required = true)
                @PathVariable(value = "productId") final Long productId,
            @ApiParam(value = "ID of a specific media", required = true)
                @PathVariable(value = "key") final String key,
            @ApiParam(value = "(Full) Description of an updated media")
                @Valid @RequestBody final MediaDto mediaDto
    ) {

        final Sku productDefaultSku = getDefaultSkuForProductById(productId);

        Optional.ofNullable(productDefaultSku.getSkuMediaXref().remove(key))
                .ifPresent(genericEntityService::remove);


        final Media mediaEntity = mediaConverter.createEntity(mediaDto);

        final SkuMediaXref newSkuMediaXref = new SkuMediaXrefImpl();
        newSkuMediaXref.setMedia(mediaEntity);
        newSkuMediaXref.setSku(productDefaultSku);
        newSkuMediaXref.setKey(key);
        productDefaultSku.getSkuMediaXref().put(key, newSkuMediaXref);

        catalogService.saveSku(productDefaultSku );
    }




    private Sku getDefaultSkuForProductById(final long productId) throws ResourceNotFoundException {
        return Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils.nonArchivedProduct)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getDefaultSku();
    }

    public Product getProductById(final long productId) throws ResourceNotFoundException {
        return Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils.nonArchivedProduct)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));
    }

    private boolean hasDuplicates(final String productName) {
        return catalogService.findProductsByName(productName).stream()
                .filter(CatalogUtils.nonArchivedProduct)
                .count() > 0;
    }
}