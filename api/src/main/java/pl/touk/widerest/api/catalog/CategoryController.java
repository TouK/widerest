package pl.touk.widerest.api.catalog;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

@RestController
@RequestMapping("/catalog/categories")
@Api
public class CategoryController {

    @Resource(name="blCatalogService")
    protected CatalogService catalogService;

    @ApiOperation("readCategories")
    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("permitAll")
    public List<Category> readCategories() {
        return catalogService.findAllCategories().stream()
                .map(categoryEntityToDto)
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Category readOneCategory(@PathVariable(value="id") Long id) {
        return Optional.ofNullable(catalogService.findCategoryById(id))
                .map(categoryEntityToDto)
                .orElseThrow(ResourceNotFoundException::new);
    }

    @RequestMapping(method = RequestMethod.POST)
    public void saveOneCategory(Category dto) {

    }

    public static Function<org.broadleafcommerce.core.catalog.domain.Category, Category> categoryEntityToDto
            = entity -> {
        Category dto = Category.builder().name(entity.getName()).build();
        dto.add(linkTo(methodOn(CategoryController.class).readOneCategory(entity.getId())).withSelfRel());
        return dto;
    };

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public class ResourceNotFoundException extends RuntimeException {}

}