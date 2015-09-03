package pl.touk.widerest.multitenancy;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.MacSigner;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;


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

    @Resource
    protected MacSigner signerVerifier;

    @Resource
    protected ObjectMapper objectMapper;

    @Autowired
    private TenantAdminService tenantAdminService;

    TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    public void shouldCreateTenantWhenNoTenantTokenHeader() throws SQLException, IOException {

        // when new tenant requested
        String tenantToken = restTemplate.postForObject("http://localhost:{serverPort}/tenant", TenantRequest.builder().adminPassword("test").adminEmail("test@test.xx").build(), String.class, serverPort);

        // then valid tenant token returned
        String tenantId = verifyTokenAndConvertToTenantId(tenantToken);

        // then database schema created
        Connection connection = dataSource.getConnection();
        try {
            connection.createStatement().execute("SET SCHEMA " + tenantId + "");
        } finally {
            connection.close();
        }
        Mockito.verify(tenantAdminService, Mockito.times(1)).createAdminUser(Mockito.eq("test@test.xx"), Mockito.eq("test"));
    }

    @Test
    public void shouldReadValidTokenForExistingTenant() {

        // given existing tenant
        String tenantToken = restTemplate.postForObject("http://localhost:{serverPort}/tenant", TenantRequest.builder().adminPassword("test").adminEmail("test@test.xx").build(), String.class, serverPort);

        // when tenant requested
        HttpHeaders headers = new HttpHeaders();
        headers.add(TenantHeaderRequestFilter.TENANT_TOKEN_HEADER, tenantToken);
        ResponseEntity<String> responseEntity = restTemplate.exchange("http://localhost:{serverPort}/tenant", HttpMethod.GET, new HttpEntity<Object>(headers), String.class, serverPort);

        // then tokens equal
        Assert.assertEquals(tenantToken, responseEntity.getBody());

    }

    @Test
    public void shouldFailReadingTenantWhenNoTokenGiven() {
        // when tenant requested without token header
        ResponseEntity<String> responseEntity = restTemplate.getForEntity("http://localhost:{serverPort}/tenant", String.class, serverPort);

        // then responded with error
        // TODO: change to a client error (4xx)
        Assert.assertTrue(responseEntity.getStatusCode().is5xxServerError());
    }

    @Test
    public void shouldFailReadingTenantWithInvalidToken() {
        // when tenant requested without token header
        HttpHeaders headers = new HttpHeaders();
        headers.add(TenantHeaderRequestFilter.TENANT_TOKEN_HEADER, createInvalidToken());
        ResponseEntity<String> responseEntity = restTemplate.exchange("http://localhost:{serverPort}/tenant", HttpMethod.GET, new HttpEntity<Object>(headers), String.class, serverPort);

        // then responded with error
        // TODO: change to a client error (4xx)
        Assert.assertTrue(responseEntity.getStatusCode().is5xxServerError());
    }

    @Test
    public void shouldFailCreatingTenantWithInvalidToken() throws SQLException {
        String tenantToken = createInvalidTokenForTenantId("INVALID");

        // when tenant requested without token header
        HttpHeaders headers = new HttpHeaders();
        headers.add(TenantHeaderRequestFilter.TENANT_TOKEN_HEADER, tenantToken);
        ResponseEntity<String> responseEntity = restTemplate.exchange("http://localhost:{serverPort}/tenant", HttpMethod.GET, new HttpEntity<Object>(headers), String.class, serverPort);

        // then responded with error
        // TODO: change to a client error (4xx)
        Assert.assertTrue(responseEntity.getStatusCode().is5xxServerError());

        // then no database schema created
        Connection connection = dataSource.getConnection();
        try {
            connection.createStatement().execute("SET SCHEMA INVALID");
        } catch (SQLException ex) {
            Assert.assertEquals("invalid schema name: INVALID", ex.getMessage());

        }finally {
            connection.close();
        }

    }

    @Test
    public void shouldCreateSampleEntityForTenant() throws SQLException {

        // given two tenants
        String tenantToken1 = restTemplate.postForObject("http://localhost:{serverPort}/tenant", TenantRequest.builder().adminPassword("test").adminEmail("test@test.xx").build(), String.class, serverPort);
        String tenantToken2 = restTemplate.postForObject("http://localhost:{serverPort}/tenant", TenantRequest.builder().adminPassword("test").adminEmail("test@test.xx").build(), String.class, serverPort);

        // when new sample entity requested for the first tenant
        HttpHeaders headers = new HttpHeaders();
        headers.set(TenantHeaderRequestFilter.TENANT_TOKEN_HEADER, tenantToken1);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Object> responseEntity = restTemplate.exchange("http://localhost:{serverPort}/samples", HttpMethod.POST, new HttpEntity<Object>("{ \"value\" : \"value1\" }", headers), Object.class, serverPort);
        URI sampleEntityLocation = responseEntity.getHeaders().getLocation();

        // then the sample entity is created
        Assert.assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
        responseEntity = restTemplate.exchange(sampleEntityLocation, HttpMethod.GET, new HttpEntity<Object>(headers), Object.class);
        Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        // when the same location read for the second tenant
        headers.set(TenantHeaderRequestFilter.TENANT_TOKEN_HEADER, tenantToken2);
        responseEntity = restTemplate.exchange(sampleEntityLocation, HttpMethod.GET, new HttpEntity<Object>(headers), Object.class);

        // then no sample entity found
        Assert.assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());

    }

    @Ignore("TODO")
    @Test
    public void shouldCreateSampleEntityInDefaultSchemaWhenNoTenantTokenGiven() throws SQLException {

        // when new sample entity requested without any tenant token
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Object> responseEntity = restTemplate.exchange("http://localhost:{serverPort}/samples", HttpMethod.POST, new HttpEntity<Object>("{ \"value\" : \"value1\" }", headers), Object.class, serverPort);
        URI sampleEntityLocation = responseEntity.getHeaders().getLocation();

        // then the sample entity is created
        Assert.assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
        responseEntity = restTemplate.exchange(sampleEntityLocation, HttpMethod.GET, new HttpEntity<Object>(headers), Object.class);
        Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    }

    private String verifyTokenAndConvertToTenantId(String tenantToken) throws IOException {
        return objectMapper.readValue(JwtHelper.decodeAndVerify(tenantToken, signerVerifier).getClaims(), Tenant.class).getId();
    }

    private String createInvalidToken() {
        return createInvalidTokenForTenantId("TEST");
    }

    private String createInvalidTokenForTenantId(String tenantId) {
        return JwtHelper.encode(tenantId, new MacSigner("invalid_secret")).getEncoded();
    }

}

