package pl.touk.widerest.base;

import org.springframework.http.MediaType;

public class XmlHttpRequestEntity extends TestHttpRequestEntity {

    public XmlHttpRequestEntity() {
        super();
        httpRequestHeaders.set("Accept", MediaType.APPLICATION_XML_VALUE);
    }

}
