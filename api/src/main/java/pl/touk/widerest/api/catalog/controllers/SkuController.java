package pl.touk.widerest.api.catalog.controllers;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.broadleafcommerce.common.persistence.Status;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.touk.widerest.api.catalog.DtoConverters;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.SkuDto;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by mst on 06.07.15.
 */
@RestController
@RequestMapping("/catalog/skus")
@Api(value = "skus", description = "Sku catalog endpoint")
public class SkuController {

    @Resource(name="blCatalogService")
    protected CatalogService catalogService;

    /* GET /skus */
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

    /* POST /skus */
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

    /* GET /skus/{id} */
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

    /* DELETE /skus/{id} */
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

    /* PUT /skus/{id} */
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

    /* GET /skus/count */
    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(value = "/count", method = RequestMethod.GET)
    @ApiOperation(
            value = "Count all SKUs",
            notes = "Gets a number of all SKUs available in the catalog",
            response = Long.class
    )
    public Long getAllSkusCount() {
        return catalogService.findAllSkus().stream().count();
    }
}