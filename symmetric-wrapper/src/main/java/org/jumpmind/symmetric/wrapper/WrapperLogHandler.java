package org.jumpmind.symmetric.wrapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.ErrorManager;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class WrapperLogHandler extends StreamHandler {

    protected String filename;
    protected long maxByteCount;
    protected int maxLogCount;
    protected BufferedWriter writer;
    protected long byteCount;
    protected ScheduledExecutorService executor;
    
    public WrapperLogHandler(String filename, long maxByteCount, int maxLogCount) throws IOException {
        this.filename = filename;
        this.maxByteCount = maxByteCount;
        this.maxLogCount = maxLogCount;
        rotate();
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(new LogFlushTask(), 0, 2000, TimeUnit.MILLISECONDS);
    }

    protected void rotate() throws IOException {
        if (writer != null) {
            writer.close();
        }

        File file = new File(filename);
        byteCount = 0;
        if (file.exists()) {
            byteCount = file.length();
        }

        if (byteCount >= maxByteCount) {
            new File(filename + "." + maxLogCount).delete();
            for (int i = maxLogCount - 1; i > 0; i--) {
                new File(filename + "." + i).renameTo(new File(filename + "." + (i+1)));    
            }
            new File(filename).renameTo(new File(filename + ".1"));
        }

        writer = new BufferedWriter(new FileWriter(file, true), 2048);
    }

    @Override
    public void publish(LogRecord record) {
        if (writer != null) {
            try {
                String msg = getFormatter().format(record);
                try {
                    writer.write(msg);
                    byteCount += msg.length();
                    if (byteCount >= maxByteCount) {
                        rotate();
                    }
                } catch (Exception e) {
                    reportError(null, e, ErrorManager.WRITE_FAILURE);
                }
            } catch (Exception e) {
                reportError(null, e, ErrorManager.FORMAT_FAILURE);
            }
        }
    }

    @Override
    public void flush() {
        if (writer != null) {
            try {
                writer.flush();
            } catch (Exception e) {
                reportError(null, e, ErrorManager.FLUSH_FAILURE);
            }
        }
    }

    @Override
    public void close() throws SecurityException {
        if (writer != null) {
            try {
                writer.close();
                writer = null;
                executor.shutdown();
            } catch (Exception e) {
                reportError(null, e, ErrorManager.CLOSE_FAILURE);
            }
        }
    }

    class LogFlushTask implements Runnable {
        public void run() {
            flush();
        }
    }

}
