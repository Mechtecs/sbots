package de.mechtecs.sbots;

import de.mechtecs.sbots.brain.DWRAONBrain;
import de.mechtecs.sbots.math.Float64VectorCustom;

import java.util.ArrayList;

import static de.mechtecs.sbots.Constants.*;
import static de.mechtecs.sbots.Helpers.*;

public class Agent {

    Float64VectorCustom pos;

    float health; //in [0,2]. I cant remember why.
    float angle; //of the bot

    float red;
    float gre;
    float blu;

    float w1; //wheel speeds
    float w2;
    boolean boost; //is this agent boosting

    float spikeLength;
    int age;

    boolean spiked;

    Float64VectorCustom in; //input: 2 eyes, sensors for R,G,B,proximity each, then Sound, Smell, Health
    Float64VectorCustom out; //output: Left, Right, R, G, B, SPIKE

    float repcounter; //when repcounter gets to 0, this bot reproduces
    int gencount; //generation counter
    boolean hybrid; //is this agent result of crossover?
    float clockf1, clockf2; //the frequencies of the two clocks of this bot
    float soundmul; //sound multiplier of this bot. It can scream, or be very sneaky. This is actually always set to output 8

    //variables for drawing purposes
    float indicator;
    float ir;
    float ig;
    float ib; //indicator colors
    boolean selectflag; //is this agent selected?
    float dfood; //what is change in health of this agent due to giving/receiving?

    float give;    //is this agent attempting to give food to other agent?

    int id;

    //inhereted stuff
    float herbivore; //is this agent a herbivore? between 0 and 1
    float MUTRATE1; //how often do mutations occur?
    float MUTRATE2; //how significant are they?
    float temperature_preference; //what temperature does this agent like? [0 to 1]

    float smellmod;
    float soundmod;
    float hearmod;
    float eyesensmod;
    float bloodmod;

    Float64VectorCustom eyefov; //field of view for each eye
    Float64VectorCustom eyedir; //direction of each eye

    Brain brain;

    //      DWRAONBrain brain; // THE BRAIN!!!!
    //      AssemblyBrain brain;
    //      MLPBrain brain;

    //will store the mutations that this agent has from its parent
    //can be used to tune the mutation rate
    //TODO: angucken größe
    ArrayList<String> mutations;

    Agent() {
        pos = Float64VectorCustom.valueOf(randf(0, WIDTH), randf(0, HEIGHT));
        angle = randf(-Math.PI, Math.PI);
        health = (float) (1.0 + randf(0, 0.1));
        age = 0;
        spikeLength = 0;
        red = 0;
        gre = 0;
        blu = 0;
        w1 = 0;
        w2 = 0;
        soundmul = 1;
        give = 0;
        clockf1 = randf(5, 100);
        clockf2 = randf(5, 100);
        boost = false;
        indicator = 0;
        gencount = 0;
        selectflag = false;
        ir = 0;
        ig = 0;
        ib = 0;
        temperature_preference = randf(0, 1);
        hybrid = false;
        herbivore = randf(0, 1);
        repcounter = herbivore * randf(REPRATEH - 0.1f, REPRATEH + 0.1f) + (1 - herbivore) * randf(REPRATEC - 0.1f, REPRATEC + 0.1f);

        id = 0;

        smellmod = randf(0.1f, 0.5f);
        soundmod = randf(0.2f, 0.6f);
        hearmod = randf(0.7f, 1.3f);
        eyesensmod = randf(1f, 3f);
        bloodmod = randf(1f, 3f);

        MUTRATE1 = randf(0.001f, 0.005f);
        MUTRATE2 = randf(0.03f, 0.07f);

        spiked = false;


        brain = new DWRAONBrain();
        mutations = new ArrayList<>();
        in = Float64VectorCustom.valueOf(new double[INPUTSIZE]);
        out = Float64VectorCustom.valueOf(new double[OUTPUTSIZE]);

        double[] eyefovD = new double[NUMEYES];
        double[] eyedirD = new double[NUMEYES];
        for (int i = 0; i < NUMEYES; i++) {
            eyefovD[i] = randf(0.5f, 2);
            eyedirD[i] = randf(0, (float) (2 * Math.PI));
        }

        eyefov = Float64VectorCustom.valueOf(eyefovD);
        eyedir = Float64VectorCustom.valueOf(eyedirD);
    }

    public float distance(Float64VectorCustom point) {
        return this.pos.minus(point).length();
    }

    public void printSelf() {
        System.out.println("Agent age=" + age);
        mutations.forEach(System.out::print);
        System.out.println();
    }

    public void initEvent(float size, float r, float g, float b) {
        indicator = size;
        ir = r;
        ig = g;
        ib = b;
    }

    public void tick() {
        this.out = brain.tick(this.in);
    }

    public de.mechtecs.sbots.Agent reproduce(float MR, float MR2) {
        if (BDEBUG) System.out.println("New birth---------------");
        Agent a2 = new Agent();

        //spawn the baby somewhere closeby behind agent
        //we want to spawn behind so that agents dont accidentally eat their young right away
        Float64VectorCustom fb = Float64VectorCustom.valueOf(BOTRADIUS, 0);
        fb = fb.rotate(-a2.angle);
        a2.pos = a2.pos.plus(fb).plus(Float64VectorCustom.valueOf(randf(-BOTRADIUS * 2, BOTRADIUS * 2), randf(-BOTRADIUS * 2, BOTRADIUS * 2)));

        if (a2.pos.get(0).doubleValue() < 0)
            a2.pos = Float64VectorCustom.valueOf(WIDTH + a2.pos.get(0).doubleValue(), a2.pos.get(1).doubleValue());
        if (a2.pos.get(0).doubleValue() >= WIDTH)
            a2.pos = Float64VectorCustom.valueOf(a2.pos.get(0).doubleValue() - WIDTH, a2.pos.get(1).doubleValue());
        if (a2.pos.get(1).doubleValue() < 0)
            a2.pos = Float64VectorCustom.valueOf(a2.pos.get(0).doubleValue(), HEIGHT + a2.pos.get(1).doubleValue());
        if (a2.pos.get(1).doubleValue() >= HEIGHT)
            a2.pos = Float64VectorCustom.valueOf(a2.pos.get(0).doubleValue(), a2.pos.get(1).doubleValue() - HEIGHT);

        a2.gencount++;
        a2.repcounter = a2.herbivore * randf(REPRATEH - 0.1, REPRATEH + 0.1) + (1 - a2.herbivore) * randf(REPRATEC - 0.1, REPRATEC + 0.1);

        //noisy attribute passing
        a2.MUTRATE1 = this.MUTRATE1;
        a2.MUTRATE2 = this.MUTRATE2;
        if (randf(0, 1) < 0.1) a2.MUTRATE1 = (float) randn(this.MUTRATE1, METAMUTRATE1);
        if (randf(0, 1) < 0.1) a2.MUTRATE2 = (float) randn(this.MUTRATE2, METAMUTRATE2);
        if (this.MUTRATE1 < 0.001) this.MUTRATE1 = 0.001f;
        if (this.MUTRATE2 < 0.02) this.MUTRATE2 = 0.02f;
        a2.herbivore = (float) cap(randn(this.herbivore, 0.03));
        if (randf(0, 1) < MR * 5) a2.clockf1 = (float) randn(a2.clockf1, MR2);
        if (a2.clockf1 < 2) a2.clockf1 = 2;
        if (randf(0, 1) < MR * 5) a2.clockf2 = (float) randn(a2.clockf2, MR2);
        if (a2.clockf2 < 2) a2.clockf2 = 2;

        a2.smellmod = this.smellmod;
        a2.soundmod = this.soundmod;
        a2.hearmod = this.hearmod;
        a2.eyesensmod = this.eyesensmod;
        a2.bloodmod = this.bloodmod;
        if (randf(0, 1) < MR * 5) {
            float oo = a2.smellmod;
            a2.smellmod = (float) randn(a2.smellmod, MR2);
            if (BDEBUG) System.out.print(String.format("smell mutated from %f to %f\n", oo, a2.smellmod));
        }
        if (randf(0, 1) < MR * 5) {
            float oo = a2.soundmod;
            a2.soundmod = (float) randn(a2.soundmod, MR2);
            if (BDEBUG) System.out.print(String.format("sound mutated from %f to %f\n", oo, a2.soundmod));
        }
        if (randf(0, 1) < MR * 5) {
            float oo = a2.hearmod;
            a2.hearmod = (float) randn(a2.hearmod, MR2);
            if (BDEBUG) System.out.print(String.format("hear mutated from %f to %f\n", oo, a2.hearmod));
        }
        if (randf(0, 1) < MR * 5) {
            float oo = a2.eyesensmod;
            a2.eyesensmod = (float) randn(a2.eyesensmod, MR2);
            if (BDEBUG) System.out.print(String.format("eyesens mutated from %f to %f\n", oo, a2.eyesensmod));
        }
        if (randf(0, 1) < MR * 5) {
            float oo = a2.bloodmod;
            a2.bloodmod = (float) randn(a2.bloodmod, MR2);
            if (BDEBUG) System.out.print(String.format("blood mutated from %f to %f\n", oo, a2.bloodmod));
        }

        double[] eyefovD = this.eyefov.getArray();
        double[] eyedirD = this.eyedir.getArray();

        for (int i = 0; i < NUMEYES; i++) {
            if (randf(0, 1) < MR * 5) eyefovD[i] = randn(eyefovD[i], MR2);
            if (eyefovD[i] < 0) eyefovD[i] = 0;

            if (randf(0, 1) < MR * 5) eyedirD[i] = randn(eyedirD[i], MR2);
            if (eyedirD[i] < 0) eyedirD[i] = 0;
            if (eyedirD[i] > 2 * Math.PI) eyedirD[i] = 2 * Math.PI;
        }

        a2.eyefov = Float64VectorCustom.valueOf(eyefovD);
        a2.eyedir = Float64VectorCustom.valueOf(eyedirD);

        a2.temperature_preference = (float) cap(randn(this.temperature_preference, 0.005));
//    a2.temperature_preference= this.temperature_preference;

        //mutate brain here
        a2.brain = this.brain;
        a2.brain.mutate(MR, MR2);

        if (a2.herbivore > 0.5) System.out.println("Herbivore");
        if (a2.herbivore <= 0.5) System.out.println("Carnivore");

        return a2;
    }

    Agent crossover(Agent other) {
        //this could be made faster by returning a pointer
        //instead of returning by value
        Agent anew = new Agent();
        anew.hybrid = true; //set this non-default flag
        anew.gencount = this.gencount;
        if (other.gencount < anew.gencount) anew.gencount = other.gencount;

        //agent heredity attributes
        anew.clockf1 = randf(0, 1) < 0.5 ? this.clockf1 :
                other.clockf1;
        anew.clockf2 = randf(0, 1) < 0.5 ? this.clockf2 :
                other.clockf2;
        anew.herbivore = randf(0, 1) < 0.5 ? this.herbivore :
                other.herbivore;
        anew.MUTRATE1 = randf(0, 1) < 0.5 ? this.MUTRATE1 :
                other.MUTRATE1;
        anew.MUTRATE2 = randf(0, 1) < 0.5 ? this.MUTRATE2 :
                other.MUTRATE2;
        anew.temperature_preference = randf(0, 1) < 0.5 ? this.temperature_preference :
                other.temperature_preference;

        anew.smellmod = randf(0, 1) < 0.5 ? this.smellmod :
                other.smellmod;
        anew.soundmod = randf(0, 1) < 0.5 ? this.soundmod :
                other.soundmod;
        anew.hearmod = randf(0, 1) < 0.5 ? this.hearmod :
                other.hearmod;
        anew.eyesensmod = randf(0, 1) < 0.5 ? this.eyesensmod :
                other.eyesensmod;
        anew.bloodmod = randf(0, 1) < 0.5 ? this.bloodmod :
                other.bloodmod;

        anew.eyefov = randf(0, 1) < 0.5 ? this.eyefov :
                other.eyefov;
        anew.eyedir = randf(0, 1) < 0.5 ? this.eyedir :
                other.eyedir;

        anew.brain = this.brain.crossover(other.brain);

        if (anew.brain == null) {
            System.out.println("null checker");
        }

        return anew;
    }
}