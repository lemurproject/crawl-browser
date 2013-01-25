package lemur.browser

import java.io.File
import java.io.FileWriter
import java.net.URI
import java.text.SimpleDateFormat
import com.google.gson.Gson
import java.io.FileReader
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

/**
 * Collects the following statistics about the crawl.
 * 
 * - Number of documents
 * - Number of documents by status codes
 * - Number of documents by hosts
 * - Number of documents by day 
 * 
 */
class CrawlStats(path: File) {

  val data = new HashMap[String, Int]()
  
  if (path.exists())
    load()
    
  def load() {
	  val gson = new Gson()
	  val reader = new FileReader(path)
	  val loadedData = gson.fromJson(reader, classOf[java.util.Map[String, Int]])
	  reader.close()
	  data ++= loadedData
  }
  
  def inc(group: String, item: String) {
    val key = group + ":" + item
    data.put(key, data.getOrElse(key, 0) + 1)
  }
  
  def addResponse(response: Response) {
    
    inc("general", "responses")
    
    val dateMonth = CrawlStats.dateFmtMonth.format(response.date)
    val dateDay = CrawlStats.dateFmtDay.format(response.date)
    inc("date-month", dateMonth)
    inc("date-day", dateDay)

    val domain = CrawlStats.domain(response.uri)
    inc("domain", domain)
  }

  def save() {
      val gson = new Gson()
      val writer = new FileWriter(path)
      
      val jMap = mutableMapAsJavaMap(data)
	  gson.toJson(jMap, writer)
	  writer.close()
  }
}

object CrawlStats {
  
  // Static instance of the date formatter
  val dateFmtMonth = new SimpleDateFormat("yyyy-MM")
  val dateFmtDay = new SimpleDateFormat("yyyy-MM-dd")
  
  def apply(path: File) {
    if (!path.exists()){
      path.mkdirs()
    }
    new CrawlStats(path)
  }
  
  /**
   * Extracts the 'domain' part of a URL
   */
  def domain(uri: URI): String = {
    val host = uri.getHost()
   
    val lastDot = host.lastIndexOf('.')
    if (lastDot == -1) {
      return host
    }
    val prevDot = host.lastIndexOf('.', lastDot - 1)
    if (prevDot == -1) {
      return host
    }
    return host.substring(prevDot + 1, host.length())
  }
  
}