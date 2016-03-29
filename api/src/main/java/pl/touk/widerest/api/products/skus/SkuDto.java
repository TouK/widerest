package pl.touk.widerest.api.products.skus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
import pl.touk.widerest.api.BaseDto;
import pl.touk.widerest.api.common.MediaDto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonRootName("sku")
@ApiModel(value = "Sku", description = "SKU DTO resource representation")
public class SkuDto extends BaseDto {

    @NotBlank(message = "SKU has to have a non empty name")
    @ApiModelProperty(position = 0, value = "SKU name", required = true, dataType = "java.lang.String")
    private String name;

    @ApiModelProperty(position = 1, value = "Description of the SKU", dataType = "java.lang.String")
    private String description;

    @ApiModelProperty(position = 2, value = "Sale price of the SKU", required = true, dataType = "java.math.BigDecimal")
    @DecimalMin("0.01")
    private BigDecimal salePrice;

    @ApiModelProperty(position = 3, value = "Retail price of the SKU", dataType = "java.math.BigDecimal")
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal retailPrice;

    @NotNull(message = "Sku has to have available quantity set")
    @ApiModelProperty(position = 4, value = "Available quantity of the SKU", required = true, dataType = "java.lang.Integer")
    private Integer quantityAvailable;

    @ApiModelProperty(position = 5, value = "SKU's availability", dataType = "java.lang.String")
    private String availability;

    @ApiModelProperty(position = 6, value = "Sale/retail prices currency", required = true, dataType = "java.lang.String")
    private String currencyCode;

    @ApiModelProperty(position = 7, value = "Tax code for the SKU", dataType = "java.lang.String")
    private String taxCode;

    @NotNull(message = "Sku has to have an Active Start Date set")
    @ApiModelProperty(position = 8, value = "Date from which the SKU becomes active", required = true, dataType = "java.util.Date")
    private Date activeStartDate;

    @ApiModelProperty(position = 9, value = "Date from which the SKU becomes inactive", dataType = "java.util.Date")
    private Date activeEndDate;

    @ApiModelProperty(position = 10, value = "Attributes associated with the SKU")
    private Map<String, String> skuAttributes;

    @ApiModelProperty(position = 11, value = "A set of selected product's option values", dataType = "java.util.Set")
    private Set<SkuProductOptionValueDto> skuProductOptionValues;

    @ApiModelProperty(position = 12, value = "List of medias associated with the SKU", dataType = "java.util.List")
    private Map<String, MediaDto> media;
}