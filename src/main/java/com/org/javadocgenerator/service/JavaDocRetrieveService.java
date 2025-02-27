package com.org.javadocgenerator.service;

import com.org.javadocgenerator.database.retrieve.JavaDocRetrieve;
import com.org.javadocgenerator.database.mongo.model.JavaClass;
import com.org.javadocgenerator.database.mongo.model.JavaMethod;
import com.org.javadocgenerator.database.mongo.model.Package;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JavaDocRetrieveService {

    private final JavaDocRetrieve retrieve;

    public JavaDocRetrieveService(@Qualifier("mongoHandler") JavaDocRetrieve retrieve) {
        this.retrieve = retrieve;
    }

    public List<Package> getPackages() {
        return retrieve.getPackages();
    }

    public List<JavaClass> getClasses(String packageName) {
        return retrieve.getClasses(packageName);
    }

    public JavaClass getClassDetails(String packageName,String className) {
        return retrieve.getClassDetails(packageName,className);
    }
}
