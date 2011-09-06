package gov.llnl.ontology.mains;

import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;

import java.io.BufferedReader;
import java.io.InputStreamReader;


/**
 * @author Keith Stevens
 */
public class WordnetShell {

    public static void main(String[] args) throws Exception {
        OntologyReader wordnet = WordNetCorpusReader.initialize(args[0]);
        while (true) {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(System.in));
            System.out.print("> ");
            for (String line = null; (line = br.readLine()) != null; ) {
                if (line.trim().length() != 0) {
                    int spaceIndex = line.lastIndexOf(" ");
                    String word = line.substring(0, spaceIndex).trim();
                    String p = line.substring(spaceIndex).trim();
                    PartsOfSpeech pos = PartsOfSpeech.valueOf(
                            p.toUpperCase());
                    for (Synset sense : wordnet.getSynsets(word, pos))
                        System.out.printf("%s %s\n", 
                                          sense.getName(),
                                          sense.getSenseKey(word));
                }
                System.out.print("> ");
            }
        }
    }
}
