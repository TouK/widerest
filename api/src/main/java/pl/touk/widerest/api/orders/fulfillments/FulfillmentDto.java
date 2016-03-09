package pl.touk.widerest.api.orders.fulfillments;

import com.fasterxml.jackson.annotation.JsonRootName;
import cz.jirutka.validator.collection.constraints.EachURL;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import pl.touk.widerest.api.BaseDto;
import pl.touk.widerest.api.common.AddressDto;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName("fulfillment")
@EqualsAndHashCode
@ApiModel(value = "Fulfillment", description = "Fulfillment Group DTO resource representation")
public class FulfillmentDto extends BaseDto {

    @ApiModelProperty(position = 0, value = "Fulfillment type", readOnly = true)
    private String type;

    @ApiModelProperty(position = 1, value = "Address related to a fulfillment group", required = true)
    @Valid
    private AddressDto address;

    @ApiModelProperty(position = 3, value = "List of items belonging to a fulfillment group", required = true)
    @EachURL
    private List<String> items;

    @ApiModelProperty(value = "The selected fulfillment option", required = true)
    private String selectedFulfillmentOption;

    @ApiModelProperty(value = "Available options for fulfillment", readOnly = true)
    private Map<String, FulfillmentOptionDto> fulfillmentOptions;


}
