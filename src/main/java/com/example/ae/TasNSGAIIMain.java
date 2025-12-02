package com.example.ae;

import com.example.ae.model.Employee;
import com.example.ae.model.TasInstance;
import com.example.ae.model.Task;
import com.example.ae.problem.TasProblem;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.PMXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.PermutationSwapMutation;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.comparator.dominanceComparator.impl.DefaultDominanceComparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TasNSGAIIMain {

    public static void main(String[] args) {
        // ejemplo con 3 skill dimensions
        List<Task> tasks = new ArrayList<>();
        tasks.add(new Task(0, 3, List.of(),          new double[]{1, 0, 0}, 0));
        tasks.add(new Task(1, 2, List.of(0),        new double[]{0, 1, 0}, 0));
        tasks.add(new Task(2, 4, List.of(0, 1),     new double[]{0, 0, 1}, 0));

        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee(0, new double[]{1, 1, 0}, 8)); // E0: decente en habilidad 0/1
        employees.add(new Employee(1, new double[]{1, 2, 1}, 8)); // E1: mejor en habilidad 1/2

        // pesos α_k para sobrecalificacion
        double[] alpha = new double[]{1.0, 1.0, 1.0};
        double lambdaOver = 2.0;  // la sobrecarga se penaliza mas
        double lambdaOverq = 1.0; // sobrecalificacion

        TasInstance instance = new TasInstance(tasks, employees, alpha, lambdaOver, lambdaOverq);

        Problem<PermutationSolution<Integer>> problem = new TasProblem(instance);

        CrossoverOperator<PermutationSolution<Integer>> crossover = new PMXCrossover(0.8);
        MutationOperator<PermutationSolution<Integer>> mutation = new PermutationSwapMutation(0.1);
        SelectionOperator<List<PermutationSolution<Integer>>, PermutationSolution<Integer>> selection =
                new BinaryTournamentSelection<>(new DefaultDominanceComparator<>());

        int populationSize = 120;
        int maxEvaluations = 10000;

        Algorithm<List<PermutationSolution<Integer>>> algorithm =
                new NSGAIIBuilder<>(problem, crossover, mutation, populationSize)
                        .setSelectionOperator(selection)
                        .setMaxEvaluations(maxEvaluations)
                        .build();

        algorithm.run();
        List<PermutationSolution<Integer>> result = algorithm.result();

        for (PermutationSolution<Integer> sol : result) {
            System.out.println("Solucion: F1 = " + sol.objectives()[0] +
                               "  F2 = " + sol.objectives()[1]);
        }
    }
}
