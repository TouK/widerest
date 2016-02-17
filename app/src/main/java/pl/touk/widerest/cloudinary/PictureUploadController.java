package pl.touk.widerest.cloudinary;

import com.cloudinary.Cloudinary;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import pl.touk.widerest.api.settings.SettingsConsumer;
import pl.touk.widerest.api.settings.SettingsService;
import pl.touk.widerest.security.config.ResourceServerConfig;
import javax.annotation.Resource;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping(value = ResourceServerConfig.API_PATH + "/pictureUpload")
@Api(value = "pictureUpload", description = "Upload authorization token endpoint")
public class PictureUploadController implements SettingsConsumer {

    public static final String CLOUDINARY_API_KEY = "cloudinaryKey";
    public static final String CLOUDINARY_API_SECRET = "cloudinarySecret";

    @Resource
    private SettingsService service;

    private final Cloudinary cloudinary = new Cloudinary();

    @ApiOperation(value = "Get token", response = PictureUploadToken.class)
    @RequestMapping(value = "requestToken", method = RequestMethod.GET)
    @ResponseBody
    public PictureUploadToken getToken() throws PictureUploadAuthorizationException {
        return authorizePictureUpload();
    }

    @Override
    public void setSettingsService(SettingsService settingsService) {
        this.service = settingsService;
    }

    @Override
    public Set<String> getHandledProperties() {
        return Sets.newHashSet(CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET);
    }

    private PictureUploadToken authorizePictureUpload() throws PictureUploadAuthorizationException{

        final UUID publicId = UUID.randomUUID();
        final int timestamp = (int) (System.currentTimeMillis() / 1000L);

        final Optional<String> secret = service.getProperty(CLOUDINARY_API_SECRET);
        final Optional<String> key = service.getProperty(CLOUDINARY_API_KEY);

        if(secret.isPresent() && key.isPresent()) {
            final String signature = requestSignature(publicId, timestamp, secret.get());

            return PictureUploadToken.builder()
                    .apiKey(key.get())
                    .publicId(publicId)
                    .timestamp(timestamp)
                    .signature(signature)
                    .build();
        }
        throw new PictureUploadAuthorizationException("Authorization failed");
    }

    private String requestSignature(final UUID publicId, final int timestamp, final String secret) {
        return cloudinary.apiSignRequest(
                ImmutableMap.of(
                        "public_id", publicId,
                        "timestamp", timestamp),
                secret);
    }
}
