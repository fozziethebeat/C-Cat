/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
 * conditions therein.
 *
 * The C-Cat package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package gov.llnl.ontology.util;

import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vectors;

import org.apache.mahout.math.AbstractVector;
import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.Vector;

import java.util.Iterator;


/**
 * A wrapper around an S-Space {@link SparseDoubleVector} into the Mahout style
 * vector.
 *
 * @author Keith Stevens
 */
public class MahoutSparseVector extends AbstractVector {

    /**
     * The base {@link SparseDoubleVector} holding values.
     */
    private final SparseDoubleVector vector;

    /**
     * The maximum index valid in {@link vector}.
     */
    private final int length;

    /**
     * Creates a new {@link MahoutSparseVector} from a {@link
     * SparseDoubleVector}.  The {@code length} will be either {@code vector}'s
     * length, if it is not {@link Integer#MAX_VALUE}.  Otherwise it will be the
     * last index in the non zero indices array.
     */
    public MahoutSparseVector(SparseDoubleVector vector) {
        this(vector, (vector.length() == Integer.MAX_VALUE)
            ? vector.getNonZeroIndices()[vector.getNonZeroIndices().length-1]
            : vector.length());
    }

    /**
     * Creates a {@link MahoutSparseVector} from a given {@link
     * SparseDoubleVector} with the given length.  {@code} length must be less
     * than the maximum length of {@code vector}.
     */
    public MahoutSparseVector(SparseDoubleVector vector, int length) {
        super(length);
        this.vector = vector;
        this.length = length;
        if (length >= vector.length())
            throw new IllegalArgumentException("Invalid maximum length set.");
    }

    /**
     * {@inheritDoc}
     */
    public Matrix matrixLike(int rows, int columns) {
        throw new UnsupportedOperationException("Cannot create a matrix.");
    }

    /**
     * {@inheritDoc}
     */
    public int getNumNondefaultElements() {
        return vector.getNonZeroIndices().length;
    }

    /**
     * {@inheritDoc}
     */
    public void setQuick(int index, double value) {
        vector.set(index, value);
    }

    /**
     * {@inheritDoc}
     */
    public double getQuick(int index) {
        return vector.get(index);
    }

    /**
     * {@inheritDoc}
     */
    public Vector like() {
        return new MahoutSparseVector(Vectors.instanceOf(vector), length);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Element> iterateNonZero() {
        return new ElementIterator(new SparseElement());
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Element> iterator() {
        return new ElementIterator(new DenseElement());
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSequentialAccess() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDense() {
        return false;
    }

    /**
     * An internal {@link Element} interface that allows for incrementing the
     * index value in each element.
     */
    private interface VectorElement extends Element {

        /**
         * Increments the index value for the current {@link VectorElement}.
         */
        public void inc();
    }

    /**
     * A {@link VectorElement} for sparse values.  Only values in the non zero
     * indices array of {@code vector} will be accessed.
     */
    private class SparseElement implements VectorElement {

        public int index;

        public SparseElement() {
            index = -1;
        }

        /**
         * {@inheritDoc}
         */
        public void inc() {
            index++;
        }

        /**
         * {@inheritDoc}
         */
        public double get() {
            return vector.get(vector.getNonZeroIndices()[index]);
        }

        /**
         * {@inheritDoc}
         */
        public int index() {
            return (index >= vector.getNonZeroIndices().length)
                ? -1
                : vector.getNonZeroIndices()[index];
        }

        /**
         * {@inheritDoc}
         */
        public void set(double value) {
            vector.set(vector.getNonZeroIndices()[index], value);
        }
    }

    /**
     * A {@link VectorElement} for all values.  All indices are covered by this
     * {@link VectorElement}.
     */
    private class DenseElement implements VectorElement {

        public int index;

        public DenseElement() {
            index = -1;
        }

        /**
         * {@inheritDoc}
         */
        public void inc() {
            index++;
        }

        /**
         * {@inheritDoc}
         */
        public double get() {
            return vector.get(index);
        }

        /**
         * {@inheritDoc}
         */
        public int index() {
            return (index >= length) ? -1 : index;
        }

        /**
         * {@inheritDoc}
         */
        public void set(double value) {
            vector.set(index, value);
        }
    }

    /**
     * An internal {@link Iterator} over {@link VectorElement}s.  Each call
     * to {@link hasNext} will increment the internal {@link VectorElement},
     * while {@link next} simply returns the current {@link Element}.
     */
    private class ElementIterator implements Iterator<Element> {

        private VectorElement element;

        public ElementIterator(VectorElement element) {
            this.element = element;
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            element.inc();
            return element.index() != -1;
        }

        /**
         * {@inheritDoc}
         */
        public Element next() {
            return element;
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
        }

    }
}
