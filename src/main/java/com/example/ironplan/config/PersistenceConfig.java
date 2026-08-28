package com.example.ironplan.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;

/**
 * OSIV como filtro servlet (antes de Spring Security) para que
 * {@code UserRepository.getReferenceById} en JwtAuthFilter no quede
 * con un EntityManager cerrado al llegar al controller.
 */
@Configuration
public class PersistenceConfig {

    @Bean
    public FilterRegistrationBean<OpenEntityManagerInViewFilter> openEntityManagerInViewFilter() {
        FilterRegistrationBean<OpenEntityManagerInViewFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new OpenEntityManagerInViewFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        bean.addUrlPatterns("/*");
        return bean;
    }
}
