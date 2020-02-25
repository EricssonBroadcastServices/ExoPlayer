package com.google.android.exoplayer2.playback;

public interface PlaybackRateController {
    void onPositionsUpdated(SpeedHandler speedHandler, long positionUs, long liveTimeUs);

    interface SpeedHandler {
        void setPlaybackSpeed(float playbackSpeed);
    }
}
