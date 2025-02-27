package com.org.javadocgenerator.database.mongo;

import com.org.javadocgenerator.database.mongo.model.JavaClass;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MongoJavaClassRepository extends MongoRepository<JavaClass, String> {
    List<JavaClass> findByPackageName(String packageName);

    JavaClass findByPackageNameAndClassName(String packageName, String className);
}
