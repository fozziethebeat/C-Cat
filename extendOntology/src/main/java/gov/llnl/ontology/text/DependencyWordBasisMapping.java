package gov.llnl.ontology.text;

import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dv.DependencyPathBasisMapping;


/**
 * @author Keith Stevens
 */
public class DependencyWordBasisMapping
        extends AbstractBasisMapping<DependencyPath, String>
        implements DependencyPathBasisMapping {

    public int getDimension(DependencyPath path) {
        return getDimensionInternal(path.last().word());
    }
}

