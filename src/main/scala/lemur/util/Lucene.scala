package lemur.util

import java.io.File
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.standard.StandardAnalyzer

object Lucene {
    def getWriter(indexPath: String): IndexWriter = {
      getWriter(new File(indexPath))
    }
  
    def getWriter(indexPath: File, ramBuffer: Double = 256.0): IndexWriter = {
      val dir = FSDirectory.open(indexPath);      
      val analyzer = new StandardAnalyzer(Version.LUCENE_40);
      val iwc = new IndexWriterConfig(Version.LUCENE_40, analyzer);
      
      iwc.setRAMBufferSizeMB(ramBuffer)
      val writer = new IndexWriter(dir, iwc);
      
      writer      
    }
}