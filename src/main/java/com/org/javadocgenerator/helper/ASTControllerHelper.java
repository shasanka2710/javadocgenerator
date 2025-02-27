package com.org.javadocgenerator.helper;


import com.org.javadocgenerator.service.JavaDocGeneratorService;
import com.org.javadocgenerator.service.JavaDocRetrieveService;
import com.org.javadocgenerator.database.mongo.model.JavaClass;
import com.org.javadocgenerator.database.mongo.model.JavaMethod;
import com.org.javadocgenerator.database.mongo.model.Package;
import org.springframework.stereotype.Component;


import java.util.List;

@Component
public class ASTControllerHelper {
    private final JavaDocGeneratorService generatorService;
    private final JavaDocRetrieveService retrieveService;

    public ASTControllerHelper(JavaDocGeneratorService generatorService, JavaDocRetrieveService retrieveService) {
        this.generatorService = generatorService;
        this.retrieveService = retrieveService;
    }

    public List<Package> getPackages(){
        return retrieveService.getPackages();
    }

    public void parseAndStoreProject(String projectId,String projectPath) throws Exception {
        generatorService.parseAndStoreProject(projectId,projectPath);
    }
    public List<JavaClass> getClasses(String packageName){
        return retrieveService.getClasses(packageName);
    }

    public JavaClass getClassDetails( String packageName, String className) {
       return retrieveService.getClassDetails(packageName,className);
    }
}
