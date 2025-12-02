package com.example.ae.model;

import java.util.Arrays;
import java.util.List;

public class Task {
    private final int id;
    private final int duration;
    private final List<Integer> predecessors;
    private final double[] requiredSkills;
    private final int releaseDate; // tiempo de inicio mas temprano permitida

    // Constructor
    public Task(int id,
            int duration,
            List<Integer> predecessors,
            double[] requiredSkills,
            int releaseDate) {
	    this.id = id;
	    this.duration = duration;
	    this.predecessors = predecessors;
	    this.requiredSkills = Arrays.copyOf(requiredSkills, requiredSkills.length);
	    this.releaseDate = releaseDate;
    }
    
    public Task(int id,
            int duration,
            List<Integer> predecessors,
            double[] requiredSkills) {
    	this(id, duration, predecessors, requiredSkills, 0);
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

    public double[] requiredSkills() {
        return Arrays.copyOf(requiredSkills, requiredSkills.length);
    }

    public double requiredSkill(int k) {
        return (k >= 0 && k < requiredSkills.length) ? requiredSkills[k] : 0.0;
    }

    // Backwards-compatible: first skill as int
    public int requiredSkill() {
        return (int) Math.round(requiredSkill(0));
    }

    public int releaseDate() {
        return releaseDate;
    }

}
