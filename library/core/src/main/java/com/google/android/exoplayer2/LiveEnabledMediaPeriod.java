package com.google.android.exoplayer2;

import com.google.android.exoplayer2.source.MediaPeriod;

public interface LiveEnabledMediaPeriod extends MediaPeriod {
    long getLiveEdgePositionUs();
}
