package pl.touk.widerest.multitenancy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.EntityManagerFactoryAccessor;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class MultiTenancyService extends EntityManagerFactoryAccessor {
    // It should have the create schema permission
    @Resource
    protected DataSource privilegedDataSource;

    @Autowired(required = false)
    Consumer<TenantRequest> setTenantDetails;

    abstract void createTenantSchema(String tenantIdentifier, Optional<TenantRequest> tenantDetails) throws Exception;

    public boolean checkIfTenantSchemaExists(String tenantIdentifier) {
        try {
            return privilegedDataSource.getConnection().getMetaData().getSchemas(null, MultiTenancyConfig.TENANT_SCHEMA_PREFIX + tenantIdentifier).next();
        } catch (SQLException e) {
            return false;
        }
    }
}
