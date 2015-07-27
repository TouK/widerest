package pl.touk.widerest.api.catalog.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.catalog.domain.CategoryProductXref;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.inventory.service.InventoryService;
import org.broadleafcommerce.core.rating.domain.RatingSummary;
import org.broadleafcommerce.core.rating.service.RatingService;
import org.broadleafcommerce.core.rating.service.type.RatingType;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import pl.touk.widerest.api.catalog.CatalogUtils;
import pl.touk.widerest.api.catalog.DtoConverters;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.dto.RatingDto;
import pl.touk.widerest.api.catalog.dto.ReviewDto;
import pl.touk.widerest.api.catalog.dto.SkuDto;
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
                .map(DtoConverters.productEntityToDto)
                .collect(Collectors.toList());
    }

    /* POST /products */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation(
            value = "Add a new product",
            notes = "Adds a new product to the catalog. Returns an URL to the newly added " +
                    "product in the Location field of the HTTP response header",
            response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A new product successfully created")
    })
    public ResponseEntity<?> saveOneProduct(@RequestBody ProductDto productDto) {

        Sku defaultSku = Optional.ofNullable(productDto.getDefaultSku())
                .map(DtoConverters.skuDtoToEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Default SKU for product not provided"));

        /* TODO: (mst) creating Product Bundles */
        
        /* what if both Product and SKU return null?! */
        //Product newProduct = catalogService.createProduct(ProductType.PRODUCT);

        Product newProduct = DtoConverters.productDtoToEntity.apply(productDto);
        /* this one is probably redundant */
        newProduct.setDefaultSku(defaultSku);

        /* TODO: (mst) what if the Category has not been provided?! */

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
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public ProductDto readOneProduct(@PathVariable(value = "productId") Long productId) {

        return Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .map(DtoConverters.productEntityToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));
    }

    /* PUT /products/{id} */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Update an existing product",
            notes = "Updates an existing product with new details",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful update of the specified product"),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public void changeOneProduct(@PathVariable(value = "productId") Long productId, @RequestBody ProductDto productDto) {
        Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .map(p -> {
                    catalogService.saveProduct(DtoConverters.productDtoToEntity.apply(productDto));
                    return p;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Cannot change product with id " + productId + ". Not Found"));
    }

    /* DELETE /products/{id} */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Delete an existing product",
            notes = "Removes an existing product from catalog",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of product details"),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public void removeOneProduct(@PathVariable(value = "productId") Long productId) {
        Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .map(e -> {
                    catalogService.removeProduct(e);
                    return e;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Cannot delete product with ID: " + productId + ". Product does not exist"));
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
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllParentCategoryXrefs().stream()
                .map(CategoryProductXref::getCategory)
                .filter(CatalogUtils::archivedCategoryFilter)
                .map(DtoConverters.categoryEntityToDto)
                .collect(Collectors.toList());

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
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllSkus().stream()
                .map(DtoConverters.skuEntityToDto)
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

        Sku newSkuEntity = DtoConverters.skuDtoToEntity.apply(skuDto);
        newSkuEntity.setProduct(product);
        newSkuEntity = catalogService.saveSku(newSkuEntity);

        // !!! (mst)Shallow copy!
        List<Sku> allProductSkus = new ArrayList<>(product.getAllSkus());
        allProductSkus.add(newSkuEntity);

        //product.getAllSkus().add(newSkuEntity);
        product.setAdditionalSkus(allProductSkus);
        catalogService.saveProduct(product);

        HttpHeaders responseHeader = new HttpHeaders();

        responseHeader.setLocation(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/products/{productId}/skus/{skuId}")
                .buildAndExpand(productId,newSkuEntity.getId())
                .toUri());

        return new ResponseEntity<>(null, responseHeader, HttpStatus.CREATED);
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
    public SkuDto getSkuById(
            @PathVariable(value = "productId") Long productId,
            @PathVariable(value = "skuId") Long skuId) {

        return Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils::archivedProductFilter)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllSkus().stream()
                .filter(x -> x.getId() == skuId)
                .findAny()
                .map(DtoConverters.skuEntityToDto)
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
                .map(DtoConverters.skuEntityToDto)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));
    }

    /* POST /products/{productId}/skus/default */
    /* (mst) Experimental */
    @Transactional
    @PreAuthorize("permitAll")
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

        Sku newSkuEntity = DtoConverters.skuDtoToEntity.apply(defaultSkuDto);
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

        return new ResponseEntity<>(null, responseHeader, HttpStatus.CREATED);
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
            notes = "Removes an existing SKU from catalog",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful removal of the specified SKU"),
            @ApiResponse(code = 404, message = "The specified SKU or product does not exist")
    })
    public void deleteOneSkuById(
            @PathVariable(value = "productId") Long productId,
            @PathVariable(value = "skuId") Long skuId) {


        Product product = Optional.ofNullable(catalogService.findProductById(productId))
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"));

        if(product.getDefaultSku().getId() == skuId) {
            throw new RuntimeException("Cannot delete SKU with ID: " + skuId + " of product with ID: " + productId + " - default SKU");
        }

        Optional<Sku> skuToDelete = product.getAllSkus().stream()
                .filter(x -> x.getId().longValue() == skuId)
                .findFirst();

        if(!skuToDelete.isPresent()) {
            throw new ResourceNotFoundException("Cannot delete SKU with ID: " + skuId + ". SKU is not related to product with ID: " + productId + " or does not exist");
        }

        Sku skuToDeleteEntity = skuToDelete.get();

        /*
        List<Sku> newProductSkus = new ArrayList<>(
        		product.getAllSkus().stream().filter(x -> x.getId().longValue() != skuId).collect(Collectors.toList())
        		);
        */

        List<Sku> newProductSkus = new ArrayList<>(product.getAllSkus());
        newProductSkus.remove(skuToDeleteEntity);
        /* TODO: do we need to remove it also from product's getAllSkus() list? I dont think so... */
        catalogService.removeSku(skuToDeleteEntity);
        product.setAdditionalSkus(newProductSkus);
        catalogService.saveProduct(product);
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



/* This is a copy of an old SKU controller, just in case I missed something while copying/rewriting it to ProductController */
/*
public class SkuController {

    @Resource(name="blCatalogService")
    protected CatalogService catalogService;


    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(
            value = "List all SKUs",
            notes = "Gets a list of all available SKUs in the catalog",
            response = SkuDto.class,
            responseContainer = "List"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of SKUs list", response = SkuDto.class)
    })
    public List<SkuDto> getAllSkus() {
        return catalogService.findAllSkus().stream().map(DtoConverters.skuEntityToDto).collect(Collectors.toList());
    }


    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation(
            value = "Add a new SKU",
            notes = "Adds a new SKU to the catalog. Returns an URL to the newly added SKU in the Location field" +
                    "of the HTTP response header",
            response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A new SKU entry successfully created")
    })
    public ResponseEntity<?> saveOneSku(@RequestBody SkuDto skuDto) {

        Sku createdSkuEntity = catalogService.saveSku(DtoConverters.skuDtoToEntity.apply(skuDto));

        HttpHeaders responseHeader = new HttpHeaders();

        responseHeader.setLocation(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdSkuEntity.getId())
                .toUri());

        return new ResponseEntity<>(null, responseHeader, HttpStatus.CREATED);
    }


    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get a single SKU details",
            notes = "Gets details of a single SKU, specified by its ID",
            response = SkuDto.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of SKU details"),
            @ApiResponse(code = 404, message = "The specified SKU does not exist")
    })
    public SkuDto getSkusById(@PathVariable (value = "id") Long skuId) {

        Sku sku = catalogService.findSkuById(skuId);

        if(sku == null) {
            throw new ResourceNotFoundException("Cannot find SKU with ID: " + skuId);
        }

        return DtoConverters.skuEntityToDto.apply(sku);
    }


    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Delete an existing SKU",
            notes = "Removes an existing SKU from catalog",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful removal of the specified SKU"),
            @ApiResponse(code = 404, message = "The specified SKU does not exist")
    })
    public void deleteOneSkuById(@PathVariable (value = "id") Long skuId) {

        Sku skuToDelete = catalogService.findSkuById(skuId);

        if(skuToDelete == null) {
            throw new ResourceNotFoundException("Sku of ID: " + skuId + ". Not found");
        }

        catalogService.removeSku(skuToDelete);
    }


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