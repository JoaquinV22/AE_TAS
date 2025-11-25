package com.example.ae.model;

import java.util.List;

public class Task {
    private final int id;
    private final int duration;
    private final List<Integer> predecessors;
    private final int requiredSkill;

    // Constructor
    public Task(int id, int duration, List<Integer> predecessors, int requiredSkill) {
        this.id = id;
        this.duration = duration;
        this.predecessors = predecessors;
        this.requiredSkill = requiredSkill;
    }

    public int id() {
        return id;
    }

    public int duration() {
        return duration;
    }

    public List<Integer> predecessors() {
        return predecessors;
    }

    public int requiredSkill() {
        return requiredSkill;
    }
}
