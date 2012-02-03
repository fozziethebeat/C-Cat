import scala.xml._


object ExtractSemCorTerms {
    def main(args:Array[String]) {
        val semCor = XML.loadFile(args(0))
        val docName = args(0).split("\\.")(0)
        for ( (s, si) <- (semCor \\ "s") zipWithIndex;
              (n, ni) <- (s \ "_") zipWithIndex ) { 
                val lemma = n \ "@lemma"
                val lexsn = n \ "@lexsn"
                if (lemma.size != 0 && lexsn.size != 0) {
                    printf("%s %s.s%03d.t%03d ", docName, docName, si, ni)
                    println(lemma + "%" + lexsn)
                }
        }
    }
}
