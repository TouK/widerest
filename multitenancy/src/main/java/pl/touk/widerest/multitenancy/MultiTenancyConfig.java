package pl.touk.widerest.multitenancy;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.security.jwt.crypto.sign.MacSigner;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class MultiTenancyConfig {

    @Value("${mulititenacy.tokenSecret:secret}")
    private String tenantTokenSecret;

//    @Bean
    public LocalSessionFactoryBean mySessionFactory(DataSource dataSource) {
        LocalSessionFactoryBean localSessionFactoryBean = new LocalSessionFactoryBean();
        localSessionFactoryBean.setDataSource(dataSource);
        Properties properties = new Properties();
        properties.setProperty(AvailableSettings.DIALECT, "org.hibernate.dialect.HSQLDialect");
        properties.setProperty(AvailableSettings.HBM2DDL_AUTO, "create");
        localSessionFactoryBean.setHibernateProperties(properties);
        return localSessionFactoryBean;
    }

    @Bean
    public MacSigner macSigner() {
        return new MacSigner(tenantTokenSecret);
    }

}
