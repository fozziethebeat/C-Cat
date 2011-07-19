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
import gov.llnl.ontology.wordnet.Synset.Relation;

import edu.ucla.sspace.util.CombinedIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.Reader;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;


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
    private static final Log LOG =
        LogFactory.getLog(WordNetCorpusReader.class);

    /**
     * The set of part of speech tags.
     */
    public static final String[] POS_TAGS = {"n", "v", "a", "r", "s"};

    /**
     * A simple mapping from part of speech characters their respective {@link
     * ParstOfSpeech} enumerations.
     */
    public static final Map<String, PartsOfSpeech> POS_MAP =
        new HashMap<String, PartsOfSpeech>();

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
     * The number of bytes needed to represent an offset pointer in the WordNet
     * dictionary files.  This is only used when serializing wordnet.
     */
    private int finalOffsetSize;

    /**
     * {@inheritDoc}
     */
    public Iterator<String> morphy(String form) {
        List<Iterator<String>> formIters = new ArrayList<Iterator<String>>();
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
     * Saves the WordNet dictionary to the appropriate index and data files.
     * New index and data files will be created in the path specified by {@code
     * newDictPath}.  Only index and data files will be created, since details 
     * such as morphological exceptions, verb frames, and any thing else are
     * left unchanged.  The resulting data and index files will be in no
     * particular order and have no license information.  Users should not
     * modify these files after they have been created as that will more than
     * likely corrupt offset values which are used by other WordNet interfaces.
     */
    public void saveWordNet(String newDictPath) throws IOException {
        int[] requiredSize = new int[POS_TAGS.length];
        int[] numNodes = new int[POS_TAGS.length];
        int[] numRelations = new int[POS_TAGS.length];

        // Precompute the known size of each synset that does not involve the
        // lemmas, relations, or gloss.
        int sizePerSynset = 0;
        // Space after offset.
        sizePerSynset += 1;
        // Lex file name + space.
        sizePerSynset += 3;
        // POS tag + space.
        sizePerSynset += 2;
        // Number of lemmas + space.
        sizePerSynset += 3;
        // Number of relations + space.
        sizePerSynset += 3;
        // Pipe separator for gloss + space.
        sizePerSynset += 2;
        // Newline.
        sizePerSynset += 1;

        // Precompute the size required for each relation that does not include
        // the offset size.
        int sizePerRelation = 0;
        // Add a byte for a space.
        sizePerRelation += 1;
        // Add a byte for the relation type and a space.
        //sizePerRelation += 2;
        // Add a byte for the part of speech tag and a space.
        sizePerRelation += 2;
        // Add 4 bytes for the lemma index and 1 for a space.
        sizePerRelation += 5;

        // Precompute the size required for each verb frame.
        int sizePerVerbFrame = 0;
        //Add a byte for the + and for a space.
        sizePerVerbFrame += 2;
        // Add two bytes for the frame id and one for a space.
        sizePerVerbFrame += 3;
        // Add two bytes for the lemma id and one for a space.
        sizePerVerbFrame += 3;

        Map<Synset, Integer> synsetSizes = new HashMap<Synset, Integer>();

        // Compute the number of nodes, total number of relations stemming from
        // each node, and the total synset size for each part of speech,
        for (Map.Entry<String, Synset[][]> entry : 
                lemmaPosOffsetMap.entrySet()) {
            Synset[][] lemmaSynsets = entry.getValue();
            for (int pos = 0; pos < POS_TAGS.length; ++pos) {
                for (Synset synset : lemmaSynsets[pos]) {
                    numNodes[pos]++;
                    numRelations[pos] += synset.getNumRelations();

                    int relationSize = 0;
                    for (String relationType : synset.getKnownRelationTypes()) {
                        int typeSize = 
                            relationType.toString().getBytes(UTF8).length + 1;
                        int numRelationsPerType = 
                            synset.getRelations(relationType).size();
                        relationSize += numRelationsPerType * typeSize;
                        relationSize += numRelationsPerType * sizePerRelation;
                    }

                    // Count up the size required for storing the lemmas.
                    int lemmaSize = 0;
                    for (Lemma lemma : synset.getLemmas()) {
                        // Get the number of bytes need to store this lemma in
                        // utf-8 format.
                        lemmaSize += lemma.getLemmaName().getBytes(UTF8).length;

                        // Include 2 spaces and a single byte integer.
                        lemmaSize += 3;
                    }

                    // Get the number of bytes needed to store the complete
                    // gloss.
                    int glossSize = synset.getGloss().getBytes(UTF8).length;

                    // Compute any part of speech specific information.
                    int extraSize = 0;
                    if (POS_TAGS[pos].equals("v")) {
                        // Add 3 bytes, one for a space and two for a two byte
                        // integer.
                        extraSize += 3;

                        // Get the frame information.  For each frame pointer,
                        // add in the bytes needed to represent the frame
                        // assignment.
                        int[] frameIds = synset.getFrameIds();
                        int[] lemmaIds = synset.getLemmaIds();
                        if (frameIds != null && lemmaIds != null)
                            extraSize += lemmaIds.length * sizePerVerbFrame;
                    }

                    // Compute the number of bytes needed to represent this
                    // synset, minus any offset information.
                    int synsetSize = sizePerSynset + relationSize +
                                     lemmaSize + glossSize + extraSize + 1;
                    synsetSizes.put(synset, synsetSize);
                    requiredSize[pos] += synsetSize;
                }
            }
        }
        
        // Figure out the number of bytes needed to represent the data for each
        // part of speech.  We will use the maximal number of bytes to represent
        // every offset.
        int[] offsetSize = new int[POS_TAGS.length];
        finalOffsetSize = 0;
        for (int pos = 0; pos < POS_TAGS.length; ++pos) {
            // Compute the minimum number of bytes to represent the data
            // information that does not include the offsets.
            int bytesPerOffset = Integer.toString(requiredSize[pos]).length();
            boolean converged = false;

            while (!converged) {
                // Compute the number of bytes we would use if we include the
                // offset values.
                int totalBytes = requiredSize[pos] +
                                 bytesPerOffset * numNodes[pos] +
                                 bytesPerOffset * numRelations[pos];
                // Compute the number of bytes needed to represent the entire
                // data file.
                int newBytesPerOffset = Integer.toString(totalBytes).length();

                // Determine if the bytes needed to represent the full data set
                // matches the expected number of bytes needed.  If so, we are
                // done.
                if (newBytesPerOffset == bytesPerOffset)
                    converged = true;
                else
                    // If we are not done, try incrementing the number of bytes
                    // per offset by 1.
                    bytesPerOffset++;
            }

            // Take the maximal number of bytes used to represent each offset.
            offsetSize[pos] = bytesPerOffset;
            if (offsetSize[pos] > finalOffsetSize)
                finalOffsetSize = offsetSize[pos];
        }

        // Write out the data files for each part of speech.  This is done by
        // first iterating through each synset for a given part of speech,
        // giving it an offset, and then giving an offset to each of it's
        // related synsets.  Once all of it's related synsetse has an offset,
        // they are written to their respective part of speech files.
        int[] dataFileSize = new int[POS_TAGS.length];
        Set<Synset> writtenSynsets = new HashSet<Synset>();

        // Add output streams for each part of speech.
        List<BufferedOutputStream> dataOutputStreams =
            new ArrayList<BufferedOutputStream>(POS_TAGS.length);
        List<BufferedOutputStream> indexOutputStreams =
            new ArrayList<BufferedOutputStream>(POS_TAGS.length);
        List<PrintWriter> senseMappingWriters =
            new ArrayList<PrintWriter>(POS_TAGS.length);
        for (int pos = 0; pos < POS_TAGS.length; ++pos) {
            if (FILE_EXTENSIONS[pos].length() != 0) {
                dataOutputStreams.add(new BufferedOutputStream(
                            new FileOutputStream(
                                newDictPath + "/data." + FILE_EXTENSIONS[pos])));
                indexOutputStreams.add(new BufferedOutputStream(
                            new FileOutputStream(
                                newDictPath + "/index." + FILE_EXTENSIONS[pos])));
                senseMappingWriters.add(new PrintWriter(
                            newDictPath + "/senseMap." + FILE_EXTENSIONS[pos]));
            }
            else    {
                dataOutputStreams.add(null);
                indexOutputStreams.add(null);
                senseMappingWriters.add(null);
            }
        }

 
        // Create an array that will store the sense keys lines.  The sense key
        // lines will be written to "index.sense" in alphabetical order after
        // all sense keys have been generated.
        List<String> senseKeys = new ArrayList<String>();

        Set<String> lemmaKeys = new TreeSet<String>(lemmaPosOffsetMap.keySet());

        SynsetWriter synsetWriter = new WordNetSynsetWriter(finalOffsetSize);

        // Iterate through each synset for a given part of speech.
        for (String lemmaKey : lemmaKeys) {
            Synset[][] lemmaSynsets = lemmaPosOffsetMap.get(lemmaKey);
            for (int posIndex = 0; posIndex < POS_TAGS.length; ++posIndex) {
                for (Synset synset : lemmaSynsets[posIndex]) {

                    // Ignore synsets that have already been written to disk.
                    if (writtenSynsets.contains(synset))
                        continue;

                    // Initialize a queue that will hold all synsets we find
                    // starting from the current synset.
                    Queue<Synset> toWriteSynsets = new LinkedList<Synset>();

                    int pos = posIndex;
                    // Remap all sat adjectives to use the adjective data.
                    if (pos == PartsOfSpeech.ADJECTIVE_SAT.ordinal())
                        pos = PartsOfSpeech.ADJECTIVE.ordinal();

                    if (synset.getId() != 0)
                        senseMappingWriters.get(pos).printf(
                                "%d %d\n", synset.getId(), dataFileSize[pos]);

                    // Setup the offset size for this synset, give it an offset,
                    // and compute the number of bytes needed to represent the
                    // synset.
                    synset.setId(dataFileSize[pos]);
                    dataFileSize[pos] += synsetSizes.get(synset) + 
                        (synset.getNumRelations() + 1) * finalOffsetSize;

                    // Mark this synset as the first to be written.
                    writtenSynsets.add(synset);
                    toWriteSynsets.offer(synset);

                    // Traverse each synset that is reachable from the given
                    // synset.
                    while (!toWriteSynsets.isEmpty()) {
                        Synset s = toWriteSynsets.remove();

                        // Remap the adjective sat part of speech to the
                        // adjective data.
                        PartsOfSpeech p = s.getPartOfSpeech();
                        if (p == PartsOfSpeech.ADJECTIVE_SAT)
                            p = PartsOfSpeech.ADJECTIVE;
                        int synsetPos = p.ordinal();

                        // Add the related synsets to the synsets we want to
                        // write.    We have to assign offsets for each of the
                        // related synsets.
                        for (Relation r : Relation.values()) {
                            for (Synset relatedSynset : s.getRelations(r)) {
                                // Skip any synsets that have already been
                                // processed.
                                if (writtenSynsets.contains(relatedSynset))
                                    continue;
                                Synset related = (Synset) relatedSynset;

                                // Remap the adjective sat part of speech to the
                                // adjective data.
                                PartsOfSpeech rp = related.getPartOfSpeech();
                                if (rp == PartsOfSpeech.ADJECTIVE_SAT)
                                    rp = PartsOfSpeech.ADJECTIVE;
                                int relatedPos = rp.ordinal();

                                // Setup the offset size and offset value, and
                                // then compute the number of bytes needed to
                                // write this synset.
                                related.setId(dataFileSize[relatedPos]);
                                dataFileSize[relatedPos] += 
                                    synsetSizes.get(related) +
                                    (related.getNumRelations() + 1) *
                                    finalOffsetSize;

                                // Add the synset to the set of written synsets
                                // and queue it up for writing.
                                toWriteSynsets.offer(related);
                                writtenSynsets.add(related);
                            }
                        }

                        // Actually write the synset to the correct data file.
                        String t = synsetWriter.serializeSynset(s);
                        byte[] bytes = t.getBytes(UTF8);
                        dataOutputStreams.get(
                                synsetPos).write(bytes, 0, bytes.length);

                        // Store the sense key line for this synset.
                        senseKeys.add(synsetWriter.serializeSynsetKey(s));
                    }
                }
            }
        }

        // Write out the sorted sense key values to index.sense.
        Collections.sort(senseKeys);
        PrintWriter writer = new PrintWriter(newDictPath + "/index.sense");
        for (String senseLine : senseKeys)
            writer.println(senseLine);
        writer.close();

        // Next, write out the index files.  This should be relatively easy
        // since it's just a mapping from each lemma to it's possible synsets.
        // All of this is in the lemmaPosOffsetMap.
        String offsetFormat = "%0" + finalOffsetSize + "d ";
        for (Map.Entry<String, Synset[][]> entry :
                lemmaPosOffsetMap.entrySet()) {
            String lemma = entry.getKey();
            Synset[][] posSynsets = entry.getValue();
            for (int posIndex = 0; posIndex < posSynsets.length; ++posIndex) {
                Synset[] synsets = posSynsets[posIndex];
                // Ignore parts of speech that have no synsets for this lemma.
                if (synsets.length == 0)
                    continue;

                int pos = posIndex;
                StringBuilder sb = new StringBuilder();

                // Write out the lemma, pos tag, and number of senses.
                sb.append(lemma).append(" ");
                sb.append(POS_TAGS[pos]).append(" ");
                sb.append(synsets.length).append(" ");

                // Write out the number of pointers that all senses have and
                // then each of the pointers.
                Set<String> pointers = new HashSet<String>();
                // Compute the set of pointers for all synsets.
                for (Synset synset : synsets)
                    pointers.addAll(synset.getKnownRelationTypes());

                // Write the set of pointers.
                sb.append(pointers.size()).append(" ");
                for (String pointerType : pointers)
                    sb.append(pointerType).append(" ");

                // Rewrite the number of senses.
                sb.append(synsets.length).append(" ");
                // Write a 0 for the number of ordered senses.
                sb.append("0 ");

                // Write out the offset for each synset matching this lemma and
                // part of speech.
                for (Synset synset : synsets)
                    sb.append(String.format(offsetFormat, synset.getId()));
                sb.append("\n");

                // Write out the line to the appropriate file in utf-8 format.
                byte[] bytes = sb.toString().getBytes(UTF8);
                if (pos == PartsOfSpeech.ADJECTIVE_SAT.ordinal())
                    pos = PartsOfSpeech.ADJECTIVE.ordinal();
                indexOutputStreams.get(pos).write(bytes, 0, bytes.length);
            }
        }

        // Close the index and data output streams.
        for (int pos = 0; pos < POS_TAGS.length; ++pos) {
            if (indexOutputStreams.get(pos) == null)
                continue;
            indexOutputStreams.get(pos).close();
            dataOutputStreams.get(pos).close();
            senseMappingWriters.get(pos).close();
        }
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

        Set<String> seenLemmas = new HashSet<String>();

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

        Set<String> seenLemmas = new HashSet<String>();

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
        Set<String> replacementLemmas = new HashSet<String>();
        for (Lemma lemma : replacement.getLemmas())
            replacementLemmas.add(lemma.getLemmaName().toLowerCase());

        // Iterate through all of the lemmas that could refer to this synset.
        Set<String> seenLemmas = new HashSet<String>();
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
        Set<String> posLemmas = new HashSet<String>();
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
        List<Synset> allSynsets = new ArrayList<Synset>();
        for (PartsOfSpeech pos : PartsOfSpeech.values())
            allSynsets.addAll(Arrays.asList(getSynsets(lemma, pos)));
        return allSynsets.toArray(new Synset[allSynsets.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public Synset[] getSynsets(String lemma, PartsOfSpeech pos) {
        // Get the synsets for the original form.
        Synset[][] lemmaSynsets = lemmaPosOffsetMap.get(lemma);
        if (lemmaSynsets != null && lemmaSynsets[pos.ordinal()].length > 0)
            return lemmaSynsets[pos.ordinal()];

        // Find the Synsets for each morphological variation.
        List<Synset> allSynsets = new ArrayList<Synset>();
        Iterator<String> formIter = morphy(lemma, pos);
        while (formIter.hasNext()) {
            String alternative = formIter.next();
            lemmaSynsets = lemmaPosOffsetMap.get(alternative);
            if (lemmaSynsets != null && lemmaSynsets[pos.ordinal()].length > 0)
                allSynsets.addAll(Arrays.asList(lemmaSynsets[pos.ordinal()]));
        }
        return allSynsets.toArray(new Synset[allSynsets.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public Synset getSynset(String fullSynsetName) {
        String[] parts = fullSynsetName.split("\\.");
        String lemma = parts[0];
        PartsOfSpeech pos = POS_MAP.get(parts[1]);
        int senseNum = Integer.parseInt(parts[2]);
        return getSynset(lemma, pos, senseNum);
    }

    /**
     * {@inheritDoc}
     */
    public Synset getSynset(String lemma, PartsOfSpeech pos, int senseNum) {
        Synset[][] lemmaSynsets = lemmaPosOffsetMap.get(lemma);
        if (lemmaSynsets == null)
            return null;
        if (senseNum < 1 || senseNum > lemmaSynsets[pos.ordinal()].length)
            return null;
        return lemmaSynsets[pos.ordinal()][senseNum-1];
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

    /* package private */ Synset getSynsetFromOffset(int offset,
                                                                                                     PartsOfSpeech pos) {
        return posOffsetToSynsetMap.get(pos.ordinal()).get(offset);
    }

    public void saveInformationContent(Map<Synset, Integer> contentMap,
                                       String filename) {
        if (finalOffsetSize == 0)
            throw new IllegalArgumentException(
                    "The synset hierarchy must be saved before the " +
                    "information content can be saved");

        try {
            PrintWriter writer = new PrintWriter(filename);
            String contentFormat = "%0" + finalOffsetSize + "d%s %d\n";
            for (Map.Entry<Synset, Integer> content : contentMap.entrySet()) {
                writer.printf(contentFormat,
                                            content.getKey().getId(),
                                            content.getKey().getPartOfSpeech(),
                                            content.getValue());
            }
            writer.close();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
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
        lemmaPosOffsetMap = new HashMap<String, Synset[][]>();
        posExceptionMap = new ArrayList<Map<String, String>>();
        verbFrames = new ArrayList<String>();
        posOffsetToSynsetMap = new ArrayList<Map<Integer, Synset>>(
                POS_TAGS.length);
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
        List<String> names = new ArrayList<String>();
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
            Map<Integer, Synset> offsetToSynset =
                new HashMap<Integer, Synset>();
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
                String lemma = tokens[index++];
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
                if (posTag == PartsOfSpeech.ADJECTIVE)
                    posToOffsets[PartsOfSpeech.ADJECTIVE_SAT.ordinal()] = 
                        synsets;
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

                    // Get the lemma id for this pointed to synset.  Add a
                    // related link if the lemmaId is nonzero.
                    String lemmaId = columns[index++];
                    if (lemmaId.equals("0000"))
                        synset.addRelation(symbol, pointerSynset);
                    else {
                        // When the lemma id is non zero it is a derivationally
                        // related lemma.  Since not all lemmas have been added
                        // to their Synsets yet, just add in a link based on the
                        // Synset and the lemma index that corresponds to the
                        // related form.
                        int sourceIndex = Integer.parseInt(
                                lemmaId.substring(0, 2), 16);
                        int targetIndex = Integer.parseInt(
                                lemmaId.substring(2), 16);
                        synset.addRelation(symbol, pointerSynset);
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
            synset.setSenseKey(tokens[0]);

            synset.setSenseNumber(Integer.parseInt(tokens[2]));

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
                    WordNetCorpusReader.class.getResourceAsStream(filename));
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
            this.form = form;
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
            replacementIndex++;
            for (; replacementIndex < posReplacements.length;
                    replacementIndex++) {
                String[] replacement = posReplacements[replacementIndex-1];
                if (form.endsWith(replacement[0])) {
                    String base = form.substring(
                            0, form.length()-replacement[0].length());
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

    public static void main(String[] args) throws Exception {
        /*
        Synset[] synsets = reader.getSynsets("cat", PartsOfSpeech.NOUN);
        Set<String> writtenLinks = new HashSet<String>();
        for (Synset synset : synsets) {
            for (List<Synset> parentPath : synset.getParentPaths()) {
                Synset prev = null;
                System.out.println("parent Path");
                for (Synset parent : parentPath) {
                    if (prev != null)
                        writtenLinks.add("\"" + prev.getName() + "\" -- \"" + parent.getName()+ "\"");
                    prev = parent;
                    System.out.println(parent.getName());
                }
            }
        }

        synsets = reader.getSynsets("bacteria", PartsOfSpeech.NOUN);
        for (Synset synset : synsets) {
            for (List<Synset> parentPath : synset.getParentPaths()) {
                Synset prev = null;
                System.out.println("parent Path");
                for (Synset parent : parentPath) {
                    if (prev != null)
                        writtenLinks.add("\"" + prev.getName() + "\" -- \"" + parent.getName()+ "\"");
                    prev = parent;
                    System.out.println(parent.getName());
                }
            }
        }

        PrintWriter writer = new PrintWriter("graph.dat");
        writer.println("graph    {");
        for (String link : writtenLinks)
            writer.println(link);
        writer.println("}");
        writer.close();

        Synset[] catSynsets = reader.getSynsets("cat", PartsOfSpeech.NOUN);
        for (Synset synset : catSynsets)
            System.out.printf("%s, %s\n", synset.getName(), synset.getDefinition());

        catSynsets = reader.getSynsets("direction", PartsOfSpeech.NOUN);
        for (Synset synset : catSynsets)
            System.out.printf("%s, %s, %s\n", synset.getName(), synset.getDefinition(), synset.getId());

        List<List<Synset>> parentPaths = reader.getSynset(
                "dog", PartsOfSpeech.NOUN, 1).getParentPaths();
        for (List<Synset> parentPath : parentPaths) {
            System.out.println("PARENT PATH");
            for (Synset parent : parentPath)
                System.out.println(parent);
        }

        System.out.println(SynsetRelations.getHypernymStatus("dog", "canine").toString());
        System.out.println(SynsetRelations.getHypernymStatus("dog", "domestic_animal").toString());
        System.out.println(SynsetRelations.getHypernymStatus("dog", "entity").toString());
        System.out.println(SynsetRelations.getCousinDistance("dog", "canine", 7).toString());
        System.out.println(SynsetRelations.getCousinDistance("dog", "entity", 7).toString());
        System.out.println(SynsetRelations.getCousinDistance("dog", "wolf", 7).toString());
        System.out.println(SynsetRelations.getCousinDistance("puppy", "wolf", 7).toString());
        System.out.println(SynsetRelations.getCousinDistance("puppy", "dog", 7).toString());
        System.out.println(SynsetRelations.getCousinDistance("puppy", "DOESNOTEXIST", 7).toString());


        System.out.println(SynsetRelations.getCousinDistance("13th", "gate", 7).toString());
        System.out.println(SynsetRelations.getCousinDistance("145th", "train", 7).toString());
        System.out.println(SynsetRelations.getCousinDistance("12th", "tie", 7).toString());
        System.out.println(SynsetRelations.getCousinDistance("0", "tie", 7).toString());
        System.out.println(SynsetRelations.getCousinDistance("1960", "gates", 7).toString());
        */
    }
}
