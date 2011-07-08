package gov.llnl.document.preprocess;

import edu.ucla.sspace.text.IteratorFactory;
import java.util.logging.Logger;



/**
 * This pre-processor class is meant to take an article from the BBC News website {@link http://news.bbc.co.uk}
 * and extracts only the contents of the article and title (stripping away all HTML, Javascript and whatnot away).
 *
 * @author Terry Huang
 */

public class BBCPreprocessor implements Preprocessor {
        
    private static final Logger LOGGER = 
        Logger.getLogger(BBCPreprocessor.class.getName());


    public BBCPreprocessor() {
	
    }

    
    
    public void preprocess () {


    }
}