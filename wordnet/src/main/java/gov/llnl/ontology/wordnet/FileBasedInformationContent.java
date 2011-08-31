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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;


/**
 * This {@link InformationContent} implementation loads the content data from
 * plain text based file where each line contains an offset, part of speech, and
 * content count.  This is the typical format used content files distributed
 * with the WordNet::Similarity perl package.
 *
 * @author Keith Stevens
 */
public class FileBasedInformationContent implements InformationContent {

    /**
     * The content for each known synset.
     */
    private Map<Synset, Double> synsetContent;

    /**
     * The total content for each part of speech.
     */
    private double[] posContent;

    /**
     * Creates a {@link FileBasedInformationContent} from the provided file
     * name.  tThis {@link InformationContent} should only be loaded after
     * initializing the {@link WordNetCorpusReader}.
     *
     * @throws IllegalArgumentException When an offset id does not match any
     *         known offset value in wordnet as this is indicitive that the
     *         incorrect content file is being used.
     */
    public FileBasedInformationContent(String icFilename) {
        posContent = new double[WordNetCorpusReader.POS_TAGS.length];
        synsetContent = new HashMap<Synset, Double>();
        WordNetCorpusReader wordnet = WordNetCorpusReader.getWordNet();

        try {
            // Read in each line of the content file.  Each line, except for the
            // first is formatted such that the offset value and the part of
            // speech share the same token.
            BufferedReader br = new BufferedReader(new FileReader(icFilename));
            int index = 0;
            for (String line = null; (line = br.readLine()) != null; ) {
                // Skip the first line of the informaton content files as they
                // have a junk header.
                if (index++ == 0)
                    continue;

                // Tokenize the line.
                String[] tokens = line.split("\\s+");
                // Extract the offset value and the part of speech.
                Integer offset = Integer.parseInt(
                        tokens[0].substring(0, tokens[0].length() - 1));
                PartsOfSpeech pos = WordNetCorpusReader.POS_MAP.get(
                        tokens[0].substring(tokens[0].length()-1));

                // Extract the content value.
                Double value = Double.parseDouble(tokens[1]);

                // Get the synset value for the given synset.  This may fail if
                // the user loaded up a wordnet dictionary that has been
                // modified but did not load a corresponding information content
                // file.  If that is the case, crash and inform the user.
                Synset synset = wordnet.getSynsetFromOffset(offset, pos);
                if (synset == null)
                    throw new IllegalArgumentException(
                            "An offset in the information content file did " +
                            "not match any known synset offset.  Please " + 
                            "check that the correct file is being used");

                // Save the values
                synsetContent.put(synset, value);
                posContent[pos.ordinal()] += value;
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public double contentForSynset(Synset synset) {
        Double content = synsetContent.get(synset);
        return (content == null) ? -1 : content;
    }

    /**
     * {@inheritDoc}
     */
    public double contentForPartOfSpeech(PartsOfSpeech pos) {
        return posContent[pos.ordinal()];
    }

    /**
     * {@inheritDoc}
     */
    public double informationContent(Synset synset) {
        double content = contentForSynset(synset);
        double posContent = contentForPartOfSpeech(synset.getPartOfSpeech());
        return (content == -1) ? -1 : -Math.log(content / posContent);
    }
}
