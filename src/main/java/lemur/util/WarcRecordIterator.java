package lemur.util;

import java.io.IOException;
import java.io.InputStream;

import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

/**
 *
 */
public class WarcRecordIterator extends AbstractIterator<WarcRecord> {

	private WarcReader reader;

    public WarcRecordIterator(InputStream inStream) throws IOException {
        this.reader = WarcReaderFactory.getReader(inStream);
    }
    
    @Override
    protected WarcRecord computeNext() {
        try {
            WarcRecord record = reader.getNextRecord();
            if (record != null) {
                return record;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return endOfData();
    }
}
