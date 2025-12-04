package com.example.ae.io;

import com.example.ae.model.Employee;
import com.example.ae.model.Task;
import com.example.ae.model.TasInstance;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class TasInstanceLoader {

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
        public double[] requiredSkills;
        public int releaseDate;
    }

    public static class EmployeeDTO {
        public int id;
        public double[] skills;
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
                        t.requiredSkills,
                        t.releaseDate
                ))
                .collect(Collectors.toList());

        List<Employee> employees = dto.employees.stream()
                .map(e -> new Employee(
                        e.id,
                        e.skills,
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
}
