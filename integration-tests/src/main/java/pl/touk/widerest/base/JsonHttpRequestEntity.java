package pl.touk.widerest.base;

import org.springframework.http.MediaType;

public class JsonHttpRequestEntity extends TestHttpRequestEntity {
    public JsonHttpRequestEntity() {
        super();
        httpRequestHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
    }
}
