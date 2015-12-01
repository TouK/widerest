package pl.touk.widerest.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import pl.touk.widerest.multitenancy.sample.SampleApplication;
import pl.touk.widerest.multitenancy.sample.SampleEntity;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleApplication.class)
@WebIntegrationTest({
        "server.port:0"
})
@Slf4j
public class MultiTenancyTest {

    @Value("${local.server.port}")
    protected String serverPort;

    @Resource
    protected DataSource dataSource;

    @Autowired
    private Consumer<TenantRequest> setTenantDetails;

    @Resource
    private IdentifierTool identifierTool;

    TestRestTemplate noTenantRestTemplate = new TestRestTemplate();

    @Test
    public void shouldCreateTenantWithoutTenantIdentifier() throws SQLException, IOException {

        Mockito.reset(setTenantDetails);

        // when new tenant requested
        TenantRequest tenantDetails = TenantRequest.builder().adminPassword("test").adminEmail("test@test.xx").build();
        String tenantIdentifier = noTenantRestTemplate.postForObject("http://localhost:{serverPort}/tenant", tenantDetails, String.class, serverPort);

        // then valid tenant token returned
        verifyTenantIdentifier(tenantIdentifier);

        // then database schema created
        Connection connection = dataSource.getConnection();
        try {
            connection.setSchema(MultiTenancyConfig.TENANT_SCHEMA_PREFIX + tenantIdentifier);
        } finally {
            connection.close();
        }
        Mockito.verify(setTenantDetails, Mockito.times(1)).accept(Mockito.eq(tenantDetails));
    }

    @Test
    public void shouldReadTenantWithValidTenantIdentifier() {

        // given existing tenant
        String tenantIdentifier = noTenantRestTemplate.postForObject("http://localhost:{serverPort}/tenant", TenantRequest.builder().adminPassword("test").adminEmail("test@test.xx").build(), String.class, serverPort);
        TestRestTemplate tenantRestTemplate = createTenantRestTemplate(tenantIdentifier);

        // when tenant requested
        ResponseEntity<List> responseEntity = tenantRestTemplate.getForEntity("http://localhost:{serverPort}/tenant", List.class, serverPort);

        // then tokens equal
        Assert.assertEquals(tenantIdentifier, responseEntity.getBody().get(0));

    }

    @Test
    public void shouldReturnDefaultTenantWhithoutTenantIdentifier() {
        // when tenant requested without tenant identifier
        ResponseEntity<List> responseEntity = noTenantRestTemplate.getForEntity("http://localhost:{serverPort}/tenant", List.class, serverPort);

        // then responded with error
        Assert.assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
        Assert.assertTrue(responseEntity.getBody().contains(MultiTenancyConfig.DEFAULT_TENANT_IDENTIFIER));
    }

    @Test
    public void shouldFailReadingTenantWithInvalidTenantIdentifier() {

        // when tenant requested with invalid identifier
        TestRestTemplate restTemplate = createTenantRestTemplate(UUID.randomUUID().toString());
        ResponseEntity<String> responseEntity = restTemplate.getForEntity("http://localhost:{serverPort}/tenant", String.class, serverPort);

        // then responded with error
        Assert.assertTrue(responseEntity.getStatusCode().toString(), responseEntity.getStatusCode().is4xxClientError());
    }

    @Test
    public void shouldFailCreatingTenantWithInvalidIdentifer() throws SQLException {

        // when new tenant requested with invalid identifier
        TestRestTemplate restTemplate = createTenantRestTemplate("INVALID");
        TenantRequest tenantDetails = TenantRequest.builder().adminPassword("test").adminEmail("test@test.xx").build();
        ResponseEntity<String> responseEntity = restTemplate.postForEntity("http://localhost:{serverPort}/tenant", tenantDetails, String.class, serverPort);

        // then responded with error
        Assert.assertTrue(responseEntity.getStatusCode().is4xxClientError());

        // then no database schema created
        Connection connection = dataSource.getConnection();
        try {
            connection.setSchema(MultiTenancyConfig.TENANT_SCHEMA_PREFIX + "INVALID");
        } catch (SQLException ex) {
            Assert.assertEquals("invalid schema name: " + MultiTenancyConfig.TENANT_SCHEMA_PREFIX + "INVALID", ex.getMessage());

        }finally {
            connection.close();
        }

    }

    @Test
    public void shouldCreateSampleEntityForTenant() throws SQLException {

        // given two tenants
        String tenantIdentifier1 = noTenantRestTemplate.postForObject("http://localhost:{serverPort}/tenant", TenantRequest.builder().adminPassword("test").adminEmail("test@test.xx").build(), String.class, serverPort);
        TestRestTemplate tenant1RestTemplate = createTenantRestTemplate(tenantIdentifier1);
        String tenantIdentifier2 = noTenantRestTemplate.postForObject("http://localhost:{serverPort}/tenant", TenantRequest.builder().adminPassword("test").adminEmail("test@test.xx").build(), String.class, serverPort);
        TestRestTemplate tenant2RestTemplate = createTenantRestTemplate(tenantIdentifier2);

        ResponseEntity<Object> responseEntity;

        // when new sample entity requested for the first tenant
        responseEntity = tenant1RestTemplate.postForEntity("http://localhost:{serverPort}/samples", SampleEntity.builder().value("value1").build(), Object.class, serverPort);
        URI sampleEntityLocation = responseEntity.getHeaders().getLocation();

        // then the sample entity is created
        Assert.assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
        responseEntity = tenant1RestTemplate.getForEntity(sampleEntityLocation, Object.class);
        Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        // when the same location read for the second tenant
        responseEntity = tenant2RestTemplate.getForEntity(sampleEntityLocation, Object.class);

        // then no sample entity found
        Assert.assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());

    }

    @Test
    public void shouldCreateSampleEntityInDefaultSchemaWhenNoTenantTokenGiven() throws SQLException {

        // when new sample entity requested without any tenant token
        ResponseEntity<Object> responseEntity = noTenantRestTemplate.postForEntity("http://localhost:{serverPort}/samples", SampleEntity.builder().value("value1").build(), Object.class, serverPort);
        URI sampleEntityLocation = responseEntity.getHeaders().getLocation();

        // then the sample entity is created
        Assert.assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
        responseEntity = noTenantRestTemplate.getForEntity(sampleEntityLocation, Object.class);
        Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    }

    private void verifyTenantIdentifier(String tenantToken) throws IOException {
        identifierTool.verifyIdentifier(tenantToken);
    }

    private TestRestTemplate createTenantRestTemplate(String tenantIdentifier) {
        TestRestTemplate restTemplate = new TestRestTemplate();
        restTemplate.setInterceptors(Arrays.asList(new TenantHeaderInterceptor(tenantIdentifier)));
        return restTemplate;
    }

    private static class TenantHeaderInterceptor implements ClientHttpRequestInterceptor {

        private String tenantIdentifier;

        public TenantHeaderInterceptor(String tenantIdentifier) {
            this.tenantIdentifier = tenantIdentifier;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            request.getHeaders().add(TenantHeaderRequestFilter.TENANT_TOKEN_HEADER, tenantIdentifier);
            return execution.execute(request, body);
        }
    }

}

