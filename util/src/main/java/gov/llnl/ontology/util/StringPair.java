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

import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
 * A pair of Strings that can be used as a key in map reduce jobs.
 *
 * @author Keith Stevens
 */
public class StringPair implements Comparable, WritableComparable {

    public String x;

    public String y;

    /**
     * Constructs an empty {@link StringPair} for use with the {@link
     * WritableComparable} interface.
     */
    public StringPair() {
    }

    /**
     * Constructs a new {@link StringPair}.
     */
    public StringPair(String x, String y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Compares this {@link StringPair} to another {@link StringPair}.  Ordering
     * is based first on the {@code x} value and then the {@code y} if there is
     * a tie.
     */
    public int compareTo(Object o) {
        return compareTo((StringPair) o);
    }

    /**
     * {@inheritDoc}
     */
    public void write(DataOutput out) throws IOException {
        out.writeUTF(x);
        out.writeUTF(y);
    }

    /**
     * {@inheritDoc}
     */
    public void readFields(DataInput in) throws IOException {
        x = in.readUTF();
        y = in.readUTF();
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(StringPair w) {
        int diff = x.compareTo(w.x);
        return (diff == 0) ? y.compareTo(w.y) : diff;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (o == null || !(o instanceof StringPair))
            return false;
        StringPair p = (StringPair)o;
        return (x == p.x || (x != null && x.equals(p.x))) &&
               (y == p.y || (y != null && y.equals(p.y)));
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return ((x == null) ? 0 : x.hashCode()) ^
               ((y == null) ? 0 : y.hashCode());
    }

    /**
     * Returns a new {@link StringPair} from the contents of a string that has
     * been printed via {@link #toString}.
     */
    public static StringPair fromString(String text) {
        text = text.trim();
        text = text.substring(1, text.length() - 1);
        String[] parts = text.split(", ", 2);
        return new StringPair(parts[0].replaceAll("&comma;", ","),
                              parts[1].replaceAll("&comma;", ","));
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "{" + x.replaceAll(",", "&comma;") + 
               ", " + y.replaceAll(",", "&comma;") + "}";
    }
}
