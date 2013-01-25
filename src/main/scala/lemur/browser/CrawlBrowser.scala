
package lemur.browser;

import java.io.File
import java.util.Date
import scala.Array.fallbackCanBuildFrom
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.TraversableOnce.flattenTraversableOnce
import scala.util.Random
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.IndexWriter
import org.jwat.warc.WarcRecord
import com.cybozu.labs.langdetect.LangDetectException
import lemur.util.ArgParse
import lemur.util.LangDetect
import lemur.util.Lucene
import lemur.util.Warc
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.impl.Arguments
import org.slf4j.LoggerFactory
import lemur.util.ProgressIterator
import org.apache.commons.io.IOUtils

object CrawlBrowser {

  val logger = LoggerFactory.getLogger("CrawlBrowser")
  
  var sampleSize = 1.0f
  var indexWriter: IndexWriter = null
  var crawlStats: CrawlStats = null
  var langDetect: LangDetect = null
  var defaultLang: String = null
  
  var reportEvery = 1000

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

  def randomSample(sampleSize: Float, item: Any) = {
    Random.nextFloat <= sampleSize
  }

  /**
   * Creates a Response object from a WarcRecord.
   *
   * This method extracts the HTML document and creates a text based version of it.
   *
   */
  def extractResponse(record: WarcRecord): Option[Response] = {
    try {
      val (headers, content) = Warc.parseResponse(record)

      // Quick and dirty HTML parsing
      val html = IOUtils.toString(content)
      val text = html.replaceAll("<[^>]*>", "")

      Some(new Response(record, headers, text))
    } catch {
      case e: Exception => {
    	  logger.debug("Error parsing record: ", e)
    	  None 
      }
    }
  }

  /**
   * Determines whether a Response object is included in the index or not.
   */
  def filterResponse(response: Response): Boolean = {
    val detector = langDetect.createDetector()
    detector.append(response.text.substring(0, math.min(response.text.length(), 1000)))
    try {

      val lang = detector.detect()
      return lang == defaultLang
    } catch {
      case e: LangDetectException => {
        return false
      }
    }
  }

  // WARC headers excluded from the index
  val excludedHeaders = "WARC-Target-URI" :: "WARC-IP-Address" ::
    "Content-Type" :: "WARC-Type" :: "WARC-Payload-Digest" :: Nil

  /**
   * Adds a response object to the Lucene index
   */
  def indexResponse(response: Response) {
    val doc = new Document()
    doc.add(new StringField("url", response.uri.toASCIIString, Field.Store.YES))

    val headers = for (
      head <- response.record.getHeaderList if !excludedHeaders.contains(head.name)
    ) yield head

    // Add a new field for each HTTP header
    headers.foreach(header => {
      doc.add(new StringField(header.name, header.value, Field.Store.YES))
    })

    // Add the full text as 'body' field
    doc.add(new TextField("body", response.text, Field.Store.NO))
    indexWriter.addDocument(doc)
  }

  def progress(iters: Int, tGap: Long, tTotal: Long) {
    logger.info("Processed records: %d".format(iters))
  }
  
  def processFiles(files: Seq[File]) {
    val listRecords = for (file <- files.iterator) yield Warc.readResponses(file)
    val records = listRecords.flatten

    // Parse the responses 
    val responsesAll = records.flatMap(extractResponse)
    //Filter them by language
    val responsesEng = responsesAll.filter(filterResponse)

    // Sample the responses
    val sampledResponses = responsesEng.filter(randomSample(sampleSize, _))
    
    var nResp = 0
    
    val responsesProgress = new ProgressIterator(progress, sampledResponses).every(reportEvery)
    
    responsesProgress.foreach(resp => {
      // Add them to the Lucene Index    
      indexResponse(resp)

      // Register the statistics
      crawlStats.addResponse(resp)
      
      nResp += 1
    })
    logger.info("Finished. Responses=%s".format(nResp))
  }

  def main(args: Array[String]) {
    
    // Argument Parser
    val parser = ArgumentParsers.newArgumentParser("CrawlBrowser");
    parser.addArgument("--lang")
      .help("Select only the documents written in this language. 'en', by default.")
    parser.addArgument("--sample").`type`(classOf[java.lang.Float]).help("Sample size [0,1]")
    parser.addArgument("--every").`type`(classOf[java.lang.Integer]).help("Log every N records")
    parser.addArgument("--max-date").`type`(new ArgParse.DateType("yyyy-MM-dd"))
    parser.addArgument("--min-date").`type`(new ArgParse.DateType("yyyy-MM-dd"))
    parser.addArgument("profileDir").help("Language detection profiles directory")
    parser.addArgument("outputDir").`type`(Arguments.fileType().verifyIsDirectory())
      .help("Output directory")
    parser.addArgument("inputDir").`type`(Arguments.fileType().verifyIsDirectory())
      .nargs("+").help("Input directories containing WARC files")

    val ns = parser.parseArgsOrFail(args);

    val minDate = ns.get("min_date").asInstanceOf[Date]
    val maxDate = ns.get("max_date").asInstanceOf[Date]

    val minLastModified = if (minDate != null) minDate.getTime else 0
    val maxLastModified = if (maxDate != null) maxDate.getTime else Long.MaxValue

    val profileDir = ns.getString("profileDir")

    val outputDir = ns.get("outputDir").asInstanceOf[File]

    val indexPath = new File(outputDir, "index")
    val statsPath = new File(outputDir, "stats.json")

    val inputDirs = ns.getList("inputDir").asInstanceOf[java.util.List[File]]
    val inputFiles = findFiles(inputDirs, minLastModified, maxLastModified)

    reportEvery = if (ns.get("every") != null) ns.getInt("every") else reportEvery
    sampleSize = if (ns.get("sample") != null) ns.getFloat("sample") else sampleSize
    defaultLang = if (ns.get("lang") != null) ns.getString("lang") else "en"
    langDetect = new LangDetect(profileDir)
    indexWriter = Lucene.getWriter(indexPath)
    crawlStats = new CrawlStats(statsPath)

    //Thread.sleep(5000)
    
    processFiles(inputFiles)
    indexWriter.close()
    crawlStats.save()
  }
}

