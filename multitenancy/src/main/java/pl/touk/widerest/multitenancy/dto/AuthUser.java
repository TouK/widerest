package pl.touk.widerest.multitenancy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.experimental.Builder;

import java.util.List;

/**
 * Created by mst on 16.09.15.
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthUser {

    private String email;
    private String username;

    @JsonProperty("user_metadata")
    private List<String> tokens;

}
