package com.org.javadocgenerator.database.storage;

import com.org.javadocgenerator.database.mongo.model.JavaClass;
import com.org.javadocgenerator.database.mongo.model.Package;
import com.org.javadocgenerator.database.mongo.model.Project;

import java.util.List;


public interface JavaDocStorage {
    void saveProject(Project project);
    void savePackage(Package pkg);
    void saveClass(JavaClass javaClass);
    void updateCalledBy(String methodName, List<String> calledByMethods);
}
