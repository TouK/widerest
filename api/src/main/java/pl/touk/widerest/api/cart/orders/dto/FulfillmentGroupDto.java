package pl.touk.widerest.api.cart.orders.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import pl.touk.widerest.api.BaseDto;
import pl.touk.widerest.api.cart.customers.dto.AddressDto;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName("fulfillment")
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "Fulfillment Group", description = "Fulfillment Group DTO resource representation")
public class FulfillmentGroupDto extends BaseDto {

    @ApiModelProperty(position = 0, value = "Address related to a fulfillment group", required = true,
            dataType = "pl.touk.widerest.api.cart.customers.dto.AddressDto")
    private AddressDto address;

    @ApiModelProperty(position = 1, value = "List of items belonging to a fulfillment group", required = true,
            dataType = "java.util.List")
    private List<String> items;
}
