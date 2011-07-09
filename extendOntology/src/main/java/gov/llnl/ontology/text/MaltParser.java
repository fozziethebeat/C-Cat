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

package gov.llnl.ontology.text;

import gov.llnl.ontology.util.StringPair;

import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

import org.maltparser.MaltParserService;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.symbol.SymbolTable;

import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.DependencyNode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOError;
import java.io.IOException;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.List;


/**
 * A {@link Parser} wrapper around the Malt Parser.
 *
 * @author Keith Stevens
 */
public class MaltParser implements Parser {

    /**
     * The default location of the stanford part of speech tager model .
     */
    private static final String TAGGER_MODEL = 
        "data/left3words-distsim-wsj-0-18.tagger";
    
    /**
     * The default location of the malt parser model.
     */
    private static final String PARSER_MODEL = "engmalt.linear";

    /**
     * The {@link MaxentTagger} used to part of speech tag each sentence.
     */
    private final MaxentTagger tagger;

    /**
     * The {@link MaltParserService} used to parse sentences.
     */
    private final MaltParserService parser;

    /**
     * Creates a new {@link MaltParser} using the default model paths.
     */
    public MaltParser() {
        this(TAGGER_MODEL, PARSER_MODEL);
    }

    /**
     * Creates a new {@link MaltParser} using the provided model paths.    Note
     * that this {@link Parser} cannot be readily used within a map reduce job.
     */
    public MaltParser(String posTaggerPath, String maltParserModelPath) {
        try {
            tagger = new MaxentTagger(posTaggerPath);
            parser = new MaltParserService();
            parser.initializeParserModel
                ("-c " + maltParserModelPath + " -m parse");
        } catch (MaltChainedException ioe) {
            throw new RuntimeException(ioe);
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String parseText(String header, String document) {
        StringBuilder builder = new StringBuilder();

        List<ArrayList<HasWord>> sentences =
                new ArrayList<ArrayList<HasWord>>();

        DocumentPreprocessor processor = new DocumentPreprocessor(
                new StringReader(document));
        int totalWords = 0;
        for (List<HasWord> sentence : processor) {
            if (sentence.size() == 0 || sentence.size() > 100)
                continue;
            totalWords += sentence.size();
            ArrayList<HasWord> sent = new ArrayList<HasWord>();
            for (HasWord word : sentence)
                sent.add(word);
            sentences.add(sent);
        }

        if (totalWords == 0)
           return "";

        List<ArrayList<TaggedWord>> taggedSentence = tagger.process(sentences);

        String[] tokens = new String[totalWords];
        int i = 0;
        for (List<TaggedWord> sentence : taggedSentence) {
            for (TaggedWord word : sentence)
                tokens[i] = buildLine(i++, word.word(), word.tag());
        }

        return parseTokens(tokens, header);
    }

    /**
     * {@inheritDoc}
     */
    public String parseText(String header, StringPair[] sentence) {
        String[] tokens = new String[sentence.length];
        int i = 0;
        for (StringPair word : sentence)
            tokens[i] = buildLine(i++, word.x, word.y);

        return parseTokens(tokens, header);
    }

    private String parseTokens(String[] tokens, String header) {
        StringBuilder builder = new StringBuilder();

        // Parse the sentence and write the graph to the string builder.
        try {
            DependencyStructure graph = parser.parse(tokens);

            if (header != null && !header.equals(""))
                builder.append(header).append("\n");

            for (int i = 1; i <= graph.getHighestDependencyNodeIndex(); i++) {
                DependencyNode node = graph.getDependencyNode(i);
                if (node != null) {
                    for (SymbolTable table : node.getLabelTypes())
                        builder.append(node.getLabelSymbol(table)).append("\t");

                    if (node.hasHead()) {
                        Edge e = node.getHeadEdge();
                        builder.append(e.getSource().getIndex()).append("\t");
                        if (e.isLabeled()) {
                            for (SymbolTable table : e.getLabelTypes())
                                builder.append(e.getLabelSymbol(table)).append("\t");
                        } else {
                            for (SymbolTable table : graph.getDefaultRootEdgeLabels().keySet()) 
                                builder.append(graph.getDefaultRootEdgeLabelSymbol(table)).append("\t");
                        }
                    }
                    builder.append('\n');
                }
            }
        } catch (MaltChainedException ioe) {
            throw new RuntimeException(ioe);
        } catch (NullPointerException npe) {
            return "";
        }

        return builder.toString();
    }

    private static String buildLine(int lineNum, String word, String tag) {
        return String.format("%d\t%s\t_\t%s\t%s\t_\t_\t_", 
                             lineNum, word, tag, tag);
    }

    public static void main(String[] args) throws IOException {
        Parser parser = new MaltParser();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                    System.in));
        System.out.println(parser.parseText(in.readLine(), "" ));
    }
}
