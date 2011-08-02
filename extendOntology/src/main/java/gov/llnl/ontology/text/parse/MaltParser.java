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

package gov.llnl.ontology.text.parse;

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

import opennlp.tools.postag.POSTagger;
import opennlp.tools.tokenize.Tokenizer;

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
     * A hard limit on the number of sentences the parser can handle.  This is
     * done to try and flush the memory usage of the parser after it's processed
     * a large number of sentences.
     */
    public static final int SENTENCE_LIMIT = 30000;

    /**
     * The {@link Tokenizer} used to split each text document.
     */
    private final Tokenizer tokenizer;
    
    /**
     * The {@link POSTagger} used to part of speech tag each token.
     */
    private final POSTagger tagger;

    /**
     * The {@link MaltParserService} used to parse sentences.
     */
    private MaltParserService parser;

    private int sentenceCount;

    private String modelPath;

    /**
     * Creates a new {@link MaltParser} using the provided model paths.    Note
     * that this {@link Parser} cannot be readily used within a map reduce job.
     */
    public MaltParser(String maltParserModelPath,
                      Tokenizer tokenizer,
                      POSTagger tagger) {
        try {
            this.tagger = tagger;
            this.tokenizer = tokenizer;
            this.modelPath = maltParserModelPath;
            parser = new MaltParserService();
            parser.initializeParserModel
                ("-c " + modelPath + " -m parse");
            sentenceCount = 0;
        } catch (MaltChainedException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String parseText(String header, String document) {
        StringBuilder builder = new StringBuilder();

        String[] toks = tokenizer.tokenize(document);
        String[] pos = tagger.tag(toks);

        String[] lines = new String[toks.length];
        for (int i = 0; i < toks.length; ++i)
            lines[i] = buildLine(i, toks[i], pos[i]);

        return parseTokens(lines, header);
    }

    /**
     * {@inheritDoc}
     */
    public String parseText(String header, StringPair[] sentence) {
        String[] lines = new String[sentence.length];
        int i = 0;
        for (StringPair word : sentence)
            lines[i] = buildLine(i++, word.x, word.y);

        return parseTokens(lines, header);
    }

    private String parseTokens(String[] tokens, String header) {
        StringBuilder builder = new StringBuilder();

        // Parse the sentence and write the graph to the string builder.
        try {
            MaltParserService p = parser;
            DependencyStructure graph = p.parse(tokens);

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

        synchronized(this) {
            if (++sentenceCount >= SENTENCE_LIMIT) {
                sentenceCount = 0;
                try {
                    MaltParserService p = new MaltParserService();
                    p.initializeParserModel
                        ("-c " + modelPath + " -m parse");
                    parser = p;
                } catch (MaltChainedException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        }
        return builder.toString();
    }

    private static String buildLine(int lineNum, String word, String tag) {
        return String.format("%d\t%s\t_\t%s\t%s\t_\t_\t_", 
                             lineNum, word, tag, tag);
    }
}
