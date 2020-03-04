package com.google.android.exoplayer2.upstream;

import androidx.annotation.Nullable;

public interface BandwidthMeterAlgorithmSelector {
    /**
     * @param bandwidthMeterAlgorithmProvider
     * @return A BandwidthMeterAlgorithm to use or {@code null} if none could be selected.
     */
    @Nullable
    BandwidthMeterAlgorithm selectBandwidthMeterAlgorithm(BandwidthMeterAlgorithmProvider bandwidthMeterAlgorithmProvider);
}
