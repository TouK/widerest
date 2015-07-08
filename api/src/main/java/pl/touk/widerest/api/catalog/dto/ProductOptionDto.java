package pl.touk.widerest.api.catalog.dto;

import com.wordnik.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Builder;

import java.util.List;

/**
 * Created by mst on 06.07.15.
 */
@ApiModel
@Data
@Builder
public class ProductOptionDto {

    private String name;
    private List<String> allowedValues;
}
