package de.mechtecs.sbots;


import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static de.mechtecs.sbots.Constants.CZ;
import static de.mechtecs.sbots.Constants.WHEIGHT;
import static de.mechtecs.sbots.Constants.WWIDTH;
import static de.mechtecs.sbots.Helpers.cap;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static java.lang.Math.*;

public class LWJGLView implements View {
    private long window;
    private World world;
    private float scalemult;
    private float xtranslate = 0.0f;
    private float ytranslate = 0.0f;
    private boolean mousePressed = false;

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
        window = glfwCreateWindow(640, 480, "Hello World!", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
        });

        glfwSetCursorPosCallback(window, (window, v, v1) -> {
            if (mousePressed) {
                System.out.println(v);
                System.out.println(v1);
            }
        });

        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_RELEASE && mousePressed) {
                mousePressed = false;
            } else if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS && !mousePressed) {
                mousePressed = true;
            }
        });

        glfwSetScrollCallback(window, (window, deltaX, deltaY) -> {
            scalemult += deltaY / 50;

            // clamp
            if (scalemult <= 0.18f)
                scalemult = 0.18f;
            if (scalemult >= 1f)
                scalemult = 1f;

            System.out.println("Scale: " + scalemult);
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

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {
            world.update();
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
            glPushMatrix();

            //glTranslatef(Constants.WWIDTH / 2, Constants.WHEIGHT / 2, 0.0f);
            glScalef(scalemult, scalemult, 1.0f);
            glTranslatef(xtranslate, ytranslate, 0);

            this.drawAgents();
            this.drawFood();

            glPopMatrix();
            glfwSwapBuffers(window); // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
        }
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
                        (float) (agent.pos.get(0).floatValue() + ((Constants.BOTRADIUS * 4) * sin(aa))),
                        0
                );
            }
            glEnd();

            // body
            glBegin(GL_POLYGON);
            glColor3f(agent.red, agent.gre, agent.blu);
            drawCircle(agent.pos.get(0).floatValue(), agent.pos.get(1).floatValue(), Constants.BOTRADIUS);
            glEnd();
        }
    }
}