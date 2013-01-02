
package lemur.util;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentType;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

object ArgParse {

    /**
     *
     */
    class DateType(format: String) extends ArgumentType[Date] {

        val dateFormat = new SimpleDateFormat(format)

        def convert(parser: ArgumentParser, arg: Argument, value: String): Date = {
            try {
                dateFormat.parse(value)
            } catch {
                case (e: ParseException) => {
                    val msg = String.format("Incorrect date format: '%s'", value);
                    throw new ArgumentParserException(msg, parser, arg)
                }
            }
        }
    }
}
