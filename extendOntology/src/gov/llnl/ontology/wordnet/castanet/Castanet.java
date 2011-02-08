package gov.llnl.ontology.wordnet.castanet;


import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.castanet.Node;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;


import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.Map;


/**
 *
 * This class implements the Castanet Algorithim described by 
 *
 * @author Terry Huang
 */
public class Castanet {
    
    WordNetCorpusReader reader;


    public Castanet (String wordnet_file_path) {
	reader = WordNetCorpusReader.initialize(wordnet_file_path);

    }
    

    /** Returns a Map that has three lists. 
     *  first - Child nodes only found in first.
     *  second - Child nodes only found in second.
     *  both - Chld nodes found in both node childs.
     */
    private Map getVennList(Node first, Node second) {
	LinkedList first = new LinkedList();
	LinkedList second = new LinkedList();
	LinkedList both = new LinkedList;

	List<Node> second_node_list = second.getChildren();
	
	Iterator first_iter = first.getChildren().iterator();

	while(first_iter.hasNext()) {

	    Node node = (Node) first_iter.next(); 
	    if (second_node_list.contains(node)) {
		
		// both = first U second
		both.add(node);
	    }else{

		// found only in first
		first.add(node);

	    }

	}

	// Go through second and remove anything that is shared.
	// Thus we will have a list containing exclusive elemnts of the second node
	
	Iterator second_iter = second_node_list.iterator();

	while(second_iter.hasNext()) {
	    Node second_node = (Node)second_iter.next();
	    
	    if(!both.contains(second_node)){
		second.add(second_node);
	    }
	}



	TreeMap result = new TreeMap();

	result.put("first",first);
	result.put("second", second);
	result.put("both",both);

	return result;

    }




    /** This merges to graphs together, meaning it takes two graphs, keeps traversing until there is a split, and then attaches the difference to
	a children */
    private Node mergeOntologyGraphs (Node first, Node second) {

	if (first == null && second != null) return second;
	if (second == null && first != null) return first;
	if (first == null && second == null) return null;



	if (first.nodeValue().equals(second.nodeValue())) {

	    // Keep going down a level
	    for(Node first_child : first.getChildren()) {
		// Find a child that matches

	    }

	}else {
	    return null;

	}

    }

    private Node createOntologyGraph (List<Synset> pathToRoot, Node parent) {

	// This is the leaf
	if (pathToRoot.size() == 0)  return null;

	Node this_node = new Node (pathToRoot.get(0), parent);
	Node child = createOntologyGraph(pathToRoot.subList(1, pathToRoot.size()), this_node);

	if (child != null)
	    this_node.addChild(child);

	return this_node;
    }

    private void printGraph(Node root) {

	System.out.println(root.nodeValue());

	Iterator child_iter = root.getChildren().iterator();

	while (child_iter.hasNext()) {
	    Node next_child = (Node) child_iter.next();

	    // Use tabs to make it easier to read.
	    System.out.print(" ");

	    printGraph(next_child);

	}


    }

    /** 
     *  Takes a word, part of speech and word sense, then creates a Tree based off of its ontology starting from
     *  the root. */
    public Node getOntologyGraph(String word, PartsOfSpeech pos, int senseNum) {

	Synset word_synset = reader.getSynset(word, pos, senseNum);	
	List<List<Synset>> path_to_root = word_synset.getParentPaths();


	// DEBUG: try creating the first node    
	List<Synset> first_path = path_to_root.get(0);


	Node graph = createOntologyGraph(first_path, null);

	return graph;
    }


    public static void main (String[] args) {

	Castanet cnet = new Castanet(args[0]);

	Node pig_graph = cnet.getOntologyGraph("pig", PartsOfSpeech.NOUN, 1);

	cnet.printGraph(pig_graph);

    }

}
