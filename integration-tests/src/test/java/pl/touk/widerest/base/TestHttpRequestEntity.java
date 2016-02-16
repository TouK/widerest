package pl.touk.widerest.base;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

public abstract class TestHttpRequestEntity {

    protected final HttpHeaders httpRequestHeaders = new HttpHeaders();
    protected HttpEntity<String> httpRequestEntity;

    public HttpEntity<String> getTestHttpRequestEntity() {
        if(httpRequestEntity == null) {
            httpRequestEntity = new HttpEntity<>(httpRequestHeaders);
        }

        return httpRequestEntity;
    }
}
