package pl.touk.widerest.api.cart.dto;

import com.wordnik.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Builder;

import java.util.Map;

/**
 * Created by mst on 07.07.15.
 */
@Data
@Builder
@ApiModel
public class PaymentInfoDto {
    private String paymentUrl;

    private Map<String, String> parameters;

    private Long orderId;


}
