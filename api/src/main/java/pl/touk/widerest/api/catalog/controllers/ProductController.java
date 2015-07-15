package pl.touk.widerest.api.catalog.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@Api(value = "/catalog/products", description = "Product catalog endpoint")
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
            value = "Get all products",
            notes = "",
            response = ProductDto.class,
            responseContainer = "List")
    public List<ProductDto> getProducts() {
        return catalogService.findAllProducts().stream().map(DtoConverters.productEntityToDto).collect(Collectors.toList());
    }

    /* POST /prodcuts */
    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/", method = RequestMethod.POST)
    @ApiOperation(value = "Add a new product", response = Void.class)
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
    @ApiOperation(value = "Get a single product details", response = ProductDto.class)
    public ProductDto readOneProduct(@PathVariable(value="id") Long productId) {

        return Optional.ofNullable(catalogService.findProductById(productId))
                .map(DtoConverters.productEntityToDto)
                .orElseThrow(ResourceNotFoundException::new);


    }

    /* PUT /products/{id} */
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

    /* DELETE /prodcuts/{id} */
    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ApiOperation(value = "Delete an existing product", response = Void.class)
    public void removeOneProduct(@PathVariable(value="id") Long id) {

        Product productToDelete = catalogService.findProductById(id);

        if(productToDelete != null) {
            catalogService.removeProduct(productToDelete);
        }

    }

    /* GET /prodcuts/{id}/categories */
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

    /* GET /prodcuts/{id}/skus */
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

    /* POST /prodcuts/{id}/skus */
    @RequestMapping(value = "/{id}/skus", method = RequestMethod.POST)
    @PreAuthorize("permitAll")
    public void saveOneSkuByProduct(@PathVariable (value = "id") Long productId, @RequestBody SkuDto skusDto) {

        Product product = catalogService.findProductById(productId);

        if(product == null) {
            throw new ResourceNotFoundException("Product with ID: " + productId + " does not exist");
        }

        catalogService.saveSku(DtoConverters.skuDtoToEntity.apply(skusDto));
        product.getAllSkus().add(DtoConverters.skuDtoToEntity.apply(skusDto));
        catalogService.saveProduct(product);
    }


    /* GET /products/{id}/reviews */
    @RequestMapping(value = "/{id}/reviews", method = RequestMethod.GET)
    @PreAuthorize("permitAll")
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
    @ApiOperation(value = "Create a new review for a product", response = Void.class)
    public void saveReviewForProduct(
            @PathVariable(value = "id") Long productId,
            @RequestBody ReviewDto reviewDto,
            @AuthenticationPrincipal CustomerUserDetails customerUserDetails) {

        RatingSummary ratingSummary = ratingService.readRatingSummary(productId.toString(), RatingType.PRODUCT);

        if(ratingSummary == null) {
            /* TODO: Do we create a new one ?! */
        }

        // TODO: Customer verification

        ratingSummary.getReviews().add(DtoConverters.reviewDtoToEntity.apply(reviewDto));

        ratingService.saveRatingSummary(ratingSummary);
    }

    /* GET /products/{id}/reviews/count */
    @RequestMapping(value = "/{id}/reviews/count", method = RequestMethod.GET)
    @PreAuthorize("permitAll")
    public Integer getReviewCountForProduct(@PathVariable(value = "id") Long productId) {
        Integer reviewCount = 0;
        RatingSummary ratingSummary = ratingService.readRatingSummary(productId.toString(), RatingType.PRODUCT);

        if(ratingSummary != null) {
            reviewCount = ratingSummary.getNumberOfReviews();
        }

        return reviewCount;
    }

    /* GET /products/{id}/reviews/count */
    @RequestMapping(value = "/{id}/ratings/count", method = RequestMethod.GET)
    @PreAuthorize("permitAll")
    public Integer getRatingsCountForProduct(@PathVariable(value = "id") Long productId) {
        Integer ratingsCount = 0;
        RatingSummary ratingSummary = ratingService.readRatingSummary(productId.toString(), RatingType.PRODUCT);

        if(ratingSummary != null) {
            ratingsCount = ratingSummary.getNumberOfRatings();
        }

        return ratingsCount;
    }

    /* GET /products/{id}/ratings/avg */
    @RequestMapping(value = "/{id}/ratings/avg", method = RequestMethod.GET)
    @PreAuthorize("permitAll")
    public Double getAverageRatingForProduct(@PathVariable(value = "id") Long productId) {
        Double avgRating = 0.0;
        RatingSummary ratingSummary = ratingService.readRatingSummary(productId.toString(), RatingType.PRODUCT);

        if(ratingSummary != null) {
            avgRating = ratingSummary.getAverageRating();
        }

        return avgRating;
    }

    /* GET /products/{id}/ratings */
    @RequestMapping(value = "/{id}/ratings", method = RequestMethod.GET)
    @PreAuthorize("permitAll")
    public List<RatingDto> getRatingsForProduct(@PathVariable(value = "id") Long productId) {
        RatingSummary ratingSummary = ratingService.readRatingSummary(productId.toString(), RatingType.PRODUCT);

        if(ratingSummary != null) {
            return ratingSummary.getRatings().stream().map(DtoConverters.ratingEntityToDto).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

}