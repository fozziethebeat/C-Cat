/*
 * Copyright (c) 2010, Lawrence Livermore National Security, LLC. Produced at
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

import edu.ucla.sspace.util.Pair;


/**
 * A subclass of {@link Pair} for {@link String}s that allows for arrays of
 * these pairs.
 *
 * @author Keith Stevens
 */
public class StringPair extends Pair<String> implements Comparable {

    /**
     * Constructs a new {@link StringPair}.
     */
    public StringPair(String x, String y) {
        super(x, y);
    }

    /**
     * Compares this {@link StringPair} to another {@link StringPair}.  Ordering
     * is based first on the {@code x} value and then the {@code y} if there is
     * a tie.
     */
    public int compareTo(Object o) {
        StringPair other = (StringPair) o;
        int diff = this.x.compareTo(other.x);
        return (diff == 0) ? this.y.compareTo(other.y) : diff;
    }
}
