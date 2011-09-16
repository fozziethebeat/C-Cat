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

package gov.llnl.ontology.wordnet;

import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;

import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;


/**
 * A {@link SynsetWriter} that generates data lines in the form expected defined
 * by the original WordNet library.
 * 
 * @author Keith Stevens
 */
public class WordNetSynsetWriter implements SynsetWriter {

    /**
     * The number of bytes needed to represent each known synset offset.
     */
    public int offsetSize;

    /**
     * The string formatused to print out an offset.
     */
    public String offsetFormat;

    /**
     * Creates a new {@link WordNetSynsetWriter}.
     */
    public WordNetSynsetWriter(int offsetSize) {
        this.offsetSize = offsetSize;
        offsetFormat = "%0" + offsetSize + "d ";
    }

    /**
     * {@inheritDoc}.
     */
    public List<String> serializeSynsetKeys(Synset synset) {
        List<String> senseKeys = Lists.newArrayList();
        for (String senseKey : synset.getSenseKeys()) {
            StringBuilder sb = new StringBuilder();
            sb.append(senseKey).append(" ");
            sb.append(String.format(offsetFormat, synset.getId()));
            sb.append(synset.getSenseNumber()).append(" ");
            sb.append("0 ");
            senseKeys.add(sb.toString());
        }
        return senseKeys;
    }

    /**
     * Returns a string form of this {@link Synset} formatted in the WordNet
     * data.pos file format.  This method should only be called if an offset
     * value has been set for the {@link Synset} and an offset size has been
     * set.
     */
    public String serializeSynset(Synset synset) {
        StringBuilder sb = new StringBuilder();

        List<Lemma> lemmas = synset.getLemmas();
        PartsOfSpeech pos = synset.getPartOfSpeech();
        // Add the offest, lemma lexical file name index, and part of speech.
        // Always skip the first lemma because it is added in an ad hoc manner.
        sb.append(String.format(offsetFormat, synset.getId()));
        sb.append(String.format("%02d ", lemmas.get(1).getLexNameIndex()));
        sb.append(String.format(
                    "%s ",WordNetCorpusReader.POS_TAGS[pos.ordinal()]));

        // Add the lemmas for this synset.
        sb.append(String.format("%02x ", lemmas.size()-1));
        Iterator<Lemma> lemmaIter = lemmas.iterator();
        lemmaIter.next();
        while(lemmaIter.hasNext()) {
            Lemma lemma = lemmaIter.next();
            sb.append(lemma.getLemmaName()).append(" ");
            sb.append(String.format("%01x ", lemma.getLexicalId()));
        }

        // Add the relations that this synset has with other synsets.
        sb.append(String.format("%03d ", synset.getNumRelations()));
        for (String relationTag : synset.getKnownRelationTypes()) {

            // Iterate through the related synsets.
            for (Synset r : synset.getRelations(relationTag)) {
                int posOrdinal = r.getPartOfSpeech().ordinal();
                // Add the type of relation, offest of the related synset, and
                // it's part of speech.
                sb.append(relationTag).append(" ");
                sb.append(String.format(offsetFormat, r.getId()));
                sb.append(WordNetCorpusReader.POS_TAGS[posOrdinal]);
                sb.append(" ");

                // Add the related form indices if there are any.
                RelatedForm form = synset.getDerivationallyRelatedForm(r);
                if (form == null)
                    sb.append("0000 ");
                else
                    sb.append(String.format("%02x%02x ", 
                                            form.sourceIndex(),
                                            form.otherIndex()));
            }
        }

        // Add the verb frame information if this synset is a verb.
        if (pos == PartsOfSpeech.VERB) {
            int frameIds[]    = synset.getFrameIds();
            int lemmaIds[]    = synset.getLemmaIds();
            if (frameIds == null || lemmaIds == null) {
                sb.append("00 ");
            } else {
                sb.append(String.format("%02d ", frameIds.length));
                for (int i = 0; i < frameIds.length; ++i)
                    sb.append(String.format(
                                "+ %02d %02x ", frameIds[i], lemmaIds[i]));
            }
        }

        // Add the gloss.
        sb.append("| ");
        sb.append(synset.getGloss());

        // End the line and return the data line.
        sb.append("\n");
        return sb.toString();
    }
}
