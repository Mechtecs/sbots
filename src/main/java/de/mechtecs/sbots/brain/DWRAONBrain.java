package de.mechtecs.sbots.brain;

import de.mechtecs.sbots.Brain;
import de.mechtecs.sbots.Constants;
import de.mechtecs.sbots.Helpers;
import de.mechtecs.sbots.math.Float64VectorCustom;
import org.jscience.mathematics.number.Float64;

import java.util.ArrayList;
import java.util.stream.IntStream;

public class DWRAONBrain implements Brain {
    ArrayList<Box> boxes;

    public DWRAONBrain() {
        boxes = new ArrayList<>();
        for (int i = 0; i < Constants.BRAINSIZE; i++) {
            Box a = new Box();
            boxes.add(a);

            for (int j = 0; j < Constants.CONNS; j++) {
                if (i < Constants.BRAINSIZE / 2) {
                    a.id[j] = Helpers.randi(0, Constants.INPUTSIZE - 1);
                }
            }
        }

        init();
    }

    public DWRAONBrain(DWRAONBrain other) {
        boxes = new ArrayList<>();
        other.boxes.forEach(box -> boxes.add(box.copy()));
    }

    @Override
    public void init() {
        // stub
    }

    @Override
    public Float64VectorCustom tick(Float64VectorCustom in) {
        for (int i = 0; i < Constants.INPUTSIZE; i++) {
            this.boxes.get(i).out = in.get(i).copy();
        }

        for (int i = Constants.INPUTSIZE; i < Constants.BRAINSIZE; i++) {
            Box box = boxes.get(i);

            if (box.type == 0) {
                // AND NODE

                Float64 res = Float64.ONE;
                for (int j = 0; j < Constants.CONNS; j++) {
                    Float64 val = boxes.get(box.id[j]).out.copy();
                    if (box.notted[j])
                        val = val.times(-1);
                    res = res.times(val);
                }

                res = res.times(box.bias);
                box.target = res.floatValue();
            } else {
                // OR NODE

                float res = 0;
                for (int j = 0; j < Constants.CONNS; j++) {
                    float val = boxes.get(box.id[j]).out.floatValue();
                    if (box.notted[j])
                        val = 1 - val;
                    res = res + (val * box.w[j]);
                }

                res += box.bias;
                box.target = res;
            }

            // clamp target
            if (box.target < 0) box.target = 0;
            if (box.target > 1) box.target = 1;
        }

        // make all boxes go a bit toward target
        for (int i = Constants.INPUTSIZE; i < Constants.BRAINSIZE; i++) {
            Box box = boxes.get(i);
            box.out = Float64.valueOf(box.out.floatValue() + ((box.target - box.out.floatValue()) * box.kp));
        }

        Float64VectorCustom outVec = Float64VectorCustom.valueOf(new double[Constants.OUTPUTSIZE]);
        IntStream.range(0, Constants.OUTPUTSIZE).forEachOrdered(i -> outVec.set(i, boxes.get(Constants.BRAINSIZE - 1 - i).out.floatValue()));

        return outVec;
    }

    @Override
    public void mutate(float MR, float MR2) {
        for (int i = 0; i < Constants.BRAINSIZE; i++) {
            if (Helpers.randf(0, 1) < MR*3) {
                boxes.get(i).bias += Helpers.randn(0, MR2);
            }

            /*
            if (false && Helpers.randf(0,1)<MR*3) {
                boxes.get(i).kp+= Helpers.randn(0, MR2);
                if (boxes.get(i).kp<0.01) boxes.get(i).kp= (float) 0.01;
                if (boxes.get(i).kp>1) boxes.get(i).kp=1;
//             a2.mutations.push_back("kp jiggled\n");
            }
            */

            if (Helpers.randf(0,1)<MR*3) {
                int rc= Helpers.randi(0, Constants.CONNS - 1);
                boxes.get(i).w[rc]+= Helpers.randn(0, MR2);
                if (boxes.get(i).w[rc]<0.01) boxes.get(i).w[rc]= (float) 0.01;
//             a2.mutations.push_back("weight jiggled\n");
            }

            //more unlikely changes here
            if (Helpers.randf(0,1)<MR) {
                int rc= Helpers.randi(0, Constants.CONNS - 1);
                int ri= Helpers.randi(0, Constants.BRAINSIZE - 1);
                boxes.get(i).id[rc]= ri;
//             a2.mutations.push_back("connectivity changed\n");
            }

            if (Helpers.randf(0,1)<MR) {
                int rc= Helpers.randi(0, Constants.CONNS - 1);
                boxes.get(i).notted[rc]= !boxes.get(i).notted[rc];
//             a2.mutations.push_back("notted was flipped\n");
            }

            if (Helpers.randf(0,1)<MR) {
                boxes.get(i).type= 1-boxes.get(i).type;
//             a2.mutations.push_back("type of a box was changed\n");
            }
        }
    }

    @Override
    public Brain crossover(Brain other_) {
        DWRAONBrain other = null;
        if (other_ instanceof DWRAONBrain) {
            other = (DWRAONBrain) other_;
        } else {
            return null;
        }

        DWRAONBrain brain = new DWRAONBrain();

        for (int i = 0; i < brain.boxes.size(); i++) {
            brain.boxes.get(i).bias = Helpers.randf(0, 1) < 0.5 ? boxes.get(i).bias : other.boxes.get(i).bias;
            brain.boxes.get(i).kp = Helpers.randf(0, 1) < 0.5 ? boxes.get(i).kp : other.boxes.get(i).kp;
            brain.boxes.get(i).type = Helpers.randf(0, 1) < 0.5 ? boxes.get(i).type : other.boxes.get(i).type;

            for (int j = 0; j < brain.boxes.get(i).id.length; j++) {
                brain.boxes.get(i).id[j] = Helpers.randf(0,1) < 0.5 ? this.boxes.get(i).id[j] : other.boxes.get(i).id[j];
                brain.boxes.get(i).notted[j] = Helpers.randf(0,1) < 0.5 ? this.boxes.get(i).notted[j] : other.boxes.get(i).notted[j];
                brain.boxes.get(i).w[j] = Helpers.randf(0,1) < 0.5 ? this.boxes.get(i).w[j] : other.boxes.get(i).w[j];
            }
        }

        return brain;
    }
}

class Box {
    int type; //0: AND, 1:OR
    float kp;

    float[] w;
    int[] id;
    boolean[] notted;

    float bias;

    // state variables
    float target; // target value this node is going toward
    Float64 out; // current output, and history. 0 is farthest back. -1 is latest.

    Box() {
        w = new float[Constants.CONNS];
        id = new int[Constants.CONNS];
        notted = new boolean[Constants.CONNS];

        IntStream.range(0, Constants.CONNS).forEach(i -> {
            w[i] = Helpers.randf(0.1, 2);
            id[i] = Helpers.randi(0, Constants.BRAINSIZE - 1);
            if (Helpers.randf(0, 1) > 0.2) {
                id[i] = Helpers.randi(0, Constants.INPUTSIZE - 1);
            }
            notted[i] = Helpers.randf(0, 1) < 0.5;
        });

        type = (Helpers.randf(0,1) > 0.5) ? 0 : 1;
        kp = Helpers.randf(0.8, 1);
        bias = Helpers.randf(-1, 1);

        out = Float64.valueOf(0.0);
        target = 0;
    }

    Box copy() {
        Box b = new Box();
        b.type = type;
        b.kp = kp;
        b.w = w.clone();
        b.id = id.clone();
        b.notted = notted.clone();
        b.bias = bias;
        b.target = target;
        b.out = out.copy();
        return b;
    }
}