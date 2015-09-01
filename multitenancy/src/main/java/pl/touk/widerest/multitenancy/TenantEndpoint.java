package pl.touk.widerest.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.internal.SessionImpl;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.EntityManagerFactoryAccessor;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.MacSigner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/tenant")
@Slf4j
public class TenantEndpoint extends EntityManagerFactoryAccessor {

    // It should have the create schema permission
    @Resource
    private DataSource privilegedDataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Resource
    private MacSigner signerVerifier;

    @Autowired
    @Override
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        super.setEntityManagerFactory(emf);
    }

    @RequestMapping(method = RequestMethod.POST)
    public String create() throws SQLException {
        EntityManager em = createEntityManager();
        try {
            String tenantIdentifier = em.unwrap(Session.class).getTenantIdentifier();
            createSchema(em, tenantIdentifier);
            return JwtHelper.encode(tenantIdentifier, signerVerifier).getEncoded();
        } finally {
            EntityManagerFactoryUtils.closeEntityManager(em);
        }
    }

    @RequestMapping(method = RequestMethod.GET)
    public String read() throws SQLException {
        EntityManager em = createEntityManager();
        try {
            String tenantIdentifier = em.unwrap(Session.class).getTenantIdentifier();
            checkSchema(tenantIdentifier);
            return JwtHelper.encode(tenantIdentifier, signerVerifier).getEncoded();
        } finally {
            EntityManagerFactoryUtils.closeEntityManager(em);
        }
    }

    private void createSchema(EntityManager em, String tenantIdentifier) throws SQLException {
        Connection connection = privilegedDataSource.getConnection();
        try {
            log.info("Creating database schema for tenant {}", tenantIdentifier);
            connection.createStatement().execute("CREATE SCHEMA " + tenantIdentifier);
            connection.createStatement().execute("SET SCHEMA " + tenantIdentifier);

            Configuration configuration = new Configuration();
            configuration.setProperty(AvailableSettings.DIALECT, em.unwrap(SessionImpl.class).getSessionFactory().getDialect().toString());
            configuration.setProperty(AvailableSettings.HBM2DDL_AUTO, "create");
            configuration.setProperty(AvailableSettings.SHOW_SQL, "true");
            configuration.setProperty(AvailableSettings.DEFAULT_SCHEMA, tenantIdentifier);
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
