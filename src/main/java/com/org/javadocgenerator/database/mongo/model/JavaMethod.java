package com.org.javadocgenerator.database.mongo.model;

import lombok.Getter;
import lombok.Setter;


import java.util.List;

@Getter
@Setter
public class JavaMethod {
    private String className;
    private String packageName;
    private String methodName;
    private String returnType;
    private String visibility;
    private List<JavaParameter> parameters;
    private List<String> annotations;
    private List<String> throwsExceptions;
    private List<String> calls;
    private List<String> calledBy;
    private String javadoc;
    private List<String> comments;
    private boolean isStatic;
    private boolean isFinal;

}
