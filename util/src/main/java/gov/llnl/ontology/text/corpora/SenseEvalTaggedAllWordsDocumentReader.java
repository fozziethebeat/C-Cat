package gov.llnl.ontology.text.corpora;

import gov.llnl.ontology.text.Sentence;

import com.google.common.collect.Lists;

import edu.stanford.nlp.ling.CoreAnnotations.ValueAnnotation;
import edu.stanford.nlp.pipeline.Annotation;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * @author Keith Stevens
 */
public class SenseEvalTaggedAllWordsDocumentReader extends DefaultHandler {

    private List<Sentence> sentences = Lists.newArrayList();

    private List<Annotation> currentSentence = Lists.newArrayList();

    private String currentId;

    private boolean inSentence;

    private boolean inWord;

    private boolean inHeadWord;

    /**
     * The internal xml parser.
     */
    private SAXParser saxParser;

    public SenseEvalTaggedAllWordsDocumentReader() {
        SAXParserFactory saxfac = SAXParserFactory.newInstance();
        saxfac.setValidating(false);

        // Ignore a number of xml features, like dtd grammars and validation.
        try {
            saxfac.setFeature("http://xml.org/sax/features/validation", false);
            saxfac.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            saxfac.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            saxfac.setFeature("http://xml.org/sax/features/external-general-entities", false);
            saxfac.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            saxParser = saxfac.newSAXParser();
        } catch (SAXNotRecognizedException e1) {
            throw new RuntimeException(e1);
        }catch (SAXNotSupportedException e1) {
            throw new RuntimeException(e1);
        } catch (ParserConfigurationException e1) {
            throw new RuntimeException(e1);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    public void parse(String fileName) {
        inSentence = false;
        inWord = false;
        inHeadWord = false;
        // Parse!
        try {
            saxParser.parse(new File(fileName), this);
        } catch (SAXException se) {
            throw new RuntimeException(se);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes atts)
            throws SAXException {
        if ("s".equals(name)) {
            inSentence = true;
        } else if ("h".equals(name)) {
            inHeadWord = true;
            currentId = atts.getValue("id");
        } else if ("w".equals(name)) {
            inWord = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String name)
            throws SAXException {
        if ("w".equals(name)) {
            inWord = false;
        } else if ("h".equals(name)) {
            inHeadWord = false;
        } else if ("s".equals(name)) {
            inSentence = false;
            endSentence();
        }
    }

    private void endSentence() {
        Sentence sentence = new Sentence(0, 0, currentSentence.size());
        int i = 0;
        for (Annotation word : currentSentence)
            sentence.addAnnotation(i++, word);
        sentences.add(sentence);
        currentSentence.clear();
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (!inSentence && !inHeadWord && !inWord)
            return;

        String word = (new String(ch, start, length)).trim();
        Annotation token = new Annotation(word);
        if (inHeadWord)
            token.set(ValueAnnotation.class, currentId);
        currentSentence.add(token);
    }

    public List<Sentence> sentences() {
        return sentences;
    }
}
