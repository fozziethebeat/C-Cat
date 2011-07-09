

package gov.llnl.ontology.wordnet;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.*;


/**
 * @author Keith Stevens
 */
public class BaseSimpleRelatedForm {

    @Test public void testBasicGetters() {
        RelatedForm form = new SimpleRelatedForm(0, 1);
        assertEquals(0, form.sourceIndex());
        assertEquals(1, form.otherIndex());
    }
}
