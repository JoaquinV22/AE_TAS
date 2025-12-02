package com.example.ae.decoder;

import com.example.ae.model.Employee;
import com.example.ae.model.TasInstance;
import com.example.ae.model.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TasDecoder {

    public static TasSchedule decode(int[] pi, TasInstance instance) {
        List<Task> tasks = instance.tasks();
        List<Employee> employees = instance.employees();

        int numTasks = tasks.size();
        int numEmployees = employees.size();

        TasSchedule schedule = new TasSchedule(instance);

        // disponibilidad por empleado (tiempo final de la ultima tarea asignada)
        int[] employeeAvailableTime = new int[numEmployees];

        // Map taskId -> index in tasks list (to avoid id==index assumptions)
        Map<Integer, Integer> taskIndexById = new HashMap<>();
        for (int idx = 0; idx < numTasks; idx++) {
            taskIndexById.put(tasks.get(idx).id(), idx);
        }

        boolean[] scheduled = new boolean[numTasks];
        int scheduledCount = 0;

        while (scheduledCount < numTasks) {
            boolean progress = false;

            for (int pos = 0; pos < numTasks; pos++) {
                int taskIndex = pi[pos];
                if (taskIndex < 0 || taskIndex >= numTasks) {
                    throw new IllegalArgumentException("Permutation contains invalid task index: " + taskIndex);
                }
                if (scheduled[taskIndex]) {
                    continue;
                }

                Task task = tasks.get(taskIndex);

                // Check predecessor completion and compute earliest start
                boolean allPredScheduled = true;
                int earliestStart = task.releaseDate(); // respect release date

                for (Integer predId : task.predecessors()) {
                    Integer predIndex = taskIndexById.get(predId);
                    if (predIndex == null || !scheduled[predIndex]) {
                        allPredScheduled = false;
                        break;
                    } else {
                        Integer predFinish = schedule.getTaskFinishTime(predId);
                        if (predFinish != null && predFinish > earliestStart) {
                            earliestStart = predFinish;
                        }
                    }
                }

                if (!allPredScheduled) {
                    continue;
                }

                // Choose best employee for this task
                int bestEmpIndex = -1;
                int bestStart = Integer.MAX_VALUE;
                int bestFinish = Integer.MAX_VALUE;
                double bestCost = Double.POSITIVE_INFINITY;

                for (int empIdx = 0; empIdx < numEmployees; empIdx++) {
                    Employee e = employees.get(empIdx);

                    if (!hasSkills(e, task)) {
                        continue;
                    }

                    int start = Math.max(earliestStart, employeeAvailableTime[empIdx]);
                    int finish = start + task.duration();

                    double cost = localAssignmentCost(schedule, instance, e, task, start, finish);

                    if (cost < bestCost) {
                        bestCost = cost;
                        bestEmpIndex = empIdx;
                        bestStart = start;
                        bestFinish = finish;
                    }
                }

                if (bestEmpIndex == -1) {
                    // No feasible employee for this ready task
                    throw new IllegalStateException(
                            "No feasible employee for task " + task.id() + " given current schedule");
                }

                Employee chosen = employees.get(bestEmpIndex);
                schedule.assignTask(task.id(), chosen.id(), bestStart, bestFinish);
                employeeAvailableTime[bestEmpIndex] = bestFinish;

                scheduled[taskIndex] = true;
                scheduledCount++;
                progress = true;
            }

            if (!progress) {
                throw new IllegalStateException(
                        "Decoder stalled: could not schedule all tasks. Check precedence or time windows.");
            }
        }

        schedule.recomputeObjectives();
        return schedule;
    }

    /** Check multi-skill feasibility: empSkill_k ≥ reqSkill_k for all k where reqSkill_k > 0 */
    private static boolean hasSkills(Employee e, Task t) {
        double[] empSkills = e.skills();
        double[] reqSkills = t.requiredSkills();

        int dim = Math.max(empSkills.length, reqSkills.length);
        for (int k = 0; k < dim; k++) {
            double required = (k < reqSkills.length) ? reqSkills[k] : 0.0;
            if (required <= 0.0) {
                continue;
            }
            double level = (k < empSkills.length) ? empSkills[k] : 0.0;
            if (level < required) {
                return false;
            }
        }
        return true;
    }

    /**
     * Local heuristic cost: finish time + λ_over * overload + λ_overq * Σ_k α_k * positive overqualification
     */
    private static double localAssignmentCost(TasSchedule schedule,
                                              TasInstance instance,
                                              Employee e,
                                              Task task,
                                              int start,
                                              int finish) {
        int loadBefore = schedule.getEmployeeLoad(e.id());
        int loadAfter = loadBefore + task.duration();

        double overload = Math.max(0.0, loadAfter - e.availableTime());

        double overqual = 0.0;
        double[] empSkills = e.skills();
        double[] reqSkills = task.requiredSkills();
        int dim = Math.max(empSkills.length, reqSkills.length);

        for (int k = 0; k < dim; k++) {
            double required = (k < reqSkills.length) ? reqSkills[k] : 0.0;
            if (required <= 0.0) {
                continue;
            }
            double level = (k < empSkills.length) ? empSkills[k] : 0.0;
            double diff = level - required;
            if (diff > 0.0) {
                double alpha = instance.skillWeightAlpha(k);
                overqual += alpha * diff;
            }
        }

        double weightedOverload = instance.lambdaOver() * overload;
        double weightedOverqual = instance.lambdaOverq() * overqual;

        return finish + weightedOverload + weightedOverqual;
    }
}
