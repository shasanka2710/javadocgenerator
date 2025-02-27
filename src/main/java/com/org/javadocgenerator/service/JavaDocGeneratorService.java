package com.org.javadocgenerator.service;

import com.org.javadocgenerator.database.mongo.model.*;
import com.org.javadocgenerator.database.mongo.model.Package;
import com.org.javadocgenerator.database.storage.JavaDocStorage;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
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
        if (uniquePackages.add(packageName)) {
            Package pkg = new Package();
            pkg.setProjectId(projectId);
            pkg.setPackageName(packageName);
            storage.savePackage(pkg);
        }

        for (ClassOrInterfaceDeclaration classDecl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            JavaClass javaClassDetails = new JavaClass();
            javaClassDetails.setProjectId(projectId);
            javaClassDetails.setPackageName(packageName);
            javaClassDetails.setClassName(classDecl.getNameAsString());
            javaClassDetails.setInterface(classDecl.isInterface());
            javaClassDetails.setAbstract(classDecl.isAbstract());
            javaClassDetails.setVisibility(classDecl.getAccessSpecifier().asString());
            javaClassDetails.setAnnotations(classDecl.getAnnotations().stream().map(a -> a.getNameAsString()).toList());
            javaClassDetails.setJavadoc(classDecl.getJavadocComment().map(Comment::getContent).orElse(null));

            // Capture Fields
            javaClassDetails.setFields(classDecl.getFields().stream().map(field -> {
                JavaField javaField = new JavaField();
                javaField.setName(field.getVariable(0).getNameAsString());
                javaField.setType(field.getElementType().asString());
                javaField.setVisibility(field.getAccessSpecifier().asString());
                javaField.setStatic(field.isStatic());
                javaField.setFinal(field.isFinal());
                javaField.setAnnotations(field.getAnnotations().stream().map(a -> a.getNameAsString()).toList());
                javaField.setJavadoc(field.getJavadocComment().map(JavadocComment::getContent).orElse(null));
                return javaField;
            }).toList());

            // Capture Constructors
            javaClassDetails.setConstructors(classDecl.getConstructors().stream().map(constructor -> {
                JavaConstructor javaConstructor = new JavaConstructor();
                javaConstructor.setName(constructor.getNameAsString());
                javaConstructor.setVisibility(constructor.getAccessSpecifier().asString());
                javaConstructor.setParameters(constructor.getParameters().stream()
                        .map(param -> new JavaParameter(param.getNameAsString(), param.getType().asString()))
                        .toList());
                javaConstructor.setJavadoc(constructor.getJavadocComment().map(JavadocComment::getContent).orElse(null));
                return javaConstructor;
            }).toList());

            // Capture Methods
            javaClassDetails.setMethods(classDecl.getMethods().stream().map(methodDecl -> {
                JavaMethod javaMethod = new JavaMethod();
                javaMethod.setMethodName(methodDecl.getNameAsString());
                javaMethod.setReturnType(methodDecl.getType().asString());
                javaMethod.setVisibility(methodDecl.getAccessSpecifier().asString());
                javaMethod.setStatic(methodDecl.isStatic());
                javaMethod.setFinal(methodDecl.isFinal());
                javaMethod.setParameters(methodDecl.getParameters().stream()
                        .map(param -> new JavaParameter(param.getNameAsString(), param.getType().asString()))
                        .toList());
                javaMethod.setThrowsExceptions(methodDecl.getThrownExceptions().stream()
                        .map(throwExp -> throwExp.asString())
                        .toList());
                javaMethod.setJavadoc(methodDecl.getJavadocComment().map(JavadocComment::getContent).orElse(""));
                return javaMethod;
            }).toList());

            // Capture Static Blocks
            //javaClassDetails.setStaticBlocks(classDecl.getStaticInitializer().map(staticBlock -> staticBlock.toString()).orElse(null));

            // Save the complete class details
            storage.saveClass(javaClassDetails);
        }
    }

}