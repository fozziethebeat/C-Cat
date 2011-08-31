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

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.*;


/**
 * @author Keith Stevens
 */
public class CombinedSetTest {

    private static final String[] array1 = 
        {"one", "two", "three"};

    private static final String[] array2 = 
        {"one", "two", "three"};

    @Test public void testCombinedSize() {
        Set<String> set1 = new HashSet<String>();
        for (String s : array1)
            set1.add(s);
        Set<String> set2 = new HashSet<String>();
        for (String s : array2)
            set2.add(s);

        Set<String> combined = new CombinedSet<String>(set1, set2);
        assertEquals(6, combined.size());
    }

    @Test public void testContains() {
        Set<String> set1 = new HashSet<String>();
        for (String s : array1)
            set1.add(s);
        Set<String> set2 = new HashSet<String>();
        for (String s : array2)
            set2.add(s);

        Set<String> combined = new CombinedSet<String>(set1, set2);
        for (String s : array1)
            assertTrue(combined.contains(s));
        for (String s : array2)
            assertTrue(combined.contains(s));
    }

    @Test public void testIterator() {
        Set<String> set1 = new HashSet<String>();
        for (String s : array1)
            set1.add(s);
        Set<String> set2 = new HashSet<String>();
        for (String s : array2)
            set2.add(s);

        Set<String> combined = new CombinedSet<String>(set1, set2);
        Iterator<String> comboIter = combined.iterator();
        Iterator<String> iter1 = set1.iterator();
        Iterator<String> iter2 = set2.iterator();

        while (iter1.hasNext()) {
            assertTrue(comboIter.hasNext());
            assertEquals(iter1.next(), comboIter.next());
        }

        while (iter2.hasNext()) {
            assertTrue(comboIter.hasNext());
            assertEquals(iter2.next(), comboIter.next());
        }
    }
}
