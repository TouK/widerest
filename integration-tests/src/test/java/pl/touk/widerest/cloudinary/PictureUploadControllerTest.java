package pl.touk.widerest.cloudinary;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpServerErrorException;
import pl.touk.widerest.Application;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.security.oauth2.Scope;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringApplicationConfiguration(classes = Application.class)
public class PictureUploadControllerTest extends ApiTestBase {

    @Test
    public void shouldGetToken() throws PictureUploadAuthorizationException, IOException {
        //given
        whenLoggedIn("backoffice","admin", "admin");
        whenAuthorizationRequestedFor(Scope.STAFF);

        final String KEY = "cloudinaryKey";
        final String KEY_VALUE = "key";

        final String SECRET = "cloudinarySecret";
        final String SECRET_VALUE = "secret";

        oAuth2RestTemplate.put(SETTINGS_BY_NAME_URL, KEY_VALUE, serverPort, KEY);
        oAuth2RestTemplate.put(SETTINGS_BY_NAME_URL, SECRET_VALUE, serverPort, SECRET);

        final ResponseEntity<PictureUploadToken> recivedTokenEntity = oAuth2RestTemplate.getForEntity(PICTURE_UPLOAD_TOKEN_URL, PictureUploadToken.class, serverPort);
        final PictureUploadToken token = recivedTokenEntity.getBody();

        Assert.assertTrue(recivedTokenEntity.getStatusCode().is2xxSuccessful());
        assertThat(token.getApiKey(), equalTo(KEY_VALUE));
        assertThat(token.getTimestamp(), not(0));
        assertThat(token.getSignature(), not(""));
        assertThat(token.getSignature(), notNullValue());
    }

    @Test(expected = HttpServerErrorException.class)
    public void shouldThrowExceptionOnEmptyCredentials() throws PictureUploadAuthorizationException, IOException {
        whenLoggedIn("backoffice","admin", "admin");
        whenAuthorizationRequestedFor(Scope.STAFF);

        final ResponseEntity<PictureUploadToken> recivedTokenEntity = oAuth2RestTemplate.getForEntity(PICTURE_UPLOAD_TOKEN_URL, PictureUploadToken.class, serverPort);
    }

}
