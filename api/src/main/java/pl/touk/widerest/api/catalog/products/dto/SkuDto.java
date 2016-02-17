package pl.touk.widerest.api.catalog.products.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Builder;
import pl.touk.widerest.api.BaseDto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonRootName("sku")
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "Sku", description = "SKU DTO resource representation")
public class SkuDto extends BaseDto {

    @ApiModelProperty(position = 0, value = "SKU name", required = true, dataType = "java.lang.String")
    private String name;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 1, value = "Description of the SKU", dataType = "java.lang.String")
    private String description;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 2, value = "Sale price of the SKU", required = true, dataType = "java.math.BigDecimal")
    private BigDecimal salePrice;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 3, value = "Retail price of the SKU", dataType = "java.math.BigDecimal")
    private BigDecimal retailPrice;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 4, value = "Available quantity of the SKU", required = true, dataType = "java.lang.Integer")
    private Integer quantityAvailable;

    @ApiModelProperty(position = 5, value = "SKU's availability", dataType = "java.lang.String")
    private String availability;

    @ApiModelProperty(position = 6, value = "Sale/retail prices currency", required = true, dataType = "java.lang.String")
    private String currencyCode;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 7, value = "Tax code for the SKU", dataType = "java.lang.String")
    private String taxCode;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 8, value = "Date from which the SKU becomes active", required = true, dataType = "java.util.Date")
    private Date activeStartDate;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 9, value = "Date from which the SKU becomes inactive", dataType = "java.util.Date")
    private Date activeEndDate;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 10, value = "Attributes associated with the SKU")
    private Map<String, String> skuAttributes;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 11, value = "A set of selected product's option values", dataType = "java.util.Set")
    private Set<SkuProductOptionValueDto> skuProductOptionValues;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 12, value = "List of medias associated with the SKU", dataType = "java.util.List")
    private Map<String /*key*/, MediaDto> skuMedia;
}