package com.org.javadocgenerator.database.mongo;

import com.org.javadocgenerator.database.mongo.model.Project;
import com.org.javadocgenerator.database.retrieve.JavaDocRetrieve;
import com.org.javadocgenerator.database.mongo.model.JavaClass;
import com.org.javadocgenerator.database.mongo.model.JavaMethod;
import com.org.javadocgenerator.database.mongo.model.Package;
import com.org.javadocgenerator.database.storage.JavaDocStorage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("mongoHandler")
@ConditionalOnProperty(name = "storage.type", havingValue = "mongo",matchIfMissing = true)
public class MongoHandler implements JavaDocStorage, JavaDocRetrieve {
    private final MongoProjectRepository projectRepository;
    private final MongoJavaPackageRepository packageRepository;
    private final MongoJavaClassRepository classRepository;
    private final MongoJavaMethodRepository methodRepository;

    public MongoHandler(MongoProjectRepository projectRepository, MongoJavaPackageRepository packageRepository, MongoJavaClassRepository classRepository, MongoJavaMethodRepository methodRepository) {
        this.projectRepository = projectRepository;
        this.packageRepository = packageRepository;
        this.classRepository = classRepository;
        this.methodRepository = methodRepository;
    }

    @Override
    public void saveProject(Project project){projectRepository.save(project);}

    @Override
    public void savePackage(Package pkg) {
        packageRepository.save(pkg);
    }

    @Override
    public void saveClass(JavaClass javaClass) {
        classRepository.save(javaClass);
    }

    @Override
    public void saveMethod(JavaMethod javaMethod) {
        methodRepository.save(javaMethod);
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
        return methodRepository.findByPackageNameAndClassName(packageName, className);
    }


}
