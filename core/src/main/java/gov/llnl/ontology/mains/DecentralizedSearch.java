package gov.llnl.ontology.mains;

import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.TagLinkedOntologyReader;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;

import com.google.common.collect.Sets;
import com.google.common.collect.Maps;

import edu.ucla.sspace.basis.StringBasisMapping;
import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.util.Pair;

import graph.edu.ucla.sspace.graph.Graph;
import graph.edu.ucla.sspace.graph.SimpleEdge;
import graph.edu.ucla.sspace.graph.SparseUndirectedGraph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class DecentralizedSearch {

    public static void main(String[] args) throws Exception {
        ArgOptions options = new ArgOptions();
        options.addOption('t', "tagSynsetMapping",
                          "Provies the tag name to synset name mapping",
                          true, "FILE", "Required");
        options.addOption('d', "wordnetDir",
                          "Provides the path to the wordnet directory",
                          true, "PATH", "Required");
        options.addOption('p', "tagPairs",
                          "Provides the list of tags for document pairs",
                          true, "FILE", "Required");
        options.addOption('g', "tagGraph",
                          "Provides the structure of the tag graph",
                          true, "FILE", "Required");
        options.addOption('l', "pathLimit",
                          "Specifies the longest search path allowed",
                          true, "INT", "Required");
        options.addOption('n', "numSynsets",
                          "Specifies the number of synsets",
                          true, "INT", "Required");
        options.addOption('s', "synsetDistances",
                          "Specifies the shortest path lengths between any " +
                          "two synsets",
                          true, "FILE", "Required");
        options.parseOptions(args);

        OntologyReader reader = WordNetCorpusReader.initialize(
                options.getStringOption('d'));
        reader = new TagLinkedOntologyReader(reader, readTagMapping(
                    options.getStringOption('t')));
        Set<Pair<Set<String>>> tagPairs = readTagPairs(
                    options.getStringOption('p'));
        TagGraph tagGraph = readTagGraph(options.getStringOption('g'));
        DistanceMatrix distanceMatrix = readSynsetDistances(
                    options.getStringOption('s'), options.getIntOption('n'),
                    reader);
        int maxLength = options.getIntOption('l');

        for (Pair<Set<String>> tagPair : tagPairs) {
            int length = searchFromNeighbors(tagPair.x, tagPair.y, maxLength,
                                             tagGraph, distanceMatrix, reader);
            System.out.printf("Path Length: %d\n", maxLength - length);
        }
    }

    public static int searchFromNeighbors(Set<String> nextTags,
                                          Set<String> goalTags,
                                          int maxLength,
                                          TagGraph graph,
                                          DistanceMatrix distanceMatrix,
                                          OntologyReader reader) {
        if (maxLength == 0)
            return Integer.MIN_VALUE;

        for (String nextTag : nextTags)
            if (goalTags.contains(nextTag))
                return maxLength;

        String bestNext = null;
        double shortestPath = Integer.MAX_VALUE;
        for (String nextTag : nextTags) {
            Synset nextSynset = reader.getSynset(nextTag);
            for (String endTag : goalTags) {
                Synset endSynset = reader.getSynset(endTag);
                double distance = distanceMatrix.get(nextSynset, endSynset);
                if (distance <= shortestPath) {
                    shortestPath = distance;
                    bestNext = nextTag;
                }
            }
        }

        System.out.println("Selecting tag: " + bestNext);
        return searchFromNeighbors(
                graph.neighbors(bestNext), goalTags, maxLength-1,
                graph, distanceMatrix, reader);
    }

    public static Map<String, String> readTagMapping(String tagMapFile) 
            throws Exception {
        Map<String, String> tagMap = Maps.newHashMap();
        BufferedReader br = new BufferedReader(new FileReader(tagMapFile));
        for (String line = null; (line = br.readLine()) != null; ) {
            String[] tokens = line.split("\\s+");
            tagMap.put(tokens[0], tokens[1]);
        }
        return tagMap;
    }

    public static Set<Pair<Set<String>>> readTagPairs(String tagPairFile)
            throws Exception {
        Set<Pair<Set<String>>> tagPairs = Sets.newHashSet();
        BufferedReader br = new BufferedReader(new FileReader(tagPairFile));
        for (String line = null; (line = br.readLine()) != null; ) {
            String[] pair = line.split("\\|");
            tagPairs.add(new Pair(readTags(pair[0]), readTags(pair[1])));
        }
        return tagPairs;
    }

    public static Set<String> readTags(String tagList) {
        Set<String> tagSet = Sets.newHashSet();
        for (String tag : tagList.split(";"))
            tagSet.add(tag);
        return tagSet;
    }

    public static TagGraph readTagGraph(String tagGraphFile) 
            throws Exception {
        TagGraph graph = new TagGraph();
        BufferedReader br = new BufferedReader(new FileReader(tagGraphFile));
        for (String line = null; (line = br.readLine()) != null; ) {
            String[] tags = line.split("\\s+");
            graph.addEdge(tags[0], tags[1]);
        }
        return graph;
    }

    public static DistanceMatrix readSynsetDistances(String distanceFile,
                                                     int numSynsets,
                                                     OntologyReader reader)
            throws Exception {
        DistanceMatrix distances = new DistanceMatrix(reader, numSynsets);
        BufferedReader br = new BufferedReader(new FileReader(distanceFile));
        for (String line = null; (line = br.readLine()) != null; ) {
            String[] synsetsAndDistance = line.split("\\s+");
            String synset1 = synsetsAndDistance[0];
            String synset2 = synsetsAndDistance[1];
            int distance = Integer.parseInt(synsetsAndDistance[2]);
            distances.put(synset1, synset2, distance);
        }

        return distances;
    }

    public static class DistanceMatrix {

        public OntologyReader reader;

        public StringBasisMapping basis;

        public Matrix distances;

        public DistanceMatrix(OntologyReader reader,
                              int numSynsets) {
            this.reader = reader;
            distances = new ArrayMatrix(numSynsets, numSynsets);
            basis = new StringBasisMapping();
        }

        public void put(String s1, String s2, int distance) {
            int index1 = basis.getDimension(reader.getSynset(s1).getName());
            int index2 = basis.getDimension(reader.getSynset(s2).getName());
            distances.set(index1, index2, distance);
        }

        public double get(Synset synset1, Synset synset2) {
            int index1 = basis.getDimension(synset1.getName());
            int index2 = basis.getDimension(synset2.getName());
            return distances.get(index1, index2);
        }
    }

    public static class TagGraph {

        public Graph graph;

        public StringBasisMapping basis;

        public TagGraph() {
            graph = new SparseUndirectedGraph();
            basis = new StringBasisMapping();
        }

        public void addEdge(String tag1, String tag2) {
            int vertex1 = basis.getDimension(tag1);
            int vertex2 = basis.getDimension(tag2);
            graph.add(vertex1);
            graph.add(vertex2);
            graph.add(new SimpleEdge(vertex1, vertex2));
        }

        public Set<String> neighbors(String tag) {
            Set<String> neighborTags = Sets.newHashSet();
            int vertex = basis.getDimension(tag);
            Set<Integer> neighbors = graph.getNeighbors(vertex);
            for (int v : neighbors)
                neighborTags.add(basis.getDimensionDescription(v));
            return neighborTags;
        }
    }
}
