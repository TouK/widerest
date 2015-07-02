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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;


@RestController
@RequestMapping("/catalog/product")
@Api
public class ProductController {

    @Resource(name = "blCatalogService")
    protected CatalogService catalogService;

    @Resource(name = "blInventoryService")
    protected InventoryService inventoryService;


    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @PreAuthorize("permitAll")
    @Transactional
    public Product readOneProduct(@PathVariable(value="id") Long id) {
        return Optional.ofNullable(catalogService.findProductById(id))
                .map(productEntityToDto)
                .orElseThrow(ResourceNotFoundException::new);
    }

    public static Function<org.broadleafcommerce.core.catalog.domain.Product, Product> productEntityToDto
            = entity -> {
        Product dto = new Product();
        if(entity.getDefaultCategory() != null) {
            dto.setCategory(entity.getDefaultCategory().getName());
        }
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setId(entity.getId());
        if(entity.getLongDescription() != null && !entity.getLongDescription().isEmpty()) {
            dto.setLongDescription(entity.getLongDescription());
        }

        dto.setValidFrom(entity.getActiveStartDate());
        dto.setValidTo(entity.getActiveEndDate());
        dto.setAttributes(entity.getProductAttributes());
        /*dto.setAttributes(Maps.transformValues(entity.getProductAttributes(), new Function<ProductAttribute, String>() {
            @Nullable
            @Override
            public String apply(@Nullable ProductAttribute input) {
                return input.getValue();
            }
        }));
        dto.setOptions(Lists.transform(entity.getProductOptionXrefs(), new Function<ProductOptionXref, ProductOption>() {
            @Nullable
            @Override
            public ProductOption apply(@Nullable ProductOptionXref input) {
                return new ProductOption(input.getProductOption());
            }
        }));
        dto.setSkus(new ArrayList<SkuDto>());
        SkuDto defaultSku = (SkuDto) entity.getDefaultSku();
        if (entity.getCanSellWithoutOptions() || entity.getAdditionalSkus().isEmpty()) {
            dto.getSkus().add(new SkuDto(defaultSku, inventoryService));
        }
        List<Sku> sortedSkus = Ordering.from(new SkuByIdComparator()).sortedCopy(product.getSkus());
        dto.getSkus().addAll(Lists.transform(sortedSkus, new Function<Sku, SkuDto>() {
            @Nullable
            @Override
            public SkuDto apply(@Nullable Sku input) {
                return new SkuDto(input, inventoryService);
            }
        }));

        Collection<ProductBundle> possibleBundles = Lists.transform(
                ((VirginSkuImpl) defaultSku).getSkuBundleItems(),
                new Function<SkuBundleItem, ProductBundle>() {
                    @Nullable
                    @Override
                    public ProductBundle apply(@Nullable SkuBundleItem input) {
                        return input.getBundle();
                    }
                }
        );
        possibleBundles = Collections2.filter(
                possibleBundles,
                new Predicate<ProductBundle>() {
                    @Override
                    public boolean apply(@Nullable ProductBundle input) {
                        return ((SkuDto) input.getDefaultSku()).getDefaultProductBundle() == null;
                    }
                }
        );
        dto.setPossibleBundles(Lists.newArrayList(Iterables.transform(
                possibleBundles,
                new Function<ProductBundle, Long>() {
                    @Nullable
                    @Override
                    public Long apply(@Nullable ProductBundle input) {
                        return input.getId();
                    }
                }
        )));


        if (dto instanceof Bundle) {
            ProductBundle bundle = (ProductBundle) entity;
            ((Bundle) dto).setBundleItems(Lists.transform(bundle.getSkuBundleItems(), new Function<SkuBundleItem, BundleItem>() {
                @Nullable
                @Override
                public BundleItem apply(@Nullable final SkuBundleItem input) {
                    BundleItem itemDto = new BundleItem();
                    itemDto.setQuantity(input.getQuantity());
                    itemDto.setProductId(input.getSku().getProduct().getId());
                    return itemDto;
                }
            }));
            ((Bundle) dto).setBundlePrice(bundle.getSalePrice().getAmount());
            ((Bundle) dto).setPotentialSavings(bundle.getPotentialSavings());
        }

        dto.setPayableWithBalance(((SkuDto) defaultSku).isPayableWithBalance());
        dto.setPayableWithPayU(((SkuDto) defaultSku).isPayableWithPayU());
        dto.setDisplayOrder(defaultSku.getDisplayOrder());
*/

        return dto;
    };

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public class ResourceNotFoundException extends RuntimeException {}

}