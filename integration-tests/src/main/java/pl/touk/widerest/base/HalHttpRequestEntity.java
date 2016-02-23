package pl.touk.widerest.base;

public class HalHttpRequestEntity extends TestHttpRequestEntity {
    public HalHttpRequestEntity() {
        super();
        httpRequestHeaders.set("Accept", "application/hal+json");
    }
}
