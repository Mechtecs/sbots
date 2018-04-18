package de.mechtecs.sbots;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;

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

    private String hash = new BigInteger(Long.toString(Math.round(Math.random() * Integer.MAX_VALUE))).toString(32);

    @Override
    public void run() {
        int cnt = 0;
        int totalIteration = 0;
        while (true) {
            if (cnt % 10000 == 0 && cnt != 0) {
                try {
                    totalIteration++;
                    FileOutputStream fos = new FileOutputStream("dumbview-" + hash + "-iteration-" + totalIteration + "0000.world");
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    oos.writeObject(this.world);
                    oos.close();
                    fos.close();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                cnt = 0;
            }
            this.world.update();
            cnt++;
        }
    }
}
