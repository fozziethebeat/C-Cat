package gov.llnl.ontology.wordnet.castanet;

import gov.llnl.ontology.wordnet.Synset;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 *  This class represents a node in the Castanet tree.
 *
 *  @author thuang513@gmail.com (Terry Huang)
 */
public class Node {

	/**
	 * The logger used to record all output
	 */
	private static final Logger LOGGER =
		Logger.getLogger(Castanet.class.getName());

	private Set<Node> children;

	private Synset synset;
	
	private Node parentNode;

	public Node(Synset synset) {
		this(synset, null);
	}

	public Node(Synset synset, Node parent) {
		children = new TreeSet();
		this.synset = synset;
		this.parentNode = parent;
	}

	public Node getParent() {
		return parentNode;
	}

	public Set<Node> getChildren() {
		return Collections.unmodifiableSet(children);
	}

	public boolean addChild(Node childNode) {
		if (!children.add(childNode)) {
			LOGGER.warning("Couldn't add childnode: " + synset.toString());
			return false;
		}

		return true;
	}

	public Synset getValue() {
		return synset;
	}

	/**
	 *  Two equal nodes is defined by both having the same value. In other words
	 *  as long as two nodes have the same {@code Synset} then the two nodes are
	 *  equal.
	 */
	public boolean equals(Object other) {
		return getValue().getName().equals(((Node)other).getValue().getName());
	}
}