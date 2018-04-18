package de.mechtecs.sbots;


import org.lwjgl.BufferUtils;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static de.mechtecs.sbots.Constants.*;
import static de.mechtecs.sbots.Helpers.cap;
import static java.lang.Math.*;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBEasyFont.stb_easy_font_print;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;


public class LWJGLView implements View {
    private long window;
    private World world;
    private float scalemult;
    private float xtranslate = 0.0f;
    private float ytranslate = 0.0f;
    private boolean mousePressed = false;
    private boolean mousePressedFirst = true;
    private double mouseLastX;
    private double mouseLastY;
    private int wx;
    private int wy;
    private boolean following = false;
    private Agent selectedAgent;
    private boolean showText = true;

    private boolean enableWait = false;
    private boolean runNow = false;
    private boolean enableBreak;

    public LWJGLView() {
        scalemult = 0.2f;
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
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        init();
        loop();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE); // the window will be resizable

        // Create the window
        window = glfwCreateWindow(WWIDTH, WHEIGHT, "sBots", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            if (key == GLFW_KEY_F && action == GLFW_RELEASE) {
                following = !following;
                scalemult = 1.0f;
            }
            if (key == GLFW_KEY_N && action == GLFW_RELEASE) {
                selectOldestAgent();
            }
            if (key == GLFW_KEY_T && action == GLFW_RELEASE) {
                showText = !showText;
            }
            if (key == GLFW_KEY_F12 && action == GLFW_RELEASE) {
                this.enableWait = !this.enableWait;
            }
            if (key == GLFW_KEY_F5 && action == GLFW_RELEASE) {
                enableBreak = true;
                try {
                    FileDialog fd = new FileDialog((Dialog) null, "Save World", FileDialog.SAVE);
                    fd.setVisible(true);
                    FileOutputStream fos = new FileOutputStream(fd.getDirectory() + fd.getFile());
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    oos.writeObject(this.world);
                    oos.close();
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                enableBreak = false;
            }
            if (key == GLFW_KEY_F6 && action == GLFW_RELEASE) {
                enableBreak = true;
                try {
                    FileDialog fd = new FileDialog((Dialog) null, "Save World", FileDialog.LOAD);
                    fd.setMultipleMode(false);
                    fd.setVisible(true);
                    FileInputStream fis = new FileInputStream(fd.getDirectory() + fd.getFile());
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    this.world = (World) ois.readObject();
                    ois.close();
                    fis.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                enableBreak = false;
            }
        });

        glfwSetCursorPosCallback(window, (window, mouseX, mouseY) -> {
            wx = (int) (((mouseX - (WWIDTH / 2)) / scalemult) - xtranslate);
            wy = (int) (((mouseY - (WHEIGHT / 2)) / scalemult) - ytranslate);

            if (mousePressed) {
                if (mousePressedFirst) {
                    mousePressedFirst = false;
                } else {
                    xtranslate += (mouseX - mouseLastX);
                    ytranslate += (mouseY - mouseLastY);
                }

                mouseLastX = mouseX;
                mouseLastY = mouseY;
            }
        });

        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_RELEASE && mousePressed) {
                mousePressed = false;
            } else if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS && !mousePressed) {
                mouseLastX = -1.0d;
                mouseLastY = -1.0d;
                mousePressed = true;
                mousePressedFirst = true;
                selectedAgent = world.processMouse(wx, wy);
            }
        });

        glfwSetScrollCallback(window, (window, deltaX, deltaY) -> {
            if (!following) {
                scalemult += deltaY / 50;

                // clamp
                if (scalemult <= 0.18f)
                    scalemult = 0.18f;
                if (scalemult >= 2f)
                    scalemult = 2f;
            }
        });

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
    }

    private void selectOldestAgent() {
        Agent oldest = null;
        for (Agent agent : world.agents) {
            agent.selectflag = false;
            if (oldest == null) {
                oldest = agent;
            } else {
                if ((agent.gencount == oldest.gencount && agent.age > oldest.age) || agent.gencount > oldest.gencount) {
                    oldest = agent;
                }
            }
        }
        oldest.selectflag = true;
        selectedAgent = oldest;
    }

    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        // Set the clear color
        glClearColor(0.2f, 0.4f, 0.5f, 0.0f);

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, WWIDTH, WHEIGHT, 0, 0, 1);

        Thread worldUpdater = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                while (enableWait && !runNow || enableBreak) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                runNow = false;
                world.update();
            }
        });
        worldUpdater.start();

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
            glPushMatrix();

            if (following) {
                float xi = (float) selectedAgent.pos.getValue(0);
                float yi = (float) selectedAgent.pos.getValue(1);

                xi = (-xi) * scalemult;
                yi = (-yi) * scalemult;

                xtranslate = xi;
                ytranslate = yi;
            }

            glTranslatef(Constants.WWIDTH / 2, Constants.WHEIGHT / 2, 0.0f);
            glScalef(scalemult, scalemult, 1.0f);
            glTranslatef(xtranslate, ytranslate, 0);

            this.drawFood();
            this.drawAgents();

            glPopMatrix();

            if (showText) {
                this.drawInfo();
            }

            runNow = true;

            glfwSwapBuffers(window); // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
        }
        worldUpdater.interrupt();
    }

    private String text = "";

    private void drawInfo() {
        if (selectedAgent != null) {
            if (selectedAgent.health > 0) {
                text = "Agent Selected: " + selectedAgent.id + "\nAge: " + selectedAgent.age + "\nGeneration: " + selectedAgent.gencount + "\nHealth:" + selectedAgent.health + "\nReproduction: " + selectedAgent.repcounter;
            } else {
                selectOldestAgent();
            }
        }

        glPushMatrix();

        glColor3f(0, 0, 0);
        ByteBuffer charBuffer = BufferUtils.createByteBuffer(text.length() * 270);
        int quads = stb_easy_font_print(0, 0, text, null, charBuffer);
        glEnableClientState(GL_VERTEX_ARRAY);

        glVertexPointer(2, GL_FLOAT, 16, charBuffer);
        glScalef(6, 6, 1);
        glDrawArrays(GL_QUADS, 0, quads * 4);

        glDisableClientState(GL_VERTEX_ARRAY);

        glPopMatrix();
    }

    private void drawFood() {
        for (int x = 0; x < this.world.food.length; x++) {
            for (int y = 0; y < this.world.food[x].length; y++) {
                glBegin(GL_QUADS);
                float amount = (float) (0.9 - this.world.food[x][y]);
                glColor3f(amount, amount, amount);
                glVertex3f(x * CZ, y * CZ, 0);
                glVertex3f(x * CZ + CZ, y * CZ, 0);
                glVertex3f(x * CZ + CZ, y * CZ + CZ, 0);
                glVertex3f(x * CZ, y * CZ + CZ, 0);
                glEnd();
            }
        }
    }

    private void drawCircle(float x, float y, float r) {
        float n;
        for (int k = 0; k < 17; k++) {
            n = (float) (k * (PI / 8));
            glVertex3f((float) (x + r * sin(n)), (float) (y + r * cos(n)), 0);
        }
    }

    private void drawAgents() {
        for (Agent agent : this.world.agents) {
            float n;
            float r = Constants.BOTRADIUS;
            float rp = r + 2;

            // draw food interaction
            if (agent.dfood != 0) {
                glBegin(GL_POLYGON);

                float mag = cap(abs(agent.dfood) / Constants.FOODTRANSFER / 3);
                if (agent.dfood > 0) {
                    glColor3f(0, mag, 0);
                } else {
                    glColor3f(mag, 0, 0);
                }
                for (int k = 0; k < 17; k++) {
                    n = (float) (k * (PI / 8));
                    glVertex3f(agent.pos.get(0).plus(rp * sin(n)).floatValue(), agent.pos.get(1).plus(rp * cos(n)).floatValue(), 0);
                    n = (float) ((k + 1) * (PI / 8));
                    glVertex3f(agent.pos.get(0).plus(rp * sin(n)).floatValue(), agent.pos.get(1).plus(rp * cos(n)).floatValue(), 0);
                }
                glEnd();
            }

            // draw indicator
            if (agent.indicator > 0) {
                glBegin(GL_POLYGON);
                glColor3f(agent.ir, agent.ig, agent.ib);
                drawCircle(agent.pos.get(0).floatValue(), agent.pos.get(1).floatValue(), Constants.BOTRADIUS + ((int) agent.indicator));
                glEnd();
            }

            // draw eyes
            glBegin(GL_LINES);
            glColor3f(0.5f, 0.5f, 0.5f);
            for (int q = 0; q < Constants.NUMEYES; q++) {
                glVertex3f(agent.pos.get(0).floatValue(), agent.pos.get(1).floatValue(), 0);
                float aa = agent.angle + agent.eyedir.get(q).floatValue();
                glVertex3f(
                        (float) (agent.pos.get(0).floatValue() + ((Constants.BOTRADIUS * 4) * cos(aa))),
                        (float) (agent.pos.get(1).floatValue() + ((Constants.BOTRADIUS * 4) * sin(aa))),
                        0
                );
            }
            glEnd();

            // body selected?
            if (agent.selectflag) {
                glBegin(GL_POLYGON);
                glColor3f(1 - agent.red, 1 - agent.gre, 1 - agent.blu);
                drawCircle(agent.pos.get(0).floatValue(), agent.pos.get(1).floatValue(), Constants.BOTRADIUS * 1.25f);
                glEnd();
            }

            // body
            glBegin(GL_POLYGON);
            glColor3f(agent.red, agent.gre, agent.blu);
            drawCircle(agent.pos.get(0).floatValue(), agent.pos.get(1).floatValue(), Constants.BOTRADIUS);
            glEnd();
        }
    }
}
