package com.org.javadocgenerator.database.mongo.model;

import jakarta.persistence.Entity;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "classes")
public class JavaClass {
    @Id
    private String id;
    private String projectId;
    private String packageName;
    private String className;
    private boolean isInterface;
    private boolean isAbstract;
    private String visibility;
    private String extendsClass;
    private List<String> implementsInterfaces;
    private List<String> annotations;
    private String javadoc;
    private List<String> comments;
    private List<JavaField> fields;
    private List<JavaConstructor> constructors;
    private List<JavaMethod> methods;
    private String staticBlocks;
}
