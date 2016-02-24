package pl.touk.widerest.api.cart.orders.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
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

    private AddressDto address;

    private List<String> items;
}
