package com.google.android.exoplayer2.upstream;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.TreeMap;

public final class SharedBandwidthMeterAlgorithmManager {
    private static final Map<Class<?>, BandwidthMeterAlgorithm> algorithmInstances = new TreeMap<>((o1, o2) -> o1.getName().compareTo(o2.getName()));

    private static synchronized <T extends BandwidthMeterAlgorithm> T setInstance(Class<T> type, @NonNull T instance) {
        algorithmInstances.put(type, instance);
        return instance;
    }

    private static synchronized <T extends BandwidthMeterAlgorithm> T getInstance(Class<T> type) {
        return type.cast(algorithmInstances.get(type));
    }

    public static <T extends BandwidthMeterAlgorithm> T getSharedInstance(Class<T> type, BandwidthMeterAlgorithm.Builder<T> builder) {
        T instance;
        synchronized (algorithmInstances) {
            instance = getInstance(type);
            if(instance == null) {
                instance = setInstance(type, builder.build());
            }
        }
        return instance;
    }
}
