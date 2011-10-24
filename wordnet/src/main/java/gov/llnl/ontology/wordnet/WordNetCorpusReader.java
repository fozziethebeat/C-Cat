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

import gov.llnl.ontology.util.StreamUtil;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.Synset.Relation;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import edu.ucla.sspace.util.CombinedIterator;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.Reader;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.logging.Logger;


/**
 * This class acts as the central interface for the WordNet dictionary.  It
 * begins it's initialization by reading all of the dictionary information into
 * ram and then generates a complete graph, connecting all {@link Synset}s via
 * the specified relations.  The dictionary graph can be modified during runtime
 * and later saved to disk in the same format as the original WordNet database,
 * allowing other interfaces the ability to access the modified form of WordNet.
 *
 * </p>
 *
 * This reader is heavily based on the NLTK WordNet corpus reader.
 *
 * @author Keith Stevens
 */
public class WordNetCorpusReader implements OntologyReader {

    /**
     * The logger for this class.
     */
    private static final Logger LOG =
        Logger.getLogger(WordNetCorpusReader.class.getName());

    /**
     * The set of part of speech tags.
     */
    public static final String[] POS_TAGS = {"n", "v", "a", "r", "s"};

    /**
     * A simple mapping from part of speech characters their respective {@link
     * ParstOfSpeech} enumerations.
     */
    public static final Map<String, PartsOfSpeech> POS_MAP = Maps.newHashMap();

    static {
        // Load the mappings into the part of speech map.
        POS_MAP.put("a", PartsOfSpeech.ADJECTIVE);
        POS_MAP.put("s", PartsOfSpeech.ADJECTIVE_SAT);
        POS_MAP.put("r", PartsOfSpeech.ADVERB);
        POS_MAP.put("n", PartsOfSpeech.NOUN);
        POS_MAP.put("v", PartsOfSpeech.VERB);
    }

    /**
     * Morphological replaces for each part of speech.  For each part of speech,
     * an of pairs of old endings and new endings are provided.
     */
    static final String[][][] MORPHOLOGICAL_SUBSTITUTIONS =
    {
        // Noun replacements.
        {
            {"s", ""}, {"ses", "s"}, {"ves", "f"}, {"xes", "x"}, {"zes", "z"},
            {"ches", "ch"}, {"shes", "sh"}, {"men", "man"}, {"ies", "y"}
        },
        // Verb replacements.
        {
            {"s", ""}, {"ies", "y"}, {"es", "e"}, {"es", ""}, {"ed", "e"},
            {"ed", ""}, {"ing", "e"}, {"ing", ""}
        },
        // Adjective replacements.
        {
            {"er", ""}, {"est", ""}, {"er", "e"}, {"est", "e"}
        },
        // Adverb replacements.
        {
        },
        // Adjective SAT replacements.
        {
        },
    };

    /**
     * The file extensions for each of the data and index files in the WordNet
     * dictionary.
     */
    public static final String[] FILE_EXTENSIONS =
            {"noun", "verb", "adj", "adv", ""};

    private static String UTF8 = "utf-8";

    /**
     * A singleton instance of a {@link WordNetCorpusReader}.
     */
    private static WordNetCorpusReader corpusReader;

    /**
     * The path that specifies the location of the WordNet dictionary files.
     */
    private String dictPath;

    /**
     * If {@code true}, the {@link WordNetCorpusReader} will read the dictionary
     * files from the jar containing the java classes.
     */
    private boolean readFromJar;

    /**
     * The lexicographer file names.
     */
    private String[] lexNames;

    /**
     * A list of morphological exceptions, initially ordered by parts of speech.
     */
    private List<Map<String, String>> posExceptionMap;

    /**
     * A mapping from lemmas and parts of speech to possible {@link Synset}s.
     */
    private Map<String, Synset[][]> lemmaPosOffsetMap;

    /**
     * The mapping from an offset to it's {@link Synset} for each part of
     * speech.
     */
    private List<Map<Integer, Synset>> posOffsetToSynsetMap;

    /**
     * The list of valid verb frames.
     */
    private List<String> verbFrames;

    /**
     * The maximum depth of the IS-A hierarchy for each part of speech.
     */
    private int[] maxDepths;

    /**
     * {@inheritDoc}
     */
    public Iterator<String> morphy(String form) {
        List<Iterator<String>> formIters = Lists.newArrayList();
        for (PartsOfSpeech pos : PartsOfSpeech.values())
            formIters.add(morphy(form, pos));
        return new CombinedIterator<String>(formIters);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<String> morphy(String form, PartsOfSpeech pos) {
        // Compute the suffix for this term and part of speech.
        String suffix = "";
        if (pos == PartsOfSpeech.NOUN && form.endsWith("ful")) {
            suffix = "ful";
            form = form.substring(0, form.length() - suffix.length());
        }

        // Return the morphological iterator.
        return new FormIterator(form, suffix,
                                MORPHOLOGICAL_SUBSTITUTIONS[pos.ordinal()],
                                posExceptionMap.get(pos.ordinal()).get(form));
    }

    /**
     * {@inheritDoc}
     */
    public Set<Synset> allSynsets() {
        Set<Synset> allSynsets = Sets.newHashSet();
        for (Synset[][] lemmaSynsets : lemmaPosOffsetMap.values())
            for (Synset[] posSynsets : lemmaSynsets)
                allSynsets.addAll(Arrays.asList(posSynsets));
        return allSynsets;
    }

    /**
     * {@inheritDoc}
     */
    public Set<Synset> allSynsets(PartsOfSpeech pos) {
        Set<Synset> allSynsets = Sets.newHashSet();
        for (Synset[][] lemmaSynsets : lemmaPosOffsetMap.values())
            allSynsets.addAll(Arrays.asList(lemmaSynsets[pos.ordinal()]));
        return allSynsets;
    }

    /**
     * {@inheritDoc}
     */
    public void addSynset(Synset synset) {
        addSynset(synset, -1);
    }

    /**
     * {@inheritDoc}
     */
    public void addSynset(Synset synset, int index) {
        int pos = synset.getPartOfSpeech().ordinal();

        Set<String> seenLemmas = Sets.newHashSet();

        for (Lemma lemma : synset.getLemmas()) {
            String lemmaName = lemma.getLemmaName().toLowerCase();
            if (seenLemmas.contains(lemmaName))
                continue;
            seenLemmas.add(lemmaName);

            Synset[][] lemmaSynsets = lemmaPosOffsetMap.get(lemmaName);

            // Add the lemma if it does not already have a mapping.
            if (lemmaSynsets == null) {
                lemmaSynsets = new Synset[POS_TAGS.length][];
                for (int i = 0; i < POS_TAGS.length; ++i)
                    lemmaSynsets[i] = new Synset[0];
                lemmaPosOffsetMap.put(lemmaName, lemmaSynsets);
            }

            // Check that the index is valid.
            if (index > lemmaSynsets[pos].length || index < -1)
                throw new IllegalArgumentException(
                        "Cannot add " + synset.getName() + " to lemma " + 
                        lemmaName + " at position " + index + 
                        " .    The index is out of bounds.");

            // If the index was not originally set, put the term at the end of
            // the list.
            if (index == -1 || index > lemmaSynsets[pos].length)
                index = lemmaSynsets[pos].length;

            // Copy the old synset's for this part of speech and add the new
            // synset at the end of the list.
            Synset[] newPosSynsets = new Synset[lemmaSynsets[pos].length +1];
            System.arraycopy(lemmaSynsets[pos], 0, newPosSynsets, 0, index);
            newPosSynsets[index] = synset;
            System.arraycopy(lemmaSynsets[pos], index, newPosSynsets, index+1, 
                             lemmaSynsets[pos].length - index);

            lemmaSynsets[pos] = newPosSynsets;
        }
    }
    /**
     * {@inheritDoc}
     */
    public void removeSynset(Synset synset) {
        int pos = synset.getPartOfSpeech().ordinal();

        Set<String> seenLemmas = Sets.newHashSet();

        // Remove reflexive relations from other synsets to this synset.  This
        // is simple as nearly all relations have a reflexive form, so we can
        // get the synsets that synset is connected to and remove the inverse
        // relation to synset from that related synset.
        for (String relationStr : synset.getKnownRelationTypes()) {
            Relation relation = Relation.fromId(relationStr);
            if (relation == null)
                continue;
            Relation inverse = relation.reflexive();
            if (inverse == null)
                continue;

            for (Synset related : synset.getRelations(relation))
                related.removeRelation(inverse, synset);
        }

        // Remove synset from each lemma mapping.
        for (Lemma lemma : synset.getLemmas()) {
            String lemmaName = lemma.getLemmaName().toLowerCase();
            if (seenLemmas.contains(lemmaName))
                continue;
            seenLemmas.add(lemmaName);

            Synset[][] lemmaSynsets = lemmaPosOffsetMap.get(lemmaName);

            // If the lemma does not already have a mapping, then we're done.
            // This shouldn't happen.
            if (lemmaSynsets == null) 
                continue;

            // Copy the old synset's for this part of speech and remove desired
            // synset.  Bump the rest down a spot.
            Synset[] newPosSynsets = new Synset[lemmaSynsets[pos].length - 1];
            for (int i = 0,j=0; i < lemmaSynsets[pos].length; ++i, ++j) {
                if (lemmaSynsets[pos][i] == synset)
                    j--;
                else
                    newPosSynsets[j] = lemmaSynsets[pos][i];
            }
            lemmaSynsets[pos] = newPosSynsets;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void replaceSynset(Synset synset, Synset replacement) {
        if (synset.getPartOfSpeech() != replacement.getPartOfSpeech())
            throw new IllegalArgumentException(
                    "Cannot replace a synset with another synset having a " +
                    "different part of speech.");

        // Create the set of lemmas that the replacement synset knows about.
        // This will be used to determine when the replacement synset is in the
        // same lemma mapping as the old synset.
        Set<String> replacementLemmas = Sets.newHashSet();
        for (Lemma lemma : replacement.getLemmas())
            replacementLemmas.add(lemma.getLemmaName().toLowerCase());

        // Iterate through all of the lemmas that could refer to this synset.
        Set<String> seenLemmas = Sets.newHashSet();
        for (Lemma lemma : synset.getLemmas()) {
            String lemmaName = lemma.getLemmaName().toLowerCase();

            // Since some lemmas, such as "G" and "g" map to the same base form
            // in the lemma map, we must skip these duplicate forms otherwise
            // the second form will try to reomve the synset from the lemma
            // mapping a second time.
            if (seenLemmas.contains(lemmaName))
                continue;
            seenLemmas.add(lemmaName);

            // Get all of the synsets for this lemma.
            Synset[][] posSynsets = lemmaPosOffsetMap.get(lemmaName);
            Synset[] synsets = posSynsets[synset.getPartOfSpeech().ordinal()];

            // If the replacement synset is in this lemma mapping, then we have
            // to remove one of the synsets and bump the others further up.
            // Since replacement is the one that should remain, just remove the
            // old synset from the mapping.
            if (replacementLemmas.contains(lemmaName)) {
                // Create a shortened version of the synsets matching the part
                // of speech for the synset to be removed.  Slide synsets to the
                // right of the given synset to the left by one position, and if
                // their base lemma is the current lemma, increase their sense
                // number by 1.
                Synset[] shortenedSynsets = new Synset[synsets.length-1];
                boolean replaced = false;
                for (int s = 0; s < synsets.length; ++s) {
                    if (synsets[s] == synset)
                        replaced = true;
                    else if (replaced) {
                        shortenedSynsets[s-1] = synsets[s];
                        if (synsets[s].getName().startsWith(lemmaName+"."))
                            synsets[s].setSenseNumber(s);
                    } else
                        shortenedSynsets[s] = synsets[s];
                }

                // Store the shortened synset list.
                posSynsets[synset.getPartOfSpeech().ordinal()] = 
                    shortenedSynsets;
            } else {
                // Otherwise we just have to replace the old synset mapping with
                // a pointer to the new synset.
                for (int s = 0; s < synsets.length; ++s) {
                    if (synsets[s] == synset)
                        synsets[s] = replacement;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> wordnetTerms() {
        return lemmaPosOffsetMap.keySet();
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> wordnetTerms(PartsOfSpeech pos) {
        Set<String> posLemmas = Sets.newHashSet();
        for (Map.Entry<String, Synset[][]> entry : lemmaPosOffsetMap.entrySet())
            if (entry.getValue()[pos.ordinal()].length != 0)
                posLemmas.add(entry.getKey());
        return posLemmas;
    }

    /**
     * Returns a singleton instance of the {@link WordNetCorpusReader}.  If the
     * reader has not already been created, it will be initialzied.  This method
     * assumes that {@code dictPath} does not correspond to a jar internal path.
     */
    public static WordNetCorpusReader initialize(String dictPath) {
        return initialize(dictPath, false);
    }

    /**
     * Returns a singleton instance of the {@link WordNetCorpusReader}.  If the
     * reader has not already been created, it will be initialized. If {@code
     * readFromjar} is true, the reader will {@code dictPath} as a path within
     * the current jar running this code and read the dictionary files from the
     * jar.  In these cases, {@code dictPath} should start with "/".  A common
     * argument for {@code dictPath} is "/dict", which assumes that the
     * directory dict contains all the WordNet dictionary files and is as the
     * base directory of the jar.
     */
    public static WordNetCorpusReader initialize(String dictPath,
                                                 boolean readFromJar) {
        if (corpusReader == null)
            corpusReader = new WordNetCorpusReader(dictPath, readFromJar);
        return corpusReader;
    }

    /**
     * Returns the initialzied instance of the {@link WordNetCorpusReader}.
     */
    public static WordNetCorpusReader getWordNet() {
        return corpusReader;
    }

    /**
     * {@inheritDoc}
     */
    public Synset[] getSynsets(String lemma) {
        List<Synset> allSynsets = Lists.newArrayList();
        for (PartsOfSpeech pos : PartsOfSpeech.values())
            allSynsets.addAll(Arrays.asList(getSynsets(lemma, pos)));
        return allSynsets.toArray(new Synset[allSynsets.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public Synset[] getSynsets(String lemma, PartsOfSpeech pos) {
        return getSynsets(lemma, pos, true);
    }

    /**
     * {@inheritDoc}
     */
    public Synset[] getSynsets(String lemma, PartsOfSpeech pos, boolean useMorphy) {
        if (!useMorphy) {
            Synset[][] termSynsets = lemmaPosOffsetMap.get(lemma);
            return (termSynsets == null) ? null : termSynsets[pos.ordinal()];
        }

        // If there are spaces, replace them with underscores, since no lemma
        // has spaces in it.
        String fixedLemma = lemma.replaceAll("\\s+", "_");

        // If pos is null, try searching with the fixed lemma.
        if (pos == null)
            return getSynsets(fixedLemma);

        // Get the synsets for the original form, doing no morphological
        // parsing.
        Synset[][] lemmaSynsets = lemmaPosOffsetMap.get(fixedLemma);
        if (lemmaSynsets != null && lemmaSynsets[pos.ordinal()].length > 0) {
            for (Synset s : lemmaSynsets[pos.ordinal()])
                s.addMorphyMapping(lemma, fixedLemma); 
            return lemmaSynsets[pos.ordinal()];
        }

        // Try getting the word with the full string as it is.
        List<Synset> synsets = getWithMorphy(lemma, fixedLemma, "", pos);
        // If that failed, try splitting the string based on underscores and
        // running morphy on just the first part.  The second part will no be
        // run through morphy and just be a post fix.
        if (synsets.size() == 0) {
            String[] parts = fixedLemma.split("_", 2);
            if (parts.length == 2)
                synsets = getWithMorphy(lemma, parts[0], "_"+parts[1], pos);
        }

        // If we couldn't find anything using underscores instead of spaces, try
        // using hyphens.  If there are still spaces, then we know that -'s
        // haven't been tried, on the recursive call, this will check will fail
        // and it will return what we find, no matter what.
        if (synsets.size() == 0 && lemma.indexOf(" ") > -1)
            return getSynsets(lemma.replaceAll("\\s+", "-"), pos);
        // Similarity, try the whole thing without any spaces, some words like
        // "guest room" are like this.
        if (synsets.size() == 0 && lemma.indexOf("-") > -1)
            return getSynsets(lemma.replaceAll("-", ""), pos);

        // Otherwise return all that we have found.
        return synsets.toArray(new Synset[synsets.size()]);
    }

    private List<Synset> getWithMorphy(String lemma,
                                       String fixedLemma,
                                       String post,
                                       PartsOfSpeech pos) {
        // Find the Synsets for each morphological variation.
        List<Synset> allSynsets = Lists.newArrayList();
        Iterator<String> formIter = morphy(fixedLemma, pos);
        Synset[][] lemmaSynsets;
        while (formIter.hasNext()) {
            String alternative = formIter.next() + post;
            lemmaSynsets = lemmaPosOffsetMap.get(alternative);
            if (lemmaSynsets != null && lemmaSynsets[pos.ordinal()].length > 0)
                for (Synset s : lemmaSynsets[pos.ordinal()]) {
                    s.addMorphyMapping(lemma, alternative);
                    allSynsets.add(s);
                }
        }
        return allSynsets;
    }

    /**
     * {@inheritDoc}
     */
    public Synset getSynset(String fullSynsetName) {
        fullSynsetName = fullSynsetName.trim();

        int lastDot = fullSynsetName.lastIndexOf(".");
        int secondDot = fullSynsetName.lastIndexOf(".", lastDot-1);
        int senseNum = Integer.parseInt(fullSynsetName.substring(lastDot+1));
        PartsOfSpeech pos = POS_MAP.get(
                fullSynsetName.substring(secondDot+1, lastDot));
        String lemma = fullSynsetName.substring(0, secondDot);
        return getSynset(lemma, pos, senseNum);
    }

    /**
     * {@inheritDoc}
     */
    public Synset getSynset(String lemma, PartsOfSpeech pos, int senseNum) {
        Synset[][] lemmaSynsets = lemmaPosOffsetMap.get(lemma);
        if (lemmaSynsets == null)
            return null;
        Synset[] lemmaPosSynsets = lemmaSynsets[pos.ordinal()];
        if (senseNum < 1 || senseNum > lemmaPosSynsets.length)
            return null;
        return lemmaPosSynsets[senseNum-1];
    }

    /**
     * {@inheritDoc}
     */
    public int getMaxDepth(PartsOfSpeech pos) {
        int pIndex = pos.ordinal();
        if (maxDepths[pIndex] != 0)
            return maxDepths[pIndex];

        for (Synset[][] lemmaSynsets : lemmaPosOffsetMap.values())
            for (Synset synset : lemmaSynsets[pIndex])
                maxDepths[pIndex] = Math.max(
                        maxDepths[pIndex], synset.getMaxDepth());
        return maxDepths[pIndex];
    }

    /**
     * Returns a {@link Synset} based on it's btye offset and part of speech.
     * This is to be used internally to the package only by other methods that
     * track the offset information.
     */
    /* package private */ Synset getSynsetFromOffset(int offset,
                                                     PartsOfSpeech pos) {
        return posOffsetToSynsetMap.get(pos.ordinal()).get(offset);
    }

    /**
     * Creates a new {@link WordNetCorpusReader}.
     */
    private WordNetCorpusReader(String dictPath, boolean readFromJar) {
        // Store the values that specify how dictionary files will be read.
        this.dictPath = dictPath;
        this.readFromJar = readFromJar;
        this.maxDepths = new int[POS_TAGS.length];

        // Initialzie basic data structures.
        lemmaPosOffsetMap = Maps.newHashMap();
        posExceptionMap = Lists.newArrayList();
        verbFrames = Lists.newArrayList();
        posOffsetToSynsetMap = Lists.newArrayList();
        for (int i = 0; i < POS_TAGS.length; ++i)
            posExceptionMap.add(new HashMap<String, String>());

        try {
            LOG.info("parsing lexicographer names");
            lexNames = parseLexNames();

            LOG.info("loading lemma offsets");
            loadLemmaPosOffsetMap();
             
            LOG.info("loading verb frames");
            loadVerbFrames();

            LOG.info("loading exception map");
            loadExceptionMap();

            LOG.info("loading synset tree");
            loadSynsetTree();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        } 
    }

    /**
     * Returns the Lexicographer file names.
     */
    private String[] parseLexNames() throws IOException {
        BufferedReader br = getReader("lexnames");
        List<String> names = Lists.newArrayList();
        for (String line = null; (line = br.readLine()) != null; )
            names.add(line.split("\\s+")[1]);
        return names.toArray(new String[names.size()]);
    }

    /**
     * Sets up the verb frames, with "----" replaced with a format string so
     * that lemmas can easily be embedded into the frame via {@code
     * String.Format}.
     */
    private void loadVerbFrames() throws IOException {
        BufferedReader br = getReader("frames.vrb");
        for (String line = null; (line = br.readLine()) != null; ) {
            verbFrames.add(line.split("\\s+", 2)[1].replace("----", "%s"));
        }
    }

    /**
     * Loads the lemma, part of speech to {@link Synset} map.  This also returns
     * a mapping from offset values to the relevant {@link Synset}s.
     */
    private void loadLemmaPosOffsetMap() throws IOException {
        // Evaluate the index for for each part of speech.
        for (String suffix : FILE_EXTENSIONS) {
            // Initialize the part of speech to Synset map.
            Map<Integer, Synset> offsetToSynset = Maps.newHashMap();
            posOffsetToSynsetMap.add(offsetToSynset);

            // Skip parts of speech that do not have an index file.
            if (suffix.equals(""))
                continue;

            // Read each line in the index file.
            BufferedReader br = getReader("index." + suffix);
            for (String line = null; (line = br.readLine()) != null; ) {
                // Skip lines that begin with a space, these are for the
                // license.
                if (line.startsWith(" "))
                    continue;

                // parse the line into individual tokens according to
                // whitespace.
                int index = 0;
                String[] tokens = line.split("\\s+");

                // Extract the lemma and part of speech.
                String lemma = tokens[index++].intern();
                String pos = tokens[index++];
                PartsOfSpeech posTag = POS_MAP.get(pos);

                // Extract the number of synsets for this lemma.
                int numSynsets = Integer.parseInt(tokens[index++]);

                // Skip the pointers.  These will be parsed in the data file.
                int numPointers = Integer.parseInt(tokens[index++]);
                index += numPointers;

                // Skip the number of senses.
                index++;
                // Skip the number of senses ranked according to frequency.
                index++;

                // Get the offset values.  These correspond to the possible
                // Synsets for the given lemma.  For each offset, get the Synset
                // from the offset to synset map, if it exists, or create a new
                // Synset for the offset.
                String[] offsets = Arrays.copyOfRange(tokens, index,
                        tokens.length);
                Synset[] synsets = new Synset[offsets.length];
                for (int s = 0; s < offsets.length; ++s) {
                    int offset = Integer.parseInt(offsets[s]);
                    synsets[s] = offsetToSynset.get(offset);
                    if (synsets[s] == null) {
                        synsets[s] = new BaseSynset(offset, posTag);
                        synsets[s].setSenseNumber(s+1);
                        synsets[s].addLemma(
                                new BaseLemma(synsets[s], lemma, pos));
                        offsetToSynset.put(offset, synsets[s]);
                    }
                }

                // Store the synsets for this lemma in the lemma, part of speech
                // to synset map.
                Synset[][] posToOffsets = lemmaPosOffsetMap.get(lemma);
                if (posToOffsets == null) {
                    posToOffsets = new Synset[POS_TAGS.length][0];
                    lemmaPosOffsetMap.put(lemma, posToOffsets);
                }
                posToOffsets[posTag.ordinal()] = synsets;

                // The adjective satalite part of speech does not have an index
                // file but requries the same mappings as the adjective data, so
                // add anything added to the adjective part of speech to the
                // adjective satalite part of speech mappings.
                //if (posTag == PartsOfSpeech.ADJECTIVE)
                //    posToOffsets[PartsOfSpeech.ADJECTIVE_SAT.ordinal()] = 
                //        synsets;
            }

        }

        // The adjective satalite part of speech does not have an index file but
        // requries the same mappings as the adjective data, so add anything
        // added to the adjective part of speech to the adjective satalite part
        // of speech mappings.
        posOffsetToSynsetMap.set(
                PartsOfSpeech.ADJECTIVE_SAT.ordinal(),
                posOffsetToSynsetMap.get(PartsOfSpeech.ADJECTIVE.ordinal()));
    }

    /**
     * Loads the morphological exception map.
     */
    private void loadExceptionMap() throws IOException {
        for (int pos = 0; pos < FILE_EXTENSIONS.length; pos++) {
            // Skip any parts of speech that do not have an exception.
            if (FILE_EXTENSIONS[pos].equals(""))
                continue;

            // Exceptions are stored as "fullTerm morphedTerm".
            Map<String, String> exceptionMap = posExceptionMap.get(pos);
            BufferedReader br = getReader(FILE_EXTENSIONS[pos] + ".exc");
            for (String line = null; (line = br.readLine()) != null; ) {
                String[] exception = line.split("\\s+");
                exceptionMap.put(exception[0], exception[1]);
            }
        }
        // Make a copy of the exceptions for the adjective satalite parts of
        // speech.
        posExceptionMap.set(PartsOfSpeech.ADJECTIVE_SAT.ordinal(), 
                            posExceptionMap.get(
                                PartsOfSpeech.ADJECTIVE.ordinal())); 
    }

    /**
     * Loads the entire {@link Synset} tree into memory by parsing the data
     * files and filling in the existing {@link Synset}s that were created while
     * loading the index files.
     */
    private void loadSynsetTree() throws IOException {
        // Read the data file for each part of speech.
        for (int pos = 0; pos < FILE_EXTENSIONS.length; pos++) {
            // Skip parts of speech that do not have data files.
            if (FILE_EXTENSIONS[pos].length() == 0)
                continue;

            // Get the offset to synset map for this part of speech.
            Map<Integer, Synset> offsetToSynsetMap = 
                posOffsetToSynsetMap.get(pos);

            // Read each line in the data file.  Each line will contain the
            // synset offset, related lemmas, a set of relational links, a
            // gloss, and for verbs, a set of verb frames.
            BufferedReader br = getReader("data." + FILE_EXTENSIONS[pos]);
            for (String line = null; (line = br.readLine()) != null; ) {
                if (line.startsWith(" "))
                    continue;

                // The data columns and gloss information are separated by a
                // pipe.
                String[] columnAndGloss = line.split("\\|");

                // Extract the lemma terms for the synset and relational links
                // by examining the columns prior to the gloss.
                int index = 0;
                String[] columns = columnAndGloss[0].split("\\s+");

                // Get the synset corresponding to this offset.
                int offset = Integer.parseInt(columns[index++]);
                Synset synset = offsetToSynsetMap.get(offset);
                if (synset == null) {
                    throw new IllegalArgumentException(
                            "A synset offset was missing from the index file " +
                            "for line: " + line + " \n, which generated " +
                            "offset: " + offset);
                }

                // Extract the gloss.  Glosses contain both examples and
                // definitions.  Examples start with quotes and definitions do
                // not.
                String gloss = columnAndGloss[1].trim();
                StringBuilder defBuilder = new StringBuilder();
                for (String glossPart : gloss.split(";")) {
                    glossPart = glossPart.trim();
                    if (glossPart.startsWith("\""))
                        synset.addExample(glossPart.replace('"', ' ').trim());
                    else
                        defBuilder.append(glossPart).append("; ");
                }
                gloss = defBuilder.toString();

                // In WordNet-2.1, some adjectives lack a definition.  In these
                // cases, simply save an empty defition, otherwise save a copy
                // of the definition that lacks the final ";" character.
                if (gloss.length() < 2)
                    synset.setDefinition("");
                else 
                    synset.setDefinition(gloss.substring(0, gloss.length() - 2));

                // Extract the lexicographer file name and part of speech.
                int lexNameIndex = Integer.parseInt(columns[index++]);
                String lexName = lexNames[lexNameIndex];
                String posTag = columns[index++];

                // Extract the lemmas that are attached to this synset.
                int numLemmas = Integer.parseInt(columns[index++], 16);
                BaseLemma[] lemmas = new BaseLemma[numLemmas];
                for (int l = 0; l < numLemmas; ++l) {
                    // Lemmas sometimes have syntactic markers in the form of
                    // (pos).  Extract this additional marker and keep it
                    // separate from the base name.
                    String lemmaName = columns[index++];
                    String[] lemmaAndMarker = lemmaName.split("\\(");
                    lemmaName = lemmaAndMarker[0];
                    String marker = (lemmaAndMarker.length == 1)
                        ? ""
                        : lemmaAndMarker[1].replaceAll("[\\(\\)]", "");

                    // Create a new lemma object that maps to this synset.
                    int lexId = Integer.parseInt(columns[index++], 16);
                    lemmas[l] = new BaseLemma(synset, lemmaName, lexName, 
                                              lexNameIndex, lexId, marker);
                }

                // Extract the relations that this synset has with other
                // synsets.  Add in a relational link to the Synset graph
                // connectring this synset to other specified synsets.
                int numPointers = Integer.parseInt(columns[index++]);
                for (int p = 0; p < numPointers; ++p) {
                    String symbol = columns[index++].intern();

                    // Get the pointed to synset based on it's offset and part
                    // of speech.
                    int pointerOffset = Integer.parseInt(columns[index++]);
                    int pointerPos = POS_MAP.get(columns[index++]).ordinal();
                    Synset pointerSynset =
                        posOffsetToSynsetMap.get(pointerPos).get(pointerOffset);

                    // Add the relation link.
                    synset.addRelation(symbol, pointerSynset);

                    // When the lemma id is non zero it is a derivationally
                    // related lemma.  Since not all lemmas have been added
                    // to their Synsets yet, just add in a link based on the
                    // Synset and the lemma index that corresponds to the
                    // related form.
                    String lemmaId = columns[index++];
                    if (!lemmaId.equals("0000")) {
                        int sourceIndex = Integer.parseInt(
                                lemmaId.substring(0, 2), 16);
                        int targetIndex = Integer.parseInt(
                                lemmaId.substring(2), 16);
                        synset.addDerivationallyRelatedForm(
                                pointerSynset, new SimpleRelatedForm(
                                    sourceIndex, targetIndex));
                    }
                }

                // Read off the verb frames if we are parsing the verb data.
                if (POS_TAGS[pos].equals("v")) {
                    int frameCount = Integer.parseInt(columns[index++]);
                    int[] frameIds = new int[frameCount];
                    int[] lemmaIds = new int[frameCount];
                    for (int f = 0; f < frameCount; ++f) {
                        // Read off the +.
                        index++;

                        // Store the frame number.
                        int frameNumber = Integer.parseInt(columns[index++]);
                        frameIds[f] = frameNumber;

                        // Get the frame format of this verb.
                        String frame = verbFrames.get(frameNumber-1);

                        // Embed the specified lemmas into the verb frame and
                        // attach it to each lemma object.  A lemma number of 0
                        // signifies that all lemmas should be embedded in the
                        // frame.
                        int lemmaNumber = Integer.parseInt(columns[index++], 16);
                        lemmaIds[f] = lemmaNumber;
                        if (lemmaNumber == 0) {
                            // Embed every lemma into the frame.
                            for (BaseLemma lemma : lemmas)
                                lemma.addFrameString(String.format(
                                            frame, lemma.getLemmaName()));
                        }
                        else
                            // Embed only the specified lemma into the frame.
                            lemmas[lemmaNumber-1].addFrameString(String.format(
                                        frame, lemmas[lemmaNumber-1].getLemmaName()));
                    }
                    synset.setFrameInfo(frameIds, lemmaIds);
                }

                // Add in lemma keys for each of the lemmas.
                for (BaseLemma lemma : lemmas) {
                    lemma.setKey(String.format("%s%%%d:%02d:%02d::",
                                 lemma.getLemmaName(), pos+1, 
                                 lemma.getLexNameIndex(),
                                 lemma.getLexicalId()).toLowerCase());
                    synset.addLemma(lemma);
                }
            }
        }

        BufferedReader senseReader = getReader("index.sense");
        for (String line = null; (line = senseReader.readLine()) != null; ) {
            String[] tokens = line.split("\\s");
            String[] lemmaAndLexSense = tokens[0].split("%");
            String[] lexSense = lemmaAndLexSense[1].split(":");
            int posIndex = Integer.parseInt(lexSense[0]) - 1;
            Map<Integer, Synset> offsetToSynsetMap = posOffsetToSynsetMap.get(
                    posIndex);

            int offset = Integer.parseInt(tokens[1]);
            Synset synset = offsetToSynsetMap.get(offset); 
            synset.addSenseKey(tokens[0]);
        }
    }

    /**
     * Returns a {@link BufferedReader} for the requested {@code filename}.
     * {@code dictPath} is used as the base path for all files.  If {@code
     * readFromJar} is true, the file will be read from the jar running the
     * current code.
     */
    private BufferedReader getReader(String filename) throws IOException {
        filename = dictPath + "/" + filename;
        return getReaderFromFullFilename(filename);
    }

    /**
     * Returns a {@link BufferedReader} for the exact file name requested.
     */
    private BufferedReader getReaderFromFullFilename(String filename)
            throws IOException {
        Reader reader;
        if (readFromJar)
            reader = new InputStreamReader(
                    StreamUtil.fromJar(this.getClass(), filename));
        else
            reader = new FileReader(filename);
        return new BufferedReader(reader);
    }

    /**
     * A morphological iterator over possible word form variants.  Given a base
     * lemma, such as "geese" it will return the base form "goose", which may or
     * may not be in Word Net.  If the given lemma has a morphological
     * exception, such as "geese", the exceptional base, e.g., "goose", form is
     * returned first.  Afterwords, a fixed set of rules are applied to the base
     * form in order.  A variant is returned for each rule that applies to the
     * base form.  This {@link Iterator} ends when no more rules are applicable.
     */
    private class FormIterator implements Iterator<String> {
        private String form;
        private String suffix;
        private String[][] posReplacements;
        private int replacementIndex;
        private String exception;
        private String next;

        /**
         * Creates a new {@link FormIterator} that will create morhpological
         * variants of {@code form} by using the part of speech specific
         * replacements available in {@code posReplacements}.  If {@code
         * exception} is non {@code null} it will be returned as the first
         * variant.  If {@code suffix} is non empty, it will be appended to each
         * variant returned. 
         */
        public FormIterator(String form,
                            String suffix,
                            String[][] posReplacements,
                            String exception) {
            this.form = form.replaceAll("\\s+", "_");
            this.suffix = suffix;
            this.posReplacements = posReplacements;
            this.exception = exception;

            replacementIndex = 0;
            next = (exception == null) ? advance(form) : exception;
        }

        /**
         * Returns the next morphological variant of the original {@code form}.
         * If there are no possible replacements remaining, this returns {@code
         * null}.
         */
        private String advance(String form) {
            if (replacementIndex >= posReplacements.length)
                return null;
            for (; replacementIndex < posReplacements.length;
                    replacementIndex++) {
                String[] replacement = posReplacements[replacementIndex];
                if (form.endsWith(replacement[0])) {
                    String base = form.substring(
                            0, form.length()-replacement[0].length());
                    replacementIndex++;
                    return base + replacement[1];
                }
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return next != null;
        }

        /**
         * Returns the next morphological form of the original term.  The
         * returned form is computed by finding the next possible replacement
         * that matches the original term and changing the suffix.
         */
        public String next() {
            String curr = next;
            next = advance(form);
            return curr + suffix;
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            throw new UnsupportedOperationException(
                    "Cannot remove replacements.");
        }
    }
}
