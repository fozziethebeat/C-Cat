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
            for (String line = null; (line = br.readLine()) != null; ) {
                System.out.print("> ");
                String[] wordPos = line.split("\\s+");
                PartsOfSpeech pos = PartsOfSpeech.valueOf(
                        wordPos[1].toUpperCase());
                for (Synset sense : wordnet.getSynsets(wordPos[0], pos))
                    System.out.printf("%s %s\n", 
                                      sense.getName(),
                                      sense.getSenseKey(wordPos[0]));
            }
        }
    }
}
