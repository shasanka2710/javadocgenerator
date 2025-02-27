package com.org.javadocgenerator.controller;

import com.org.javadocgenerator.database.mongo.model.JavaClass;
import com.org.javadocgenerator.database.mongo.model.JavaMethod;
import com.org.javadocgenerator.database.mongo.model.Package;
import com.org.javadocgenerator.helper.ASTControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/api/javadoc")
public class ASTController {

    @Autowired
    private ASTControllerHelper astControllerHelper;

    /**
     * Displays the list of packages for the given project ID.
     */
    @GetMapping()
    public String getPackages(Model model) {
        List<Package> packages = astControllerHelper.getPackages();
        model.addAttribute("packages", packages);
        return "index"; // Loads the Thymeleaf template "index.html"
    }

    /**
     * Displays the list of classes inside a package.
     */
    @GetMapping("/package/{packageName}")
    public String getPackageDetails( @PathVariable String packageName, Model model) {
        List<JavaClass> classes = astControllerHelper.getClasses(packageName);
        model.addAttribute("classes", classes);
        model.addAttribute("packageName", packageName);
        return "package-details"; // A new Thymeleaf template for package-level details
    }

    /**
     * Displays class details, including methods.
     */
    @GetMapping("/package/{packageName}/class/{className}")
    public String getClassDetails(@PathVariable String packageName,
                                  @PathVariable String className, Model model) {
        List<JavaMethod> methods = astControllerHelper.getMethods(packageName, className);

        model.addAttribute("javaClass", className);
        model.addAttribute("methods", methods);
        model.addAttribute("packageName", packageName);
        return "class-details"; // Loads the Thymeleaf template "class-details.html"
    }

    /**
     * Handles parsing the project and redirects to the package list.
     */
    @PostMapping("/parse")
    public String parseProject(@RequestParam String projectId, @RequestParam String projectPath, Model model) {
        try {
            astControllerHelper.parseAndStoreProject(projectId, projectPath);
            return "redirect:/javadoc/packages?projectId=" + projectId;
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error parsing project: " + e.getMessage());
            return "error"; // Redirect to an error page
        }
    }
}