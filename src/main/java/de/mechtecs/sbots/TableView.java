package de.mechtecs.sbots;

import javax.swing.*;

public class TableView extends Thread implements View {

    private boolean running = false;
    private World world;
    JFrame frame;
    JScrollPane jScrollPane;
    JTable agentTable;

    TableView() {
        this.initGUI();
    }

    private void initGUI() {
        this.frame = new JFrame("sBots - Debug Window");
        this.frame.setSize(800, 600);
        this.frame.setResizable(false);
        this.frame.setVisible(true);
        this.frame.requestFocus();

        this.jScrollPane = new JScrollPane();

        this.frame.setLayout(null);
        this.frame.add(jScrollPane);
        this.jScrollPane.setBounds(0, 0, 800, 600);

        String[] columnNames = {
                "Agent ID",
                "health",
                "angle",
                "repcounter",
                "gencount"
        };

        this.agentTable = new JTable(new Object[][]{}, columnNames);
        this.agentTable.setFillsViewportHeight(true);
        this.jScrollPane.add(agentTable);
        this.agentTable.setBounds(0,0,800,600);

    }

    @Override
    public void setWorld(World w) {
        this.world = w;
    }

    @Override
    public World getWorld() {
        return this.world;
    }

    @Override
    public void run() {
        new Thread(() -> {
            while (true) {
                world.update();
            }
        }).run();
    }
}
