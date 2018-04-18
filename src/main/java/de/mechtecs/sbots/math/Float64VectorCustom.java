/*
 * JScience - Java(TM) Tools and Libraries for the Advancement of Sciences.
 * Copyright (C) 2006 - JScience (http://jscience.org/)
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
package de.mechtecs.sbots.math;

import javolution.context.ArrayFactory;
import javolution.lang.MathLib;
import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;
import org.jscience.mathematics.number.Float64;
import org.jscience.mathematics.structure.VectorSpaceNormed;
import org.jscience.mathematics.vector.DimensionException;
import org.jscience.mathematics.vector.Vector;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.stream.DoubleStream;

/**
 * <p> This class represents an optimized {@link Vector vector} implementation
 * for 64 bits floating point elements.</p>
 *
 * @author <a href="mailto:jean-marie@dautelle.com">Jean-Marie Dautelle</a>
 * @version 3.3, January 2, 2007
 */
public final class Float64VectorCustom extends Vector<Float64> implements
        VectorSpaceNormed<Vector<Float64>, Float64>, Serializable {

    public Float64VectorCustom rotate(double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double x2 = this._values[0] * c - this._values[1] * s;
        return Float64VectorCustom.valueOf(x2, _values[0] * s + _values[1] * c);
    }

    public float length() {
        return (float) Math.sqrt(DoubleStream.of(this.getArray()).map(number -> Math.pow(number, 2)).sum());
    }

    public double[] getArray() {
        return this._values.clone();
    }

    public synchronized void set(int pos, float value) {
        this._values[pos] = value;
    }

    public float get_angle() {
        if (this._dimension == 2) {
            return (float) Math.atan2(this._values[0], this._values[1]);
        } else {
            System.out.println("You fucked up.");
            return -1;
        }
    }


    /**
     * Holds the default XML representation. For example:
     * [code]
     * <Float64Vector dimension="2">
     * <Float64 value="1.0" />
     * <Float64 value="0.0" />
     * </Float64Vector>[/code]
     */
    protected static final XMLFormat<Float64VectorCustom> XML = new XMLFormat<Float64VectorCustom>(
            Float64VectorCustom.class) {

        @Override
        public Float64VectorCustom newInstance(Class<Float64VectorCustom> cls, InputElement xml)
                throws XMLStreamException {
            int dimension = xml.getAttribute("dimension", 0);
            Float64VectorCustom V = FACTORY.array(dimension);
            V._dimension = dimension;
            return V;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void read(InputElement xml, Float64VectorCustom V)
                throws XMLStreamException {
            for (int i = 0, n = V._dimension; i < n; ) {
                V._values[i++] = ((Float64) xml.getNext()).doubleValue();
            }
            if (xml.hasNext())
                throw new XMLStreamException("Too many elements");
        }

        @Override
        public void write(Float64VectorCustom V, OutputElement xml)
                throws XMLStreamException {
            xml.setAttribute("dimension", V._dimension);
            for (int i = 0, n = V._dimension; i < n; ) {
                xml.add(V.get(i++));
            }
        }
    };

    /**
     * Holds factory for vectors with variable size arrays.
     */
    private static final ArrayFactory<Float64VectorCustom> FACTORY
            = new ArrayFactory<Float64VectorCustom>() {

        @Override
        protected Float64VectorCustom create(int capacity) {
            return new Float64VectorCustom(capacity);
        }

    };

    /**
     * Holds the dimension.
     */
    private int _dimension;

    /**
     * Holds the values.
     */
    private final double[] _values;

    /**
     * Creates a vector of specified capacity.
     */
    private Float64VectorCustom(int capacity) {
        _values = new double[capacity];
    }

    /**
     * Returns a new vector holding the specified <code>double</code> values.
     *
     * @param values the vector values.
     * @return the vector having the specified values.
     */
    public static Float64VectorCustom valueOf(double... values) {
        int n = values.length;
        Float64VectorCustom V = FACTORY.array(n);
        V._dimension = n;
        System.arraycopy(values, 0, V._values, 0, n);
        return V;
    }

    /**
     * Returns a new vector holding the elements from the specified
     * collection.
     *
     * @param elements the collection of floating-points numbers.
     * @return the vector having the specified elements.
     */
    public static Float64VectorCustom valueOf(List<Float64> elements) {
        int n = elements.size();
        Float64VectorCustom V = FACTORY.array(n);
        V._dimension = n;
        Iterator<Float64> iterator = elements.iterator();
        for (int i = 0; i < n; i++) {
            V._values[i] = iterator.next().doubleValue();
        }
        return V;
    }

    /**
     * Returns a {@link Float64VectorCustom} instance equivalent to the
     * specified vector.
     *
     * @param that the vector to convert.
     * @return <code>that</code> or new equivalent Float64Vector.
     */
    public static Float64VectorCustom valueOf(Vector<Float64> that) {
        if (that instanceof Float64VectorCustom)
            return (Float64VectorCustom) that;
        int n = that.getDimension();
        Float64VectorCustom V = FACTORY.array(n);
        V._dimension = n;
        for (int i = 0; i < n; i++) {
            V._values[i] = that.get(i).doubleValue();
        }
        return V;
    }

    /**
     * Returns the value of a floating point number from this vector (fast).
     *
     * @param i the floating point number index.
     * @return the value of the floating point number at <code>i</code>.
     * @throws IndexOutOfBoundsException <code>(i < 0) || (i >= dimension())</code>
     */
    public double getValue(int i) {
        if (i >= _dimension)
            throw new ArrayIndexOutOfBoundsException();
        return _values[i];
    }

    /**
     * Returns the Euclidian norm of this vector (square root of the
     * dot product of this vector and itself).
     *
     * @return <code>sqrt(this · this)</code>.
     */
    public Float64 norm() {
        return Float64.valueOf(normValue());
    }

    /**
     * Returns the {@link #norm()} value of this vector.
     *
     * @return <code>this.norm().doubleValue()</code>.
     */
    public double normValue() {
        double normSquared = 0;
        for (int i = _dimension; --i >= 0; ) {
            double values = _values[i];
            normSquared += values * values;
        }
        return MathLib.sqrt(normSquared);
    }

    @Override
    public int getDimension() {
        return _dimension;
    }

    @Override
    public Float64 get(int i) {
        if (i >= _dimension)
            throw new IndexOutOfBoundsException();
        return Float64.valueOf(_values[i]);
    }

    @Override
    public Float64VectorCustom opposite() {
        Float64VectorCustom V = FACTORY.array(_dimension);
        V._dimension = _dimension;
        for (int i = 0; i < _dimension; i++) {
            V._values[i] = -_values[i];
        }
        return V;
    }

    @Override
    public Float64VectorCustom plus(Vector<Float64> that) {
        Float64VectorCustom T = Float64VectorCustom.valueOf(that);
        if (T._dimension != _dimension) throw new DimensionException();
        Float64VectorCustom V = FACTORY.array(_dimension);
        V._dimension = _dimension;
        for (int i = 0; i < _dimension; i++) {
            V._values[i] = _values[i] + T._values[i];
        }
        return V;
    }

    @Override
    public Float64VectorCustom minus(Vector<Float64> that) {
        Float64VectorCustom T = Float64VectorCustom.valueOf(that);
        if (T._dimension != _dimension) throw new DimensionException();
        Float64VectorCustom V = FACTORY.array(_dimension);
        V._dimension = _dimension;
        for (int i = 0; i < _dimension; i++) {
            V._values[i] = _values[i] - T._values[i];
        }
        return V;
    }

    @Override
    public Float64VectorCustom times(Float64 k) {
        Float64VectorCustom V = FACTORY.array(_dimension);
        V._dimension = _dimension;
        double d = k.doubleValue();
        for (int i = 0; i < _dimension; i++) {
            V._values[i] = _values[i] * d;
        }
        return V;
    }

    /**
     * Equivalent to <code>this.times(Float64.valueOf(k))</code>
     *
     * @param k the coefficient.
     * @return <code>this * k</code>
     */
    public Float64VectorCustom times(double k) {
        Float64VectorCustom V = FACTORY.array(_dimension);
        V._dimension = _dimension;
        for (int i = 0; i < _dimension; i++) {
            V._values[i] = _values[i] * k;
        }
        return V;
    }

    @Override
    public Float64 times(Vector<Float64> that) {
        Float64VectorCustom T = Float64VectorCustom.valueOf(that);
        if (T._dimension != _dimension)
            throw new DimensionException();
        double[] T_values = T._values;
        double sum = _values[0] * T_values[0];
        for (int i = 1; i < _dimension; i++) {
            sum += _values[i] * T_values[i];
        }
        return Float64.valueOf(sum);
    }


    @Override
    public Float64VectorCustom cross(Vector<Float64> that) {
        Float64VectorCustom T = Float64VectorCustom.valueOf(that);
        if ((this._dimension != 3) || (T._dimension != 3))
            throw new DimensionException(
                    "The cross product of two vectors requires "
                            + "3-dimensional vectors");
        double x = _values[1] * T._values[2] - _values[2] * T._values[1];
        double y = _values[2] * T._values[0] - _values[0] * T._values[2];
        double z = _values[0] * T._values[1] - _values[1] * T._values[0];
        return Float64VectorCustom.valueOf(x, y, z);
    }


    @Override
    public Float64VectorCustom copy() {
        Float64VectorCustom V = FACTORY.array(_dimension);
        V._dimension = _dimension;
        for (int i = 0; i < _dimension; i++) {
            V._values[i] = _values[i];
        }
        return V;
    }

    ///////////////////////////////
    // Package Private Utilities //
    ///////////////////////////////

    static Float64VectorCustom newInstance(int n) {
        Float64VectorCustom V = FACTORY.array(n);
        V._dimension = n;
        return V;
    }

    void set(int i, double v) {
        _values[i] = v;
    }

    private static final long serialVersionUID = 1L;

    public float angle_between(Float64VectorCustom other) {
        float cross = (float) (this.getValue(0) * other.getValue(1) - this.getValue(1) * other.getValue(0));
        float dot = (float) (this.getValue(0) * other.getValue(0) + (this.getValue(1) * other.getValue(1)));
        return (float) Math.atan2(cross, dot);
    }
}