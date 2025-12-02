package com.example.ae.decoder;

import java.util.HashMap;
import java.util.Map;

import com.example.ae.model.Employee;
import com.example.ae.model.TasInstance;
import com.example.ae.model.Task;

public class TasSchedule {
    private Map<Integer, Integer> taskToEmployee = new HashMap<>();  // relaciona taskId con empId
    private TasInstance instance;
    private double dissatisfaction;
    
    // mapas para tiempos de inicio y fin por tarea
    private Map<Integer, Integer> taskStartTime = new HashMap<>();
    private Map<Integer, Integer> taskFinishTime = new HashMap<>();

    private int makespan = 0;

    
    // constructor que recibe TasInstance
    public TasSchedule(TasInstance instance) {
        this.instance = instance;
    }

    // agregar la asignacion de tarea a empleado
    public void addTaskToEmployee(int taskId, int empId) {
        taskToEmployee.put(taskId, empId);
    }
    
    // registrar una tarea: guarda empleado, inicio y fin
    public void assignTask(int taskId, int empId, int startTime, int finishTime) {
        taskToEmployee.put(taskId, empId);
        taskStartTime.put(taskId, startTime);
        taskFinishTime.put(taskId, finishTime);
        if (finishTime > makespan) {
            makespan = finishTime;
        }
    }

    public int getTaskStartTime(int taskId) {
        return taskStartTime.getOrDefault(taskId, 0);
    }

    public int getTaskFinishTime(int taskId) {
        return taskFinishTime.getOrDefault(taskId, 0);
    }


    // obtener la carga de trabajo de un empleado
    public int getEmployeeLoad(int empId) {
        int load = 0;
        for (Map.Entry<Integer,Integer> entry : taskToEmployee.entrySet()) {
            int taskId = entry.getKey();
            int assignedEmp = entry.getValue();
            if (assignedEmp != empId) continue;

            Task task = instance.getTaskById(taskId); // now safe
            load += task.duration();
        }
        return load;
    }


    // funcion auxiliar para obtener una tarea por su ID desde la instancia de TasInstance
    private Task getTaskById(int taskId) {
    	return instance.getTaskById(taskId);
    }

    // actualizar la insatisfaccion de un empleado
    public void recomputeObjectives() {
        int maxFinish = 0;
        for (int finish : taskFinishTime.values()) {
            if (finish > maxFinish) {
                maxFinish = finish;
            }
        }
        this.makespan = maxFinish;
        this.dissatisfaction = computeDissatisfaction();
    }
    
    private double computeDissatisfaction() {
        double total = 0.0;

        for (Employee e : instance.employees()) {
            int empId = e.id();
            int load = getEmployeeLoad(empId);

            double overload = Math.max(0.0, load - e.availableTime());
            double overqual = 0.0;

            // Sum over all tasks assigned to this employee
            for (Task t : instance.tasks()) {
                Integer assigned = taskToEmployee.get(t.id());
                if (assigned == null || assigned != empId) {
                    continue;
                }

                double[] req = t.requiredSkills();
                double[] empSkills = e.skills();
                int dim = Math.max(req.length, empSkills.length);

                for (int k = 0; k < dim; k++) {
                    double required = (k < req.length) ? req[k] : 0.0;
                    if (required <= 0.0) {
                        continue; // skill not needed
                    }
                    double level = (k < empSkills.length) ? empSkills[k] : 0.0;
                    double diff = level - required;
                    if (diff > 0.0) {
                        double alpha = instance.skillWeightAlpha(k);
                        overqual += alpha * diff;
                    }
                }
            }

            double empDiss =
                    instance.lambdaOver() * overload +
                    instance.lambdaOverq() * overqual;

            total += empDiss;
        }

        return total;
    }




    // funcion auxiliar para obtener el skill requerido de la tarea asignada a un empleado
    private int getTaskRequiredSkillForEmployee(int empId) {
        // Aquí hay que determinar cuál es el skill necesario de la tarea que está asignada a este empleado.
        // Esto depende del modelo y de cómo gestionas las asignaciones.
        return 1; // Este es un valor de ejemplo, hay que implementarlo según la lógica.
    }


    // Método para obtener el mapa de asignaciones de tareas a empleados
    public Map<Integer, Integer> getTaskToEmployee() {
        return taskToEmployee;
    }
    
    public int getMakespan() {
        return makespan;
    }

    public void setMakespan(int makespan) {
        this.makespan = makespan;
    }
    
    
    public double getDissatisfaction() {
        return dissatisfaction;
    }
    
    public void setDissatisfaction(double dissatisfaction) {
        this.dissatisfaction = dissatisfaction;
    }
}
