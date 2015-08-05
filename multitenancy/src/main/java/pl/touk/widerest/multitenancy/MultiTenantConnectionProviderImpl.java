package pl.touk.widerest.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.config.spi.ConfigurationService;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

@Slf4j
public class MultiTenantConnectionProviderImpl implements MultiTenantConnectionProvider, ServiceRegistryAwareService {

    private DataSource dataSource;

    private ServiceRegistryImplementor serviceRegistry;

    @Override
    public Connection getAnyConnection() throws SQLException {
        ConnectionProvider service = serviceRegistry.getService(ConnectionProvider.class);
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();

    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        statement.execute("SET SCHEMA " + tenantIdentifier);
        statement.close();
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
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
        this.serviceRegistry = serviceRegistry;
        Map lSettings = serviceRegistry.getService(ConfigurationService.class).getSettings();
        DataSource localDs = (DataSource) lSettings.get("hibernate.connection.datasource");
        dataSource = localDs;

        ConnectionProvider connectionProvider = serviceRegistry.getService(ConnectionProvider.class);
    }
}
