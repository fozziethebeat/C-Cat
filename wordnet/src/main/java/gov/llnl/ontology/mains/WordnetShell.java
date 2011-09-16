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

    public static String join(String[] tokens, int start, int end, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end && i < tokens.length; ++i)
            sb.append(tokens[i]).append(sep);
        return sb.toString().trim();
    }

    public static void main(String[] args) throws Exception {
        OntologyReader wordnet = WordNetCorpusReader.initialize(args[0]);
        while (true) {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(System.in));
            System.out.print("> ");
            for (String line = null; (line = br.readLine()) != null; ) {
                if (line.trim().length() != 0) {
                    String[] tokens = line.split("\\s+");
                    String command = tokens[0];
                    if (command.equals("gs")) {
                        if (tokens.length == 2) {
                            Synset s = wordnet.getSynset(tokens[1]);
                            System.out.printf("%s %s\n",
                                              s.getName(), s.getSenseKey());
                        } else {
                            String word = join(tokens, 1, tokens.length-1, " ");
                            PartsOfSpeech pos = PartsOfSpeech.valueOf(
                                    tokens[tokens.length-1].toUpperCase());
                            System.out.println(word);
                            for (Synset sense : wordnet.getSynsets(word, pos))
                                System.out.printf("%s %s\n", 
                                                  sense.getName(),
                                                  sense.getSenseKey(word));
                        }
                    } else if (command.equals("gr")) {
                        String word = tokens[1];
                        Synset synset = wordnet.getSynset(word);
                        if (tokens.length == 2) {
                            for (Synset related : synset.allRelations())
                                System.out.printf("%s -> %s\n",
                                                  word, related.getName());
                        } else {
                            for (Synset related : synset.getRelations(tokens[2]))
                                System.out.printf("%s -> %s\n",
                                              word, related.getName());
                        }
                    } else if (command.equals("help")) {
                        System.out.println("get senses: gs word pos");
                        System.out.println("get senses: gs word.pos.#");
                        System.out.println("get relations: gr sense.pos.#");
                        System.out.println("get relations: gr sense.pos.# relation");
                        System.out.println("help: help");
                    }
                }
                System.out.print("> ");
            }
        }
    }
}
