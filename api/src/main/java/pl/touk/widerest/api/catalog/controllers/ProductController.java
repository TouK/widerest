package pl.touk.widerest.api.catalog.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.broadleafcommerce.core.catalog.domain.*;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.inventory.service.InventoryService;
import org.broadleafcommerce.core.rating.domain.RatingDetail;
import org.broadleafcommerce.core.rating.domain.RatingSummary;
import org.broadleafcommerce.core.rating.domain.ReviewDetail;
import org.broadleafcommerce.core.rating.service.RatingService;
import org.broadleafcommerce.core.rating.service.type.RatingType;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.touk.widerest.api.catalog.*;
import pl.touk.widerest.api.catalog.dto.*;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;


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
    public List<ProductDto> getProducts() {
        return catalogService.findAllProducts().stream().map(DtoConverters.productEntityToDto).collect(Collectors.toList());
    }

    /* POST /prodcuts */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
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

        Product createdProductEntity = catalogService.saveProduct(DtoConverters.productDtoToEntity.apply(productDto));

        HttpHeaders responseHeader = new HttpHeaders();

        responseHeader.setLocation(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdProductEntity.getId())
                .toUri());

        return new ResponseEntity<>(null, responseHeader, HttpStatus.CREATED);
    }

    /* GET /prodcuts/{id} */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get a single product details",
            notes = "Gets details of a single product specified by its ID",
            response = ProductDto.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of product details", response = ProductDto.class),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public ProductDto readOneProduct(@PathVariable(value="id") Long productId) {

        return Optional.ofNullable(catalogService.findProductById(productId))
                .map(DtoConverters.productEntityToDto)
                .orElseThrow(ResourceNotFoundException::new);


    }

    /* PUT /products/{id} */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Update an existing product",
            notes = "Updates an existing product with new details",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful update of the specified product"),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public void changeOneProduct(@PathVariable(value="id") Long id, @RequestBody ProductDto productDto) {

        Product productToChange = catalogService.findProductById(id);

        if(productToChange!= null) {
            catalogService.saveProduct(DtoConverters.productDtoToEntity.apply(productDto));
        } else {
            throw new ResourceNotFoundException("Cannot change product with id " + id + ". Not Found");
        }

    }

    /* DELETE /prodcuts/{id} */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Delete an existing product",
            notes = "Removes an existing product from catalog",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of product details"),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public void removeOneProduct(@PathVariable(value="id") Long id) {

        Product productToDelete = catalogService.findProductById(id);

        if(productToDelete == null) {
            throw new ResourceNotFoundException("Cannot delete product with ID: " + id + ". Product does not exist");
        }

        catalogService.removeProduct(productToDelete);
    }

    /* GET /prodcuts/{id}/categories */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{id}/categories", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get product's categories",
            notes = "Gets a list of all categories belonging to a specified product",
            response = CategoryDto.class,
            responseContainer = "List")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of product's categories"),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public List<CategoryDto> readCategoriesByProduct(@PathVariable (value = "id") Long productId) {

        Product product = catalogService.findProductById(productId);

        if (product == null) {
            throw new ResourceNotFoundException("Product with ID: " + productId + " does not exist");

        }

        /* Deprecated */
        List<Category> productCategories = product.getAllParentCategories();

        return productCategories.stream().map(DtoConverters.categoryEntityToDto).collect(Collectors.toList());

    }

    /* GET /prodcuts/{id}/skus */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{id}/skus", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get product's SKUs",
            notes = "Gets a list of all SKUs available for a specified product",
            response = SkuDto.class,
            responseContainer = "List")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of all available SKUs"),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public List<SkuDto> readSkusByProduct(@PathVariable(value = "id") Long productId) {

        Product product = catalogService.findProductById(productId);

        if (product == null) {
            throw new ResourceNotFoundException("Product with ID: " + productId + " does not exist");
        }

        return product.getAllSkus().stream().map(DtoConverters.skuEntityToDto).collect(Collectors.toList());

    }

    //TODO: what about adding SKU by ID?
    /* POST /prodcuts/{id}/skus */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/{id}/skus", method = RequestMethod.POST)
    @ApiOperation(
            value = "Add a SKU to the product",
            notes = "Adds a SKU to the existing product",
            response = SkuDto.class,
            responseContainer = "List")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Specified SKU successfully added"),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public ResponseEntity<?> saveOneSkuByProduct(@PathVariable (value = "id") Long productId, @RequestBody SkuDto skusDto) {

        Product product = catalogService.findProductById(productId);

        if(product == null) {
            throw new ResourceNotFoundException("Product with ID: " + productId + " does not exist");
        }

        catalogService.saveSku(DtoConverters.skuDtoToEntity.apply(skusDto));
        product.getAllSkus().add(DtoConverters.skuDtoToEntity.apply(skusDto));
        catalogService.saveProduct(product);

        return new ResponseEntity<>(null, HttpStatus.CREATED);
    }


    /* GET /products/{id}/reviews */
    @Transactional
    @PreAuthorize("hasRole('ROLE_ADMIN')")
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

        RatingSummary ratingSummary = ratingService.readRatingSummary(productId.toString(), RatingType.PRODUCT);

        if(ratingSummary != null) {
            List<ReviewDetail> reviewDetail = ratingSummary.getReviews();

            return reviewDetail.stream().map(DtoConverters.reviewEntityToDto).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    /* POST /products/{id}/reviews */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
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

        if(product == null) {
            throw new ResourceNotFoundException("Cannot find product with ID: " + productId);
        }

        RatingSummary ratingSummary = ratingService.readRatingSummary(productId.toString(), RatingType.PRODUCT);

        if(ratingSummary == null) {
            /* TODO: Do we create a new one ?! */
        }

        // TODO: Customer verification

        ratingSummary.getReviews().add(DtoConverters.reviewDtoToEntity.apply(reviewDto));

        ratingService.saveRatingSummary(ratingSummary);

        return new ResponseEntity<>(null, HttpStatus.CREATED);
    }

    /* GET /products/{id}/reviews/count */

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

        if(ratingSummary != null) {
            reviewCount = ratingSummary.getNumberOfReviews();
        }

        return reviewCount;
    }

    /* GET /products/{id}/reviews/count */
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

        if(ratingSummary != null) {
            ratingsCount = ratingSummary.getNumberOfRatings();
        }

        return ratingsCount;
    }

    /* GET /products/{id}/ratings/avg */
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

        if(ratingSummary != null) {
            avgRating = ratingSummary.getAverageRating();
        }

        return avgRating;
    }

    /* GET /products/{id}/ratings */
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

        if(ratingSummary != null) {
            return ratingSummary.getRatings().stream().map(DtoConverters.ratingEntityToDto).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}