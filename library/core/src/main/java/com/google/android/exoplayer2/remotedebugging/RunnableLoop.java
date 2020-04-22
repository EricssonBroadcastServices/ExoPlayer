package com.google.android.exoplayer2.remotedebugging;

import android.os.SystemClock;

public class RunnableLoop implements Runnable {
    private final long minSleep; // minSleep <= nextTask.start-task.end
    private final long targetDelay; // targetDelay = nextTask.start - task.start
    private final Runnable runnable;
    private long lastStartTime = -1;
    private Thread thread = null;

    public RunnableLoop(long minSleep, long targetDelay, Runnable runnable) {
        this.minSleep = minSleep;
        this.targetDelay = targetDelay;
        this.runnable = runnable;
    }

    @Override
    public void run() {
        thread = Thread.currentThread();
        while(!Thread.interrupted()) {
            try {
                long now = getTimeMillis();
                if(lastStartTime != -1) {
                    long sleepTime = targetDelay-(now-lastStartTime);
                    sleepTime = Math.max(minSleep, sleepTime);
                    Thread.sleep(sleepTime);
                }
                lastStartTime = getTimeMillis();
                try {
                    runnable.run();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected long getTimeMillis() {
        return SystemClock.uptimeMillis();
    }

    public void stop() {
        if(thread == null) {
            throw new RuntimeException("Not started!");
        } else {
            thread.interrupt();
        }
    }
}
