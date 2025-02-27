package com.org.javadocgenerator.database.mongo;

import com.org.javadocgenerator.database.mongo.model.Project;
import com.org.javadocgenerator.database.retrieve.JavaDocRetrieve;
import com.org.javadocgenerator.database.mongo.model.JavaClass;
import com.org.javadocgenerator.database.mongo.model.JavaMethod;
import com.org.javadocgenerator.database.mongo.model.Package;
import com.org.javadocgenerator.database.storage.JavaDocStorage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("mongoHandler")
@ConditionalOnProperty(name = "storage.type", havingValue = "mongo",matchIfMissing = true)
public class MongoHandler implements JavaDocStorage, JavaDocRetrieve {
    @Autowired
    MongoTemplate mongoTemplate;
    private final MongoProjectRepository projectRepository;
    private final MongoJavaPackageRepository packageRepository;
    private final MongoJavaClassRepository classRepository;

    public MongoHandler(MongoProjectRepository projectRepository, MongoJavaPackageRepository packageRepository, MongoJavaClassRepository classRepository) {
        this.projectRepository = projectRepository;
        this.packageRepository = packageRepository;
        this.classRepository = classRepository;
    }

    @Override
    public void saveProject(Project project){
        Query query = new Query(Criteria.where("id").is(project.getId()));
        // Delete the existing class
        mongoTemplate.remove(query, Project.class);
        // Insert the new class with updated details
        mongoTemplate.save(project);
    }

    @Override
    public void savePackage(Package pkg) {
        Query query = new Query(Criteria.where("packageName").is(pkg.getPackageName()));
        // Delete the existing class
        mongoTemplate.remove(query, Package.class);
        // Insert the new class with updated details
        mongoTemplate.save(pkg);
    }

    @Override
    public void saveClass(JavaClass javaClass) {
        Query query = new Query(Criteria.where("className").is(javaClass.getClassName())
                .and("packageName").is(javaClass.getPackageName()));
        // Delete the existing class
        mongoTemplate.remove(query, JavaClass.class);
        // Insert the new class with updated details
        mongoTemplate.save(javaClass);
    }


    @Override
    public List<Package> getPackages() {
        return packageRepository.findAll();
    }

    @Override
    public List<JavaClass> getClasses(String packageName) {
        return classRepository.findByPackageName(packageName);
    }

    @Override
    public List<JavaMethod> getMethods(String packageName, String className) {
        JavaClass javaClass = classRepository.findByPackageNameAndClassName(packageName, className);
        return javaClass != null ? javaClass.getMethods() : null;
    }


}
