package lemur.util;

import scala.collection.JavaConversions._
import java.io.File
import java.io.FileInputStream
import java.util.zip.GZIPInputStream
import org.jwat.warc.WarcReader
import org.jwat.warc.WarcReaderFactory
import org.jwat.warc.WarcRecord
import org.jwat.warc.WarcWriterFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import scala.collection.mutable.HashMap

object Warc {

  type HttpHeaders = HashMap[String, String]
  
  def isWarcResponse(rec: WarcRecord): Boolean = {
    val typeHeader = rec.getHeader("WARC-Type")
    return if (typeHeader != null) typeHeader.value == "response" else false;
  }

  /**
   * Returns an iterator with the records of a Warc file
   */
  def readResponses(file: File) = {
    val inStream = new FileInputStream(file)
    val reader = WarcReaderFactory.getReader(inStream)
    val warcIter = reader.iterator()
    val responses = warcIter.filter(isWarcResponse)
    responses
  }
  
  def parseResponse(record: WarcRecord) = {
    val payload = record.getPayloadContent()

    val reader = new BufferedReader(new InputStreamReader(payload))
    
    var inHeader = true
    var line : String = null
    
    val headers = new HashMap[String, String]()
    
    while (inHeader) {
      line = reader.readLine()
      if (line != null){
        line = line.trim()    
        if (line.length > 0) {
          val parts = line.split(":")
          if (parts.length == 2){
            headers.put(parts(0).trim, parts(1).trim)
          }
        }else{
          inHeader=false;
        }
      }else{
          inHeader=false;
      }
    }
    
    (headers, reader)    
  }
}

