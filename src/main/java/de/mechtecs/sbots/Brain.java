package de.mechtecs.sbots;

import de.mechtecs.sbots.math.Float64VectorCustom;

public interface Brain {

    public void init();
    public Float64VectorCustom tick(Float64VectorCustom in);
    public void mutate(float MR, float MR2);
    public Brain crossover(Brain other);

}
