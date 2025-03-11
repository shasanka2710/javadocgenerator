package com.org.javadocgenerator.service;

import com.org.javadocgenerator.database.mongo.model.*;
import com.org.javadocgenerator.database.mongo.model.Package;
import com.org.javadocgenerator.database.retrieve.JavaDocRetrieve;
import com.org.javadocgenerator.database.storage.JavaDocStorage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.declaration.ModifierKind;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JavaDocGeneratorServiceSpoon {
    private final JavaDocStorage storage;
    private final JavaDocRetrieve retrieve;
    private final Set<String> uniquePackages = new HashSet<>();
    private final Map<String, Set<String>> methodCallMap = new HashMap<>();
    private final Map<String, Set<String>> incomingCallMap = new HashMap<>();

    public JavaDocGeneratorServiceSpoon(@Qualifier("mongoHandler") JavaDocStorage storage,
                                        @Qualifier("mongoHandler") JavaDocRetrieve retrieve) {
        this.storage = storage;
        this.retrieve = retrieve;
    }

    public void parseAndStoreProject(String projectId, String projectPath) {
        Project project = new Project();
        project.setId(projectId);
        project.setName(new File(projectPath).getName());
        project.setRepoPath(projectPath);
        storage.saveProject(project);

        Launcher launcher = new Launcher();
        launcher.addInputResource(projectPath);
        launcher.buildModel();
        CtModel model = launcher.getModel();

        for (CtPackage pkg : model.getAllPackages()) {
            processPackage(projectId, pkg);
        }

        for (CtType<?> type : model.getAllTypes()) {
            extractMethodCalls(type);
        }

        updateCalledByRelationships();

        for (CtType<?> type : model.getAllTypes()) {
            processType(projectId, type);
        }

        uniquePackages.clear();
        methodCallMap.clear();
        incomingCallMap.clear();
    }

    private void processPackage(String projectId, CtPackage pkg) {
        if (pkg == null || pkg.getQualifiedName() == null) return;

        if (uniquePackages.add(pkg.getQualifiedName())) {
            Package packageEntry = new Package();
            packageEntry.setProjectId(projectId);
            packageEntry.setPackageName(pkg.getQualifiedName());
            storage.savePackage(packageEntry);
        }
    }

    private void processType(String projectId, CtType<?> type) {
        if (type == null || type.getPackage() == null) return;

        String packageName = type.getPackage().getQualifiedName();
        String className = type.getQualifiedName();

        JavaClass javaClassDetails = new JavaClass();
        javaClassDetails.setProjectId(projectId);
        javaClassDetails.setPackageName(packageName);
        javaClassDetails.setClassName(type.getSimpleName());
        javaClassDetails.setInterface(type instanceof CtInterface<?>);
        javaClassDetails.setAbstract(type.isAbstract());
        javaClassDetails.setVisibility(extractVisibility(type.getModifiers()));
        javaClassDetails.setAnnotations(type.getAnnotations().stream()
                .map(a -> a.getAnnotationType().getQualifiedName())
                .collect(Collectors.toList()));
        javaClassDetails.setJavadoc(type.getDocComment() != null ? type.getDocComment() : "");

        List<JavaMethod> methods = new ArrayList<>();

        for (CtMethod<?> method : type.getMethods()) {
            methods.add(processMethodOrConstructor(className, method));
        }

        if (type instanceof CtClass<?> clazz) {
            for (CtConstructor<?> constructor : clazz.getConstructors()) {
                methods.add(processMethodOrConstructor(className, constructor));
            }
        }

        for (JavaMethod method : methods) {
            String methodSignature = className + "#" + method.getMethodName();
            Set<String> callers = incomingCallMap.getOrDefault(methodSignature, new HashSet<>());
            method.setCalledBy(new ArrayList<>(callers));
        }

        javaClassDetails.setMethods(methods);
        storage.saveClass(javaClassDetails);
    }

    private JavaMethod processMethodOrConstructor(String className, CtExecutable<?> executable) {
        JavaMethod javaMethod = new JavaMethod();
        javaMethod.setMethodName(executable.getSimpleName());

        if (executable instanceof CtModifiable modifiableExecutable) {
            javaMethod.setVisibility(extractVisibility(modifiableExecutable.getModifiers()));
            javaMethod.setStatic(modifiableExecutable.hasModifier(ModifierKind.STATIC));
            javaMethod.setFinal(modifiableExecutable.hasModifier(ModifierKind.FINAL));
        } else {
            javaMethod.setVisibility("UNKNOWN");
            javaMethod.setStatic(false);
            javaMethod.setFinal(false);
        }

        List<JavaParameter> parameters = executable.getParameters().stream()
                .map(param -> new JavaParameter(param.getSimpleName(), param.getType().getQualifiedName()))
                .collect(Collectors.toList());

        CtTypeReference<?> returnType = (executable instanceof CtMethod<?>) ? ((CtMethod<?>) executable).getType() : null;
        javaMethod.setReturnType(returnType != null ? returnType.getQualifiedName() : "void");

        javaMethod.setParameters(parameters);
        javaMethod.setThrowsExceptions(executable.getThrownTypes().stream()
                .map(CtTypeReference::getQualifiedName)
                .collect(Collectors.toList()));

        javaMethod.setJavadoc(executable.getDocComment() != null ? executable.getDocComment() : "");

        return javaMethod;
    }

    private void extractMethodCalls(CtType<?> type) {
        String className = type.getQualifiedName();
        for (CtMethod<?> method : type.getMethods()) {
            captureMethodCalls(className, method);
        }
        if (type instanceof CtClass<?> clazz) {
            for (CtConstructor<?> constructor : clazz.getConstructors()) {
                captureMethodCalls(className, constructor);
            }
        }
    }

    private void captureMethodCalls(String className, CtExecutable<?> executable) {
        Set<String> calledMethods = new HashSet<>();

        for (CtInvocation<?> invocation : executable.getElements(e -> e instanceof CtInvocation<?>)
                .stream().map(CtInvocation.class::cast).toList()) {
            String methodName = resolveFullyQualifiedMethodName(invocation);
            if (methodName != null) {
                calledMethods.add(methodName);
            }
        }

        String methodSignature = className + "#" + executable.getSimpleName();
        methodCallMap.computeIfAbsent(methodSignature, k -> new HashSet<>()).addAll(calledMethods);
    }

    private String resolveFullyQualifiedMethodName(CtInvocation<?> invocation) {
        CtExecutableReference<?> execRef = invocation.getExecutable();
        if (execRef == null) {
            return null;
        }

        CtTypeReference<?> declaringType = execRef.getDeclaringType();
        if (declaringType == null) {
            return null;
        }

        return declaringType.getQualifiedName() + "#" + execRef.getSimpleName();
    }

    private void updateCalledByRelationships() {
        for (Map.Entry<String, Set<String>> entry : methodCallMap.entrySet()) {
            String callerMethod = entry.getKey();
            for (String calledMethod : entry.getValue()) {
                incomingCallMap.computeIfAbsent(calledMethod, k -> new HashSet<>()).add(callerMethod);
            }
        }
    }

    private String extractVisibility(Set<ModifierKind> modifiers) {
        return modifiers.stream()
                .filter(mod -> mod == ModifierKind.PUBLIC || mod == ModifierKind.PROTECTED || mod == ModifierKind.PRIVATE)
                .findFirst()
                .map(Enum::name)
                .orElse("PACKAGE_PRIVATE");
    }
}