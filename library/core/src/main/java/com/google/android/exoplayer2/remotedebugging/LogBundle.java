package com.google.android.exoplayer2.remotedebugging;

import android.util.JsonWriter;

import com.redbeemedia.playersapplog.log.ILog;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class LogBundle implements ILog {
    private final String logType;
    private List<LogEntry> logEntries = new ArrayList<>();

    public LogBundle(String logType) {
        this.logType = logType;
    }

    @Override
    public String getLogType() {
        return logType;
    }

    public void addEntry(long timestamp, String tag, String message) {
        logEntries.add(new LogEntry(timestamp, tag, message));
    }

    @Override
    public void appendTo(StringBuilder stringBuilder) {
        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        try {
            addObject(jsonWriter, body -> {
                addString(body, "logProtocol","sl_v1");
                addArray(body, "entries", entries -> {
                    for(LogEntry logEntry : logEntries) {
                        addObject(entries, entry -> {
                            addNumber(entry, "timestamp", logEntry.timestamp);
                            addString(entry, "tag", logEntry.tag);
                            addString(entry, "message", logEntry.message);
                        });
                    }
                });
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        stringBuilder.append(stringWriter.getBuffer().toString());
    }

    public int getEntriesSize() {
        return logEntries.size();
    }

    private static class LogEntry {
        private final long timestamp;
        private final String tag;
        private final String message;

        public LogEntry(long timestamp, String tag, String message) {
            this.timestamp = timestamp;
            this.tag = tag;
            this.message = message;
        }
    }


    private static void addObject(JsonWriter jsonWriter, IJsonBody body) throws IOException {
        jsonWriter.beginObject();
        body.apply(jsonWriter);
        jsonWriter.endObject();
    }


    private static void addArray(JsonWriter jsonWriter, String name, IJsonArrayBuilder arrayBuilder) throws IOException {
        jsonWriter.name(name);
        addArray(jsonWriter, arrayBuilder);
    }
    private static void addArray(JsonWriter jsonWriter, IJsonArrayBuilder arrayBuilder) throws IOException {
        jsonWriter.beginArray();
        arrayBuilder.addItems(jsonWriter);
        jsonWriter.endArray();
    }

    private static void addString(JsonWriter jsonWriter, String name, String value) throws IOException {
        jsonWriter.name(name);
        jsonWriter.value(value);
    }

    private static void addNumber(JsonWriter jsonWriter, String name, Number value) throws IOException {
        jsonWriter.name(name);
        jsonWriter.value(value);
    }

    private interface IJsonBody {
        void apply(JsonWriter jsonWriter) throws IOException;
    }

    private interface IJsonArrayBuilder {
        void addItems(JsonWriter jsonWriter) throws IOException;
    }
}
