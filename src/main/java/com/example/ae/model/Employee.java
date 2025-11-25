package com.example.ae.model;

public class Employee {
    private final int id;
    private final int skill;
    private int availableTime;

    // Constructor
    public Employee(int id, int skill, int availableTime) {
        this.id = id;
        this.skill = skill;
        this.availableTime = availableTime;
    }

    // Métodos getter y setter
    public int id() {
        return id;
    }

    public int skill() {
        return skill;
    }

    public int availableTime() {
        return availableTime;
    }

    public void setAvailableTime(int availableTime) {
        this.availableTime = availableTime;
    }
}
