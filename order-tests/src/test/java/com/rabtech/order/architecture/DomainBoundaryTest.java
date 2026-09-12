package com.rabtech.order.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Architecture Boundary Tests - Pure Domain Independence")
class DomainBoundaryTest {

    private static final List<String> FORBIDDEN_PACKAGES = List.of(
            "org.springframework",
            "jakarta.persistence",
            "javax.persistence",
            "org.hibernate",
            "java.sql",
            "javax.sql",
            "org.apache.http",
            "com.fasterxml.jackson"
    );

    @Test
    @DisplayName("Architecture Constraint: Domain module has ZERO dependencies on Spring, JPA, HTTP, or Database")
    void domainModuleIsPureJavaWithoutFrameworkDependencies() throws IOException {
        // Locate domain module source directory
        Path domainSourceDir = Path.of("..", "order-domain", "src", "main", "java");
        if (!Files.exists(domainSourceDir)) {
            domainSourceDir = Path.of("order-domain", "src", "main", "java");
        }

        assertThat(domainSourceDir)
                .as("Domain source directory must exist")
                .exists();

        try (Stream<Path> stream = Files.walk(domainSourceDir)) {
            List<Path> javaFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();

            assertThat(javaFiles)
                    .as("Domain module must contain Java source files")
                    .isNotEmpty();

            for (Path javaFile : javaFiles) {
                List<String> lines = Files.readAllLines(javaFile);
                for (int lineNum = 0; lineNum < lines.size(); lineNum++) {
                    String line = lines.get(lineNum).trim();
                    if (line.startsWith("import ")) {
                        for (String forbidden : FORBIDDEN_PACKAGES) {
                            assertThat(line)
                                    .as("File %s at line %d violates architecture constraint by importing '%s'",
                                            javaFile.getFileName(), lineNum + 1, forbidden)
                                    .doesNotContain(forbidden);
                        }
                    }
                }
            }
        }
    }
}
