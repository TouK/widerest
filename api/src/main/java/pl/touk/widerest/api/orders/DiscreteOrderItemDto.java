package pl.touk.widerest.api.orders;


import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.broadleafcommerce.common.money.Money;
import pl.touk.widerest.api.BaseDto;

import java.math.BigDecimal;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName("orderItem")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Discrete Order Item", description = "Discrete Order Item DTO resource description")
public class DiscreteOrderItemDto extends BaseDto {

    @ApiModelProperty
    private String externalId;

    @ApiModelProperty(position = 0, value = "Quantity of this item", required = true)
    private Integer quantity;

    @ApiModelProperty(position = 1, value = "Name of the product")
    private String productName;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 2, value = "A map of selected options for the product")
    private Map<String, String> selectedOptions;

    @ApiModelProperty(position = 3, value = "Total price for this item", required = true)
    private BigDecimal price;

    @ApiModelProperty(position = 4, value = "ID of a product this item is related to")
    private String productHref;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 6, value = "Short description of this item")
    private String description;

    @ApiModelProperty(position = 7, value = "Sale price for this item")
    protected Money salePrice;

    @ApiModelProperty(position = 8, value = "Retail price for this item")
    protected Money retailPrice;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 9, value = "Additional attributes for this item")
    private Map attributes;
}
