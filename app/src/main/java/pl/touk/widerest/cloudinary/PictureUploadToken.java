package pl.touk.widerest.cloudinary;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApiModel
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PictureUploadToken {

    private int timestamp;

    private UUID publicId;

    private String apiKey;

    private String signature;

    @Override
    public String toString() {
        return Arrays.asList(timestamp, publicId, apiKey, signature).stream()
                .map(i -> Optional.ofNullable(i)
                        .map(o -> o.toString())
                        .orElse(""))
                .collect(Collectors.joining(","));
    }
}
