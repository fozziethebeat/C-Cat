import scala.collection.mutable.HashMap
import scala.collection.JavaConversions.asScalaSet
import scala.collection.JavaConversions.asScalaBuffer
import scala.io.Source
import scala.util.matching.Regex

import gov.llnl.ontology.wordnet.OntologyReader
import gov.llnl.ontology.wordnet.WordNetCorpusReader
import gov.llnl.ontology.wordnet.Synset

object ParseWikipedia {

    /**
      * Returns a set of raw strings without any extra wikipedia title
      * characters such as parens, quotes, or dashes.
      */
    def splitPhrase(phrase: String) = 
        phrase.replaceAll("[\\-_\\(\\)/\\\\,']", " ").trim.toLowerCase.split("\\s+").toSet

    /**
      * Returns a tuple where the first item is the title of a wiki page and the
      * second is a set of disambiguation tokens.  This is used when parsing the
      * full page information file.
      */
    def pageNameSplit(items: Array[String], map: HashMap[String, (String, Set[String])]) =
        if (items(1) == "0") {
            val terms = items(2).replaceAll("[\\-_/\\\\,']", " ").trim.toLowerCase.split("\\(", 2)
            (terms(0).trim, if (terms.length == 2) splitPhrase(terms(1)) else Set[String]())
        } else 
            ("", Set[String]())

    /**
      * Returns a tuple where the first item is the title of a wiki page and the
      * second is a set of disambiguation tokens. This is used when parsing the
      * category information file.
      */
    def categorySplit(items: Array[String], map: HashMap[String, (String, Set[String])]) =
        if (map.contains(items(0))) {
            val t = map(items(0))
            (t._1, t._2 ++ splitPhrase(items(1))) 
        } else
            ("", Set[String]())

    /**
      * Returns a tuple where the first item is the title of a wiki page and the
      * second is a set of disambiguation tokens.  This is used when parsing the
      * page link information file.
      */
    def pageLinkSplit(items: Array[String], map: HashMap[String, (String, Set[String])]) =
        if (items(1) == "0" && map.contains(items(0))) {
            val t = map(items(0))
            (t._1, t._2 ++ splitPhrase(items(2))) 
        } else
            ("", Set[String]())

    /**
      * Returns true if the line starts with "INSERT", i.e. it is the start of a
      * line with informative wiki data.
      */
    def valid(line: String) = line.startsWith("INSERT")

    def addToPageMap(
            pageMap: HashMap[String, (String, Set[String])],
            reg: Regex,
            file: String,
            wnSet: Set[String],
            f: (Array[String], HashMap[String, (String, Set[String])]) => (String, Set[String])) {
        var i = 0
        val pageFile = Source.fromFile(file, "latin1")
        for (line <- pageFile.getLines) {
            val entries = if (valid(line)) line.split("\\s+", 5)(4) else ""

            if (entries == "") i += 0 else i += 1

            for (m <- reg.findAllIn(entries)) {
                val items = m.substring(1, m.length-1).split(",")
                val termSet = f(items, pageMap)
                if (termSet._1 != "")
                    pageMap(items(0)) = (termSet._1,
                                         termSet._2.intersect(wnSet)) 
            }
        }
    }

    def makePageToIdMap(file: String, reg: Regex) : Map[String, String] = {
        var i = 0
        var titleMap = Map[String, String]()
        val pageFile = Source.fromFile(file, "latin1")
        for (line <- pageFile.getLines) {
            val entries = if (valid(line)) line.split("\\s+", 5)(4) else ""

            if (entries == "") i += 0 else i += 1

            for (m <- reg.findAllIn(entries)) {
                val items = m.substring(1, m.length-1).split(",")
                titleMap += (items(2) -> items(0))
            }
        }
        titleMap
    }

    def buildWordNetContexts(reader: OntologyReader) = {
        // Now build a disambiguation context for each synset.  This context
        // includes the synset's lemmas, the lemmas for each parent, the lemmas
        // for each sibling synset, and the terms in the synset's gloss.
        var synsetMap = Map[Synset, Set[String]]()
        var wnSet = Set[String]()
        for (synset <- reader.allSynsets) {
            var context = Set[String]()

            // Traverse each parent this synset has and add the parent's lemmas.
            // also traverse each child of the parent, i.e the sisters of the
            // current synset, and add the lemmas of those synsets (this will
            // automatically include the lemmas of the current synset).
            for (parent <- synset.getParents) {
                // Handle the parent lemmas.
                for (lemma <- parent.getLemmas)
                    context += lemma.getLemmaName

                // Handle the child lemmas.
                for (child <- parent.getChildren)
                    for (lemma <- child.getLemmas)
                        context += lemma.getLemmaName
            }

            // Now add each term in the gloss
            for (glossTerm <- synset.getDefinition.split("\\s+"))
                context += glossTerm
            synsetMap += (synset -> context)
            wnSet = wnSet ++ context
        }
        (synsetMap, wnSet)
    }

    def main(args: Array[String]) {
        // Create a mapping from wiki page titles to it's related word sense in
        // wordnet.
        var wordnetMap = Map[String, String]()

        // Load up the wordnet hierarchy
        val reader = WordNetCorpusReader.initialize(args(3))

        // Get the context for each synset and also the entire set of possible
        // terms found in wordnet.  We will use this second set to filter terms
        // from wikipedia since that token set is likely to be significantly
        // larger.
        var(synsetMap, wnSet) = buildWordNetContexts(reader)
        println("Loaded sysnet context map")

        val innerMatch = ",.+?"
        // First, load up the mapping from page id's to it's title and it's
        // disambiguation context.
        val pageMap = new HashMap[String, (String, Set[String])]
        val pageReg = new Regex("\\(.+?"+innerMatch*10+"\\)")
        val categoryReg = new Regex("\\(.+?"+innerMatch*3+"\\)")
        val linkReg = new Regex("\\(.+?,.+?,'.+?'\\)")
        addToPageMap(pageMap, pageReg, args(0), wnSet, pageNameSplit)
        println("Loaded page titles")
        addToPageMap(pageMap, categoryReg, args(1), wnSet, categorySplit)
        println("Loaded page categories")
        addToPageMap(pageMap, linkReg, args(2), wnSet, pageLinkSplit)
        println("Loaded page links")

         // Next, pick out the monosemous wikipedia titles.
        var poly = Set[String]()
        var mono = Set[String]()
        for ((id, (title, terms)) <- pageMap)
            if (mono.contains(title)) {
                mono -= title
                poly += title
            } else
                mono += title
        println("Selected monosemous titles")

        // Iterate through each page we care about.  If the page is in the
        // wiki monosemous list, look for it's synsets in wordnet and assign it
        // a sense if there is only one wordnet sense.
        for ((id, (title, terms)) <- pageMap) {
            val synsets = reader.getSynsets(title)
            if (synsets.length == 1)
                wordnetMap += (id -> synsets(0).getName)
        }
        println("disambiguated monosemous titles")

        // Now traverse each wiki page that does not have a synset mapping and
        // compute the similarity between the page's context and each sense's
        // context.  
        for ((id, (title, pageContext)) <- pageMap) {
            // Get the synsets.
            val synsets = reader.getSynsets(title)
            // Skip any pages that have no senses
            if (synsets.length != 0 && !wordnetMap.contains(title)) {
                var bestScore = 0
                var bestSense = synsets(0)
                for (synset <- synsets) {
                    // Get the synset context and compare the similarity.
                    val s = sim(synsetMap(synset), pageContext)
                    if (s > bestScore) {
                        bestScore = s
                        bestSense = synset
                    }
                }
                // Add the mapping from the id to the sense.
                wordnetMap += (id -> bestSense.getName)
            }
        }
        println("Computed the sense for every page")

        // Read in a mapping from the raw document titles to it's wiki page ids.
        // We need this in order for when we re-read the page link file and
        // output a link between two synsets.
        val titleMap = makePageToIdMap(args(0), pageReg)
        // Now print out the links between each disambiguated wiki page.
        printWikiLinks(args(2), titleMap, wordnetMap, linkReg)
    }

    def sim(wiki: Set[String], synset: Set[String]) = 
        wiki.intersect(synset).size + 1

    def printWikiLinks(pageLinkFile: String,
                       titleMap: Map[String, String],
                       wordnetMap: Map[String, String],
                       reg: Regex) {
        var i = 0
        val pageFile = Source.fromFile(pageLinkFile, "latin1")
        for (line <- pageFile.getLines) {
            val entries = if (valid(line)) line.split("\\s+", 5)(4) else ""

            if (entries == "") i += 0 else i += 1

            for (m <- reg.findAllIn(entries)) {
                val e = m.substring(1, m.length-1).split(",")
                val Array(id, b, toTitle) = m.substring(1, m.length-1).split(",", 3)
                val fromSynset = wordnetMap.getOrElse(id, "")
                val toSynset = wordnetMap.getOrElse(titleMap.getOrElse(toTitle, ""), "")
                if (fromSynset != "" && toSynset != "")
                    println(fromSynset + " -> " + toSynset)
            }
        }
    }
}
