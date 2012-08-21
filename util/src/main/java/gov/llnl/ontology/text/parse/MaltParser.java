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

import com.google.common.collect.Lists;

import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.SimpleDependencyRelation;
import edu.ucla.sspace.dependency.SimpleDependencyTreeNode;

import org.maltparser.MaltParserService;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.DependencyNode;
import org.maltparser.core.syntaxgraph.Element;

import opennlp.tools.postag.POSTagger;
import opennlp.tools.tokenize.Tokenizer;

import java.util.List;
import java.util.Set;


/**
 * A {@link Parser} wrapper around the Malt Parser.
 *
 * @author Keith Stevens
 */
public class MaltParser implements Parser {

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

    /**
     * The number of sentences parsed so far.
     */
    private int sentenceCount;

    /**
     * The serialzied model path.
     */
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
    public DependencyTreeNode[] parseText(String header, 
                                          String document) {
        StringBuilder builder = new StringBuilder();

        String[] toks = tokenizer.tokenize(document);
        return parseText(header, toks);
    }

    /**
     * {@inheritDoc}
     */
    public DependencyTreeNode[] parseText(String header, 
                                          String[] tokens) {
        String[] pos = tagger.tag(tokens);

        String[] lines = new String[tokens.length];
        for (int i = 0; i < tokens.length; ++i)
            lines[i] = buildLine(i, tokens[i], pos[i]);

        return parseTokens(lines, header);
    }

    /**
     * {@inheritDoc}
     */
    public DependencyTreeNode[] parseText(String header, 
                                          StringPair[] sentence) {
        String[] lines = new String[sentence.length];
        int i = 0;
        for (StringPair word : sentence)
            lines[i] = buildLine(i++, word.x, word.y);

        return parseTokens(lines, header);
    }

    private static String[] getSymbols(Element node) {
        try {
            Set<SymbolTable> tables = node.getLabelTypes();
            String[] symbols = new String[tables.size()];
            int s = 0;
            for (SymbolTable table : node.getLabelTypes())
                symbols[s++] = node.getLabelSymbol(table);
            return symbols;
        } catch (MaltChainedException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private synchronized DependencyTreeNode[] parseTokens(String[] tokens,
                                                          String header) {
        StringBuilder builder = new StringBuilder();
        String nullLink = "null";

        List<SimpleDependencyTreeNode> tree = Lists.newArrayList();
        List<Link> links = Lists.newArrayList();

        try {
            MaltParserService p = parser;
            DependencyStructure graph = p.parse(tokens);

            for (int i = 1; i <= graph.getHighestDependencyNodeIndex(); i++) {
                DependencyNode node = graph.getDependencyNode(i);
                if (node != null) {
                    if (node.hasHead()) {
                        Edge e = node.getHeadEdge();
                        int headId = e.getSource().getIndex();
                        String relation = null;
                        if (e.isLabeled()) {
                            relation = getSymbols(e)[0];
                        } else {
                            relation = nullLink;
                            headId = 0;
                        }
                        links.add(new Link(tree.size(), relation, headId));
                    }

                    String[] labels = getSymbols(node);
                    tree.add(new SimpleDependencyTreeNode(
                                labels[1], labels[3], tree.size()));
                }
            }
        } catch (MaltChainedException ioe) {
            throw new RuntimeException(ioe);
        } catch (NullPointerException npe) {
            throw new RuntimeException(npe);
        }

        Link.addLinksToTree(tree, links);
        return tree.toArray(new DependencyTreeNode[tree.size()]);
    }

    private static String buildLine(int lineNum, String word, String tag) {
        return String.format("%d\t%s\t_\t%s\t%s\t_\t_\t_", 
                             lineNum, word, tag, tag);
    }
}
