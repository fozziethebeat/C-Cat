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

import java.util.Iterator;


/**
 * An {@link Iterator} for arrays of objects.
 *
 * @author Keith Stevens
 */
public class ArrayIterator<T> implements Iterator<T> {
    
    /**
     * The original array of objects.
     */
    private final T[] values;

    /**
     * The index of the next value to return.
     */
    private int index;

    /**
     * Creates a new {@link ArrayIterator}
     */
    public ArrayIterator(T[] values) {
        this.values = values;
        index = 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        return index < values.length;
    }

    /**
     * {@inheritDoc}
     */
    public T next() {
        return values[index++];
    }

    /**
     * throws a {@link UnsupportedOperationException}
     */
    public void remove() {
        throw new UnsupportedOperationException(
                "Values cannot be removed from an ArrayIterator");
    }
}
