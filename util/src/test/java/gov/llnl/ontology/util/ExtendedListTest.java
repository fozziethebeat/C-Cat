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
public class ExtendedListTest {

    private static final List<String> array1 = 
        Arrays.asList(new String[] {"one", "two", "three"});

    private static final List<String> array2 = 
        Arrays.asList(new String[] {"one", "two", "three"});

    @Test public void testAdd() {
        List<String> extendedList = new ExtendedList<String>(array1);
        for (String s : array2)
            assertTrue(extendedList.add(s));
        assertEquals(3, array1.size());
    }

    @Test public void testSize() {
        List<String> extendedList = new ExtendedList<String>(array1);
        assertEquals(array1.size(), extendedList.size());
        int size = array1.size();
        for (String s : array2) {
            extendedList.add(s);
            assertEquals(++size, extendedList.size());
        }
    }

    @Test public void testGet() {
        List<String> extendedList = new ExtendedList<String>(array1);
        for (int i = 0; i < array1.size(); ++i)
            assertEquals(array1.get(i), extendedList.get(i));

        for (String s : array2)
            extendedList.add(s);

        for (int i = 0; i < array1.size(); ++i)
            assertEquals(array1.get(i), extendedList.get(i));

        for (int i = 0; i < array2.size(); ++i)
            assertEquals(array2.get(i), extendedList.get(i + array1.size()));
    }

    @Test public void testIterator() {
        List<String> extendedList = new ExtendedList<String>(array1);
        for (String s : array2)
            extendedList.add(s);

        Iterator<String> comboIter = extendedList.iterator();
        Iterator<String> iter1 = array1.iterator();
        Iterator<String> iter2 = array2.iterator();

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
