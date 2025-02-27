package com.org.javadocgenerator.database.storage;

import com.org.javadocgenerator.database.mongo.model.JavaClass;
import com.org.javadocgenerator.database.mongo.model.JavaMethod;
import com.org.javadocgenerator.database.mongo.model.Package;
import com.org.javadocgenerator.database.mongo.model.Project;


public interface JavaDocStorage {
    void saveProject(Project project);
    void savePackage(Package pkg);
    void saveClass(JavaClass javaClass);
    void saveMethod(JavaMethod javaMethod);


}
