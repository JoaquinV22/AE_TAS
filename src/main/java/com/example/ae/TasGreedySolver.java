package com.example.ae;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.example.ae.decoder.TasDecoder;
import com.example.ae.decoder.TasSchedule;
import com.example.ae.model.TasInstance;
import com.example.ae.model.Task;

public class TasGreedySolver {

    // Baseline greedy:
    //  - ordena las tareas por fecha de liberacion y duracion
    //  - usa ese orden como permutacion
    //  - llama al mismo decodificador que el NSGA-II
    public static TasSchedule solve(TasInstance instance) {
        List<Task> tasks = instance.tasks();
        int n = tasks.size();

        // lista de indices 0..n-1
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            order.add(i);
        }

        // ordenar por releaseDate y luego por duracion (crecientes)
        Collections.sort(order, new Comparator<Integer>() {
            @Override
            public int compare(Integer i1, Integer i2) {
                Task t1 = tasks.get(i1);
                Task t2 = tasks.get(i2);

                int cmp = Integer.compare(t1.releaseDate(), t2.releaseDate());
                if (cmp != 0) {
                    return cmp;
                }
                // si misma releaseDate, ordenar por duracion
                return Integer.compare(t1.duration(), t2.duration());
            }
        });

        // construir la permutacion pi
        int[] pi = new int[n];
        for (int i = 0; i < n; i++) {
            pi[i] = order.get(i);
        }

        // usar el mismo decodificador que el NSGA-II
        return TasDecoder.decode(pi, instance);
    }
}
