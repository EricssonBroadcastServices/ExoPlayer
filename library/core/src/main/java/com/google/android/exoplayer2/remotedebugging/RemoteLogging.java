package com.google.android.exoplayer2.remotedebugging;

import com.redbeemedia.playersapplog.GlobalAppLogger;
import com.redbeemedia.playersapplog.log.ILog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Users need to: <br/>
 * 1. Call sendAllLogs when app exits <br/>
 * 2. Call sendAllLogs when an exception happens <br/>
 * 3. Start a thread that runs result of createSendLogLoop() and shuts it down (by calling RunnableLoop::stop()) when app exists </br>
 */
public class RemoteLogging {
    private static final String CAT_ABR = "abr";
    private static final String CAT_DEBUG = "debug";
    private static final String CAT_BANDWIDTH = "bandwidth";
    private static final Map<String, LogBundle> bundles = new HashMap<>();

    static {
        bundles.put(CAT_ABR, new LogBundle("abr_metering"));
        bundles.put(CAT_DEBUG, new LogBundle("debug"));
        bundles.put(CAT_BANDWIDTH, new LogBundle("bandwidth_metering"));
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

    public static ILogCategory getDebug() {
        return new LogCategory(CAT_DEBUG);
    }

    public static ILogCategory getBandwidthMetering() {
        return new LogCategory(CAT_BANDWIDTH);
    }

    private static class LogCategory implements ILogCategory {
        private final String logName;

        public LogCategory(String logName) {
            this.logName = logName;
        }

        @Override
        public void log(String tag, String message) {
            // TODO Matte, this is leaking memory
            //long timestamp = getTimestamp();
            //synchronized (bundles) {
                //bundles.get(logName).addEntry(timestamp, tag, message);
            //}
        }
    }

    /**
     *  logProtocol: "sl_v1", //simple log v1
     *  entries: [
     *      {
     *          timestamp: ${millis}
     *          tag: ${a tag}
     *          message: $[logMessage}
     *      },...
     *  ]
     * }
     */
}
