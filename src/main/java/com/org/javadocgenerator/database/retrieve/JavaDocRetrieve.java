package com.org.javadocgenerator.database.retrieve;

import com.org.javadocgenerator.database.mongo.model.JavaClass;
import com.org.javadocgenerator.database.mongo.model.JavaMethod;
import com.org.javadocgenerator.database.mongo.model.Package;

import java.util.List;

public interface JavaDocRetrieve {
    List<Package> getPackages();
    List<JavaClass> getClasses(String packageName);
    List<JavaClass> findAllClasses();
    JavaClass getClassDetails(String packageName,String className);
}
