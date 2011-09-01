package gov.llnl.ontology.mains;

import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.SynsetSimilarity;

import com.google.common.collect.Maps;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.util.HashMultiMap;
import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.ReflectionUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class WordsiWordnetSim {

    public static void main(String[] args) throws Exception {
        ArgOptions options = new ArgOptions();
        options.addOption('k', "senseEvalKey",
                          "The sense eval mapping from instance ids to " +
                          "onto notes sense ids.",
                          true, "FILE", "Required");
        options.addOption('m', "ontoNotesWordNetMapping",
                          "The mapping from ontoNotes senses to wordnet senses",
                          true, "FILE", "Required");
        options.addOption('s', "synsetSimilarityFunc",
                          "The SynsetSimilarity function to apply.",
                          true, "CLASSNAME", "Required");
        options.addOption('d', "wordnetDir",
                          "The directory holding wordnet data files",
                          true, "PATH", "Required");
        options.addOption('A', "clusterClusterAssignments",
                          "Set to true if  word sense cluster assignments " +
                          "are supplied.",
                          false, null, "Optional");
        options.parseOptions(args);

        OntologyReader wordnet = WordNetCorpusReader.initialize(
                options.getStringOption('d'));

        // Load up the onto notes to wordnet mappings.  Each onto note sense may
        // map to multiple wordnet senses, so we use a multi map.
        MultiMap<String, Synset> ontoNoteMap =
            new HashMultiMap<String, Synset>();
        String ontoMapFile = options.getStringOption('m');
        BufferedReader br = new BufferedReader(new FileReader(ontoMapFile));
        for (String line = null; (line = br.readLine()) != null;) {
            String[] tokens = line.split("\\s+", 2);
            // Some onto notes senses have no wordnet sense, these end with NM.
            // Skip them.  Similarity for senses ending with NP.
            if (tokens[1].endsWith("NM") ||
                tokens[1].endsWith("NP"))
                continue;
            ontoNoteMap.put(tokens[0], wordnet.getSynset(tokens[1]));
        }

        // Load up the instance id mappings to their wordnet mappings.
        // Since each instance maps to a single onto notes sense and each onto
        // notes sense maps to a set of word net senses, we use a multi map.
        MultiMap<String, Synset> instanceMap =
            new HashMultiMap<String, Synset>();
        String keyFile = options.getStringOption('k');
        br = new BufferedReader(new FileReader(keyFile));
        for (String line = null; (line = br.readLine()) != null;) {
            String[] tokens = line.split("\\s+", 3);
            Set<Synset> synsets = ontoNoteMap.get(tokens[2]);
            if (synsets != null)
                for (Synset s : synsets)
                    if (s != null)
                        instanceMap.put(tokens[1], s);
        }
        ontoNoteMap = null;

        // Load up the synset similarity function.
        SynsetSimilarity simFunc = ReflectionUtil.getObjectInstance(
                options.getStringOption('s'));

        if (options.hasOption('A')) {
            for (int i = 0; i < options.numPositionalArgs(); ) {
                String name = options.getPositionalArg(i++);
                String clustFile = options.getPositionalArg(i++);
                String ccFile = options.getPositionalArg(i++);
                Map<String, String> termMap = Maps.newHashMap();

                MultiMap<String, Synset> clustMap = loadClusterMap(
                        clustFile, instanceMap, termMap);
                clustMap = loadClustClustMap(ccFile, clustMap, termMap);
                scoreClusterMap(clustMap, simFunc, name);
            }
        } else {
            for (int i = 0; i < options.numPositionalArgs(); ) {
                String name = options.getPositionalArg(i++);
                String clustFile = options.getPositionalArg(i++);
                Map<String, String> termMap = Maps.newHashMap();

                MultiMap<String, Synset> clustMap = loadClusterMap(
                        clustFile, instanceMap, termMap);
                scoreClusterMap(clustMap, simFunc, name);
            }
        }
    }

    private static void scoreClusterMap(MultiMap<String, Synset> clusterMap, 
                                        SynsetSimilarity simFunc,
                                        String name) {
        // Compute the total pairwise similarity of each synset assigned to the
        // same cluster and output the total score, the number of comparisons,
        // and then the average score.
        List<Double> scores = new ArrayList<Double>();
        double objectiveScore = 0;
        for (String clusterId : clusterMap.keySet()) {
            Synset[] senses = clusterMap.get(clusterId).toArray(new Synset[0]);
            double numPairs = senses.length * (senses.length + 1) / 2.0;
            double totalSim = 0;
            for (int i = 0; i < senses.length; ++i)
                for (int j = i+1; j < senses.length; ++j) {
                    if (senses[i].getPartOfSpeech() == PartsOfSpeech.NOUN &&
                        senses[j].getPartOfSpeech() == PartsOfSpeech.NOUN) {
                        double sim = simFunc.similarity(senses[i], senses[j]);
                        totalSim += sim;
                        scores.add(sim);
                    }
                }
            objectiveScore += totalSim / numPairs;
        }

        Collections.sort(scores);
        double min = scores.get(0);
        double max = scores.get(scores.size()-1);
        double median = scores.get(scores.size() / 2);
        double bottomQuarter = scores.get(scores.size() / 4);
        double topQuarter = scores.get(scores.size() * 3 / 4);
        System.out.printf("%f %f %f %f %f %s %d %f\n",
                          min, bottomQuarter, median, topQuarter, max, name,
                          clusterMap.size(), objectiveScore);
    }

    private static MultiMap<String, Synset> loadClusterMap(
            String file,
            MultiMap<String, Synset> instanceMap,
            Map<String, String> termMap) throws IOException {
        // Load up the cluster id mappings to their wordnet senses.  Each
        // cluster can have multiple instances which each may have multiple
        // wordnet senses, so we use a multimap.
        MultiMap<String, Synset> clusterMap =
            new HashMultiMap<String, Synset>();
        BufferedReader br = new BufferedReader(new FileReader(file ));
        for (String line = null; (line = br.readLine()) != null;) {
            String[] tokens = line.split("\\s+");
            Set<Synset> synsets = instanceMap.get(tokens[1]);
            if (synsets != null)
                clusterMap.putMulti(tokens[2], synsets);

            String[] termParts = tokens[2].split("\\.");
            int id = Integer.parseInt(termParts[2]);
            String senseName = (id > 0)
                ? termParts[0] + "-" + id
                : termParts[0];
            termMap.put(senseName, tokens[2]);
        }

        return clusterMap;
    }

    private static MultiMap<String, Synset> loadClustClustMap(
            String file,
            MultiMap<String, Synset> clustMap,
            Map<String, String> termMap) throws IOException {
            MultiMap<String, Synset> ccMap = new HashMultiMap<String, Synset>();
            BufferedReader br = new BufferedReader(new FileReader(file));
            for (String line = null; (line = br.readLine()) != null;) {
                String[] tokens = line.split("\\s+");
                String clusterName = termMap.get(tokens[0]);
                Set<Synset> synsets = clustMap.get(clusterName);
                if (synsets != null)
                    ccMap.putMulti(tokens[1], synsets);
            }
            return ccMap;
        }
}
