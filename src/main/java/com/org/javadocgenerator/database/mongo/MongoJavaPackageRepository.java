package com.org.javadocgenerator.database.mongo;

import com.org.javadocgenerator.database.mongo.model.Package;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MongoJavaPackageRepository extends MongoRepository<Package, String> {
    List<Package> findByProjectId(String projectId);
}
