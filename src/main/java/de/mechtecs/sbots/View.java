package de.mechtecs.sbots;

public interface View {
    /**
     * Draw food at pos x,y with intensity i
     *  @param x x position
     * @param y y position
     * @param i intensity (0-1)
     * @param w
     */
    /*
    void drawFood(int x, int y, float i);

    void drawMisc();

    void drawAgent(Agent agent);
    */

    void setWorld(World w);
    World getWorld();
    void run();
}
