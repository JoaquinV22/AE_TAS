package com.example.ae.io;

import com.example.ae.model.Employee;
import com.example.ae.model.Task;
import com.example.ae.model.TasInstance;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TasInstanceLoader {

	private static final String[] SKILL_ORDER = {
		    "csharp_dotnet9",
		    "blazor_mudblazor_apexcharts",
		    "pwa",
		    "ms_sql",
		    "visual_studio"
		};

    public static class InstanceDTO {
        public double[] skillWeightsAlpha;
        public double lambdaOver;
        public double lambdaOverq;
        public List<TaskDTO> tasks;
        public List<EmployeeDTO> employees;
    }

    public static class TaskDTO {
        public int id;
        public int duration;
        public List<Integer> predecessors;
        public Map<String, Double> requiredSkills;
        public int releaseDate;
    }

    public static class EmployeeDTO {
        public int id;
        public Map<String, Double> skills;
        public int availableTime;
    }

    public static TasInstance fromJson(Path path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        InstanceDTO dto = mapper.readValue(path.toFile(), InstanceDTO.class);

        // convertir DTOs a clases de modelo
        List<Task> tasks = dto.tasks.stream()
                .map(t -> new Task(
                        t.id,
                        t.duration,
                        t.predecessors,
                        buildSkillVector(t.requiredSkills),   // Map -> double[]
                        t.releaseDate
                ))
                .collect(Collectors.toList());

        List<Employee> employees = dto.employees.stream()
                .map(e -> new Employee(
                        e.id,
                        buildSkillVector(e.skills),            // Map -> double[]
                        e.availableTime
                ))
                .collect(Collectors.toList());

        return new TasInstance(
                tasks,
                employees,
                dto.skillWeightsAlpha,
                dto.lambdaOver,
                dto.lambdaOverq
        );
    }

    // convierte { "csharp_dotnet9": 1, "blazor_mudblazor_apexcharts": 3, ... } a [1,3,...] segun SKILL_ORDER
    private static double[] buildSkillVector(Map<String, Double> byName) {
        double[] v = new double[SKILL_ORDER.length];
        if (byName == null) {
            return v;
        }
        for (int i = 0; i < SKILL_ORDER.length; i++) {
            String skillName = SKILL_ORDER[i];
            Double value = byName.get(skillName);
            v[i] = (value != null) ? value : 0.0;
        }
        return v;
    }
}
