package pl.touk.widerest.api.products.skus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.broadleafcommerce.common.media.domain.Media;
import org.broadleafcommerce.common.service.GenericEntityService;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.catalog.domain.SkuMediaXref;
import org.broadleafcommerce.core.catalog.domain.SkuMediaXrefImpl;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.inventory.service.InventoryService;
import org.broadleafcommerce.core.inventory.service.type.InventoryType;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.touk.widerest.api.common.CatalogUtils;
import pl.touk.widerest.api.common.MediaConverter;
import pl.touk.widerest.api.common.MediaDto;
import pl.touk.widerest.api.common.ResourceNotFoundException;
import pl.touk.widerest.api.products.ProductController;
import pl.touk.widerest.api.products.ProductConverter;
import pl.touk.widerest.api.products.RequiredProductOptionsNotProvided;
import pl.touk.widerest.security.oauth2.ResourceServerConfig;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@RequestMapping(value = ResourceServerConfig.API_PATH + "/products", produces = { MediaTypes.HAL_JSON_VALUE })
@Api(value = "product skus", description = "Product catalog endpoint (skus)", produces = MediaTypes.HAL_JSON_VALUE)
public class SkuController {

    @Resource
    protected ProductController productController;

    @Resource(name = "blCatalogService")
    protected CatalogService catalogService;

    @Resource(name = "blGenericEntityService")
    protected GenericEntityService genericEntityService;

    @Resource
    protected SkuConverter skuConverter;

    @Resource
    protected ProductConverter productConverter;

    @Resource
    protected MediaConverter mediaConverter;

    /* GET /products/{id}/skus */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{productId}/skus", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get product's SKUs",
            notes = "Gets a list of all SKUs available for a specified product",
            response = SkuDto.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful retrieval of all available SKUs", responseContainer = "List"),
            @ApiResponse(code = 404, message = "The specified product does not exist")
    })
    public Resources<SkuDto> readSkusForProductById(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") final Long productId
    ) {
        return new Resources<>(
                productController.getProductById(productId).getAllSkus().stream()
                        .map(sku -> skuConverter.createDto(sku))
                        .collect(toList()),

                linkTo(methodOn(getClass()).readSkusForProductById(productId)).withSelfRel()
        );
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
            @PathVariable(value = "productId") final Long productId,
            @ApiParam(value = "Description of a new SKU", required = true)
            @Valid @RequestBody final SkuDto skuDto
    ) throws RequiredProductOptionsNotProvided {

        final Product product = productController.getProductById(productId);


        /* (mst) Basically, when you're adding a new SKU to a product which has options, you have to
                provide them.
         */
        if (product.getProductOptionXrefs() != null && !product.getProductOptionXrefs().isEmpty()) {
            if (skuDto.getSkuProductOptionValues() == null || skuDto.getSkuProductOptionValues().isEmpty() ||
                    skuDto.getSkuProductOptionValues().size() != product.getProductOptionXrefs().size()) {
                throw new RequiredProductOptionsNotProvided();
            }
        }


        Sku newSkuEntity = skuConverter.createEntity(skuDto);
        newSkuEntity.setProduct(product);


        /* (mst) TODO: Merge with SKU with the same Product Options set if it already exists in catalog */
        final Sku skuParam = newSkuEntity;

        if (skuDto.getSkuProductOptionValues() != null && !skuDto.getSkuProductOptionValues().isEmpty()) {
            newSkuEntity.setProductOptionValueXrefs(
                    skuDto.getSkuProductOptionValues().stream()
                            .map(e -> productConverter.generateXref(e, skuParam, product))
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

        return ResponseEntity.created(
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/products/{productId}/skus/{skuId}")
                        .buildAndExpand(productId, newSkuEntity.getId())
                        .toUri()
        ).build();
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
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") final Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") final Long skuId) {

        return Optional.of(getSkuByIdForProductById(productId, skuId))
                .map(skuEntity -> skuConverter.createDto(skuEntity))
                .get();
    }



    //    /* GET /skus/count */
//    @Transactional
//    @PreAuthorize("permitAll")
//    @RequestMapping(value = "/{productId}/skus/count", method = RequestMethod.GET)
//    @ApiOperation(
//            value = "Count all SKUs",
//            notes = "Gets a number of all SKUs related to the specific product",
//            response = Long.class
//    )
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "Successful retrieval of SKU count"),
//            @ApiResponse(code = 404, message = "The specified product does not exist")
//    })
//    public ResponseEntity<Long> getSkusCountByProductId(
//            @ApiParam(value = "ID of a specific product", required = true)
//            @PathVariable(value = "productId") final Long productId) {
//
//
//        final long skusCount = Optional.ofNullable(catalogService.findProductById(productId))
//                .filter(CatalogUtils::nonArchivedProduct)
//                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
//                .getAllSkus().stream()
//                .count();
//
//        return ResponseEntity.ok(skusCount);
//    }

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
            @PathVariable(value = "productId") final Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") final Long skuId
    ) {
        return ResponseEntity.ok(
                getSkuByIdForProductById(productId, skuId).getQuantityAvailable()
        );
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
    public void updateSkuByIdQuantity(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") final Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") final Long skuId,
            @ApiParam(value = "Quantity of a specific SKU")
            @RequestBody final Integer quantity
    )
    {
        Optional.of(getSkuByIdForProductById(productId, skuId))
                .map(e -> {
                    e.setQuantityAvailable(quantity);
                    return e;
                })
                .map(catalogService::saveSku);
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
    public void updateSkuByIdAvailability(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") final Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") final Long skuId,
            @ApiParam(value = "Inventory type: ALWAYS_AVAILABLE, UNAVAILABLE, CHECK_QUANTITY")
            @RequestBody final String availability) {


        Optional.of(getSkuByIdForProductById(productId, skuId))
                .map(e -> {
                    e.setInventoryType(Optional.ofNullable(InventoryType.getInstance(availability))
                            .orElseThrow(() -> new ResourceNotFoundException("The specified Inventory Type does not exist")));
                    return e;
                })
                .map(catalogService::saveSku);
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
            @PathVariable(value = "productId") final Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") final Long skuId) {

        final Sku sku = getSkuByIdForProductById(productId, skuId);

        final String skuAvailability = Optional.ofNullable(sku.getInventoryType())
                .map(InventoryType::getType)
                .orElse("");

        return ResponseEntity.ok(skuAvailability);
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
    public void deleteOneSkuById(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") final Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") final Long skuId) {

        final Product product = productController.getProductById(productId);

        if (!product.getAdditionalSkus().removeIf(sku -> sku.getId() == skuId)) {
            throw new ResourceNotFoundException(
                    "Cannot delete SKU with ID: " + skuId + ". SKU is not related to product with ID: " + productId + " or does not exist"
            );
        };

        catalogService.saveProduct(product);
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
    public void updateOneSkuByProductId(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") final Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") final Long skuId,
            @ApiParam(value = "(Full) Description of an updated SKU", required = true)
            @Valid @RequestBody final SkuDto skuDto) {

        Optional.of(getSkuByIdForProductById(productId, skuId))
                .map(e -> skuConverter.updateEntity(e, skuDto))
                .map(catalogService::saveSku);

    }

    /* ---------------------------- MEDIA ENDPOINTS ---------------------------- */

    /* GET /products/{productId}/skus/{skuId}/media */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{productId}/skus/{skuId}/media", method = RequestMethod.GET)
    @ApiOperation(
            value = "List all SKU's media",
            notes = "Gets a list of all medias belonging to a specified SKU",
            response = MediaDto.class,
            responseContainer = "List"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of media details", responseContainer = "List"),
            @ApiResponse(code = 404, message = "The specified SKU or product does not exist")
    })
    public Resources<MediaDto> getMediaBySkuId(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") final Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") final Long skuId
    ) {
        return new Resources<>(
                getSkuByIdForProductById(productId, skuId).getSkuMediaXref().entrySet().stream()
                        .map(Map.Entry::getValue)
                        .map(skuMediaXref -> {
                            final MediaDto mediaDto = mediaConverter.createDto(skuMediaXref.getMedia(), true, true);
                            mediaDto.add(linkTo(methodOn(getClass()).getMediaByIdForSku(productId, skuId, skuMediaXref.getKey())).withSelfRel());
                            return mediaDto;
                        })
                        .collect(toList()),

                linkTo(methodOn(getClass()).getMediaBySkuId(productId, skuId)).withSelfRel()
        );
    }

    /* GET /products/{productId}/skus/{skuId}/media/{key} */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/{productId}/skus/{skuId}/media/{key}", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get a single media details",
            notes = "Gets details of a particular media belonging to a specified SKU",
            response = MediaDto.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of media details"),
            @ApiResponse(code = 404, message = "The specified SKU or product does not exist")
    })
    public MediaDto getMediaByIdForSku(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") final Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") final Long skuId,
            @ApiParam(value = "ID of a specific media", required = true)
            @PathVariable(value = "key") final String key) {

        /* (mst) Here is the deal: if the specified SKU does not contain any medias
         *       BL's service will return medias associated with Default SKU instead.,
         */


        final Sku sku = getSkuByIdForProductById(productId, skuId);

        return Optional.ofNullable(sku.getSkuMediaXref().get(key))
                .map(SkuMediaXref::getMedia)
                .map(media -> {
                    final MediaDto mediaDto = mediaConverter.createDto(media);
                    mediaDto.add(linkTo(methodOn(getClass()).getMediaByIdForSku(productId, skuId, key)).withSelfRel());
                    return mediaDto;
                })
                .orElseThrow(() -> new ResourceNotFoundException("No media with key " + key + " for this sku"));
    }

    /* DELETE /products/{productId}/skus/{id}/media/{key} */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}/skus/{skuId}/media/key", method = RequestMethod.DELETE)
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
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") final Long skuId,
            @ApiParam(value = "ID of a specific media", required = true)
            @PathVariable(value = "key") final String key
    ) {

        final Sku skuEntity = getSkuByIdForProductById(productId, skuId);

        final SkuMediaXref skuMediaXrefToBeRemoved = Optional.ofNullable(skuEntity.getSkuMediaXref().remove(key))
                .orElseThrow(() -> new ResourceNotFoundException("No media with key " + key + "for this sku"));

        catalogService.saveSku(skuEntity);
        genericEntityService.remove(skuMediaXrefToBeRemoved);
    }

    /* PUT /{productId}/skus/{skuId}/media/{key} */
    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_PRODUCT')")
    @RequestMapping(value = "/{productId}/skus/{skuId}/media/{key}", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Create new or update existing media",
            notes = "Updates an existing media with new details. If the media does not exist, it creates it!",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful update of the specified media"),
            @ApiResponse(code = 400, message = "Not enough data has been provided"),
            @ApiResponse(code = 404, message = "The specified product or SKU does not exist"),
    })
    public void updateMediaForSkuById(
            @ApiParam(value = "ID of a specific product", required = true)
            @PathVariable(value = "productId") final Long productId,
            @ApiParam(value = "ID of a specific SKU", required = true)
            @PathVariable(value = "skuId") final Long skuId,
            @ApiParam(value = "ID of a specific media", required = true)
            @PathVariable(value = "key") final String key,
            @ApiParam(value = "(Full) Description of an updated media")
            @Valid @RequestBody final MediaDto mediaDto
    ) {

        final Sku sku = getSkuByIdForProductById(productId, skuId);

        Optional.ofNullable(sku.getSkuMediaXref().remove(key))
                .ifPresent(genericEntityService::remove);

        final Media mediaEntity = mediaConverter.createEntity(mediaDto);

        final SkuMediaXref newSkuMediaXref = new SkuMediaXrefImpl();
        newSkuMediaXref.setMedia(mediaEntity);
        newSkuMediaXref.setSku(sku);
        newSkuMediaXref.setKey(key);
        sku.getSkuMediaXref().put(key, newSkuMediaXref);

        catalogService.saveSku(sku);
    }

/* ---------------------------- MEDIA ENDPOINTS ---------------------------- */


    private Sku getSkuByIdForProductById(final long productId, final long skuId) throws ResourceNotFoundException {
        return Optional.ofNullable(catalogService.findProductById(productId))
                .filter(CatalogUtils.nonArchivedProduct)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID: " + productId + " does not exist"))
                .getAllSkus().stream()
                .filter(x -> x.getId() == skuId)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SKU with ID: " + skuId + " does not exist or is not related to product with ID: " + productId
                ));
    }



}
