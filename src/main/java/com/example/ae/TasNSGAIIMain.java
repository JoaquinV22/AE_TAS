package com.example.ae;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAII;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.PMXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.PermutationSwapMutation;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.comparator.dominanceComparator.DominanceComparator;

import org.uma.jmetal.util.comparator.dominanceComparator.impl.DefaultDominanceComparator;


import com.example.ae.model.Employee;
import com.example.ae.model.TasInstance;
import com.example.ae.model.Task;
import com.example.ae.problem.TasProblem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TasNSGAIIMain {
    public static void main(String[] args) {
        // Crear listas de tareas y empleados
        List<Task> tasks = new ArrayList<>();
        tasks.add(new Task(0, 5, Arrays.asList(1), 1)); // Tarea 0, duración 5, habilidad 1
        tasks.add(new Task(1, 3, Arrays.asList(2), 2)); // Tarea 1, duración 3, habilidad 2
        tasks.add(new Task(2, 4, Arrays.asList(1, 2), 1)); // Tarea 2, duración 4, habilidad 1

        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee(0, 1, 8)); // Empleado 0, habilidad 1, 8 horas disponibles
        employees.add(new Employee(1, 2, 8)); // Empleado 1, habilidad 2, 8 horas disponibles

        // Crear la instancia del problema
        TasInstance instance = new TasInstance(tasks, employees);

        // Crear el problema para jMetal
        Problem<PermutationSolution<Integer>> problem = new TasProblem(instance);

        // Crear operadores
        CrossoverOperator<PermutationSolution<Integer>> crossover = new PMXCrossover(0.8);
        MutationOperator<PermutationSolution<Integer>> mutation = new PermutationSwapMutation<>(0.1);

        // Operador de selección: Tournament Selection
        SelectionOperator<List<PermutationSolution<Integer>>, PermutationSolution<Integer>> selection =
        	    new BinaryTournamentSelection<>();


        // Parámetros principales
        int populationSize = 120;
        int offspringPopulationSize = 120; // puede ajustarse según el diseño
        int maxEvaluations = 10000;
        
        DominanceComparator<PermutationSolution<Integer>> dominanceComparator =
        	    new DefaultDominanceComparator<>();

        // Crear el algoritmo NSGA-II utilizando NSGAIIBuilder
        NSGAIIBuilder<PermutationSolution<Integer>> builder =
        	    new NSGAIIBuilder<>(problem, crossover, mutation, populationSize)
        	    	//.setOffspringPopulationSize(offspringPopulationSize)
        	        .setSelectionOperator(selection)
        	        .setMaxEvaluations(maxEvaluations)
        	        .setDominanceComparator(dominanceComparator);

        // Crear el algoritmo NSGA-II
        Algorithm<List<PermutationSolution<Integer>>> algorithm = builder.build();

        // Ejecutar el algoritmo
        algorithm.run();

        // Obtener el resultado
        List<PermutationSolution<Integer>> result = algorithm.result();

        // Mostrar soluciones
        for (PermutationSolution<Integer> sol : result) {
            System.out.println("Solución: F1=" + sol.objectives()[0] + "  F2=" + sol.objectives()[1]);
        }
    }
}
