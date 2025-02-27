package com.org.javadocgenerator.database.mongo;

import com.org.javadocgenerator.database.mongo.model.JavaMethod;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MongoJavaMethodRepository extends MongoRepository<JavaMethod, String> {
    List<JavaMethod> findByPackageNameAndClassName(String packageName,String className);
}