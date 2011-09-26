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

package gov.llnl.ontology.wordnet;

import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;


public class GenericMockReader extends OntologyReaderAdaptor {

    private Map<String, Synset> synsetMap;

    private String[][] synsetData;

    public GenericMockReader(String[][] synsetData) {
        super(new UnsupportedOntologyReader());
        synsetMap = Maps.newHashMap();
        for (String[] synsetAndGloss : synsetData)
            synsetMap.put(synsetAndGloss[0], makeSynset(synsetAndGloss));
    }

    public Set<Synset> allSynsets() {
        return Sets.newHashSet(synsetMap.values());
    }

    public Set<String> wordnetTerms() {
        return synsetMap.keySet();
    }

    public Synset[] getSynsets(String lemma) {
        List<Synset> found = Lists.newArrayList();
        for (Map.Entry<String, Synset> e : synsetMap.entrySet())
            if (e.getKey().startsWith(lemma))
                found.add(e.getValue());
        return found.toArray(new Synset[0]);
    }

    public Synset[] getSynsets(String lemma, PartsOfSpeech pos) {
        if (pos == null)
            return getSynsets(lemma);

        return getSynsets(lemma + "." + pos.toString());
    }

    public Synset getSynset(String lemma) {
        for (Map.Entry<String, Synset> e : synsetMap.entrySet())
            if (e.getKey().equals(lemma))
                return e.getValue();
        return null;
    }

    private Synset makeSynset(String[] synsetAndGloss) {
        Synset synset = new BaseSynset(0, PartsOfSpeech.NOUN);
        synset.addSenseKey(synsetAndGloss[0]);
        synset.setDefinition(synsetAndGloss[1]);
        return synset;
    }
}
