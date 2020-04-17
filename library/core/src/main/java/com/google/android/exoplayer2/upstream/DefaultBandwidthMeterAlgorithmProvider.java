package com.google.android.exoplayer2.upstream;

import android.content.Context;

import com.google.android.exoplayer2.lowlatency.LowLatencyBandwidthMeterAlgorithm;

public final class DefaultBandwidthMeterAlgorithmProvider implements BandwidthMeterAlgorithmProvider {
    public static final String ALGORITHM_DEFAULT = "default";
    public static final String ALGORITHM_LOW_LATENCY = "lowlatency";

    private final Context context;

    public DefaultBandwidthMeterAlgorithmProvider(Context context) {
        this.context = context == null ? null : context.getApplicationContext();
    }

    @Override
    public BandwidthMeterAlgorithm getInitialAlgorithm() {
        return getAlgorithm(ALGORITHM_DEFAULT);
    }

    @Override
    public BandwidthMeterAlgorithm getAlgorithm(String algorithmName) {
        if(ALGORITHM_DEFAULT.equals(algorithmName)) {
            return SharedBandwidthMeterAlgorithmManager.getSharedInstance(SlidingPercentileBandwidthMeterAlgorithm.class, new SlidingPercentileBandwidthMeterAlgorithm.Builder(context));
        } else if(ALGORITHM_LOW_LATENCY.equals(algorithmName)) {
            return new LowLatencyBandwidthMeterAlgorithm();
        } else {
            return getAlgorithm(ALGORITHM_DEFAULT);
        }
    }
}
