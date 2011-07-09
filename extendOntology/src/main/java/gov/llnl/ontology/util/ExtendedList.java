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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;


/**
 * A {@link List} that extends an existing {@link List} without modifying the
 * original {@link List}.  New items added to this {@link List} are stored in an
 * internal {@link List} that represents all of the values proceeding the
 * elements in the extended {@link List}.  The extended {@link List} is
 * unmodified.
 *
 * </p>
 *
 * This is extermely useful for applications that need to make different
 * extensions to an existing {@link List} but cannot make multiple copies of the
 * original {@link List} due to memory constraints.
 *
 * @author Keith Stevens
 */
public class ExtendedList<T> extends AbstractList<T> {
    
    /**
     * The base {@link List} that is being extended.  Only read access is given.
     */
    private List<T> baseItems;

    /**
     * The {@link List} used to store any items that are added to this {@link
     * ExtendedList}
     */
    private List<T> extendedItems;

    /**
     * Creates a new extended view of the given {@link List}.  Any new items
     * added to the {@link ExtendedList} with calls to {@code add} will be added
     * to a secondary {@link List}, and thus leave {@code baseItems} unmodified.
     * Only read access is given to {@code baseItems}.  As long as {@code
     * baseItems} is synchronized, it can be extended with multiple {@link
     * ExtendedList}s without and concern.
     */
    public ExtendedList(List<T> baseItems) {
        this.baseItems = baseItems;
        this.extendedItems = new ArrayList<T>();
    }

    /**
     * {@inheritDoc}
     */
    public boolean add(T e) {
        return extendedItems.add(e);
    }

    /**
     * {@inheritDoc}
     */
    public T get(int index) {
        return (index < baseItems.size())
            ? baseItems.get(index) 
            : extendedItems.get(index - baseItems.size());
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return baseItems.size() + extendedItems.size();
    }
}
