package pl.touk.widerest.base;

import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class HttpHeadersWithTokenFactory {

    private final HttpHeaders jsonHttpHeadersWithToken;
    private final HttpHeaders halHttpHeadersWithToken;

    public HttpHeadersWithTokenFactory() {
        halHttpHeadersWithToken = new HttpHeaders();
        halHttpHeadersWithToken.set("Accept", MediaTypes.HAL_JSON_VALUE);

        jsonHttpHeadersWithToken = new HttpHeaders();
        jsonHttpHeadersWithToken.set("Accept", MediaType.APPLICATION_JSON_VALUE);
    }

    public HttpHeaders getJsonHttpHeadersWithToken(final String token) {
        setAuthorizationToken(jsonHttpHeadersWithToken, token);
        return jsonHttpHeadersWithToken;
    }

    public HttpHeaders getHalHttpHeadersWithToken(final String token) {
        setAuthorizationToken(halHttpHeadersWithToken, token);
        return halHttpHeadersWithToken;
    }

    private void setAuthorizationToken(final HttpHeaders httpHeaders, final String token) {
        httpHeaders.set("Authorization", "Bearer " + token);
    }
}
