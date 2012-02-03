import gov.llnl.ontology.text.SimpleAnnotation
import gov.llnl.ontology.text.Sentence
import gov.llnl.ontology.wordnet.WordNetCorpusReader
import gov.llnl.ontology.wordnet.wsd.WordSenseDisambiguation

import edu.ucla.sspace.util.ReflectionUtil

import java.util.HashSet

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.xml._


object ParseSemCore {
    def main(args:Array[String]) {
        val alg:WordSenseDisambiguation = ReflectionUtil.getObjectInstance(args(1))
        val semCor = XML.loadFile(args(0))

        val docName = args(0).split("\\.")(0)
        alg.setup(WordNetCorpusReader.initialize("dict", true))
        for ( (s, si) <- (semCor \\ "s") zipWithIndex) {
            val tokens = s \ "_"
            val sentence = new Sentence(0, 0, tokens.size)
            val selectedIndices = new HashSet[java.lang.Integer]()
            for ( (n, ni) <- (s \ "_") zipWithIndex) { 
                val annot = new SimpleAnnotation(n.text)
                val (pos, cmd, lemma, lexsn) = (n \ "@pos", n \ "@cmd", n \ "@lemma", n \ "@lexsn")
                if (lemma.size != 0 && lexsn.size != 0) {
                    annot.setPos(pos.text)
                    annot.setSense("%s %s.s%03d.t%03d".format(
                        docName, docName, si, ni))
                    annot.setWord(lemma.text)
                    selectedIndices.add(ni)
                }
                sentence.addAnnotation(ni, annot)
            }

            val taggedSentence = alg.disambiguate(sentence, selectedIndices)
            for ((tagged, orig) <- taggedSentence zip sentence;
                 if orig.hasSense)
                if (tagged.hasSense)
                    printf("%s %s\n", orig.sense, tagged.sense)
                else
                    printf("%s U\n", orig.sense)
        }
    }
}
