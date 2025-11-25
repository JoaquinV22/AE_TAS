package com.example.ae.model;

import java.util.List;

public class TasInstance {
    private final List<Task> tasks;
    private final List<Employee> employees;

    public TasInstance(List<Task> tasks, List<Employee> employees) {
        this.tasks = tasks;
        this.employees = employees;
    }

    public List<Task> tasks() {
        return tasks;
    }

    public List<Employee> employees() {
        return employees;
    }

    public int numberOfTasks() {
        return tasks.size();
    }

    public int numberOfEmployees() {
        return employees.size();
    }
}
