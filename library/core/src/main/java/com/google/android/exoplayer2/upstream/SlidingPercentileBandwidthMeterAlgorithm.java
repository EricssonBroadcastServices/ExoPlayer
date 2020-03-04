package com.google.android.exoplayer2.upstream;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Clock;
import com.google.android.exoplayer2.util.SlidingPercentile;

public final class SlidingPercentileBandwidthMeterAlgorithm implements BandwidthMeterAlgorithm, TransferListener {
    /** Default maximum weight for the sliding window. */
    public static final int DEFAULT_SLIDING_WINDOW_MAX_WEIGHT = 2000;

    public static final class Builder implements BandwidthMeterAlgorithm.Builder<SlidingPercentileBandwidthMeterAlgorithm> {
        private int slidingWindowMaxWeight;
        private Clock clock;

        public Builder(Context context) {
            this.slidingWindowMaxWeight = DEFAULT_SLIDING_WINDOW_MAX_WEIGHT;
            this.clock = Clock.DEFAULT;
        }

        /**
         * Sets the maximum weight for the sliding window.
         *
         * @param slidingWindowMaxWeight The maximum weight for the sliding window.
         * @return This builder.
         */
        public Builder setSlidingWindowMaxWeight(int slidingWindowMaxWeight) {
            this.slidingWindowMaxWeight = slidingWindowMaxWeight;
            return this;
        }

        /**
         * Sets the clock used to estimate bandwidth from data transfers. Should only be set for testing
         * purposes.
         *
         * @param clock The clock used to estimate bandwidth from data transfers.
         * @return This builder.
         */
        public Builder setClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        @NonNull
        @Override
        public SlidingPercentileBandwidthMeterAlgorithm build() {
            return new SlidingPercentileBandwidthMeterAlgorithm(
                    slidingWindowMaxWeight,
                    clock);
        }
    }

    private static final int ELAPSED_MILLIS_FOR_ESTIMATE = 2000;
    private static final int BYTES_TRANSFERRED_FOR_ESTIMATE = 512 * 1024;

    private final SlidingPercentile slidingPercentile;
    private final Clock clock;
    private final ListenerEventDispatcher listenerEventDispatcher = new ListenerEventDispatcher();

    private int streamCount;
    private long sampleStartTimeMs;
    private long sampleBytesTransferred;

    @C.NetworkType private int networkType;
    private long totalElapsedTimeMs;
    private long totalBytesTransferred;

    private SlidingPercentileBandwidthMeterAlgorithm(int maxWeight, Clock clock) {
        this.slidingPercentile = new SlidingPercentile(maxWeight);
        this.clock = clock;
    }

    @Nullable
    @Override
    public TransferListener getTransferListener() {
        return this;
    }

    @Override
    public synchronized void onNetworkTypeChanged(@C.NetworkType int networkType) {
        if(this.networkType == networkType) {
            return;
        }
        this.networkType = networkType;

        if (networkType == C.NETWORK_TYPE_OFFLINE
                || networkType == C.NETWORK_TYPE_UNKNOWN
                || networkType == C.NETWORK_TYPE_OTHER) {
            // It's better not to reset the bandwidth meter for these network types.
            return;
        }

        // Reset the bitrate estimate and report it, along with any bytes transferred.
        long nowMs = clock.elapsedRealtime();
        listenerEventDispatcher.onBandwidthSample(sampleBytesTransferred);

        // Reset the remainder of the state.
        sampleStartTimeMs = nowMs;
        sampleBytesTransferred = 0;
        totalBytesTransferred = 0;
        totalElapsedTimeMs = 0;
        slidingPercentile.reset();
    }

    @Override
    public boolean addListener(Listener listener) {
        return listenerEventDispatcher.addListener(listener);
    }

    @Override
    public boolean removeListener(Listener listener) {
        return listenerEventDispatcher.removeListener(listener);
    }

    @Override
    public void onTransferInitializing(DataSource source, DataSpec dataSpec, boolean isNetwork) {
        // Do nothing.
    }

    @Override
    public synchronized void onTransferStart(
            DataSource source, DataSpec dataSpec, boolean isNetwork) {
        if (!isNetwork) {
            return;
        }
        if (streamCount == 0) {
            sampleStartTimeMs = clock.elapsedRealtime();
        }
        streamCount++;
    }

    @Override
    public synchronized void onBytesTransferred(
            DataSource source, DataSpec dataSpec, boolean isNetwork, int bytes) {
        if (!isNetwork) {
            return;
        }
        sampleBytesTransferred += bytes;
    }

    @Override
    public synchronized void onTransferEnd(DataSource source, DataSpec dataSpec, boolean isNetwork) {
        if (!isNetwork) {
            return;
        }
        Assertions.checkState(streamCount > 0);
        long nowMs = clock.elapsedRealtime();
        int sampleElapsedTimeMs = (int) (nowMs - sampleStartTimeMs);
        totalElapsedTimeMs += sampleElapsedTimeMs;
        totalBytesTransferred += sampleBytesTransferred;
        if (sampleElapsedTimeMs > 0) {
            float bitsPerSecond = (sampleBytesTransferred * 8000f) / sampleElapsedTimeMs;
            slidingPercentile.addSample((int) Math.sqrt(sampleBytesTransferred), bitsPerSecond);
            if (totalElapsedTimeMs >= ELAPSED_MILLIS_FOR_ESTIMATE
                    || totalBytesTransferred >= BYTES_TRANSFERRED_FOR_ESTIMATE) {
                long newBitrateEstimate = (long) slidingPercentile.getPercentile(0.5f);
                listenerEventDispatcher.onBandwidthEstimate(newBitrateEstimate);
            }
            listenerEventDispatcher.onBandwidthSample(sampleBytesTransferred);
            sampleStartTimeMs = nowMs;
            sampleBytesTransferred = 0;
        } // Else any sample bytes transferred will be carried forward into the next sample.
        streamCount--;
    }
}
