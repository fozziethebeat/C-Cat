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

import edu.ucla.sspace.util.CombinedIterator;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Keith Stevens
 */
public class ExtendedSet<T> extends AbstractSet<T> {

    private Set<T> initialSet;

    private Set<T> secondSet;

    public ExtendedSet(Set<T> initialSet) {
        this(initialSet, new HashSet<T>());
    }

    public ExtendedSet(Set<T> initialSet, Set<T> secondSet) {
        this.initialSet = initialSet;
        this.secondSet = secondSet;
    }

    public boolean add(T item) {
        if (initialSet.contains(item))
            return false;
        return secondSet.add(item);
    }

    public Iterator<T> iterator() {
        return new CombinedIterator<T>(
                initialSet.iterator(), secondSet.iterator());
    }

    public int size() {
        return initialSet.size() + secondSet.size();
    }
}
