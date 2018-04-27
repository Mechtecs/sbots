package de.mechtecs.sbots;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

public class Main {

    public static void main(String... args) {
        World w = new World();
        View view;

        if (args.length < 1 || args.length > 2) {
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
        } else {
            switch (args[0]) {
                case "dumbview":
                default:
                    view = new DumbView();
                    break;
                case "lwjglview":
                    view = new LWJGLView();
                    break;
            }

            if (args.length == 2) {
                try {
                    FileInputStream fis = new FileInputStream(args[1]);
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    w = (World) ois.readObject();
                    ois.close();
                    fis.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        view.setWorld(w);
        view.run();
    }
}
