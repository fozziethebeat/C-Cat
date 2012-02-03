package gov.llnl.ontology.mains;

import gov.llnl.ontology.text.Annotation;
import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.text.corpora.SenseEvalAllWordsDocumentReader;
import gov.llnl.ontology.text.tag.OpenNlpMEPOSTagger;

import opennlp.tools.postag.POSTagger;

import java.io.PrintWriter;


/**
 * @author Keith Stevens
 */
public class SaveTaggedSenseTask {

    public static void main(String[] args) throws Exception {
        String testCorpusName = args[0];
        String outputPostFix = args[1];

        POSTagger tagger = new OpenNlpMEPOSTagger();

        SenseEvalAllWordsDocumentReader reader =
            new SenseEvalAllWordsDocumentReader();
        reader.parse(testCorpusName);

        PrintWriter writer = new PrintWriter(outputPostFix);
        writer.println("<?xml version=\"1.0\"?>" +
                       "<!DOCTYPE corpus SYSTEM  \"all-words.dtd\">" +
                       "<corpus lang=\"en\">");
        for (final Sentence sentence : reader.sentences()) {
            writer.println("<s>");
            int i = 0;
            String[] tokens = new String[sentence.numTokens()];
            for (Annotation annot : sentence) {
                String word = annot.word();
                if (word.indexOf(" ") != -1)
                    word = annot.lemma();
                tokens[i++] = word;
            }

            String[] tags = tagger.tag(tokens);

            i = 0;
            for (Annotation annot : sentence) {
                String word = annot.word();
                String id = annot.sense();
                String tag = tags[i++];
                if (id == null)
                    writer.printf("<w pos=\"%s\">%s</w>\n", tag, word);
                else
                    writer.printf("<h pos=\"%s\" id=\"%s\">%s</h>\n",
                                  tag, id, word);
            }
            writer.println("</s>");
        }

        writer.close();
    }
} 
