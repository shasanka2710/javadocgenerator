package com.org.javadocgenerator.parser;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.description.JavadocDescription;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.org.javadocgenerator.ai.SpringAiCommentGenerator;
import com.org.javadocgenerator.config.AppConfig;
import com.org.javadocgenerator.model.ClassDetails;
import com.org.javadocgenerator.model.MethodDetails;
import com.org.javadocgenerator.model.PackageDetails;
import com.org.javadocgenerator.util.PathConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.org.javadocgenerator.util.StringUtil.cleanJavaCode;

@Component
public class JavaCodeParser {

    private static final Logger logger = LoggerFactory.getLogger(JavaCodeParser.class);

    @Autowired
    private SpringAiCommentGenerator aiCommentGenerator;

    @Autowired
    private AppConfig appConfig;

    public JavaCodeParser() {
        //Set symbol solver
        setSymbolSolver();
    }

    public void parseAndGenerateDocs(File javaFile) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(javaFile);
        Optional<TypeDeclaration<?>> typeDeclaration = cu.getPrimaryType();
        if (typeDeclaration.isEmpty()) {
            logger.warn("No primary type found in file: {}", javaFile.getName());
            return;
        }
        String className = typeDeclaration.get().getNameAsString();
        //Class level Java documentation
        //  Javadoc classJavadoc = createOrUpdateClassJavadoc(typeDeclaration.get(), className);
        // typeDeclaration.get().setJavadocComment(classJavadoc);
        //Method Iteration
        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
            //Identifying cyclomatic complexity
            //method level java documentation
            // Fixed: Conditionally invoke aiCommentGenerator
            Javadoc javadoc = appConfig.isEnableAi() && aiCommentGenerator != null ? createOrUpdateMethodDoc(method, className) : createOrUpdateMethodDoc(method);
            method.setJavadocComment(javadoc);
            // Generate call graph
            // generateCallGraph(method, javaFile);
        }
        // Save the modified CompilationUnit back to the file
        if (!appConfig.isDryRun()) {
            Files.write(javaFile.toPath(), cu.toString().getBytes());
        }
    }

    private static void setSymbolSolver() {
        // Create a CombinedTypeSolver and add the ReflectionTypeSolver
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(new File("src/main/java")));
        // Add the ClassLoaderTypeSolver to include all JAR files from the classpath
        combinedTypeSolver.add(new ClassLoaderTypeSolver(Thread.currentThread().getContextClassLoader()));
        // Create a JavaSymbolSolver with the CombinedTypeSolver
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        //
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
    }

    private Javadoc createOrUpdateClassJavadoc(TypeDeclaration<?> typeDeclaration, String className) {
        Javadoc javadoc = typeDeclaration.getJavadoc().orElse(new Javadoc(new JavadocDescription()));
        // Update main description for the class
        if (javadoc.getDescription().isEmpty()) {
            // Fixed: Conditionally invoke aiCommentGenerator
            String classDescription = (appConfig.isEnableAi() && aiCommentGenerator != null) ? aiCommentGenerator.generateClassComment(typeDeclaration.toString(), className) : "TODO: Add class description here.";
            javadoc = new Javadoc(JavadocDescription.parseText(classDescription));
        }
        return javadoc;
    }

    // Fixed: Method overloading to handle cases where AI comment generation is disabled
    private Javadoc createOrUpdateMethodDoc(MethodDeclaration method) {
        Javadoc javadoc = method.getJavadoc().orElse(new Javadoc(new JavadocDescription()));
        // Update main description
        if (javadoc.getDescription().isEmpty()) {
            javadoc = new Javadoc(JavadocDescription.parseText("TODO: Add method description here."));
        }
        return javadoc;
    }

    private Javadoc createOrUpdateMethodDoc(MethodDeclaration method, String className) {
        Javadoc javadoc = method.getJavadoc().orElse(new Javadoc(new JavadocDescription()));
        // Update main description
        if (javadoc.getDescription().isEmpty()) {
            String methodCode = method.toString();
            String aiComment = aiCommentGenerator.generateMethodComment(methodCode, className);
            javadoc = new Javadoc(JavadocDescription.parseText(aiComment));
        }
        // Update or add parameter descriptions
        Javadoc finalJavadoc = javadoc;
        methodParameterAndReturnDocGen(method, finalJavadoc, javadoc);
        return finalJavadoc;
    }

    private static void methodParameterAndReturnDocGen(MethodDeclaration method, Javadoc finalJavadoc, Javadoc javadoc) {
        method.getParameters().forEach(parameter -> {
            String paramName = parameter.getNameAsString();
            Optional<JavadocBlockTag> existingTag = finalJavadoc.getBlockTags().stream().filter(tag -> tag.getType() == JavadocBlockTag.Type.PARAM && tag.getName().equals(paramName)).findFirst();
            if (existingTag.isEmpty()) {
                finalJavadoc.addBlockTag("param", paramName, "TODO: Add parameter description.");
            }
        });
        // Update or add return description
        if (!method.getType().isVoidType()) {
            Optional<JavadocBlockTag> returnTag = javadoc.getBlockTags().stream().filter(tag -> tag.getType() == JavadocBlockTag.Type.RETURN).findFirst();
            if (returnTag.isEmpty()) {
                javadoc.addBlockTag("return", "TODO: Add return value description.");
            }
        }
        // Update or add throws description
        method.getThrownExceptions().forEach(thrownException -> {
            String exceptionName = thrownException.asString();
            Optional<JavadocBlockTag> throwsTag = finalJavadoc.getBlockTags().stream().filter(tag -> tag.getType() == JavadocBlockTag.Type.THROWS && tag.getName().equals(exceptionName)).findFirst();
            if (throwsTag.isEmpty()) {
                finalJavadoc.addBlockTag("throws", exceptionName, "TODO: Add exception description.");
            }
        });
    }

   // Removed unused method: calculateCyclomaticComplexity

    private void generateCallGraph(MethodDeclaration method, File javaFile) throws IOException {
        String methodName = method.getNameAsString();
        String relativePath = javaFile.getPath().replaceFirst("uploads", "call-graph");
        Path outputPath = Paths.get(relativePath).getParent();
        if (outputPath != null) {
            Files.createDirectories(outputPath);
        }
        Path callGraphFile = outputPath.resolve(methodName + ".txt");
        String callGraph = buildCallGraph(method, 0, appConfig.getCallGraphDepth());
        Files.write(callGraphFile, callGraph.getBytes());
    }

    private String buildCallGraph(MethodDeclaration method, int currentDepth, int maxDepth) {
        if (currentDepth > maxDepth) {
            return "";
        }
        StringBuilder callGraph = new StringBuilder();
        callGraph.append(method.getNameAsString()).append("\n");
        method.findAll(MethodCallExpr.class).forEach(call -> {
            try {
                Optional<MethodDeclaration> calledMethod = call.resolve().toAst().filter(MethodDeclaration.class::isInstance).map(MethodDeclaration.class::cast);
                calledMethod.ifPresent(m -> callGraph.append("  ".repeat(Math.max(0, currentDepth + 1))).append(buildCallGraph(m, currentDepth + 1, maxDepth)));
            } catch (IllegalStateException e) {
                logger.error("Symbol resolution not configured for method call: {}", call, e);
            }
        });
        return callGraph.toString();
    }

    public ClassDetails parseClassDetails(File javaFile) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(javaFile);
        TypeDeclaration<?> typeDeclaration = cu.getPrimaryType().orElseThrow(() -> new IllegalArgumentException("No primary type found"));
        String className = typeDeclaration.getNameAsString();
        String classDescription = "Description of " + className;
        List<String> fields = typeDeclaration.getFields().stream().map(FieldDeclaration::toString).collect(Collectors.toList());
        List<String> constructors = typeDeclaration.getConstructors().stream().map(ConstructorDeclaration::getNameAsString).collect(Collectors.toList());
        // Fixed: Using Stream.toList() for better performance
        List<MethodDetails> methods = typeDeclaration.getMethods().stream().map(method -> new MethodDetails(method.getDeclarationAsString(), (method.getJavadoc().isPresent() && method.getJavadoc().get().toText() != null) ? method.getJavadoc().get().toText() : "Description of " + method.getNameAsString(), method.getType().asString(), method.getThrownExceptions().toString())).toList();
        return new ClassDetails(className, classDescription, fields, constructors, methods);
    }

    public List<PackageDetails> parsePackages() {
        List<PackageDetails> packages = new ArrayList<>();
        try {
            Files.walk(Paths.get("src/main/java/com/org/javadocgenerator")).filter(Files::isDirectory).forEach(path -> {
                String packageName = path.toString().replace("src/main/java/", "").replace("/", ".");
                packages.add(new PackageDetails(packageName, "Description of " + packageName));
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return packages;
    }

    public List<ClassDetails> parseClasses(String packageName) {
        List<ClassDetails> classes = new ArrayList<>();
        try {
            Path packagePath = Paths.get("src/main/java/" + packageName.replace(".", "/"));
            Files.list(packagePath).filter(Files::isRegularFile).filter(file -> file.toString().endsWith(".java")).forEach(file -> {
                try {
                    CompilationUnit cu = StaticJavaParser.parse(file);
                    TypeDeclaration<?> typeDeclaration = cu.getPrimaryType().orElseThrow(() -> new IllegalArgumentException("No primary type found"));
                    classes.add(new ClassDetails(typeDeclaration.getNameAsString(), (typeDeclaration.getJavadocComment() != null) ? typeDeclaration.getJavadocComment().toString() : "TODO: Description of " + typeDeclaration.getNameAsString(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classes;
    }

    public String identifyFixUsingLLModel(String className, Set<String> description) throws FileNotFoundException {
        logger.info("Identifying fix using LL model for class: {}", className);
        CompilationUnit cu = getCompilationUnit(className);
        Optional<TypeDeclaration<?>> typeDeclaration = cu.getPrimaryType();
        // Fixed: Conditionally invoke aiCommentGenerator
        String fixedCode = (appConfig.isEnableAi() && aiCommentGenerator != null) ? aiCommentGenerator.fixSonarIssues(className, typeDeclaration.get().getParentNode().get().toString(), description) : typeDeclaration.get().toString();
        logger.info("Original code: {}", typeDeclaration.get().getParentNode().get());
        logger.info("Fixed code: {}", fixedCode);
        return cleanJavaCode(fixedCode);
    }

    public static CompilationUnit getCompilationUnit(String className) throws FileNotFoundException {
        Path filePath = Paths.get(PathConverter.toSlashedPath(className));
        File file = new File(String.valueOf(filePath));
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filePath);
        }
        CompilationUnit cu = StaticJavaParser.parse(file);
        return cu;
    }

    public boolean isValidJavaCode(String code) {
        try {
            // Attempt to parse the code
            CompilationUnit compilationUnit = StaticJavaParser.parse(code);
            // Successfully parsed
            return true;
        } catch (ParseProblemException | IllegalArgumentException e) {
            // Syntax or parsing issues
            logger.error("Code validation failed: {}", e.getMessage());
            return false;
        }
    }
}