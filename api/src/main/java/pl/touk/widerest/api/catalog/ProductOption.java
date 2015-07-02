package pl.touk.widerest.api.catalog;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Builder;
import org.broadleafcommerce.core.catalog.domain.ProductOptionValue;

import javax.annotation.Nullable;
import java.util.List;

@ApiModel
@Data
@Builder
public class ProductOption {

    private String name;

    private List<String> allowedValues;

    // Potrzebne dla sciagniecia ProductOption w /catalog/product/{id}
    public ProductOption(org.broadleafcommerce.core.catalog.domain.ProductOption productOption) {
        this.name = productOption.getAttributeName();
        /*this.allowedValues = Lists.transform(productOption.getAllowedValues(), new Function<ProductOptionValue, String>() {
            @Nullable
            @Override
            public String apply(ProductOptionValue productOptionValue) {
                return productOptionValue.getAttributeValue();
            }
        });
        */
    }

}
