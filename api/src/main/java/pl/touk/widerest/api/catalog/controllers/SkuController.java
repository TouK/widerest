package pl.touk.widerest.api.catalog.controllers;

import com.wordnik.swagger.annotations.ApiOperation;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pl.touk.widerest.api.catalog.DtoConverters;
import pl.touk.widerest.api.catalog.dto.SkuDto;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by mst on 06.07.15.
 */
@RestController
@RequestMapping("/catalog/skus")
public class SkuController {

    @Resource(name="blCatalogService")
    protected CatalogService catalogService;


    @Transactional
    @PreAuthorize("permitAll")
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(value = "Get a flat list of all SKUs", response = List.class)
    public ResponseEntity<List<SkuDto>> getAllSkus() {
        return new ResponseEntity<> (
                catalogService.findAllSkus().stream().map(DtoConverters.skuEntityToDto).collect(Collectors.toList()),
                HttpStatus.OK);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation(value = "Add a new SKU", response = Void.class)
    public void saveOneSku(@RequestBody SkuDto skuDto) {
        if(skuDto != null) {
            Sku newSkuEntity = catalogService.createSku();
            

        }
    }




    @Transactional
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<SkuDto> getSkusById(@PathVariable (value = "id") Long skuId) {
        return new ResponseEntity<> (DtoConverters.skuEntityToDto.apply(catalogService.findSkuById(skuId)), HttpStatus.OK);
    }


}
