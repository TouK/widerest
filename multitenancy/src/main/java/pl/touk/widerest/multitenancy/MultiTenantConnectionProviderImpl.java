package pl.touk.widerest.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.config.spi.ConfigurationService;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
public class MultiTenantConnectionProviderImpl implements MultiTenantConnectionProvider, ServiceRegistryAwareService {

    private DataSource dataSource;

    @Override
    public Connection getAnyConnection() throws SQLException {
        log.debug("Getting any connection");
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        log.debug("Releasing any connection");
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        log.debug("Getting connection for tenant: {}", tenantIdentifier);
        Connection connection = dataSource.getConnection();
        connection.setSchema(
                Optional.ofNullable(tenantIdentifier)
                        .filter(StringUtils::isNotEmpty)
                        .filter(Predicate.isEqual(MultiTenancyConfig.DEFAULT_TENANT_IDENTIFIER).negate())
                        .map(MultiTenancyConfig.TENANT_SCHEMA_PREFIX::concat)
                        .orElse(null)
        );
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        log.debug("Releasing {} connection", tenantIdentifier);
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return true;
    }

    @Override
    public boolean isUnwrappableAs(Class unwrapType) {
        return ConnectionProvider.class.equals( unwrapType ) ||
                MultiTenantConnectionProviderImpl.class.isAssignableFrom( unwrapType ) ||
                DataSource.class.isAssignableFrom( unwrapType );
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        if ( ConnectionProvider.class.equals( unwrapType ) ||
                MultiTenantConnectionProviderImpl.class.isAssignableFrom( unwrapType ) ) {
            return (T) this;
        }
        else if ( DataSource.class.isAssignableFrom( unwrapType ) ) {
            return (T) dataSource;
        }
        else {
            throw new UnknownUnwrapTypeException( unwrapType );
        }
    }

    @Override
    public void injectServices(ServiceRegistryImplementor serviceRegistry) {
        Map lSettings = serviceRegistry.getService(ConfigurationService.class).getSettings();
        DataSource localDs = (DataSource) lSettings.get("hibernate.connection.datasource");
        dataSource = localDs;
    }
}
