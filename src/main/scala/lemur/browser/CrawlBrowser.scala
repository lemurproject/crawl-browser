
package lemur.browser;

import scala.collection.JavaConversions._
import java.io.File
import java.util.Date
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.impl.Arguments
import net.sourceforge.argparse4j.inf.ArgumentParser
import net.sourceforge.argparse4j.inf.ArgumentParserException
import net.sourceforge.argparse4j.inf.Namespace
import lemur.util.ArgParse
import lemur.util.Warc
import org.jwat.warc.WarcRecord
import de.l3s.boilerpipe.extractors.ArticleExtractor
import net.htmlparser.jericho.Source
import net.htmlparser.jericho.TextExtractor
import lemur.util.LangDetect
import com.cybozu.labs.langdetect.Detector
import lemur.util.Lucene
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.document.Document
import org.apache.lucene.document.TextField
import org.apache.lucene.document.StringField
import org.apache.lucene.document.Field
import org.jwat.warc.WarcHeader
import net.htmlparser.jericho.Config
import net.htmlparser.jericho.LoggerProvider

object CrawlBrowser {
  
  var indexWriter: IndexWriter = null  
  var langDetect: LangDetect = null 
  var defaultLang: String = null

  class Response(
      val record: WarcRecord, val headers: Warc.HttpHeaders,
      val source: Source, val text: String) {
  }
  
  /**
   * Finds the files in the given directory that have been modified within a range
   */
  def findFiles(inputDir: File, minLastModified: Long, maxLastModified: Long): Seq[File] = {
    for (
      file <- inputDir.listFiles;
      if file.lastModified >= minLastModified;
      if file.lastModified <= maxLastModified
    ) yield file
  }

  /**
   * Finds the files in the given directories, that have been modified within a range.
   */
  def findFiles(inputDirs: Seq[File], minLastModified: Long, maxLastModified: Long): Seq[File] = {
    val fileLists = for (
      path <- inputDirs; dir = path
    ) yield findFiles(dir, minLastModified, maxLastModified)

    fileLists.flatten
  }

  /**
   * Creates a Response object from a WarcRecord. 
   * 
   * This method extracts the HTML document and creates a text based version of it.
   * 
   */
  def extractResponse(record: WarcRecord) = {
    val (headers, content) = Warc.parseResponse(record)
    val source = new Source(content)
    val extractor = new TextExtractor(source)
    val text = extractor.toString()
    
    new Response(record, headers, source, text)
  }
  
  /**
   * Determines whether a Response object is included in the index or not.
   */
  def filterResponse(response: Response) = {
    val detector = langDetect.createDetector()
    detector.append(response.text)
    val lang = detector.detect()
    lang == defaultLang
  }
  
  /**
   * Adds a response object to the index
   */
  def indexResponse(response: Response){
    val headerUri = response.record.getHeader("WARC-Target-URI")    
    val url = if (headerUri != null) headerUri.value else "";
    
    val doc = new Document()    
    doc.add(new StringField("url", url, Field.Store.YES))
    
    response.record.getHeaderList().foreach(header => {
      doc.add(new StringField(header.name, header.value, Field.Store.YES))     
    })
    
    doc.add(new TextField("body", response.text, Field.Store.NO))    
    indexWriter.addDocument(doc)
  }
  
  def processFiles(files: Seq[File]) {
    val listRecords = for (file <- files.iterator) yield Warc.readResponses(file)
    val records = listRecords.flatten

    // Parse the responses 
    val responses = records.map(extractResponse)

    // Filter by language
    val filtered = responses.filter(filterResponse)
    
    // Add them to the Lucene Index    
    filtered.foreach(indexResponse)
  }

  def main(args: Array[String]) {
    val parser = ArgumentParsers.newArgumentParser("CrawlBrowser");
    parser.addArgument("--lang")
    parser.addArgument("--max-date").`type`(new ArgParse.DateType("yyyy-MM-dd"))
    parser.addArgument("--min-date").`type`(new ArgParse.DateType("yyyy-MM-dd"))
    parser.addArgument("profileDir")
    parser.addArgument("indexPath")
    parser.addArgument("inputDir").nargs("+").`type`(Arguments.fileType().verifyIsDirectory())

    val ns = parser.parseArgsOrFail(args);

    val minDate = ns.get("min_date").asInstanceOf[Date]
    val maxDate = ns.get("max_date").asInstanceOf[Date]

    val minLastModified = if (minDate != null) minDate.getTime else 0
    val maxLastModified = if (maxDate != null) maxDate.getTime else Long.MaxValue

    val profileDir = ns.getString("profileDir")
    val indexPath = ns.getString("indexPath")
    val inputDirs = ns.getList("inputDir").asInstanceOf[java.util.List[File]]
    val inputFiles = findFiles(inputDirs, minLastModified, maxLastModified)

    //Global settings
    Config.LoggerProvider = LoggerProvider.DISABLED
    
    defaultLang = if (ns.getString("lang") != null) ns.getString("lang") else "en"
    langDetect = new LangDetect(profileDir)    
    indexWriter = Lucene.getWriter(indexPath)
    
    processFiles(inputFiles)
    indexWriter.close()    
  }
}

