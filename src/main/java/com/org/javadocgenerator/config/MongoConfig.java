package com.org.javadocgenerator.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.org.javadocgenerator.database.mongo")
@ConditionalOnProperty(name = "storage.type", havingValue = "mongo")
public class MongoConfig {
}
