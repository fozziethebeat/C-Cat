import gov.llnl.ontology.util.Counter;

import scala.collection.JavaConversions.asScalaSet
import scala.io.Source


val wsdKeys = args.slice(2, args.length).map(Source.fromFile(_).getLines)
val numLines = args(0).toInt
val numAgreements = args(1).toInt
for (l <- 0 until numLines) {
    val counter = new Counter[String]()
    val senses = wsdKeys.foreach( lines => counter.count(lines.next) )
    for (sense <- counter.items)
        if (counter.getCount(sense) == numAgreements)
            println(sense)
}
