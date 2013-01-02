package lemur.util
import com.cybozu.labs.langdetect.DetectorFactory

class LangDetect(profileDir: String) {

  DetectorFactory.loadProfile(profileDir);

  def createDetector() = {
    DetectorFactory.create();
  }

}