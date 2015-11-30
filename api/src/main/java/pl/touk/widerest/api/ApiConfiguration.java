package pl.touk.widerest.api;

import org.broadleafcommerce.common.web.BroadleafRequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class ApiConfiguration extends WebMvcConfigurerAdapter {

    @Bean
    public BroadleafRequestInterceptor broadleafRequestInterceptor() {
        return new BroadleafRequestInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addWebRequestInterceptor(broadleafRequestInterceptor());
    }

}
