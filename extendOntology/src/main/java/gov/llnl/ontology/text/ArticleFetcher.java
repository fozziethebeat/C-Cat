package gov.llnl.ontology.text;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * This interface details the requirements of an Article object. How an article
 * actually grabs content and parse is up to the designer of that class.
 */
public interface ArticleFetcher {

    /**
     *  Getter for the content of the article.
     *  NOTE: fetchAndParseContent() must be called beforehand for this to work,
     *  otherwise it will return an empty article (no sentence {@link String}s).
     *  @return A {@link List} of sentences (each sentence is a {@link String}s).
     */
     List<String> getContent();

    /**
     *  Fetches the content of the article and parses the article, extracting
     *  all the text.
     *
     * @return The status of the fetch (true if successful, false if not).
     */
    boolean fetchAndParseContent();

    /**
     *  Writes the content of the article into a File.
     */
     boolean writeToFile(File out);
}