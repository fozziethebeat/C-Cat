package gov.llnl.ontology.wordnet.castanet;

import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;
import gov.llnl.ontology.wordnet.castanet.Node;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 *  This class represents the Castanet tree presented in:
 *
 *  <li style="font-family:Garamond, Georgia, serif"> E. Stoica, M. A. Hearst, and M. Richardson. 
 *  Automating creation of hierarchical faceted metadata structures. In Proc.
 *  NAACL-HLT 2007, pages 244–251, 2007.</li>
 *
 *  @author thuang513@gmail.com (Terry Huang)
 */
public class Castanet {

	/**
	 * The logger used to record all output
	 */
	private static final Logger LOGGER =
		Logger.getLogger(Castanet.class.getName());

	private String stopWordsFile;
	private File directory;

	private static final int TOP_N_KEYWORDS = 10;

	/**
	 *  WordNet Reader used to create the Castanet tree.
	 */
	private static WordNetCorpusReader reader;
	
	public Castanet(String directory, String wordnetFilePath) {
		this(directory, wordnetFilePath, "");
	}

	public Castanet(String directory, String wordNetFilePath, String stopWordsFile) {
		File directoryFile = new File(directory);
		if (!directoryFile.isDirectory()) {
			LOGGER.severe("Invalid directory given to Castanet!");
			throw new IllegalStateException("Invalid directory given to Castanet!");
		}

		this.directory = directoryFile;
		this.stopWordsFile = stopWordsFile;

		reader = WordNetCorpusReader.initialize(wordNetFilePath);
	}

	// Builds the Castanet tree of the files in a given directory.
	public void buildTree() {
		// Get all the keywords for a document.
		//		Map<File, List<String>> fileKeywords = 
		//	keywordExtractor.extractKeywordsFromFiles(directory, 
		//                                          TOP_N_KEYWORDS, 
		//                                          stopWordsFile);

		// For every keyword, Build its "chain" to the root in WordNet
		
		// Merge this chain with the tree that we are building.
	}

	protected Node getChainToRoot(String word, PartsOfSpeech pos, int sense) {
		Synset wordSynset = reader.getSynset(word, pos, sense);
		List<List<Synset>> listOfRootPaths = wordSynset.getParentPaths();

		Node root = null;
		
		// For all the paths to the parent, we must merge them together into one tree.
		for (List<Synset> pathToRoot : listOfRootPaths) {
			Node  newChain = createChainOfNodes(pathToRoot);
			
		}

		return null;
	}
	
	/**
	 *  Merge two Castanet trees together. And returns the root.
	 */
	protected Node mergeTree(Node originalTree, Node toMergeTree) {
		// Make sure the two nodes are not null
		if (originalTree == null || toMergeTree == null) {
			return null;
		}

		// Make sure that the two tree root nodes are the same (in value), 
		// otherwise this is a confusing issue that we want to avoid.
		if (!originalTree.getValue().getName().equals(
		                                           toMergeTree.getValue().getName())) {
			LOGGER.warning("Two unequal nodes attempting to merge!");
			return null;
		}
		
		Set<Node> originalTreeChildren = originalTree.getChildren();

		// Look for children that originalTree doesn't have, but toMergeTree has.
		// Then take these children and add them to the original tree.
		for (Node child : toMergeTree.getChildren()) {
			
			// Add children that the current node doesn't already have.
			if (!originalTreeChildren.contains(child)) {
				originalTree.addChild(child);
				continue;
			}

			// TODO(thuang513): figure out a way to do this with better performance.
			// Find the common element by iterating through the list.
			Iterator<Node> iter = originalTreeChildren.iterator();
			while (iter.hasNext()) {
				Node current = (Node)iter.next();

				// When we have the nodes with the same values,
				// go down and merge their children.
				if (child.equals(current)) {
					mergeTree(current, child);
				}
			}
		}

		return originalTree;
	}

	/**
	 *  Create a chain of nodes.
	 */
	protected Node createChainOfNodes(List<Synset> pathToRoot) {
		if (pathToRoot.isEmpty()) {
			LOGGER.warning("There was nothing in the path to the root node...");
			return null;
		}
		
		// Go through the list of synset, make them all Nodes, and connect them.
		Node rootChainNode = null;
		Node lastNode = null;
		for (Synset synset : pathToRoot) {
			Node newNode = new Node(synset, lastNode);

			// Ideally these should be the same conditions.
			// There should be no root node if there wasn't
			// a last node.
			if (rootChainNode == null || lastNode == null) {
				rootChainNode = newNode;

				// Also since there was no last node, then keep going.
				continue;
			}
			
			lastNode.addChild(newNode);
			lastNode = newNode;
		}

		return rootChainNode;
	}
	
	/**
	 *  Returns the root node of the Castanet tree.
	 */
	public void getRootNode() {

	}


}