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

package gov.llnl.ontology.mains;

import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;

import gov.llnl.ontology.wordnet.builder.WordNetBuilder;
import gov.llnl.ontology.wordnet.builder.OntologicalSortWordNetBuilder;
import gov.llnl.ontology.wordnet.builder.DepthFirstBnBWordNetBuilder;
import gov.llnl.ontology.wordnet.builder.UnorderedWordNetBuilder;

import edu.ucla.sspace.common.ArgOptions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class TestWordNetBuilder {
    public static void main(String[] args) throws Exception {
        ArgOptions options = new ArgOptions();
        options.addOption('w', "wordnetDir",
                          "Set the directory holding wordnet data files",
                          true, "DIR", "Required");
        options.addOption('l', "testWordList",
                          "Set the test word list",
                          true, "FILE", "Required");
        options.addOption('b', "wordnetBuilder",
                          "Set the wordnetBuilder",
                          true, "u|o|d|x", "Required");
        options.addOption('o', "order",
                          "If set, this will order terms based on the number " +
                          "of possible parents in the new word list, rather " +
                          "than in wordnet.",
                          true, "STRING", "Optional");
        options.parseOptions(args);

        if (!options.hasOption('b') ||
            !options.hasOption('w') ||
            !options.hasOption('l') ||
            options.numPositionalArgs() != 1) {
            System.err.println(
                    "usage: java ExtendWordNet [OPTIONS] <noun-pair-scores>\n" +
                    options.prettyPrint());
            System.exit(1);
        }

        // Load the test word list, and then wordnet.  Then remove each word in
        // the word list from wordnet and save the possible parents for each
        // word.
        Set<String> wordList = loadWordList(options.getStringOption('l'));
        OntologyReader wordnet = WordNetCorpusReader.initialize(
                options.getStringOption('w'));
        Map<String, Set<Synset>> wordParents = loadTestParents(
                wordList, wordnet);

        boolean orderInWn = !options.hasOption('o');
        // Create the builder.
        WordNetBuilder wnBuilder = null;
        if (options.getStringOption('b').equals("u"))
            wnBuilder = new UnorderedWordNetBuilder(wordnet);
        else if (options.getStringOption('b').equals("d"))
            wnBuilder = new DepthFirstBnBWordNetBuilder(wordnet, orderInWn, true);
        else if (options.getStringOption('b').equals("x"))
            wnBuilder = new DepthFirstBnBWordNetBuilder(wordnet, orderInWn, false);
        else if (options.getStringOption('b').equals("o"))
            wnBuilder = new OntologicalSortWordNetBuilder(wordnet, orderInWn);

        // Load the evidence and pass it to the word net builder.
        Map<String, Evidence> evidenceMap = loadEvidence(
                options.getPositionalArg(0));
        for (Map.Entry<String, Evidence> e : evidenceMap.entrySet()) {
            String termToAdd = e.getKey();
            String[] parents = e.getValue().parentsAsArray();
            double[] scores = e.getValue().scoresAsArray();
            Map<String, Double> cousinScores = new HashMap<String, Double>();
            wnBuilder.addEvidence(termToAdd, parents, scores, cousinScores);
        }

        BuilderScorer scorer = new BuilderScorer(wordParents);
        wnBuilder.addTerms(wordnet, scorer);
    }


    /**
     * Return a {@link Set} of terms that should be used to evaluate the word
     * net builder.
     */
    private static Set<String> loadWordList(String wordListFile)
            throws IOException {
        Set<String> words = new HashSet<String>();
        BufferedReader br = new BufferedReader(new FileReader(wordListFile));
        for (String line = null; (line = br.readLine()) != null;)
            words.add(line);
        return words;
    }

    /**
     * Get the parent {@link Synset}s for each word in the {@code wordList} and
     * remove each word's {@link Synset}s from the {@code wordnet} instance.
     */
    private static Map<String, Set<Synset>> loadTestParents(
            Set<String> wordList,
            OntologyReader wordnet) {
        Map<String, Set<Synset>> parentMaps =
            new HashMap<String, Set<Synset>>();
        for (String word : wordList) {
            Set<Synset> parents = new HashSet<Synset>();
            parentMaps.put(word, parents);
            for (Synset synset : wordnet.getSynsets(word, PartsOfSpeech.NOUN)) {
                parents.addAll(synset.getParents());
                wordnet.removeSynset(synset);
            }
        }

        return parentMaps;
    }

    public static Map<String, Evidence> loadEvidence(String evidenceFile) 
            throws Exception {
        Map<String, Evidence> evidenceMap = new HashMap<String, Evidence>();
        BufferedReader br = new BufferedReader(new FileReader(evidenceFile));
        for (String line = null; (line = br.readLine()) != null; ) {
            String[] toks = line.split("\\s+");
            String keyTerm = toks[0];
            String parentTerm = toks[1];
            double score = Double.parseDouble(toks[2]);
            Evidence evidence = evidenceMap.get(keyTerm);
            if (evidence == null) {
                evidence = new Evidence();
                evidenceMap.put(keyTerm, evidence);
            }
            evidence.add(parentTerm, score);
        }

        return evidenceMap;
    }

    public static class Evidence {
        public ArrayList<String> parents;
        public ArrayList<Double> scores;

        public Evidence() {
            parents = new ArrayList<String>();
            scores = new ArrayList<Double>();
        }

        public void add(String parent, double score) {
            parents.add(parent);
            scores.add(score);
        }

        public String[] parentsAsArray() {
            return parents.toArray(new String[parents.size()]);
        }

        public double[] scoresAsArray() {
            double[] doubleScores = new double[scores.size()];
            for (int i = 0; i < doubleScores.length; ++i)
                doubleScores[i] = scores.get(i);
            return doubleScores;
        }
    }
}
