package com.example.ae;

import com.example.ae.decoder.TasDecoder;
import com.example.ae.decoder.TasSchedule;
import com.example.ae.io.TasInstanceLoader;
import com.example.ae.visual.TasSchedulePlotter;
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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TasSingleRunMain {

    public static void main(String[] args) {
    	
    	
    	TasInstance instance = null;
		try {
			instance = TasInstanceLoader.fromJson(
			        Paths.get("instances/instancia_mediana.json")
			);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
        
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

            // extraer la permutacion
            int n = sol.variables().size();
            int[] pi = new int[n];
            for (int i = 0; i < n; i++) {
                pi[i] = sol.variables().get(i);
            }

            // decodificar a un schedule
            TasSchedule schedule = TasDecoder.decode(pi, instance);

            // mostrar grafico en lugar del texto
            String title = String.format("Schedule - F1=%.2f F2=%.2f",
                                         sol.objectives()[0],
                                         sol.objectives()[1]);
            TasSchedulePlotter.showSchedule(schedule, title);

            // imprimir el schedule
            System.out.println(schedule);
        }
    }
}