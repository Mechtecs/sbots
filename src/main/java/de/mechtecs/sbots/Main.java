package de.mechtecs.sbots;

public class Main {

    public static void main(String... args) {
        World w = new World();
        View view = new LWJGLView();
        view.setWorld(w);
        view.run();
    }
}
