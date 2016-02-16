package pl.touk.widerest.base;

import org.springframework.http.MediaType;

public class HalHttpRequestEntity extends TestHttpRequestEntity {
    public HalHttpRequestEntity() {
        super();
        httpRequestHeaders.set("Accept", "application/hal+json");
    }
}
