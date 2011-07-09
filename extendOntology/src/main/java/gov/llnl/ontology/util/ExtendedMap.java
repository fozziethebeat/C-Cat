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

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * A {@link Map} that extends an existing map without modifying the underlying
 * map.  Given an existing {@link Map}, this class will provide read only access
 * to the elements in the {@link Map} and store all new key, value pairs in a
 * new internal {@link Map}.
 *
 * </p>
 *
 * This class is extremely useful for when several different maps need to share
 * a set of mappings provided by a base map, but duplicate copies of the map
 * cannot be created, due to memory constraints.
 *
 * @author Keith Stevens
 */
public class ExtendedMap<K, V> extends AbstractMap<K, V> {

  /**
   * The base {@link Map} that is to be extended.  Only read access is permited
   * to this map.
   */
  private Map<K, V> baseMap;

  /**
   * The map used for storing additional values added to this {@link
   * ExtendedMap}.
   */
  private Map<K, V> extendedMap;

  /**
   * Creates a new {@link ExtendedMap} that wraps the given {@link Map}.  All
   * new mappings will be stored in a new map so that {@link baseMap} is never
   * modified.
   */
  public ExtendedMap(Map<K, V> baseMap) {
    this.baseMap = baseMap;
    this.extendedMap = new HashMap<K, V>();
  }

  /**
   * {@inheritDoc}
   */
  public Set<Map.Entry<K, V>> entrySet() {
    return new CombinedSet<Map.Entry<K, V>>(
            baseMap.entrySet(), extendedMap.entrySet());
  }

  /**
   * {@inheritDoc}
   */
  public V get(Object key) {
    V value = baseMap.get(key);
    return (value != null) ? value : extendedMap.get(key);
  }

  /**
   * {@inheritDoc}
   */
  public V put(K key, V value) {
    if (baseMap.containsKey(key))
      throw new IllegalArgumentException("Should not reinsert keys");
    return extendedMap.put(key, value);
  }
}
