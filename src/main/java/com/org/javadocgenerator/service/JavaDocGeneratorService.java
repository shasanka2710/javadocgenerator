package com.org.javadocgenerator.service;

import com.org.javadocgenerator.database.storage.JavaDocStorage;
import com.org.javadocgenerator.database.mongo.model.JavaClass;
import com.org.javadocgenerator.database.mongo.model.JavaMethod;
import com.org.javadocgenerator.database.mongo.model.Package;
import com.org.javadocgenerator.database.mongo.model.Project;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

@Service
public class JavaDocGeneratorService {

    private final JavaDocStorage storage;
    private final Set<String> uniquePackages = new HashSet<>(); // To store unique package names

    public JavaDocGeneratorService(@Qualifier("mongoHandler") JavaDocStorage storage) {
        this.storage = storage;
    }

    public void parseAndStoreProject(String projectId, String projectPath) throws Exception {
        Project project = new Project();
        project.setId(projectId);
        project.setName(new File(projectPath).getName());
        project.setRepoPath(projectPath);
        storage.saveProject(project); // Save project

        Files.walk(Paths.get(projectPath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(file -> {
                    try {
                        CompilationUnit cu = new JavaParser().parse(file).getResult().orElse(null);
                        if (cu != null) {
                            processCompilationUnit(projectId, cu);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        uniquePackages.clear(); // Clear cache after processing
    }

    private void processCompilationUnit(String projectId, CompilationUnit cu) {
        String packageName = cu.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse("default");

        // Save package only if it's not already stored
        if (uniquePackages.add(packageName)) { // Ensures uniqueness
            Package pkg = new Package();
            pkg.setProjectId(projectId);
            pkg.setPackageName(packageName);
            storage.savePackage(pkg);
        }

        for (ClassOrInterfaceDeclaration classDecl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            JavaClass javaClass = new JavaClass();
            javaClass.setProjectId(projectId);
            javaClass.setPackageName(packageName);
            javaClass.setClassName(classDecl.getNameAsString());
            javaClass.setInterface(classDecl.isInterface());
            javaClass.setAbstract(classDecl.isAbstract());
            javaClass.setVisibility(classDecl.getAccessSpecifier().asString());
            javaClass.setAnnotations(classDecl.getAnnotations().stream().map(a -> a.getNameAsString()).toList());
            javaClass.setJavadoc(classDecl.getJavadocComment().map(Comment::getContent).orElse(null));

            // Save class
            storage.saveClass(javaClass);

            for (MethodDeclaration methodDecl : classDecl.getMethods()) {
                JavaMethod javaMethod = new JavaMethod();
                javaMethod.setClassId(javaClass.getId());
                javaMethod.setClassName(javaClass.getClassName()); // ✅ Store class name
                javaMethod.setPackageName(packageName); // ✅ Store package name
                javaMethod.setMethodName(methodDecl.getNameAsString());
                javaMethod.setReturnType(methodDecl.getType().asString());
                javaMethod.setVisibility(methodDecl.getAccessSpecifier().asString());
                javaMethod.setStatic(methodDecl.isStatic());
                javaMethod.setFinal(methodDecl.isFinal());

                /*javaMethod.setParameters(methodDecl.getParameters().stream()
                        .map(param -> new JavaMethod.Parameter(param.getNameAsString(), param.getType().asString()))
                        .toList());

                javaMethod.setThrowsExceptions(methodDecl.getThrownExceptions().stream()
                        .map(throwExp -> throwExp.getNameAsString())
                        .toList());*/

                javaMethod.setJavadoc(methodDecl.getJavadocComment().map(JavadocComment::getContent).orElse(null));

                // Save method
                storage.saveMethod(javaMethod);
            }
        }
    }
}