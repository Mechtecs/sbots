package de.mechtecs.sbots;

import static java.lang.Math.log;
import static java.lang.Math.random;
import static java.lang.Math.sqrt;

public class Helpers {
    public static float randf(float a, float b) {
        return (float) (((b - a) * (random())) + a);
    }

    public static float randf(double a, double b) {
        return (float) (((b - a) * (random())) + a);
    }

    public static int randi(int a, int b) {
        return (int) ((random() % (b - a)) + a);
    }



    private static boolean deviateAvailable = false;    //	flag
    private static float storedDeviate;            //	deviate from previous calculation

    public static double randn(double mu, double sigma) {
        double polar, rsquared, var1, var2;
        if (!deviateAvailable) {
            do {
                var1 = 2.0 * random() - 1.0;
                var2 = 2.0 * random() - 1.0;
                rsquared = var1 * var1 + var2 * var2;
            } while (rsquared >= 1.0 || rsquared == 0.0);
            polar = sqrt(-2.0 * log(rsquared) / rsquared);
            storedDeviate = (float) (var1 * polar);
            deviateAvailable = true;
            return var2 * polar * sigma + mu;
        } else {
            deviateAvailable = false;
            return storedDeviate * sigma + mu;
        }
    }

    public static float cap(float a) {
        if (a < 0.0f) return 0.0f;
        if (a > 1.0f) return 1.0f;
        return a;
    }

    public static double cap(double a) {
        if (a < 0.0f) return 0.0f;
        if (a > 1.0f) return 1.0f;
        return a;
    }
}
