package org.peercast.core;

import android.app.Application;
import android.util.Log;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * (c) 2015, T Yoshizawa
 *
 * Dual licensed under the MIT or GPL licenses.
 */
public class PeerCastApplication extends Application {
    private static final String TAG = "PeerCastApplication";
    private static final int MAX_LOG_SIZE = 128 * 1024;

    @Override
    public void onCreate() {
        super.onCreate();
        installLogHandlers();
    }

    private void installLogHandlers(){
//        org.seamless.util.logging.LoggingUtil.resetRootHandler(
//                new FixedAndroidLogHandler()
//        );
        Logger appLogger = Logger.getLogger(getPackageName());
        appLogger.setLevel(Level.FINEST);

        try {
            Logger rootLogger = Logger.getLogger("");
            rootLogger.setLevel(Level.INFO);

            String pat = getCacheDir().getAbsolutePath() + "/app%g.log";
            FileHandler handler = new FileHandler(pat, MAX_LOG_SIZE, 1, true);

            handler.setFormatter(new CsvFormatter());
            rootLogger.addHandler(handler);
        } catch (IOException e){
            Log.e(TAG, "new FileHandler()", e);
        }
    }

    public Collection<File> getLogFiles() {
        return FileUtils.listFiles(
                getCacheDir(), new String[]{"log"}, false);
    }

    /**
     * ログをCSVとして保存する。<br>
     * date, sourceClass, sourceMethod, levelInt, message, stackTrace
     * */
    static class CsvFormatter extends Formatter {

        private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SS");

        @Override
        public String format(LogRecord r) {
            long time = r.getMillis();
            Throwable th = r.getThrown();

            StringWriter out = new StringWriter(256);
            try {
                new CSVPrinter(out, CSVFormat.DEFAULT).printRecord(
                        DATE_FORMAT.format(time),
                        r.getSourceClassName(),
                        r.getSourceMethodName(),
                        r.getLevel().intValue(),
                        formatMessage(r),
                        th != null ? stackTrace(th) : ""
                );
                return out.toString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        private String stackTrace(Throwable th) {
            String s = "Throwable occurred: ";

            StringWriter sw = new StringWriter(256);
            PrintWriter pw = new PrintWriter(sw);
            th.printStackTrace(pw);
            s += sw.toString();

            pw.close();
            return s;
        }

    }
}
