package com.example.ae.model;

import java.util.Arrays;

public class Employee {
    private final int id;
    private final double[] skills;
    private int availableTime;

    // Constructor
    public Employee(int id, double[] skills, int availableTime) {
        this.id = id;
        this.skills = Arrays.copyOf(skills, skills.length);
        this.availableTime = availableTime;
    }



    // getters y setters
    public int id() {
        return id;
    }

    public double[] skills() {
        return Arrays.copyOf(skills, skills.length);
    }
    
    public double skill(int k) {
        return (k >= 0 && k < skills.length) ? skills[k] : 0.0;
    }

    public int availableTime() {
        return availableTime;
    }

    public void setAvailableTime(int availableTime) {
        this.availableTime = availableTime;
    }
}
