package pl.touk.widerest.api.catalog.dto;

import com.google.common.collect.Lists;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;
import org.broadleafcommerce.core.catalog.domain.ProductOption;
import org.broadleafcommerce.core.catalog.domain.ProductOptionValue;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

/**
 * Created by mst on 06.07.15.
 */
@ApiModel
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOptionDto {

    @ApiModelProperty
    private String name;

    @ApiModelProperty
    private List<String> allowedValues;

    /*
    public ProductOptionDto(ProductOption productOption) {
        this.name = productOption.getAttributeName();
        this.allowedValues = Lists.transform(productOption.getAllowedValues(), new Function<ProductOptionValue, String>() {

            @Nullable
            @Override
            public String apply(@Nullable ProductOptionValue productOptionValue) {
                return productOptionValue.getAttributeValue();
            }
        });
    }*/
}
