package com.example.ae.decoder;

import java.util.HashMap;
import java.util.Map;

import com.example.ae.model.Employee;
import com.example.ae.model.TasInstance;
import com.example.ae.model.Task;

public class TasSchedule {
    private Map<Integer, Integer> taskToEmployee = new HashMap<>();  // Relaciona taskId con empId
    private TasInstance instance;  // Instancia de TasInstance, ahora accesible desde TasSchedule
    private double dissatisfaction;
    
    // Constructor que recibe TasInstance
    public TasSchedule(TasInstance instance) {
        this.instance = instance;
    }

    // Métodos y lógica adicional...

    // Método para agregar la asignación de tarea a empleado
    public void addTaskToEmployee(int taskId, int empId) {
        taskToEmployee.put(taskId, empId);
    }

    // Método para obtener la carga de trabajo de un empleado
    public int getEmployeeLoad(int empId) {
        int load = 0;
        for (Map.Entry<Integer, Integer> entry : taskToEmployee.entrySet()) {
            if (entry.getValue() == empId) {
                // Recuperar la tarea usando el taskId y sumar su duración
                Task task = getTaskById(entry.getKey()); // Método para obtener la tarea por ID
                load += task.duration();
            }
        }
        return load;
    }

    // Método auxiliar para obtener una tarea por su ID desde la instancia de TasInstance
    private Task getTaskById(int taskId) {
        return instance.tasks().get(taskId); // Ahora podemos acceder a las tareas directamente desde instance
    }

    // Método para actualizar la insatisfacción de un empleado
    public void updateScheduleObjectives() {
        double dissatisfaction = 0;

        // Calculamos la sobrecarga (Overload) y la sobrecualificación (Overqual)
        for (Employee e : instance.employees()) {
            int load = getEmployeeLoad(e.id()); // Cargar el trabajo de cada empleado
            int availableTime = e.availableTime();

            // Calcular sobrecarga (Overload_n)
            double overload = Math.max(0, load - availableTime);

            // Calcular sobrecualificación (Overqual_n)
            double overqual = Math.max(0, e.skill() - getTaskRequiredSkillForEmployee(e.id()));  // Método para obtener el skill de la tarea

            // Añadir al valor de insatisfacción
            dissatisfaction += overload + overqual;
        }

        // Almacenar la insatisfacción total
        setDissatisfaction(dissatisfaction); // Método que deberías implementar para guardar la insatisfacción
    }

    // Método auxiliar para obtener el skill requerido de la tarea asignada a un empleado
    private int getTaskRequiredSkillForEmployee(int empId) {
        // Aquí debes determinar cuál es el skill necesario de la tarea que está asignada a este empleado.
        // Esto depende de tu modelo y de cómo gestionas las asignaciones.
        return 1; // Este es un valor de ejemplo, debes implementarlo según tu lógica.
    }


    // Método para obtener el mapa de asignaciones de tareas a empleados
    public Map<Integer, Integer> getTaskToEmployee() {
        return taskToEmployee;
    }
    
    public double getMakespan() {
        int makespan = 0;
        for (Map.Entry<Integer, Integer> entry : taskToEmployee.entrySet()) {
            Task task = getTaskById(entry.getKey());  // Obtener la tarea por ID
            int finishTime = taskFinishTime(task);  // Calcular el tiempo de finalización de la tarea
            makespan = Math.max(makespan, finishTime);
        }
        return makespan;
    }
    
 // Método para calcular el tiempo de finalización de la tarea
    private int taskFinishTime(Task task) {
        // Aquí calculamos el tiempo de finalización de la tarea (asumido)
        // Este es solo un ejemplo de cómo puedes hacerlo, ajusta según el modelo real.
        return task.duration(); // Simplemente devuelve la duración de la tarea por ahora
    }
    
    
    public double getDissatisfaction() {
        return dissatisfaction;
    }
    
    public void setDissatisfaction(double dissatisfaction) {
        this.dissatisfaction = dissatisfaction;
    }
}
