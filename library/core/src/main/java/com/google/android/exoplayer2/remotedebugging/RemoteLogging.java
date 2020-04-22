package com.google.android.exoplayer2.remotedebugging;

import com.redbeemedia.playersapplog.GlobalAppLogger;
import com.redbeemedia.playersapplog.log.ILog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteLogging {
    private static final String CAT_ABR = "abr";
//    private static final String CAT_DEBUG = "debug";
    private static final Map<String, LogBundle> bundles = new HashMap<>();

    static {
        bundles.put(CAT_ABR, new LogBundle("abr_metering"));
//        bundles.put(CAT_DEBUG, new LogBundle("debug"));
    }

    private static List<ILog> harvestLogs(boolean sendEmpty) {
        Map<String,LogBundle> harvestedBundles = new HashMap<>();
        synchronized (bundles) {
            harvestedBundles.putAll(bundles);
            bundles.clear();
            for(Map.Entry<String, LogBundle> entry : harvestedBundles.entrySet()) {
                bundles.put(entry.getKey(), new LogBundle(entry.getValue().getLogType()));
            }
        }
        List<ILog> logs = new ArrayList<>();
        for(LogBundle logBundle : harvestedBundles.values()) {
            if(sendEmpty || (logBundle.getEntriesSize() > 0)) {
                logs.add(logBundle);
            }
        }
        return logs;
    }

    public static void sendAllLogs(boolean sendEmpty) {
        List<ILog> logs = harvestLogs(sendEmpty);
        Exception exception = null;
        for(ILog log : logs) {
            try {
                GlobalAppLogger.get().sendLog(log);
            } catch (Exception e) {
                if(exception == null) {
                    exception = e;
                } else {
                    exception.addSuppressed(e);
                }
            }
        }
        if(exception != null) {
            throw new RuntimeException(exception);
        }
    }

    public static RunnableLoop createSendLogLoop() {
        return new RunnableLoop(1000L * 10L, 1000L * 60L * 5L, () -> sendAllLogs(false));
    }

    private static long getTimestamp() {
        return System.currentTimeMillis();
    }

    public static ILogCategory getAbrMetering() {
        return new LogCategory(CAT_ABR);
    }

    private static class LogCategory implements ILogCategory {
        private final String logName;

        public LogCategory(String logName) {
            this.logName = logName;
        }

        @Override
        public void log(String tag, String message) {
            long timestamp = getTimestamp();
            synchronized (bundles) {
                bundles.get(logName).addEntry(timestamp, tag, message);
            }
        }
    }
}
