package de.mechtecs.sbots;

import javax.swing.*;

public class Main {

    public static void main(String... args) {
        World w = new World();
        View view;

        int viewOption = JOptionPane.showOptionDialog(null, "Which View do you want to start?", null, JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[]{"DumbView", "LWJGLView"}, null);
        switch (viewOption) {
            case 0:
                view = new DumbView();
                break;
            case 1:
            default:
                view = new LWJGLView();
                break;
        }

        view.setWorld(w);
        view.run();
    }
}
