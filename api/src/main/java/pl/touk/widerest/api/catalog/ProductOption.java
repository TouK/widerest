package pl.touk.widerest.api.catalog;

import com.wordnik.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Builder;

import java.util.List;

@ApiModel
@Data
@Builder
public class ProductOption {

    private String name;

    private List<String> allowedValues;

}
