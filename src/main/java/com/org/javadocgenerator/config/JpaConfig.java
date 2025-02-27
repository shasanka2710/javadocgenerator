package com.org.javadocgenerator.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.org.javadocgenerator.database.h2")
@ConditionalOnProperty(name = "storage.type", havingValue = "h2")
public class JpaConfig {
}
