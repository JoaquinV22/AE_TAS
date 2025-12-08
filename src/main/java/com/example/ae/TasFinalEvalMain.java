package com.example.ae;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.PMXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.PermutationSwapMutation;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.comparator.dominanceComparator.impl.DefaultDominanceComparator;

import com.example.ae.decoder.TasSchedule;
import com.example.ae.io.TasInstanceLoader;
import com.example.ae.model.TasInstance;
import com.example.ae.problem.TasProblem;

import org.uma.jmetal.problem.Problem;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Locale;

public class TasFinalEvalMain {

    private static final int NUM_RUNS = 30;   // cantidad de ejecuciones por instancia y algoritmo

    public static void main(String[] args) {

        // instancias
        String[] instanceFiles = {
                "instances/instancia_pequena.json",
                "instances/instancia_mediana.json",
                "instances/instancia_grande.json"
        };

        // parametros ya elegidos de NSGA-II
        final int    populationSize   = 120;
        final double crossoverProb    = 0.90;
        final double mutationProb     = 0.10;
        final int    maxEvaluations   = 10000;

        // CSV de salida
        Path outputPath = Paths.get("results", "tas_final_eval_runs.csv");

        try {
            if (outputPath.getParent() != null) {
                Files.createDirectories(outputPath.getParent());
            }

            boolean writeHeader = Files.notExists(outputPath);

            try (BufferedWriter bw = Files.newBufferedWriter(
                        outputPath,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
                 PrintWriter pw = new PrintWriter(bw)) {

                if (writeHeader) {
                    pw.println("instance,algorithm,populationSize,crossoverProb,mutationProb,run,solutionIndex,f1_makespan,f2_dissatisfaction,timeMillis");
                }

                // recorrer todas las instancias
                for (String instanceFile : instanceFiles) {
                    System.out.println("Evaluando instancia: " + instanceFile);

                    TasInstance instance;
                    try {
                        instance = TasInstanceLoader.fromJson(Paths.get(instanceFile));
                    } catch (IOException e) {
                        System.err.println("Error cargando instancia " + instanceFile);
                        e.printStackTrace();
                        continue;
                    }

                    String instanceName = Paths.get(instanceFile).getFileName().toString();

                    // problema multiobjetivo TAS para NSGA-II
                    Problem<PermutationSolution<Integer>> problem = new TasProblem(instance);

                    // algoritmo 1: NSGA-II
                    for (int run = 0; run < NUM_RUNS; run++) {

                        CrossoverOperator<PermutationSolution<Integer>> crossover =
                                new PMXCrossover(crossoverProb);

                        MutationOperator<PermutationSolution<Integer>> mutation =
                                new PermutationSwapMutation(mutationProb);

                        SelectionOperator<List<PermutationSolution<Integer>>, PermutationSolution<Integer>> selection =
                                new BinaryTournamentSelection<>(new DefaultDominanceComparator<>());

                        Algorithm<List<PermutationSolution<Integer>>> algorithm =
                                new NSGAIIBuilder<>(problem, crossover, mutation, populationSize)
                                        .setSelectionOperator(selection)
                                        .setMaxEvaluations(maxEvaluations)
                                        .build();

                        long startTime = System.currentTimeMillis();
                        algorithm.run();
                        long endTime   = System.currentTimeMillis();
                        long timeMillis = endTime - startTime;

                        List<PermutationSolution<Integer>> result = algorithm.result();

                        System.out.printf(
                                "[NSGAII] inst=%s run=%d tiempo=%d ms soluciones=%d%n",
                                instanceName, run, timeMillis, result.size()
                        );

                        int solIndex = 0;
                        for (PermutationSolution<Integer> sol : result) {
                            double f1 = sol.objectives()[0];
                            double f2 = sol.objectives()[1];

                            pw.printf(
                            		Locale.US,  // para que ponga puntos y no comas
                                    "%s,%s,%d,%.2f,%.2f,%d,%d,%.4f,%.4f,%d%n",
                                    instanceName,
                                    "NSGAII",
                                    populationSize,
                                    crossoverProb,
                                    mutationProb,
                                    run,
                                    solIndex,
                                    f1,
                                    f2,
                                    timeMillis
                            );
                            solIndex++;
                        }
                        pw.flush();
                    }

                    // algoritmo 2: GREEDY (baseline)
                    for (int run = 0; run < NUM_RUNS; run++) {

                        long startTime = System.currentTimeMillis();
                        TasSchedule schedule = TasGreedySolver.solve(instance);
                        long endTime   = System.currentTimeMillis();
                        long timeMillis = endTime - startTime;

                        double f1 = schedule.getMakespan();
                        double f2 = schedule.getDissatisfaction();

                        System.out.printf(
                                "[GREEDY] inst=%s run=%d tiempo=%d ms F1=%.4f F2=%.4f%n",
                                instanceName, run, timeMillis, f1, f2
                        );

                        // para greedy solo hay una solucion por ejecucion -> solutionIndex = 0
                        pw.printf(
                        		Locale.US,  // para que ponga puntos y no comas
                                "%s,%s,%d,%.2f,%.2f,%d,%d,%.4f,%.4f,%d%n",
                                instanceName,
                                "GREEDY",
                                0,       // populationSize no aplica
                                0.0,     // crossoverProb no aplica
                                0.0,     // mutationProb no aplica
                                run,
                                0,
                                f1,
                                f2,
                                timeMillis
                        );
                        pw.flush();
                    }
                }

            }

            System.out.println("Resultados de evaluacion final guardados en: " + outputPath.toAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
