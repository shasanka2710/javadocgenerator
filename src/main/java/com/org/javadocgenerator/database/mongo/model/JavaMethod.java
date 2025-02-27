package com.org.javadocgenerator.database.mongo.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "methods")
public class JavaMethod {
    @Id
    private String id;
    private String classId;
    private String className;
    private String packageName;
    private String methodName;
    private String returnType;
    private String visibility;
    private List<Parameter> parameters;
    private List<String> annotations;
    private List<String> throwsExceptions;
    private List<String> calls;
    private List<String> calledBy;
    private String javadoc;
    private List<String> comments;
    private boolean isStatic;
    private boolean isFinal;

    @Data
    public static class Parameter {
        private String name;
        private String type;
    }
}
