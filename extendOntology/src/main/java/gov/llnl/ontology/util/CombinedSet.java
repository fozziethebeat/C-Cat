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

import edu.ucla.sspace.util.CombinedIterator;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * A set that is composed of several sets.  The original sets are unmodified,
 * even if there are duplicate entries in multiple sets.
 *
 * @author Keith Stevens
 */
public class CombinedSet<T> extends AbstractSet<T> {

    /**
    * The list of sets that this {@link CombinedSet} combines.
    */
    private List<Set<T>> sets;

    /**
    * Creates a {@link CombinedSet} from all of the provided {@link Set}s.
    */
    public CombinedSet(Set<T>...sets) {
        this(Arrays.asList(sets));
    }

    /**
    * Creates a {@link CombinedSet} from all of the provided {@link Set}s.
    */
    public CombinedSet(Collection<Set<T>> setCollection) {
        this.sets = new ArrayList<Set<T>>();
        this.sets.addAll(setCollection);
    }

    /**
    * {@inheritDoc}
    */
    public Iterator<T> iterator() {
        List<Iterator<T>> iters = new ArrayList<Iterator<T>>();
        for (Set<T> set : sets)
            iters.add(set.iterator());
        return new CombinedIterator<T>(iters);
    }

    /**
    * {@inheritDoc}
    */
    public int size() {
        int size = 0;
        for (Set<T> set : sets)
            size += set.size();
        return size;
    }
    }
