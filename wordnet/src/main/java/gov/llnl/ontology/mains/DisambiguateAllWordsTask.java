package gov.llnl.ontology.mains;

import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.text.corpora.SenseEvalAllWordsDocumentReader;
import gov.llnl.ontology.util.AnnotationUtil;
import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;
import gov.llnl.ontology.wordnet.wsd.WordSenseDisambiguation;
import gov.llnl.ontology.text.tag.OpenNlpMEPOSTagger;

import com.google.common.collect.Sets;

import edu.stanford.nlp.ling.CoreAnnotations.StemAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.ValueAnnotation;
import edu.stanford.nlp.pipeline.Annotation;

import edu.ucla.sspace.util.ReflectionUtil;

import opennlp.tools.postag.POSTagger;

import java.io.PrintWriter;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class DisambiguateAllWordsTask {

    public static void main(String[] args) throws Exception {
        String dataDir = args[0];
        String algName = args[1];
        String testCorpusName = args[2];
        String outputPostFix = args[3];

        POSTagger tagger = new OpenNlpMEPOSTagger();

        OntologyReader wordnet = WordNetCorpusReader.initialize(dataDir);

        WordSenseDisambiguation wsd = ReflectionUtil.getObjectInstance(algName);
        wsd.setup(wordnet);

        SenseEvalAllWordsDocumentReader reader =
            new SenseEvalAllWordsDocumentReader();
        reader.parse(testCorpusName);

        PrintWriter writer = new PrintWriter(wsd.toString() + outputPostFix);
        String prevId = null;
        for (Sentence sentence : reader.sentences()) {
            int i = 0;
            Set<Integer> focusIndices = Sets.newHashSet();
            String[] tokens = new String[sentence.numTokens()];
            for (Annotation annot : sentence) {
                tokens[i] = AnnotationUtil.word(annot);
                if (tokens[i].indexOf(" ") != -1)
                    tokens[i] = annot.get(StemAnnotation.class);

                if (annot.get(ValueAnnotation.class) != null) {
                    focusIndices.add(i);
                }
                ++i;
            }

            i = 0;
            String[] tags = tagger.tag(tokens);
            for (Annotation annot : sentence)
                AnnotationUtil.setPos(annot, tags[i++]);

            i = 0;
            Sentence disambiguated = wsd.disambiguate(sentence, focusIndices);
            for (Annotation annot : disambiguated) {
                String id = sentence.getAnnotation(i).get(
                        ValueAnnotation.class);
                if (id != null) {
                    String sense = AnnotationUtil.wordSense(annot);
                    if (sense == null)
                        sense = "U";

                    if (prevId == null)
                        writer.printf("%s %s", id, sense);
                    else if (prevId.equals(id))
                        writer.printf(" %s", sense);
                    else
                        writer.printf("\n%s %s", id, sense);
                    prevId = id;
                }

                ++i;
            }
        }
        writer.close();
    }
}

