package com.example.ae.problem;

import com.example.ae.model.TasInstance;
import com.example.ae.decoder.TasDecoder;
import com.example.ae.decoder.TasSchedule;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.solution.permutationsolution.impl.IntegerPermutationSolution;


public class TasProblem implements Problem<PermutationSolution<Integer>> {

    private TasInstance instance;

    public TasProblem(TasInstance instance) {
        this.instance = instance;
    }

    @Override
    public int numberOfVariables() { 
        return instance.numberOfTasks(); 
    }

    @Override
    public int numberOfObjectives() { return 2; }

    @Override
    public int numberOfConstraints() { return 0; }

    @Override
    public String name() { return "TAS_NSGAII"; }

    @Override
    public PermutationSolution<Integer> evaluate(PermutationSolution<Integer> solution) {
        // extraer permutacion π
        int n = solution.variables().size();
        int[] pi = new int[n];

        for (int i = 0; i < n; i++) {
            pi[i] = solution.variables().get(i);
        }

        // decodificar con heuristica TAS
        TasSchedule schedule = TasDecoder.decode(pi, instance);

        double makespan = schedule.getMakespan();              // F1
        double dissatisfaction = schedule.getDissatisfaction(); // F2

        solution.objectives()[0] = makespan;
        solution.objectives()[1] = dissatisfaction;

        return solution;
    }

    @Override
    public PermutationSolution<Integer> createSolution() {
        int nTasks = instance.numberOfTasks();

        return new IntegerPermutationSolution(
                nTasks,
                numberOfObjectives(),
                numberOfConstraints()
        );
    }

}