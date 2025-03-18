package main.generator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RepositoryGenerator {
    private static final String BASE_PACKAGE = "main.repository";
    private static final String BASE_PATH = "src/main/java/main/repository";
    private final EntityGenerator entityGenerator;

    public void generateRepositories() {
        createRepositoryDirectory();
        Set<String> nodeEntities = entityGenerator.getNodeEntityNames();
        for (String entityName : nodeEntities) {
            generateRepository(entityName);
        }
    }

    private void createRepositoryDirectory() {
        try {
            Path path = Paths.get(BASE_PATH);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create repository directory", e);
        }
    }

    private void generateRepository(String entityName) {
        String className = entityName + "Repository";
        String repositoryCode = generateRepositoryCode(entityName, className);
        writeToFile(className, repositoryCode);
    }

    private String generateRepositoryCode(String entityName, String className) {
        StringBuilder code = new StringBuilder();
        code.append("package ").append(BASE_PACKAGE).append(";\n\n");
        code.append("import org.springframework.data.neo4j.repository.Neo4jRepository;\n");
        code.append("import org.springframework.stereotype.Repository;\n");
        code.append("import main.entity.").append(entityName).append(";\n\n");

        code.append("@Repository\n");
        code.append("public interface ").append(className)
            .append(" extends Neo4jRepository<").append(entityName).append(", Long> {\n");
        code.append("}\n");

        return code.toString();
    }

    private void writeToFile(String className, String content) {
        try {
            String filePath = BASE_PATH + "/" + className + ".java";
            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(content);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write repository file: " + className, e);
        }
    }
}