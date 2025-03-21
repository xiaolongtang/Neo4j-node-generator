package main.generator;

import lombok.RequiredArgsConstructor;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class EntityGenerator {
    private final Driver driver;
    private static final String BASE_PACKAGE = "main.entity";
    private static final String BASE_PATH = "src/main/java/main/entity";

    public void generateEntities() {
        createEntityDirectory();
        generateNodeEntities();
        generateRelationshipEntities();
    }

    private void createEntityDirectory() {
        try {
            Path path = Paths.get(BASE_PATH);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create entity directory", e);
        }
    }

    private void generateNodeEntities() {
        try (Session session = driver.session()) {
            Result result = session.run("CALL db.labels()");
            Set<String> labels = new HashSet<>();
            result.forEachRemaining(record -> labels.add(record.get(0).asString()));

            for (String label : labels) {
                generateNodeEntity(label, session);
            }
        }
    }

    private void generateNodeEntity(String label, Session session) {
        Result result = session.run(
                "MATCH (n:`" + label + "`) UNWIND keys(n) as key " +
                        "RETURN key, apoc.meta.type(n[key]) as type LIMIT 1"
        );

        String className = formatClassName(label);
        Map<String, String> propertyTypes = new HashMap<>();

        while (result.hasNext()) {
            var record = result.next();
            String key = record.get("key").asString();
            String type = record.get("type").asString();
            propertyTypes.put(key, type);
        }

        String entityCode = generateNodeEntityCode(className, propertyTypes);
        writeToFile(className, entityCode);
    }


private void generateRelationshipEntities() {
    try (Session session = driver.session()) {
        Result result = session.run("CALL db.relationshipTypes()");
        Set<String> types = new HashSet<>();
        result.forEachRemaining(record -> types.add(record.get(0).asString()));

        for (String type : types) {
            generateRelationshipEntity(type, session);
        }
    }
}

private void generateRelationshipEntity(String type, Session session) {
    Result result = session.run(
            "MATCH ()-[r:`" + type + "`]->() RETURN keys(r) as keys, "
                    + "labels(startNode(r))[0] as startLabel, "
                    + "labels(endNode(r))[0] as endLabel LIMIT 1"
    );
    if (result.hasNext()) {
        var record = result.single();
        Set<String> properties = new HashSet<>();
        record.get("keys").asList(value -> value.asString())
                .forEach(properties::add);
        String startLabel = record.get("startLabel").asString();
        String endLabel = record.get("endLabel").asString();

        String className = formatClassName(type) + "Relationship";
        String entityCode = generateRelationshipEntityCode(
                className, properties, startLabel, endLabel, type
        );
        writeToFile(className, entityCode);
    }
}

private String generateNodeEntityCode(String className, Map<String, String> propertyTypes) {
    StringBuilder code = new StringBuilder();
    code.append("package ").append(BASE_PACKAGE).append(";\n");
    code.append("import lombok.Data;\n");
    code.append("import org.springframework.data.neo4j.core.schema.Id;\n");
    code.append("import org.springframework.data.neo4j.core.schema.Node;\n");
    code.append("import org.springframework.data.neo4j.core.schema.Relationship;\n");
    code.append("import java.util.Set;\n");
    code.append("import java.time.LocalDateTime;\n\n");

    code.append("@Data\n");
    code.append("@Node").append("(\"").append(className).append("\")\n");
    code.append("public class ").append(className).append(" {\n\n");

    // Add ID field
    code.append("    @Id\n");
    code.append("    private Long id;\n\n");

    // Add properties
    for (Map.Entry<String, String> entry : propertyTypes.entrySet()) {
        String property = entry.getKey();
        String type = entry.getValue();
        String javaType = getJavaType(type);

        code.append("    private ").append(javaType).append(" ")
                .append(formatPropertyName(property)).append(";\n");
    }

    // Add relationships
    try (Session session = driver.session()) {
        Result result = session.run(
                "MATCH (n:"+className+")-[r]->(m) " +
                        "RETURN DISTINCT type(r) as type, labels(m)[0] as targetLabel");
        while (result.hasNext()) {
            var record = result.next();
            String relType = record.get("type").asString();
            String targetLabel = record.get("targetLabel").asString();
            String targetClass = formatClassName(targetLabel);

            code.append("\n    @Relationship(type = \"").append(relType)
                    .append("\", direction = Relationship.Direction.OUTGOING)\n");
            code.append("    private Set<").append(targetClass).append("> ")
                    .append(formatPropertyName(relType)).append(";");
        }

        result = session.run(
                "MATCH (n:"+className+")<-[r]-(m) " +
                        "RETURN DISTINCT type(r) as type, labels(m)[0] as sourceLabel");
        while (result.hasNext()) {
            var record = result.next();
            String relType = record.get("type").asString();
            String sourceLabel = record.get("sourceLabel").asString();
            String sourceClass = formatClassName(sourceLabel);

            code.append("\n    @Relationship(type = \"").append(relType)
                    .append("\", direction = Relationship.Direction.INCOMING)\n");
            code.append("    private Set<").append(sourceClass).append("> ")
                    .append(formatPropertyName(relType)).append("Incoming;");
        }
    }

    code.append("\n}\n");
    return code.toString();
}

private String getJavaType(String neo4jType) {
    switch (neo4jType) {
        case "INTEGER":
            return "Integer";
        case "FLOAT":
            return "Double";
        case "BOOLEAN":
            return "Boolean";
        case "DATETIME":
            return "LocalDateTime";
        case "STRING":
        default:
            return "String";
    }
}

private String generateRelationshipEntityCode(
        String className,
        Set<String> properties,
        String startLabel,
        String endLabel,
        String type
) {
    StringBuilder code = new StringBuilder();
    code.append("package ").append(BASE_PACKAGE).append(";\n");
    code.append("import lombok.Data;\n");
    code.append("import org.springframework.data.neo4j.core.schema.RelationshipProperties;\n");
    code.append("import org.springframework.data.neo4j.core.schema.TargetNode;\n");
    code.append("import org.springframework.data.neo4j.core.schema.Relationship;\n\n");

    code.append("@Data\n");
    code.append("@RelationshipProperties\n");
    code.append("public class ").append(className).append(" {\n\n");

    // Add ID field
    code.append("    private Long id;\n\n");

    // Add start node reference
    code.append("    @Relationship(type = \"").append(type).append("\")\n");
    code.append("    private ").append(formatClassName(startLabel))
            .append(" startNode;\n\n");

    // Add end node reference
    code.append("    @TargetNode\n");
    code.append("    private ").append(formatClassName(endLabel))
            .append(" endNode;\n\n");

    // Add properties
    for (String property : properties) {
        code.append("    private String ").append(formatPropertyName(property))
                .append(";\n");
    }

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
        throw new RuntimeException("Failed to write entity file: " + className, e);
    }
}

private String formatClassName(String label) {
    String[] parts = label.split("[_\\s]+");
    StringBuilder className = new StringBuilder();
    for (String part : parts) {
        if (!part.isEmpty()) {
            className.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1).toLowerCase());
        }
    }
    return className.toString();
}

private String formatPropertyName(String property) {
    String[] parts = property.split("[_\\s]+");
    StringBuilder propertyName = new StringBuilder(parts[0].toLowerCase());
    for (int i = 1; i < parts.length; i++) {
        if (!parts[i].isEmpty()) {
            propertyName.append(Character.toUpperCase(parts[i].charAt(0)))
                    .append(parts[i].substring(1).toLowerCase());
        }
    }
    return propertyName.toString();
}

public Set<String> getNodeEntityNames() {
    Set<String> nodeEntities = new HashSet<>();
    try (Session session = driver.session()) {
        Result result = session.run("CALL db.labels()");
        result.forEachRemaining(record -> {
            String label = record.get(0).asString();
            nodeEntities.add(formatClassName(label));
        });
    }
    return nodeEntities;
}
}
