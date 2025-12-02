package com.example.ae.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TasInstance {
    private final List<Task> tasks;
    private final List<Employee> employees;
    private final double[] skillWeightsAlpha;
    private final double lambdaOver;
    private final double lambdaOverq;
    
    private final Map<Integer, Task> taskById;

    public TasInstance(List<Task> tasks,
            List<Employee> employees,
            double[] skillWeightsAlpha,
            double lambdaOver,
            double lambdaOverq) {
			this.tasks = tasks;
			this.employees = employees;
			this.skillWeightsAlpha = skillWeightsAlpha;
			this.lambdaOver = lambdaOver;
			this.lambdaOverq = lambdaOverq;
			
			// build the map once
			this.taskById = new HashMap<>();
			for (Task t : tasks) {
			 this.taskById.put(t.id(), t);
			}
		}


    public List<Task> tasks() {
        return tasks;
    }

    public List<Employee> employees() {
        return employees;
    }
    
    public Task getTaskById(int taskId) {
        return taskById.get(taskId);
    }

    public int numberOfTasks() {
        return tasks.size();
    }

    public int numberOfEmployees() {
        return employees.size();
    }
    
    public int numberOfSkills() {
        return skillWeightsAlpha.length;
    }

    public double[] skillWeightsAlpha() {
        return Arrays.copyOf(skillWeightsAlpha, skillWeightsAlpha.length);
    }

    public double skillWeightAlpha(int k) {
        return (k >= 0 && k < skillWeightsAlpha.length) ? skillWeightsAlpha[k] : 0.0;
    }

    public double lambdaOver() {
        return lambdaOver;
    }

    public double lambdaOverq() {
        return lambdaOverq;
    }

}
