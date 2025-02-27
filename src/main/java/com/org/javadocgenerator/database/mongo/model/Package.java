package com.org.javadocgenerator.database.mongo.model;

import jakarta.persistence.Entity;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "packages")
public class Package {
    @Id
    private String id;
    private String projectId;
    private String packageName;
    private List<String> classes;
}
