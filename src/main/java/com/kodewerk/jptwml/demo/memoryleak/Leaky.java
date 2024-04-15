package com.kodewerk.jptwml.demo.memoryleak;

/********************************************
 * Copyright (c) 2019 Kirk Pepperdine
 * All right reserved
 ********************************************/

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.lang.management.ManagementFactory;
import java.util.concurrent.*;

public class Leaky extends JFrame {
    private final LeakyModel model = new LeakyModel();
    private final XYSeries heapOccupancy = new XYSeries("Heap Occupancy");
    private long baseTime = System.currentTimeMillis();
    private final static ExecutorService leakerService = Executors.newSingleThreadExecutor();

    public Leaky() {
        super("Leaky");

        var chartPanel = new ChartPanel(createChart(heapOccupancy));

        var numberOfObjectsField = new JTextField("1000000", 10);

        var button = new JButton("Do Stuff");
        button.addActionListener(e -> {
            button.setEnabled(false);
            numberOfObjectsField.setEnabled(false);
            leakerService.submit(() -> {
                try {
                    model.leak(Long.parseLong(numberOfObjectsField.getText()));
                    EventQueue.invokeLater(() -> {
                        button.setEnabled(true);
                        numberOfObjectsField.setEnabled(true);
                        numberOfObjectsField.requestFocus();
                    });
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
        });
        button.setDefaultCapable(true);
        getRootPane().setDefaultButton(button);

        var controls = new JPanel();
        controls.add(numberOfObjectsField);
        controls.add(button);

        add(controls, BorderLayout.NORTH);
        add(chartPanel, BorderLayout.CENTER);

        new Timer(1000, event -> {
            long currentHeapOccupancy = ManagementFactory.getMemoryMXBean()
                    .getHeapMemoryUsage()
                    .getUsed() / (1024 * 1024);
            double currentTimeSeconds = (double) (System.currentTimeMillis() - baseTime) / 1000.0d;
            heapOccupancy.add(currentHeapOccupancy, currentTimeSeconds);
        }).start();
    }

    private JFreeChart createChart(XYSeries series) {
        return ChartFactory.createScatterPlot(
                "Memory Use",
                "Occupancy (MB)",
                "Time (seconds)",
                new XYSeriesCollection(series),
                PlotOrientation.HORIZONTAL, true, true, true);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            var leaky = new Leaky();
            leaky.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            leaky.setSize(900, 400);
            leaky.setLocationRelativeTo(null);
            leaky.setVisible(true);
        });
    }
}
