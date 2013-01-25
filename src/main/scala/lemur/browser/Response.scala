package lemur.browser

import lemur.util.Warc
import org.jwat.warc.WarcRecord
import org.jwat.warc.WarcDateParser
import java.net.URI

class Response(
  val record: WarcRecord, val headers: Warc.HttpHeaders, val text: String) {
  
  val requiredHeaders = "WARC-Target-URI" :: "WARC-Date" :: Nil
  val isValid = requiredHeaders.forall(record.getHeader(_) != null)
  require(isValid, "Required headers not present")

  val uri = new URI(record.getHeader("WARC-Target-URI").value)
  val date = WarcDateParser.getDate(record.getHeader("WARC-Date").value)

}
