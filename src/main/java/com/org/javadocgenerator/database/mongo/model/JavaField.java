package com.org.javadocgenerator.database.mongo.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class JavaField {
    private String name;
    private String type;
    private String visibility;
    private boolean isStatic;
    private boolean isFinal;
    private List<String> annotations;
    private String javadoc;
}
