package com.org.javadocgenerator.service;

import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.org.javadocgenerator.database.mongo.model.*;
import com.org.javadocgenerator.database.mongo.model.Package;
import com.org.javadocgenerator.database.retrieve.JavaDocRetrieve;
import com.org.javadocgenerator.database.storage.JavaDocStorage;
import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JavaDocGeneratorService {
    private final JavaDocStorage storage;
    private final JavaDocRetrieve retrieve;
    private final Set<String> uniquePackages = new HashSet<>();
    private final Map<String, Set<String>> methodCallMap = new HashMap<>(); // Stores method calls
    private final Map<String, Set<String>> incomingCallMap = new HashMap<>(); // Stores calledBy
    private SymbolResolver symbolResolver;

    public JavaDocGeneratorService(@Qualifier("mongoHandler") JavaDocStorage storage,
                                   @Qualifier("mongoHandler") JavaDocRetrieve retrieve) {
        this.storage = storage;
        this.retrieve = retrieve;
        this.symbolResolver = createSymbolSolver();
    }

    /**
     * Initializes JavaSymbolSolver for resolving method calls.
     */
    private SymbolResolver createSymbolSolver() {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(new File("src/main/java")));
        combinedTypeSolver.add(new ClassLoaderTypeSolver(Thread.currentThread().getContextClassLoader()));
        return new JavaSymbolSolver(combinedTypeSolver);
    }

    /**
     * Parses the project and stores class details into MongoDB.
     */
    public void parseAndStoreProject(String projectId, String projectPath) throws Exception {
        Project project = new Project();
        project.setId(projectId);
        project.setName(new File(projectPath).getName());
        project.setRepoPath(projectPath);
        storage.saveProject(project); // Save project metadata

        Files.walk(Paths.get(projectPath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(file -> {
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(file);
                        if (cu != null) {
                            processCompilationUnit(projectId, cu);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        // Update calledBy relationships after all classes are processed
        updateCalledByRelationships();

        uniquePackages.clear(); // Clear cache after processing
    }

    private void processCompilationUnit(String projectId, CompilationUnit cu) {
        String packageName = cu.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse("default");

        if (uniquePackages.add(packageName)) {
            Package pkg = new Package();
            pkg.setProjectId(projectId);
            pkg.setPackageName(packageName);
            storage.savePackage(pkg);
        }

        for (ClassOrInterfaceDeclaration classDecl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            String className = classDecl.getFullyQualifiedName().orElse(packageName + "." + classDecl.getNameAsString());

            JavaClass javaClassDetails = new JavaClass();
            javaClassDetails.setProjectId(projectId);
            javaClassDetails.setPackageName(packageName);
            javaClassDetails.setClassName(classDecl.getNameAsString());
            javaClassDetails.setInterface(classDecl.isInterface());
            javaClassDetails.setAbstract(classDecl.isAbstract());
            javaClassDetails.setVisibility(classDecl.getAccessSpecifier().asString());
            javaClassDetails.setAnnotations(classDecl.getAnnotations().stream().map(a -> a.getNameAsString()).toList());
            javaClassDetails.setJavadoc(classDecl.getJavadocComment().map(Comment::getContent).orElse(null));

            javaClassDetails.setMethods(classDecl.getMethods().stream().map(methodDecl -> {
                String methodSignature = className + "." + methodDecl.getNameAsString();
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

                // Capture method calls
                Set<String> calledMethods = captureMethodCalls(methodDecl);
                javaMethod.setCalls(new ArrayList<>(calledMethods));

                // Store method calls for reverse lookup
                methodCallMap.put(methodSignature, calledMethods);

                // Populate `calledBy` (reverse map)
                for (String calledMethod : calledMethods) {
                    incomingCallMap.computeIfAbsent(calledMethod, k -> new HashSet<>()).add(methodSignature);
                }

                return javaMethod;
            }).toList());

            storage.saveClass(javaClassDetails);
        }
    }

    /**
     * Captures method calls inside a given method.
     */
    private Set<String> captureMethodCalls(MethodDeclaration methodDecl) {
        Set<String> calledMethods = new HashSet<>();

        for (MethodCallExpr methodCall : methodDecl.findAll(MethodCallExpr.class)) {
            resolveFullyQualifiedMethodName(methodCall).ifPresent(calledMethods::add);
        }
        return calledMethods;
    }

    /**
     * Resolves the fully qualified method name for a method call.
     */
    private Optional<String> resolveFullyQualifiedMethodName(MethodCallExpr methodCall) {
        try {
            ResolvedMethodDeclaration methodDecl = symbolResolver.resolveDeclaration(methodCall, ResolvedMethodDeclaration.class);
            ResolvedReferenceTypeDeclaration declaringClass = methodDecl.declaringType();
            return Optional.of(declaringClass.getQualifiedName() + "." + methodDecl.getName());
        } catch (Exception e) {
            System.err.println("Could not resolve method: " + methodCall.getNameAsString());
        }
        return Optional.empty();
    }

    /**
     * Updates calledBy relationships in MongoDB.
     */
    private void updateCalledByRelationships() {
        List<JavaClass> allClasses = retrieve.findAllClasses();
        for (JavaClass javaClass : allClasses) {
            for (JavaMethod method : javaClass.getMethods()) {
                String methodSignature = javaClass.getPackageName() + "." + javaClass.getClassName() + "." + method.getMethodName();
                Set<String> incomingCalls = incomingCallMap.getOrDefault(methodSignature, Collections.emptySet());
                method.setCalledBy(new ArrayList<>(incomingCalls));
            }
            storage.saveClass(javaClass);
        }
    }

    /**
     * Retrieves all methods that invoke a given method.
     */
    public Set<String> getIncomingMethodCalls(String fullyQualifiedMethodName) {
        return incomingCallMap.getOrDefault(fullyQualifiedMethodName, Collections.emptySet());
    }
}


