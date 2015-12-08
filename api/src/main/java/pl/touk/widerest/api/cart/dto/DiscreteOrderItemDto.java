package pl.touk.widerest.api.cart.dto;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;
import org.broadleafcommerce.common.money.Money;
import org.springframework.hateoas.ResourceSupport;
import pl.touk.widerest.api.catalog.dto.ProductOptionDto;

import java.math.BigDecimal;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Discrete Order Item", description = "Discrete Order Item DTO resource description")
public class DiscreteOrderItemDto extends ResourceSupport {

    /* TODO: (mst) ID should probably be removed, I kept it because there were few tests that use it */
    @ApiModelProperty
    private long itemId;

    @ApiModelProperty(position = 0, value = "Quantity of this item", required = true, dataType = "java.lang.Integer")
    private Integer quantity;

    @ApiModelProperty(position = 1, value = "Name of the product", dataType = "java.lang.String")
    private String productName;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 2, value = "Product Options related to this item",
            dataType = "pl.touk.widerest.api.catalog.dto.ProductOptionDto")
    private ProductOptionDto options;

    @ApiModelProperty(position = 3, value = "Total price for this item", required = true, dataType = "java.math.BigDecimal")
    private BigDecimal price;

    @ApiModelProperty(position = 4, value = "ID of a product this item is related to", dataType = "java.lang.Long")
    private Long productId;

    @ApiModelProperty(position = 5, value = "ID of a SKU this item is related to", required = true, dataType = "java.lang.Long")
    private Long skuId;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 6, value = "Short description of this item", dataType = "java.lang.String")
    private String description;

    @ApiModelProperty(position = 7, value = "Sale price for this item", dataType = "org.broadleafcommerce.common.money.Money")
    protected Money salePrice;

    @ApiModelProperty(position = 8, value = "Retail price for this item", dataType = "org.broadleafcommerce.common.money.Money")
    protected Money retailPrice;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 9, value = "Additional attributes for this item")
    private Map attributes;
}
