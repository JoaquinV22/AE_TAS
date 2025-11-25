package com.example.ae.decoder;

import com.example.ae.model.TasInstance;
import com.example.ae.model.Task;

import java.util.Map;

import com.example.ae.model.Employee;

public class TasDecoder {

	public static TasSchedule decode(int[] pi, TasInstance instance) {
	    int numEmployees = instance.numberOfEmployees();
	    int[] employeeAvailableTime = new int[numEmployees];
	    int numTasks = instance.numberOfTasks();
	    int[] taskFinish = new int[numTasks];

	    TasSchedule schedule = new TasSchedule(instance);

	    for (int pos = 0; pos < pi.length; pos++) {
	        Task task = instance.tasks().get(pi[pos]);

	        // Calcular el earliestStart según las precedencias
	        int earliestStart = 0;
	        for (int predId : task.predecessors()) {
	            earliestStart = Math.max(earliestStart, taskFinish[predId]);
	        }

	        // Elegir el empleado que minimice la función de coste local
	        int bestEmpIndex = -1;
	        double bestScore = Double.POSITIVE_INFINITY;

	        for (Employee e : instance.employees()) {
	            int empIndex = e.id(); // Asumimos id = índice
	            int start = Math.max(earliestStart, employeeAvailableTime[empIndex]);

	            // Verificar si el empleado tiene la habilidad requerida y si tiene tiempo disponible
	            if (e.skill() < task.requiredSkill()) {
	                continue; // Si no tiene la habilidad necesaria, continuar con el siguiente empleado
	            }

	            double localCost = localAssignmentCost(task, e, start, instance);

	            if (localCost < bestScore) {
	                bestScore = localCost;
	                bestEmpIndex = empIndex;
	            }
	        }

	        // Asignar al mejor empleado
	        int startTime = Math.max(earliestStart, employeeAvailableTime[bestEmpIndex]);
	        int finishTime = startTime + task.duration();

	        schedule.getTaskToEmployee().put(task.id(), bestEmpIndex);
	        employeeAvailableTime[bestEmpIndex] = finishTime;
	        taskFinish[task.id()] = finishTime;
	    }

	    return schedule;
	}


    // ========= métodos auxiliares =========

    private static double localAssignmentCost(Task task, Employee e, int start, TasInstance instance) {
        // Penalización de habilidad: si el empleado no tiene la habilidad requerida, penalizamos
        if (e.skill() < task.requiredSkill()) {
            return Double.POSITIVE_INFINITY;  // Inviable: no tiene la habilidad necesaria
        }

        // Calcular la hora de finalización de la tarea
        int finish = start + task.duration();

        // Penalización por sobrecarga: si el empleado está trabajando más horas de las disponibles
        double load = finish - start;
        double overwork = Math.max(0, load - e.availableTime()); // exceso de trabajo

        // Penalización por sobrecualificación: si el empleado tiene más habilidad de la necesaria
        double skillMismatch = Math.max(0, e.skill() - task.requiredSkill());

        // Coste total (simplificado)
        return finish + overwork + skillMismatch;
    }


    public void updateScheduleObjectives(TasSchedule schedule, TasInstance instance) {
        double dissatisfaction = 0;

        // Calculamos la sobrecarga (Overload) y la sobrecualificación (Overqual)
        for (Employee e : instance.employees()) {
            int load = schedule.getEmployeeLoad(e.id()); // Cargar el trabajo de cada empleado
            int availableTime = e.availableTime();

            // Calcular sobrecarga (Overload_n)
            double overload = Math.max(0, load - availableTime);

            // Calcular sobrecualificación (Overqual_n) basándonos en las tareas asignadas
            double overqual = 0;
            for (Map.Entry<Integer, Integer> entry : schedule.getTaskToEmployee().entrySet()) {
                if (entry.getValue() == e.id()) {
                    Task task = instance.tasks().get(entry.getKey());  // Obtener la tarea por ID
                    overqual += Math.max(0, e.skill() - task.requiredSkill());
                }
            }

            // Añadir al valor de insatisfacción
            dissatisfaction += overload + overqual;
        }

        // Almacenar la insatisfacción total
        schedule.setDissatisfaction(dissatisfaction);
    }



}
