package gov.llnl.ontology.wordnet.castanet;

import gov.llnl.ontology.text.Document;
import gov.llnl.ontology.text.CorpusReader;
import gov.llnl.ontology.text.NewLineSplitCorpusReader;
import gov.llnl.ontology.text.corpora.TasaDocumentReader;
import gov.llnl.ontology.wordnet.Attribute;
import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.Synset;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.ucla.sspace.matrix.TfIdfTransform;

import java.io.PrintWriter;
import java.util.List;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;


/**
 *
 * This class implements the Castanet Algorithim described by 
 *
 *   <li style="font-family:Garamond, Georgia, serif"> E. Stoica, M. A. Hearst, and M. Richardson. Automating
 *  creation of hierarchical faceted metadata structures. In Proc.
 *  NAACL-HLT 2007, pages 244–251, 2007.
 *
 * @author Terry Huang
 */
public class Castanet {
    
    /**
     * The logger used to record all output
     */
    private static final Logger LOGGER = 
        Logger.getLogger(Castanet.class.getName());

    /**
     * The {@link OntologyReader} used to access {@link Synset}s.
     */
    private final OntologyReader reader;

    /**
     * The {@link KeywordExtractor} used to extract keywords from each document.
     */
    private final KeywordExtractor extractor;

    public Castanet (OntologyReader reader, KeywordExtractor extractor) {
        this.reader = reader;
        this.extractor = extractor;
    }

    private Node eliminateSingleParents(Node root) {
        Set<Node> children = root.getChildren();
        Node parent = root.getParent();
        if (children.size() == 0)
            return root;
        if (children.size() == 1 && parent != null && 
            !root.getValue().attributeLabels().contains("files"))
            return eliminateSingleParents(children.iterator().next());
        else {
            List<Node> newChildren = Lists.newArrayList();
            for (Node child : children)
                newChildren.add(eliminateSingleParents(child));
            root.setChildren(newChildren);
            return root;
        }
    }

    /**
     * Merges the Synset attributes of both nodes. The receiver Node, will get
     * all the attributes the giver Node has
     */
    private Node mergeAttributes(Node receiver, Node giver) {
        Set<String> receiverLabels = receiver.getValue().attributeLabels();
        Set<String> giverLabels = giver.getValue().attributeLabels();

        for (String giverLabel : giverLabels) {
            Attribute giveAttr = giver.getValue().getAttribute(giverLabel);
            if (receiverLabels.contains(giverLabel)) {
                Attribute recAttr = receiver.getValue().getAttribute(giverLabel);
                recAttr.merge(giveAttr);
            } else {
                receiver.getValue().setAttribute(giverLabel, giveAttr);
            }
        }

        return receiver;
    }

    /** 
     * Merges two graphs. This keeps traversing the two graphs until there is a
     * split, and then attaches the difference to a children
     */
    private Node mergeOntologyGraphs(Node first, Node second) {
        if (first == null && second != null)
            return second;
        if (second == null && first != null) 
            return first;
        if (first == null && second == null)
            return null;

        if (first.compareTo(second) == 0) {
            Set<Node> firstChildren = first.getChildren();
            Set<Node> secondChildren = second.getChildren();

            List<Node> newChildren = Lists.newArrayList();
            newChildren.addAll(Sets.difference(firstChildren, secondChildren));
            newChildren.addAll(Sets.difference(secondChildren, firstChildren));

            for (Node firstChild : firstChildren)
                if (secondChildren.contains(firstChild))
                    for (Node secondChild : secondChildren)
                        if (firstChild.compareTo(secondChild) == 0) {
                            Node merged = mergeOntologyGraphs(
                                    firstChild, secondChild);
                            if (merged != null)
                                newChildren.add(merged);
                        }
            first.setChildren(newChildren);
            return first;
        }
        return null;
    }


    /**
     * Creates a node tree structure all the way down to the leaf.
     */
    private Node createOntologyGraph(List<Synset> pathToRoot, Node parent) {
        // This is the leaf
        if (pathToRoot.size() == 0)  
            return null;

        Node currentNode = new Node(pathToRoot.get(0), parent);
        Node child = createOntologyGraph(
                pathToRoot.subList(1, pathToRoot.size()), currentNode);
        // This is a non leaf node. 
        if (child != null) 
            currentNode.addChild(child);
        return currentNode;
    }

    /**
     * Used for debugging purposes... 
     */
    public void printGraph(Node root, PrintWriter out) {
        out.print("[");
        out.print(root.getValue().getName());
        for (Node child : root.getChildren()) {
            // Use tabs to make it easier to read.
            out.print("<");
            printGraph(child, out);
            out.print(">");
        }
        out.print("]");
    }

    /** 
     *  Takes a word, part of speech and word sense, then creates a Tree based
     *  off of its ontology starting from
     *  the root. 
     */
    public Node getOntologyGraph(String word, PartsOfSpeech pos, int senseNum) {
        return getOntologyGraph(reader.getSynset(word, pos, senseNum));
    }

    /** 
     *  Takes a {@link Synset}, then creates a Tree based off of its ontology
     *  starting from the root.
     */
    public Node getOntologyGraph(Synset synset) {
        List<List<Synset>> pathsToRoot = synset.getParentPaths();
        Node castanetGraph = null;
        // For all the paths to the parent, we must merge them together into one tree.
        for(List<Synset> path : pathsToRoot) {
            Node backboneTree = createOntologyGraph(path, null);
            if (castanetGraph == null)
                castanetGraph = backboneTree;
            else
                castanetGraph = mergeOntologyGraphs(
                        castanetGraph, backboneTree);
        }
        return castanetGraph;
    }

    public Node runCastanet(CorpusReader corpus, String stopWordsFile) {
        corpus.initialize();
        Keyword[][] keywords = extractor.extractKeywords(
                corpus, 5, stopWordsFile);

        Node masterGraph = null;
        corpus.initialize();
        for (int docId = 0; docId < keywords.length; ++docId) {
            Keyword[] docKeywords = keywords[docId];
            Document doc = corpus.next();
            for (Keyword k : docKeywords) {
                String word = k.getWord();
                Synset wordSynset = reader.getSynset(word, PartsOfSpeech.NOUN, 1);

                // if the synset does not exist then skip it
                if (wordSynset == null)
                    continue;

                // For every keyword, create an ontology tree, attach a File object to it.
                // Check if there is a DocumentListAttribute, if yes add to it, if not create one.
                Attribute fileAttribute = wordSynset.getAttribute("files");
                
                List<Document> synsetDocs = null;
                if (fileAttribute == null) {
                    synsetDocs = Lists.newArrayList();
                    DocumentListAttribute docList = new DocumentListAttribute();
                    wordSynset.setAttribute("files", docList);
                } else {
                    DocumentListAttribute docList = (DocumentListAttribute) fileAttribute;
                    synsetDocs = docList.object();
                }
                synsetDocs.add(doc);

                Node wordGraph = getOntologyGraph(wordSynset);

                if (masterGraph == null)
                    masterGraph = wordGraph;
                else
                    masterGraph = mergeOntologyGraphs(masterGraph, wordGraph);
            }
        }

        // Remove the single parents
        eliminateSingleParents(masterGraph);
        return masterGraph;
    }
    
    public Node followTheRabbit(Node rootNode, String[] path){
        Node currentNode = rootNode;

        for (int i = 0; i < path.length; i++) {
            String nextPath = path[i];

            // Find the child with the unique id (the name).
            boolean foundChild = false;
            for (Node child : currentNode.getChildren()) {
                String childID = child.getValue().getName();
                if(nextPath.equals(childID)) {
                    // Follow the rabbit...
                    currentNode = child;
                    foundChild = true;
                    break;
                } else
                    System.out.printf("Looking for %s but got %s = Failed!\n",
                                       nextPath, childID);
            }
            if (!foundChild) {
                 throw new IllegalStateException(
                         "Could not find node: " + nextPath +
                         " in "+ rootNode.getValue().getName());
            }
        }
        return currentNode;
    }

    public static void main(String[] args) {
        KeywordExtractor extractor = new TransformKeywordExtractor(
                new TfIdfTransform());
        OntologyReader wordnet = WordNetCorpusReader.initialize(args[0]);
        Castanet cnet = new Castanet(wordnet, extractor);

        CorpusReader corpReader = new NewLineSplitCorpusReader(
                args[1], new TasaDocumentReader());
        cnet.runCastanet(corpReader, args[2]);
    }
}
