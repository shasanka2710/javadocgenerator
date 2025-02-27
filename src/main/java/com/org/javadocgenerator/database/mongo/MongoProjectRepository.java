package com.org.javadocgenerator.database.mongo;

import com.org.javadocgenerator.database.mongo.model.Project;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoProjectRepository extends MongoRepository<Project, String> {
}
