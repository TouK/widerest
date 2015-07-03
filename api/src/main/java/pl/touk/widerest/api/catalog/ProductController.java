package pl.touk.widerest.api.catalog;

import com.google.common.collect.*;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.broadleafcommerce.core.catalog.domain.*;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.inventory.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;


@RestController
@RequestMapping("/catalog/product")
@Api
public class ProductController {

    @Resource(name = "blCatalogService")
    protected CatalogService catalogService;

    @Resource(name = "blInventoryService")
    protected InventoryService inventoryService;

    @ApiOperation("readProducts")
    @PreAuthorize("permitALL")
    @RequestMapping(value = "/", method = RequestMethod.GET)
    @Transactional
    public List<Product> getProducts() {
        return catalogService.findAllProducts().stream()
                .map(DtoConverters.productEntityToDto)
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @PreAuthorize("permitAll")
    @Transactional
    public Product readOneProduct(@PathVariable(value="id") Long id) {
        return Optional.ofNullable(catalogService.findProductById(id))
                .map(DtoConverters.productEntityToDto)
                .orElseThrow(ResourceNotFoundException::new);
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public class ResourceNotFoundException extends RuntimeException {}

}