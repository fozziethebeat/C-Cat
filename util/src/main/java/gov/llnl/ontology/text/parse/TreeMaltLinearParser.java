package gov.llnl.ontology.text.parse;

import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.SimpleDependencyRelation;
import edu.ucla.sspace.dependency.SimpleDependencyTreeNode;

import org.maltparser.MaltParserService;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.DependencyNode;


public class TreeMaltLinearParser implements TreeParser {

    /**
     * The {@link MaltParserService} used to parse sentences.
     */
    private MaltParserService parser;

    /**
     * The serialzied model path.
     */
    private String modelPath;

    /** 
     * The default location of the malt parser model. 
     */
    public static final String PARSER_MODEL =
        "engmalt.linear";

    public TreeMaltLinearParser() {
        this(PARSER_MODEL);
    }

    /**
     * Creates a new {@link MaltParser} using the provided model paths. Note
     * that this {@link Parser} cannot be readily used within a map reduce job.
     */
    public TreeMaltLinearParser(String maltParserModelPath) {
        try {
            this.modelPath = maltParserModelPath;
            parser = new MaltParserService();
            parser.initializeParserModel
                ("-c " + modelPath + " -m parse");
        } catch (MaltChainedException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public DependencyTreeNode[] parse(DependencyTreeNode[] tree) {
        // Create the string lines that the malt parser needs and make a copy of
        // in the input tree that we can modify with new relations.
        String[] lines = new String[tree.length];
        SimpleDependencyTreeNode[] parsedTree = 
            new SimpleDependencyTreeNode[tree.length];
        for (int i = 0; i < tree.length; ++i) {
            lines[i] = buildLine(tree[i]);
            parsedTree[i] = new SimpleDependencyTreeNode(
                    tree[i].word(), tree[i].pos(),
                    tree[i].lemma(), tree[i].index());
        }

        try {
            // Get a copy of the parser and parse the sentence.
            MaltParserService p = parser;
            DependencyStructure graph = p.parse(lines);

            // Extract the nodes in the parsed malt graph and turn the edges
            // into DependencyRelations betwee TreeNodes.
            for (int i = 1, j = 0; i <= graph.getHighestDependencyNodeIndex(); i++) {
                DependencyNode node = graph.getDependencyNode(i);

                // Skip any nodes that are null.  How often does this happen? I
                // don't really know but I distrust malt.
                if (node != null) {
                    // Get the actualy dependency tree node that this graph node
                    // corresponds to.
                    SimpleDependencyTreeNode treeNode = parsedTree[j++];

                    // If we've got an edge, add a relation.
                    if (node.hasHead()) {
                        Edge e = node.getHeadEdge();
                        int parentIndex = e.getSource().getIndex() - 1;

                        // We don't need to add a link from the head node to the
                        // null node.
                        if (parentIndex < 0)
                            continue;

                        // Get the relation.  This is really funky looking
                        // because Malt does everything with abstract symbol
                        // tables, so you can't request labels in any direct
                        // fashion
                        String relationType = e.getLabelSymbol(
                                e.getLabelTypes().iterator().next());

                        // Get the parent node for this relation.
                        SimpleDependencyTreeNode parentNode = parsedTree[parentIndex];

                        // Create the relation and add it to both nodes.
                        DependencyRelation relation = new SimpleDependencyRelation(
                                parentNode, relationType, treeNode);
                        treeNode.addNeighbor(relation);
                        parentNode.addNeighbor(relation);
                    }
                }
            }
        } catch (Exception e) {
            // Malt can throw all kinds of exceptions, if this happens, just die
            // horribly.
            throw new RuntimeException(e);
        } 

        return parsedTree;
    }

    /**
     * Transforms a {@link DependencyTreeNode} into a line of text suitable for
     * the malt parser.
     */
    private static String buildLine(DependencyTreeNode node) {
        return String.format("%d\t%s\t%s\t%s\t%s\t_\t_\t_", 
                             node.index(), node.word(), node.lemma(),
                             node.pos(), node.pos());
    }
}
