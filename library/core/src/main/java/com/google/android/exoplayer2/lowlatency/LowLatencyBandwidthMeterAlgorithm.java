package com.google.android.exoplayer2.lowlatency;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.BandwidthMeterAlgorithm;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Clock;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.SlidingPercentile;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

public class LowLatencyBandwidthMeterAlgorithm implements BandwidthMeterAlgorithm, TransferListener {
    private final BandwidthMeterAlgorithm.ListenerEventDispatcher eventDispatcher = new ListenerEventDispatcher();

    private SlidingPercentile slidingPercentile = new SlidingPercentile(2000);
    private long lastByteUpdate = C.TIME_UNSET;
    private Clock clock = Clock.DEFAULT;

    private long totalBytesTransferred;

    @Nullable
    @Override
    public TransferListener getTransferListener() {
        return this;
    }

    @Override
    public void onNetworkTypeChanged(int networkType) {

    }

    @Override
    public boolean addListener(Listener listener) {
        return eventDispatcher.addListener(listener);
    }

    @Override
    public boolean removeListener(Listener listener) {
        return eventDispatcher.removeListener(listener);
    }

    @Override
    public void onTransferInitializing(DataSource source, DataSpec dataSpec, boolean isNetwork) {
    }

    @Override
    public synchronized void onTransferStart(DataSource source, DataSpec dataSpec, boolean isNetwork) {
        if(!isNetwork) {
            return;
        }
        lastByteUpdate = clock.elapsedRealtime();
        totalBytesTransferred = 0;
    }

    @Override
    public synchronized void onBytesTransferred(DataSource source, DataSpec dataSpec, boolean isNetwork, int bytesTransferred) {
        if(!isNetwork) {
            return;
        }
        totalBytesTransferred += bytesTransferred;

        long now = clock.elapsedRealtime();
        float sampleElapsedTimeMs = now - lastByteUpdate;
        if(sampleElapsedTimeMs > 100 || bytesTransferred > 100) {
            float bitsPerSecond = (bytesTransferred * 8000f) / sampleElapsedTimeMs;
            slidingPercentile.addSample((int) Math.sqrt(bytesTransferred), bitsPerSecond);
            long estimate = (long) slidingPercentile.getPercentile(0.5f);
            eventDispatcher.onBandwidthEstimate(estimate);
            Log.d("MATTE_DEBUG", "\testimate: "+estimate);
        } else {
            Log.d("MATTE_DEBUG", "Ignored "+bytesTransferred +" (timediff: "+sampleElapsedTimeMs+")");
        }
        lastByteUpdate = now;
        eventDispatcher.onBandwidthSample(bytesTransferred);


        eventDispatcher.onBandwidthEstimate(20000000L);
        eventDispatcher.onBandwidthSample(bytesTransferred);
    }

    @Override
    public synchronized void onTransferEnd(DataSource source, DataSpec dataSpec, boolean isNetwork) {
        if(!isNetwork) {
            return;
        }
        if(lastByteUpdate == 0) {
            long now = clock.elapsedRealtime();
            long diff = now - lastByteUpdate;
            if(diff > 0) {
                long bitsPerSecond = (totalBytesTransferred * 8000) / diff;

                DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
                DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();

                symbols.setGroupingSeparator(' ');
                formatter.setDecimalFormatSymbols(symbols);

                eventDispatcher.onBandwidthEstimate(bitsPerSecond);
                eventDispatcher.onBandwidthSample(totalBytesTransferred);
            }
            lastByteUpdate = 0;
            totalBytesTransferred = 0;
        }
    }
}
