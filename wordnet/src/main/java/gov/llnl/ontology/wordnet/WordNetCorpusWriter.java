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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.io.BufferedOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class WordNetCorpusWriter implements OntologyWriter {

    private static String UTF8 = "utf-8";

    /**
     * The file extensions for each of the data and index files in the WordNet
     * dictionary.
     */
    public static final String[] FILE_EXTENSIONS =
            {"noun", "verb", "adj", "adv", ""};

    /**
     * Saves the Wordnet lexical mappings to the standard wordnet data format.
     * New index and data files will be created in the path specified by {@code
     * outputDir}.  Only index and data files will be created, since details 
     * such as morphological exceptions, verb frames, and anything else are
     * left unchanged.  The resulting data and index files will be in no
     * particular order and have no license information.  Users should not
     * modify these files after they have been created as that will more than
     * likely corrupt offset values which are used by other WordNet interfaces.
     */
    public void saveOntology(OntologyReader reader, String outputDir) {
        int numPartsOfSpeech = WordNetCorpusReader.POS_TAGS.length;
        int[] requiredSize = new int[numPartsOfSpeech];
        int[] numNodes = new int[numPartsOfSpeech];
        int[] numRelations = new int[numPartsOfSpeech];

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

        Map<Synset, Integer> synsetSizes = Maps.newHashMap();

        // Compute the number of nodes, total number of relations stemming from
        // each node, and the total synset size for each part of speech,
        for (Synset synset : reader.allSynsets()) {
            PartsOfSpeech tag = synset.getPartOfSpeech();
            int pos = tag.ordinal();

            numNodes[pos]++;
            numRelations[pos] += synset.getNumRelations();

            int relationSize = 0;
            for (String relationType : synset.getKnownRelationTypes()) {
                try {
                    int typeSize = 
                        relationType.toString().getBytes(UTF8).length + 1;
                    int numRelationsPerType = 
                        synset.getRelations(relationType).size();
                    relationSize += numRelationsPerType * typeSize;
                    relationSize += numRelationsPerType * sizePerRelation;
                } catch (IOException ioe) {
                    throw new IOError(ioe);
                }
            }

            // Count up the size required for storing the lemmas.
            // Always skip the first lemma because it lacks any valuable
            // information and won't be printed.
            int lemmaSize = 0;
            Iterator<Lemma> lemmaIter = synset.getLemmas().iterator();
            lemmaIter.next();
            while (lemmaIter.hasNext()) {
                Lemma lemma = lemmaIter.next();

                try {
                    // Get the number of bytes need to store this lemma in
                    // utf-8 format.
                    lemmaSize += lemma.getLemmaName().getBytes(UTF8).length;
                } catch (IOException ioe) {
                    throw new IOError(ioe);
                }

                // Include 2 spaces and a single byte integer.
                lemmaSize += 3;
            }

            // Get the number of bytes needed to store the complete
            // gloss.
            int glossSize = 0;
            try {
                glossSize = synset.getGloss().getBytes(UTF8).length;
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }

            // Compute any part of speech specific information.
            int extraSize = 0;
            if (tag == PartsOfSpeech.VERB) {
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

        // Figure out the number of bytes needed to represent the data for each
        // part of speech.  We will use the maximal number of bytes to represent
        // every offset.
        int[] offsetSize = new int[numPartsOfSpeech];
        int finalOffsetSize = 0;
        for (int pos = 0; pos < numPartsOfSpeech; ++pos) {
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
        int[] dataFileSize = new int[numPartsOfSpeech];
        Set<Synset> writtenSynsets = Sets.newHashSet();

        // Add output streams for each part of speech.
        List<BufferedOutputStream> dataOutputStreams = Lists.newArrayList();
        List<BufferedOutputStream> indexOutputStreams = Lists.newArrayList();
        List<PrintWriter> senseMappingWriters = Lists.newArrayList();
        String FORMAT = "%s/%s.%s";
        for (int pos = 0; pos < numPartsOfSpeech; ++pos) {
            if (WordNetCorpusReader.FILE_EXTENSIONS[pos].length() != 0) {
                String ext = FILE_EXTENSIONS[pos];
                try {
                    dataOutputStreams.add(new BufferedOutputStream(
                                new FileOutputStream(String.format(FORMAT,
                                        outputDir, "data", ext))));
                    indexOutputStreams.add(new BufferedOutputStream(
                                new FileOutputStream(String.format(FORMAT,
                                        outputDir, "index", ext))));
                    senseMappingWriters.add(new PrintWriter(
                                String.format(FORMAT, 
                                    outputDir, "senseMap", ext)));
                } catch (IOException ioe) {
                    throw new IOError(ioe);
                }
            } else {
                dataOutputStreams.add(null);
                indexOutputStreams.add(null);
                senseMappingWriters.add(null);
            }
        }
 
        // Create an array that will store the sense keys lines.  The sense key
        // lines will be written to "index.sense" in alphabetical order after
        // all sense keys have been generated.
        List<String> senseKeys = Lists.newArrayList();

        SynsetWriter synsetWriter = new WordNetSynsetWriter(finalOffsetSize);

        // Iterate through each synset and write all of it's lexical
        // information.
        for (Synset synset : reader.allSynsets()) {
            // Ignore any synsets that have already been written.  This can
            // happen when the synset is linked directly to one that is
            // traversed earlier.
            if (writtenSynsets.contains(synset))
                continue;

            // Initialize a queue that will hold all synsets we find
            // starting from the current synset.
            Queue<Synset> toWriteSynsets = Lists.newLinkedList();

            int pos = synset.getPartOfSpeech().ordinal();

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

            // Traverse each synset that is reachable from the given synset.
            while (!toWriteSynsets.isEmpty()) {
                Synset s = toWriteSynsets.remove();

                // Remap the adjective sat part of speech to the adjective data.
                PartsOfSpeech p = s.getPartOfSpeech();
                if (p == PartsOfSpeech.ADJECTIVE_SAT)
                    p = PartsOfSpeech.ADJECTIVE;
                int synsetPos = p.ordinal();

                // Add the related synsets to the synsets we want to write.  We
                // have to assign offsets for each of the related synsets.
                for (Relation r : Relation.values()) {
                    for (Synset relatedSynset : s.getRelations(r)) {
                        // Skip any synsets that have already been processed.
                        if (writtenSynsets.contains(relatedSynset))
                            continue;

                        Synset related = (Synset) relatedSynset;

                        // Remap the adjective sat part of speech to the
                        // adjective data.
                        PartsOfSpeech rp = related.getPartOfSpeech();
                        if (rp == PartsOfSpeech.ADJECTIVE_SAT)
                            rp = PartsOfSpeech.ADJECTIVE;
                        int relatedPos = rp.ordinal();

                        // Setup the offset size and offset value, and then
                        // compute the number of bytes needed to write this
                        // synset.
                        related.setId(dataFileSize[relatedPos]);
                        dataFileSize[relatedPos] += 
                            synsetSizes.get(related) +
                            (related.getNumRelations() + 1) *
                            finalOffsetSize;

                        // Add the synset to the set of written synsets and
                        // queue it up for writing.
                        toWriteSynsets.offer(related);
                        writtenSynsets.add(related);
                    }
                }

                try {
                    // Actually write the synset to the correct data file.
                    String t = synsetWriter.serializeSynset(s);
                    byte[] bytes = t.getBytes(UTF8);
                    dataOutputStreams.get(synsetPos).write(
                            bytes, 0, bytes.length);
                } catch (IOException ioe) {
                    throw new IOError(ioe);
                }

                // Store the sense key line for this synset.
                senseKeys.addAll(synsetWriter.serializeSynsetKeys(s));
            }
        }

        // Write out the sorted sense key values to index.sense.
        Collections.sort(senseKeys);
        try {
            PrintWriter writer = new PrintWriter(outputDir + "/index.sense");
            for (String senseLine : senseKeys)
                writer.println(senseLine);
            writer.close();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }

        // Next, write out the index files.  This should be relatively easy
        // since it's just a mapping from each lemma to it's possible synsets.
        String offsetFormat = "%0" + finalOffsetSize + "d ";
        for (String lemma : reader.wordnetTerms()) {
            for (PartsOfSpeech tag : PartsOfSpeech.values()) {
                Synset[] synsets = reader.getSynsets(lemma, tag, false);

                // Ignore parts of speech that have no synsets for this lemma.
                if (synsets.length == 0)
                    continue;

                int pos = tag.ordinal();
                StringBuilder sb = new StringBuilder();

                // Write out the lemma, pos tag, and number of senses.
                sb.append(lemma).append(" ");
                sb.append(tag.toString()).append(" ");
                sb.append(synsets.length).append(" ");

                // Write out the number of pointers that all senses have and
                // then each of the pointers.
                Set<String> pointers = Sets.newHashSet();

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
                if (pos == PartsOfSpeech.ADJECTIVE_SAT.ordinal())
                    pos = PartsOfSpeech.ADJECTIVE.ordinal();

                try {
                    byte[] bytes = sb.toString().getBytes(UTF8);
                    indexOutputStreams.get(pos).write(bytes, 0, bytes.length);
                } catch (IOException ioe) {
                    throw new IOError(ioe);
                }
            }
        }

        // Close the index and data output streams.
        for (int pos = 0; pos < numPartsOfSpeech; ++pos) {
            if (indexOutputStreams.get(pos) == null)
                continue;
            try {
                indexOutputStreams.get(pos).close();
                dataOutputStreams.get(pos).close();
                senseMappingWriters.get(pos).close();
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
        }
    }

    /*
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
    */

}
