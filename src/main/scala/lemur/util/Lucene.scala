package lemur.util

import java.io.File
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.standard.StandardAnalyzer

object Lucene {
    def getWriter(indexPath: String) = {
      val dir = FSDirectory.open(new File(indexPath));
      
      val analyzer = new StandardAnalyzer(Version.LUCENE_40);
      val iwc = new IndexWriterConfig(Version.LUCENE_40, analyzer);      
      val writer = new IndexWriter(dir, iwc);
      
      writer      
    }
}