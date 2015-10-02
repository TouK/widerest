package pl.touk.widerest.multitenancy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Tenant {

    private String id;

    private String subscriptionType;

}
