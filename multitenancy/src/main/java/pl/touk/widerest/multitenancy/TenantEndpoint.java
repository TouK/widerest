package pl.touk.widerest.multitenancy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.DefaultNamingStrategy;
import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.internal.SessionImpl;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.orm.jpa.hibernate.SpringNamingStrategy;
import org.springframework.orm.jpa.EntityManagerFactoryAccessor;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.MacSigner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.validation.Valid;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/tenant")
@Slf4j
@Api(value = "tenants", description = "Tenants registration endpoint")
public class TenantEndpoint extends EntityManagerFactoryAccessor {

    // It should have the create schema permission
    @Resource
    private DataSource privilegedDataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Resource
    private MacSigner signerVerifier;

    @Resource
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private TenantAdminService tenantAdminService;

    @Autowired
    @Override
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        super.setEntityManagerFactory(emf);
    }

    @RequestMapping(method = RequestMethod.POST)
    public String create(@ApiParam @RequestBody @Valid TenantRequest tenantRequest, HttpServletRequest request) throws SQLException, JsonProcessingException {
        EntityManager em = createEntityManager();
        try {
            String tenantIdentifier = em.unwrap(Session.class).getTenantIdentifier();
            if (StringUtils.isEmpty(tenantIdentifier)) {
                tenantIdentifier = RandomStringUtils.randomAlphabetic(16).toUpperCase();
            }
            Tenant tenant = Tenant.builder().id(tenantIdentifier).subscriptionType("free").build();
            request.setAttribute(CurrentTenantIdentifierResolverImpl.TENANT_ATTRIBUTE, tenant);
            createSchema(em, tenantIdentifier);
            createAdminUser(tenantRequest.getAdminEmail(), tenantRequest.getAdminPassword());
            return JwtHelper.encode(objectMapper.writeValueAsString(tenant), signerVerifier).getEncoded();
        } finally {
            EntityManagerFactoryUtils.closeEntityManager(em);
        }
    }

    private void createAdminUser(String adminEmail, String adminPassword) {
        Optional.ofNullable(tenantAdminService).ifPresent(s -> s.createAdminUser(adminEmail, adminPassword));
    }

    @RequestMapping(method = RequestMethod.GET)
    public String read(HttpServletRequest request) throws SQLException, JsonProcessingException {
        Tenant tenant = (Tenant) request.getAttribute(CurrentTenantIdentifierResolverImpl.TENANT_ATTRIBUTE);
        String tenantIdentifier = tenant.getId();
        checkSchema(tenantIdentifier);
        return JwtHelper.encode(objectMapper.writeValueAsString(tenant), signerVerifier).getEncoded();
    }

    private void createSchema(EntityManager em, String tenantIdentifier) throws SQLException {
        Connection connection = privilegedDataSource.getConnection();
        try {
            log.info("Creating database schema for tenant {}", tenantIdentifier);
            connection.createStatement().execute("CREATE SCHEMA " + tenantIdentifier);
            connection.setSchema(tenantIdentifier);

            Configuration configuration = new Configuration();
            configuration.setProperty(AvailableSettings.DIALECT, em.unwrap(SessionImpl.class).getSessionFactory().getDialect().toString());
            configuration.setProperty(AvailableSettings.HBM2DDL_AUTO, "create");
            configuration.setProperty(AvailableSettings.SHOW_SQL, "true");
            configuration.setProperty(AvailableSettings.DEFAULT_SCHEMA, tenantIdentifier);
            configuration.setNamingStrategy(new EJB3NamingStrategy());
            Optional.ofNullable(em.getEntityManagerFactory().getProperties().get("hibernate.ejb.naming_strategy"))
                    .map(String.class::cast)
                    .ifPresent(namingStrategy -> {
                        try {
                            configuration.setNamingStrategy((NamingStrategy) Class.forName(namingStrategy).newInstance());
                        } catch (Exception e) {
                            log.error("Could not instantiate naming strategy");
                        }
                    });

            Metamodel metamodel = em.getMetamodel();
            Set<EntityType<?>> entities = metamodel.getEntities();
            entities.stream()
                    .map(EntityType::getBindableJavaType)
                    .forEach(configuration::addAnnotatedClass);
            configuration.buildMappings();

            SchemaExport schemaUpdate = new SchemaExport(configuration, connection);
            schemaUpdate.execute(true, true, false, false);
            log.info("Database schema for tenant {} created", tenantIdentifier);
        } catch (Throwable ex) {
            log.error("Problem creating database schema for tenant {}", tenantIdentifier);
            throw ex;
        } finally {
            connection.close();
        }
    }

    @Transactional
    private void checkSchema(String tenantIdentifier) {
        // Open a transaction just to see if there is a database schema for the tenant identifier
        // resolved by CurrentTenantIdentifierResolver
        TransactionStatus transaction = transactionManager.getTransaction(new DefaultTransactionDefinition());
        transactionManager.rollback(transaction);
    }

}
