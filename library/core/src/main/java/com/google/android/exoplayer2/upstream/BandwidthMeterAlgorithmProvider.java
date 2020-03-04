package com.google.android.exoplayer2.upstream;

public interface BandwidthMeterAlgorithmProvider {
    /**
     * Gets an initial algorithm to be used before any information about media is provided.
     * @return The initial algorithm
     */
    BandwidthMeterAlgorithm getInitialAlgorithm();
    BandwidthMeterAlgorithm getAlgorithm(String algorithmName);
}
