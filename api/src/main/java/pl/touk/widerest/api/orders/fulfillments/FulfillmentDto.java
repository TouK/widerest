package pl.touk.widerest.api.orders.fulfillments;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import cz.jirutka.validator.collection.constraints.EachURL;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import pl.touk.widerest.api.BaseDto;
import pl.touk.widerest.api.common.AddressDto;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonRootName("fulfillment")
@ApiModel(value = "Fulfillment", description = "Fulfillment Group DTO resource representation")
public class FulfillmentDto extends BaseDto {

    @ApiModelProperty(position = 0, value = "Fulfillment type", readOnly = true)
    private String type;

    @ApiModelProperty(position = 1, value = "Address related to a fulfillment group", required = true)
    @Valid
    private AddressDto address;

    @ApiModelProperty(position = 3, value = "List of items belonging to a fulfillment group", required = true)
    @NotEmpty
    @EachURL
    private List<String> itemHrefs;

    @ApiModelProperty(value = "The selected fulfillment option", required = true)
    private String selectedFulfillmentOption;

    @ApiModelProperty(value = "Price to charge for fulfillment", readOnly = true)
    private BigDecimal fulfillmentPrice;

    @ApiModelProperty(value = "Available options for fulfillment", readOnly = true)
    private Map<String, FulfillmentOptionDto> fulfillmentOptions;


}
