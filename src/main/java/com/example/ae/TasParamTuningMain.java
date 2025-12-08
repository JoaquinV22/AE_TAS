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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;


public class TasParamTuningMain {

    private static final int NUM_RUNS = 30;  // cantidad de ejecuciones por configuracion

    public static void main(String[] args) {

        // instancias
        String[] tuningInstanceFiles = {
                "instances/instancia_pequena.json",
                "instances/instancia_mediana.json"
        };

        // rangos de parametros a explorar
        int[] populationSizes   = {60, 120, 200};
        double[] crossoverProbs = {0.7, 0.9, 1.0};
        double[] mutationProbs  = {0.05, 0.10, 0.20};
        int maxEvaluations      = 10000;

        // CSV de salida
        Path outputPath = Paths.get("results", "tas_nsgaii_config_runs.csv");

        try {
            // crear carpeta "results" si no existe
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
                    pw.println("instance,populationSize,crossoverProb,mutationProb,run,solutionIndex,f1_makespan,f2_dissatisfaction,timeMillis");
                }

                // bucle sobre las instancias de configuracion
                for (String instanceFile : tuningInstanceFiles) {

                    System.out.println("Ajustando parametros en instancia: " + instanceFile);

                    // cargar la instancia
                    TasInstance instance;
                    try {
                        instance = TasInstanceLoader.fromJson(Paths.get(instanceFile));
                    } catch (IOException e) {
                        System.err.println("Error cargando instancia " + instanceFile);
                        e.printStackTrace();
                        // pasar a la siguiente instancia
                        continue;
                    }

                    // nombre de la instancia para el CSV
                    String instanceName = Paths.get(instanceFile).getFileName().toString();

                    // problema asociado a esta instancia
                    Problem<PermutationSolution<Integer>> problem = new TasProblem(instance);

                    // bucles para recorrer todas las combinaciones de parametros
                    for (int populationSize : populationSizes) {
                        for (double crossoverProbability : crossoverProbs) {
                            for (double mutationProbability : mutationProbs) {

                                // varias ejecuciones por configuracion
                                for (int run = 0; run < NUM_RUNS; run++) {

                                    // operadores con los parametros de esta combinacion
                                    CrossoverOperator<PermutationSolution<Integer>> crossover =
                                            new PMXCrossover(crossoverProbability);

                                    MutationOperator<PermutationSolution<Integer>> mutation =
                                            new PermutationSwapMutation(mutationProbability);

                                    SelectionOperator<List<PermutationSolution<Integer>>, PermutationSolution<Integer>> selection =
                                            new BinaryTournamentSelection<>(new DefaultDominanceComparator<>());

                                    Algorithm<List<PermutationSolution<Integer>>> algorithm =
                                            new NSGAIIBuilder<>(problem, crossover, mutation, populationSize)
                                                    .setSelectionOperator(selection)
                                                    .setMaxEvaluations(maxEvaluations)
                                                    .build();

                                    long startTime = System.currentTimeMillis();
                                    algorithm.run();
                                    long endTime = System.currentTimeMillis();
                                    long timeMillis = endTime - startTime;

                                    List<PermutationSolution<Integer>> result = algorithm.result();

                                    System.out.printf(
                                            "Instance=%s | pop=%d, pc=%.2f, pm=%.2f, run=%d, tiempo=%d ms, soluciones=%d%n",
                                            instanceName,
                                            populationSize,
                                            crossoverProbability,
                                            mutationProbability,
                                            run,
                                            timeMillis,
                                            result.size()
                                    );

                                    // escribir soluciones en el CSV
                                    int solIndex = 0;
                                    for (PermutationSolution<Integer> sol : result) {
                                        double f1 = sol.objectives()[0]; // makespan
                                        double f2 = sol.objectives()[1]; // insatisfaccion

                                        // instance,pop,pC,pM,run,solutionIndex,f1,f2,timeMillis
                                        pw.printf(
                                                "%s,%d,%.2f,%.2f,%d,%d,%.4f,%.4f,%d%n",
                                                instanceName,
                                                populationSize,
                                                crossoverProbability,
                                                mutationProbability,
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
                            }
                        }
                    }
                }

            }

            System.out.println("Resultados de configuracion guardados en: " + outputPath.toAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}