package com.example.hegemony.application.rules;

import com.example.hegemony.domain.model.PolicyCourse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

@Component
public class RuleSpecLoader {
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final String ruleSpecPath;

    public RuleSpecLoader(@Value("${hegemony.rule-spec-path:../specs/rule-spec.yaml}") String ruleSpecPath) {
        this.ruleSpecPath = ruleSpecPath;
    }

    public Map<PolicyCourse, Map<PolicyCourse, Integer>> loadWorkerTaxMatrix() {
        try {
            JsonNode root = readRootNode();
            JsonNode matrixNode = root.path("taxation")
                    .path("worker_income_tax_matrix")
                    .path("policy_2_labor_market_to_policy_3_taxation");
            return parseMatrix(matrixNode);
        } catch (Exception ex) {
            return defaultMatrix();
        }
    }

    private JsonNode readRootNode() throws IOException {
        Path external = Path.of(ruleSpecPath).toAbsolutePath().normalize();
        if (Files.exists(external)) {
            try (InputStream in = Files.newInputStream(external)) {
                return yamlMapper.readTree(in);
            }
        }

        Resource resource = new ClassPathResource("specs/rule-spec.yaml");
        if (resource.exists()) {
            try (InputStream in = resource.getInputStream()) {
                return yamlMapper.readTree(in);
            }
        }

        throw new IllegalStateException("rule-spec.yaml not found at " + external + " or classpath.");
    }

    private Map<PolicyCourse, Map<PolicyCourse, Integer>> parseMatrix(JsonNode matrixNode) {
        Map<PolicyCourse, Map<PolicyCourse, Integer>> matrix = new EnumMap<>(PolicyCourse.class);
        for (PolicyCourse labor : PolicyCourse.values()) {
            JsonNode rowNode = matrixNode.path(labor.name());
            Map<PolicyCourse, Integer> row = new EnumMap<>(PolicyCourse.class);
            for (PolicyCourse tax : PolicyCourse.values()) {
                row.put(tax, rowNode.path(tax.name()).asInt(defaultMatrix().get(labor).get(tax)));
            }
            matrix.put(labor, row);
        }
        return matrix;
    }

    private Map<PolicyCourse, Map<PolicyCourse, Integer>> defaultMatrix() {
        Map<PolicyCourse, Map<PolicyCourse, Integer>> matrix = new EnumMap<>(PolicyCourse.class);

        Map<PolicyCourse, Integer> rowA = new EnumMap<>(PolicyCourse.class);
        rowA.put(PolicyCourse.A, 0);
        rowA.put(PolicyCourse.B, 1);
        rowA.put(PolicyCourse.C, 2);
        matrix.put(PolicyCourse.A, rowA);

        Map<PolicyCourse, Integer> rowB = new EnumMap<>(PolicyCourse.class);
        rowB.put(PolicyCourse.A, 1);
        rowB.put(PolicyCourse.B, 2);
        rowB.put(PolicyCourse.C, 3);
        matrix.put(PolicyCourse.B, rowB);

        Map<PolicyCourse, Integer> rowC = new EnumMap<>(PolicyCourse.class);
        rowC.put(PolicyCourse.A, 2);
        rowC.put(PolicyCourse.B, 3);
        rowC.put(PolicyCourse.C, 4);
        matrix.put(PolicyCourse.C, rowC);

        return matrix;
    }
}

