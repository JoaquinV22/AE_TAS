package com.example.ae.visual;

import com.example.ae.decoder.TasSchedule;
import com.example.ae.model.Employee;
import com.example.ae.model.TasInstance;
import com.example.ae.model.Task;

import javax.swing.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TasSchedulePlotter {

    public static void showSchedule(TasSchedule schedule, String title) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(title);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new SchedulePanel(schedule));
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setMinimumSize(new Dimension(1000, 600));
            frame.setLocationRelativeTo(null);
            
            frame.setVisible(true);
        });
    }

    private static class SchedulePanel extends JPanel {
        private final TasSchedule schedule;
        private final TasInstance instance;
        private final Map<Integer, Integer> employeeRow = new HashMap<>();
        
        // info de cada barra para tooltips
        private static class BarInfo {
            Rectangle rect;
            int taskId;
            int empId;
            int start;
            int finish;
        }
        
        private final List<BarInfo> bars = new ArrayList<>();

        SchedulePanel(TasSchedule schedule) {
            this.schedule = schedule;
            this.instance = schedule.getInstance();

            int row = 0;
            for (Employee e : instance.employees()) {
                employeeRow.put(e.id(), row++);
            }

            // habilitar tooltips
            setToolTipText("");
            ToolTipManager.sharedInstance().registerComponent(this);
            
            ToolTipManager.sharedInstance().setInitialDelay(80);
            ToolTipManager.sharedInstance().setDismissDelay(10000);
        }


        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);

            bars.clear(); // reconstruimos la lista de barras cada vez que repintamos

            int marginLeft = 80;
            int marginTop = 50;
            int rowHeight = 40;
            int rowGap = 10;

            int makespan = schedule.getMakespan();
            if (makespan <= 0) {
                return;
            }

            int availableWidth = getWidth() - marginLeft - 40;
            if (availableWidth <= 0) {
                availableWidth = 1;
            }
            double pxPerUnit = (double) availableWidth / (double) makespan;

            // eje y: empleados
            g.setFont(getFont().deriveFont(Font.BOLD, 12f));
            for (Employee e : instance.employees()) {
                int row = employeeRow.get(e.id());
                int yCenter = marginTop + row * (rowHeight + rowGap) + rowHeight / 2;
                g.drawString("Emp " + e.id(), 10, yCenter + 4);
            }

            // eje x: tiempo
            g.drawLine(marginLeft, marginTop - 20,
                       marginLeft + availableWidth, marginTop - 20);
            int tickStep = Math.max(1, makespan / 10);
            g.setFont(getFont().deriveFont(Font.PLAIN, 10f));
            for (int t = 0; t <= makespan; t += tickStep) {
                int x = marginLeft + (int) Math.round(t * pxPerUnit);
                g.drawLine(x, marginTop - 25, x, marginTop - 15);
                g.drawString(Integer.toString(t), x - 5, marginTop - 30);
            }

            // barras de tareas
            g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
            FontMetrics fm = g.getFontMetrics();

            for (Task task : instance.tasks()) {
                int taskId = task.id();
                Integer empId = schedule.getEmployeeOfTask(taskId);
                if (empId == null) {
                    continue; // tarea no asignada
                }
                Integer row = employeeRow.get(empId);
                if (row == null) {
                    continue;
                }

                int start = schedule.getTaskStartTime(taskId);
                int finish = schedule.getTaskFinishTime(taskId);
                int duration = finish - start;
                if (duration <= 0) {
                    continue;
                }

                int x = marginLeft + (int) Math.round(start * pxPerUnit);
                int width = Math.max(1, (int) Math.round(duration * pxPerUnit));
                int y = marginTop + row * (rowHeight + rowGap);
                int height = rowHeight - 4;

                Rectangle rect = new Rectangle(x, y, width, height);

                // guardar info para tooltip
                BarInfo info = new BarInfo();
                info.rect = rect;
                info.taskId = taskId;
                info.empId = empId;
                info.start = start;
                info.finish = finish;
                bars.add(info);

                // dibujar barra
                g.setColor(new Color(100, 149, 237));
                g.fillRect(x, y, width, height);

                g.setColor(Color.BLACK);
                g.drawRect(x, y, width, height);

                // texto solo si entra completo en la barra
                String label = "T" + taskId + " [" + start + "," + finish + ")";
                int labelWidth = fm.stringWidth(label);
                int padding = 6; // margen a izquierda/derecha

                if (width >= labelWidth + 2 * padding) {
                    int labelX = x + padding;
                    int labelY = y + (rowHeight / 2) + 4;
                    g.drawString(label, labelX, labelY);
                }
            }
        }
        
        @Override
        public String getToolTipText(MouseEvent event) {
            Point p = event.getPoint();
            for (BarInfo bar : bars) {
                if (bar.rect.contains(p)) {
                    return String.format("Empleado %d - Tarea %d [%d, %d)",
                            bar.empId, bar.taskId, bar.start, bar.finish);
                }
            }
            return null;
        }


    }
}
