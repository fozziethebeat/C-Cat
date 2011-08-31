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

public class ExtendedMapTest {

    private static final HashMap<String, String> map1 = 
        new HashMap<String, String>();

    private static final HashMap<String, String> map2 = 
        new HashMap<String, String>();

    static {
        map1.put("a", "1");
        map1.put("b", "2");
        map1.put("c", "3");

        map2.put("d", "5");
        map2.put("e", "6");
        map2.put("f", "4");
    }

    @Test public void testPut() {
        Map<String,String> extendedMap = new ExtendedMap<String, String>(map1);
        for (Map.Entry<String, String> s : map2.entrySet())
            assertNull(extendedMap.put(s.getKey(), s.getValue()));
        assertEquals(3, map1.size());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testPutFailure() {
        Map<String,String> extendedMap = new ExtendedMap<String, String>(map1);
        for (Map.Entry<String, String> s : map2.entrySet())
            extendedMap.put(s.getKey(), s.getValue());

        for (Map.Entry<String, String> s : map2.entrySet())
            assertNotNull(extendedMap.put(s.getKey(), s.getValue()));

        for (Map.Entry<String, String> s : map1.entrySet())
            extendedMap.put(s.getKey(), s.getValue());
    }

    @Test public void testSize() {
        Map<String,String> extendedMap = new ExtendedMap<String, String>(map1);
        assertEquals(map1.size(), extendedMap.size());
        int size = map1.size();
        for (Map.Entry<String, String> s : map2.entrySet()) {
            extendedMap.put(s.getKey(), s.getValue());
            assertEquals(++size, extendedMap.size());
        }
    }

    @Test public void testGet() {
        Map<String,String> extendedMap = new ExtendedMap<String, String>(map1);
        for (String key : map1.keySet())
            assertEquals(map1.get(key), extendedMap.get(key));

        for (Map.Entry<String, String> s : map2.entrySet())
            extendedMap.put(s.getKey(), s.getValue());

        for (String key : map1.keySet())
            assertEquals(map1.get(key), extendedMap.get(key));

        for (String key : map2.keySet())
            if (!map1.containsKey(key))
                assertEquals(map2.get(key), extendedMap.get(key));
    }

    @Test public void testIterator() {
        Map<String, String> extendedMap = new ExtendedMap<String, String>(map1);
        for (Map.Entry<String, String> s : map2.entrySet())
            extendedMap.put(s.getKey(), s.getValue());

        Iterator<Map.Entry<String, String>> comboIter = 
            extendedMap.entrySet().iterator();
        Iterator<Map.Entry<String, String>> iter1 = map1.entrySet().iterator();
        Iterator<Map.Entry<String, String>> iter2 = map2.entrySet().iterator();

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
