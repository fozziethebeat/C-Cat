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

import gov.llnl.ontology.util.StreamUtil;
import gov.llnl.ontology.util.StringPair;

import com.google.common.collect.Lists;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

import edu.stanford.nlp.process.DocumentPreprocessor;

import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;

import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.SimpleDependencyRelation;
import edu.ucla.sspace.dependency.SimpleDependencyTreeNode;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.StringReader;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * A {@link Parser} wrapper around the Stanford Parser. 
 *
 * @author Keith Stevens
 */
public class StanfordParser implements Parser {

    /**
     * The default location of the stanford parser information.
     */
    public static final String PARSER_MODEL =
        "models/stanford/englishPCFG.ser.gz";

    /**
     * The {@link LexicalizedParser} responsible for parsing sentences.
     */
    private final LexicalizedParser parser;

    /**
     * A utility class used to extract dependency parse trees from PCFG parse
     * trees.
     */
    private final GrammaticalStructureFactory gsf;

    /**
     * Creates a new {@link StanfordParser} using the default model location.
     */
    public StanfordParser() {
        this(PARSER_MODEL, true);
    }

    /**
     * Creates a new {@link StanfordParser} using the provided model location.
     * If {@code loadFromJar} is {@code true}, then the path is assumed to refer
     * to a file within the currently running jar.  This {@link Parser} can
     * readily by used within a map reduce job by setting {@code loadFromJar} to
     * true and including the parser model within the map reduce jar.
     */
    public StanfordParser(String parserModel, boolean loadFromJar) {
        LexicalizedParser p = null;
        GrammaticalStructureFactory g = null;
        try {
            InputStream inStream = (loadFromJar)
                ? StreamUtil.fromJar(this.getClass(), parserModel)
                : new FileInputStream(parserModel);
            p = new LexicalizedParser(new ObjectInputStream(
                        StreamUtil.gzipInputStream(inStream)));

            g = p.getOp().tlpParams.treebankLanguagePack().grammaticalStructureFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        parser = p;
        gsf = g;
    }

    /**
     * {@inheritDoc}
     */
    public DependencyTreeNode[] parseText(String header, String document) {
        DocumentPreprocessor processor = new DocumentPreprocessor(
                                new StringReader(document));
        for (List<HasWord> sentence : processor)
            parseTokens(header, sentence);
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public DependencyTreeNode[] parseText(String header, String[] tokens) {
        List<HasWord> sentence = Lists.newArrayList();
        for (String token : tokens)
            sentence.add(new Word(token));
        return parseTokens(header, sentence).toArray(new DependencyTreeNode[0]);
    }

    /**
     * {@inheritDoc}
     */
    public DependencyTreeNode[] parseText(String header, 
                                          StringPair[] sentence) {
        List<HasWord> tokens = Lists.newArrayList();
        for (StringPair word : sentence)
            if (word.x != null && word.y != null)
                tokens.add(new TaggedWord(word.x, word.y));
        return parseTokens(header, tokens).toArray(new DependencyTreeNode[0]);
    }

    private List<SimpleDependencyTreeNode> parseTokens(String header, 
                                                       List<HasWord> sentence) {
        List<SimpleDependencyTreeNode> nodes = Lists.newArrayList();

        // Parse the sentence.  If the sentence has no tokens or the
        // parser fails, simply return an empty string.
        if (sentence.size() == 0 || 
            sentence.size() > 100 ||
            !parser.parse(sentence))
            return nodes;

        // Get the parse tree and tagged words for the sentence.
        Tree tree = parser.getBestParse();
        List<TaggedWord> taggedSent = tree.taggedYield();

        // Convert the tree to a collection of dependency links.
        GrammaticalStructure gs = gsf.newGrammaticalStructure(tree);
        Collection<TypedDependency> dep = gs.typedDependencies();

        List<Link> links = Lists.newArrayList();

        for (TypedDependency dependency : dep) {
            int nodeIndex = dependency.dep().index();
            int parentIndex = dependency.gov().index();
            String relation = dependency.reln().toString();
            String token = taggedSent.get(nodeIndex-1).word();
            String pos = taggedSent.get(nodeIndex-1).tag();

            nodes.add(new SimpleDependencyTreeNode(token, pos, nodeIndex));
            links.add(new Link(nodeIndex, relation, parentIndex));
        }

        Link.addLinksToTree(nodes, links);
        return nodes;
    }
}
