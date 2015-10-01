package pl.touk.widerest.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.internal.SessionImpl;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import java.sql.Connection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class SchemaExportMultiTenancyService extends MultiTenancyService {

    protected EntityManager em;

    @PostConstruct
    public void initialize() throws Exception {
        em = createEntityManager();
        if (!checkIfTenantSchemaExists(MultiTenancyConfig.DEFAULT_TENANT_IDENTIFIER)) {
            createTenantSchema(MultiTenancyConfig.DEFAULT_TENANT_IDENTIFIER, Optional.empty());
        }
    }

    @Override
    public void createTenantSchema(String tenantIdentifier, Optional<TenantRequest> tenantDetails) throws Exception {
        Connection connection = privilegedDataSource.getConnection();
        try {
            log.info("Creating database schema for tenant {}", MultiTenancyConfig.TENANT_SCHEMA_PREFIX + tenantIdentifier);
            if (!MultiTenancyConfig.DEFAULT_TENANT_IDENTIFIER.equals(tenantIdentifier)) {
                connection.createStatement().execute("CREATE SCHEMA \"" + MultiTenancyConfig.TENANT_SCHEMA_PREFIX + tenantIdentifier + "\"");
                connection.setSchema(MultiTenancyConfig.TENANT_SCHEMA_PREFIX + tenantIdentifier);
            }

            Map<String, Object> properties = getEntityManagerFactory().getProperties().entrySet().stream()
                    .filter(e -> e.getKey().startsWith("hibernate.")).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            Configuration configuration = new Configuration();
            configuration.setProperty(AvailableSettings.DIALECT, em.unwrap(SessionImpl.class).getSessionFactory().getDialect().toString());
            configuration.setProperty(AvailableSettings.HBM2DDL_AUTO, "create");
            configuration.setProperty(AvailableSettings.SHOW_SQL, "true");

            Optional.ofNullable(properties.get(AvailableSettings.HBM2DDL_IMPORT_FILES))
                    .map(String.class::cast)
                    .ifPresent(importFiles -> configuration.setProperty(AvailableSettings.HBM2DDL_IMPORT_FILES, importFiles));
            Optional.ofNullable(properties.get("hibernate.ejb.naming_strategy"))
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
            schemaUpdate.setDelimiter(";");
            schemaUpdate.setFormat(true);
            schemaUpdate.setOutputFile("initial.sql");
            schemaUpdate.execute(true, true, false, false);

            tenantDetails.ifPresent(
                    Optional.ofNullable(setTenantDetails).orElseGet(() -> o -> {})
            );
            log.info("Database schema for tenant {} created", tenantIdentifier);
        } catch (Throwable ex) {
            log.error("Problem creating database schema for tenant {}", tenantIdentifier);
            throw ex;
        } finally {
            connection.close();
        }
    }

}
