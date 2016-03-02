package pl.touk.widerest.api.orders.fulfillments;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import pl.touk.widerest.api.BaseDto;
import pl.touk.widerest.api.common.AddressDto;

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

    @ApiModelProperty(position = 0, value = "Fulfillment type", required = true,
            dataType = "java.lang.String")
    private String type;

    @ApiModelProperty(position = 1, value = "Address related to a fulfillment group", required = true,
            dataType = "pl.touk.widerest.api.common.AddressDto")
    private AddressDto address;

    @ApiModelProperty(position = 2, value = "List of items belonging to a fulfillment group", required = true,
            dataType = "java.util.List")
    private List<String> items;
}
