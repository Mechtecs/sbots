package de.mechtecs.sbots;

public class DumbView extends Thread implements View {

    private boolean running = false;
    private World world;

    DumbView() {

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
        while (true) {
            this.world.update();
        }
    }
}
