package pl.touk.widerest.api.orders;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.touk.widerest.api.BaseDto;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonRootName("order")
@ApiModel(value = "Order", description = "Order DTO resource representation")
public class OrderDto extends BaseDto {

    @ApiModelProperty(position = 0, value = "Order number", readOnly = true)
    private String orderNumber;

    @ApiModelProperty(position = 1, value = "Status of the order", readOnly = true)
    private String status;

    @ApiModelProperty(position = 5, value = "Total price for the order", readOnly = true)
    private BigDecimal totalPrice;

    @ApiModelProperty(position = 6, value = "Additional attributes for the order")
    private Map<String, String> attributes;

}
