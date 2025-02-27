package com.org.javadocgenerator.database.mongo.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class JavaConstructor {
    private String name;
    private String visibility;
    private List<JavaParameter> parameters;
    private String javadoc;
}
