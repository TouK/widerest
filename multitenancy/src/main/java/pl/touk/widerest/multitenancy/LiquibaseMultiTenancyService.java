package pl.touk.widerest.multitenancy;

import liquibase.database.Database;
import liquibase.database.ObjectQuotingStrategy;
import liquibase.database.core.HsqlDatabase;
import liquibase.exception.DatabaseException;
import liquibase.integration.spring.MultiTenantSpringLiquibase;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.resource.ResourceAccessor;
import liquibase.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.util.Optional;

@Slf4j
public class LiquibaseMultiTenancyService extends MultiTenancyService implements ResourceLoaderAware {

    @Autowired
    private MultiTenantSpringLiquibase multiTenantSpringLiquibase;

    private ResourceLoader resourceLoader;

    @Override
    @Transactional
    public void createTenantSchema(String tenantIdentifier, Optional<TenantRequest> tenantDetails) throws Exception {
        Connection connection = privilegedDataSource.getConnection();
        try {
            log.info("Creating database schema for tenant {}", tenantIdentifier);
            if (!MultiTenancyConfig.DEFAULT_TENANT_IDENTIFIER.equals(tenantIdentifier)) {
                connection.createStatement().execute("CREATE SCHEMA \"" + MultiTenancyConfig.TENANT_SCHEMA_PREFIX + tenantIdentifier + "\"");
                connection.setSchema(MultiTenancyConfig.TENANT_SCHEMA_PREFIX + tenantIdentifier);
            }

            SpringLiquibase liquibase = new SpringLiquibase() {

                @Override
                protected Database createDatabase(Connection c, ResourceAccessor resourceAccessor) throws DatabaseException {
                    Database database = super.createDatabase(c, resourceAccessor);
                    // An ugly hack to make Hsql based tests work
                    if (database instanceof HsqlDatabase) {
                        database.setObjectQuotingStrategy(ObjectQuotingStrategy.QUOTE_ALL_OBJECTS);
                        if (StringUtils.trimToNull(this.defaultSchema) != null) {
                            database.setDefaultSchemaName(this.defaultSchema);
                        }
                    }
                    return database;
                }
            };
            liquibase.setChangeLog(multiTenantSpringLiquibase.getChangeLog());
            liquibase.setContexts(multiTenantSpringLiquibase.getContexts());
            liquibase.setDropFirst(multiTenantSpringLiquibase.isDropFirst());
            liquibase.setShouldRun(multiTenantSpringLiquibase.isShouldRun());
            liquibase.setDataSource(this.privilegedDataSource);
            liquibase.setResourceLoader(this.resourceLoader);
            liquibase.setDefaultSchema(MultiTenancyConfig.TENANT_SCHEMA_PREFIX + tenantIdentifier);

            liquibase.afterPropertiesSet();

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

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
