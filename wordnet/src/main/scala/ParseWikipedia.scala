import scala.collection.mutable.HashMap
import scala.io.Source
import scala.util.matching.Regex

object ParseWikipedia {

    def splitPhrase(phrase: String) = 
        phrase.replaceAll("[\\-_\\(\\)/\\\\,']", " ").trim.toLowerCase.split("\\s+").toSet

    def pageNameSplit(items: Array[String], map: HashMap[String, Set[String]]) =
        if (items(1) == "0") splitPhrase(items(2)) else Set[String]()

    def categorySplit(items: Array[String], map: HashMap[String, Set[String]]) =
        if (map.contains(items(0)))
            map(items(0)) ++ splitPhrase(items(1)) 
        else
            Set[String]()

    def pageLinkSplit(items: Array[String], map: HashMap[String, Set[String]]) =
        if (items(1) == "0" && map.contains(items(0))) 
            map(items(0)) ++ splitPhrase(items(2))
        else
            Set[String]()

    def valid(line: String) = line.startsWith("INSERT")

    def addToPageMap(
            pageMap: HashMap[String, Set[String]],
            reg: Regex,
            file: String,
            f: (Array[String], HashMap[String, Set[String]]) => Set[String]) {
        var i = 0
        val pageFile = Source.fromFile(file)
        for (line <- pageFile.getLines) {
            if (i == 3)
                return

            val entries = if (valid(line)) line.split("\\s+", 5)(4) else ""

            if (entries == "") i += 0 else i += 1

            for (m <- reg.findAllIn(entries)) {
                val items = m.substring(1, m.length-1).split(",")
                val termSet = f(items, pageMap)
                if (!termSet.isEmpty)
                    pageMap(items(0)) = termSet
            }
        }
    }

    def main(args: Array[String]) {
        val innerMatch = ",.+?"
        val pageMap = new HashMap[String, Set[String]]
        addToPageMap(pageMap, new Regex("\\(.+?"+innerMatch*10+"\\)"),
                     args(0), pageNameSplit)
        addToPageMap(pageMap, new Regex("\\(.+?"+innerMatch*3+"\\)"),
                     args(1), categorySplit)
        addToPageMap(pageMap, new Regex("\\(.+?"+innerMatch*3+"\\)"),
                     args(2), pageLinkSplit)
        for ((id, terms) <- pageMap)
            println(id + " : " + terms)
    }
}
