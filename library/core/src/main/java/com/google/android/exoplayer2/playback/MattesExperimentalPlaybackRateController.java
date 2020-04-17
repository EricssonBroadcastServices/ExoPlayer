package com.google.android.exoplayer2.playback;

import com.google.android.exoplayer2.C;

public final class MattesExperimentalPlaybackRateController implements PlaybackRateController {
    public static final float DEFAULT_CATCHUP_PLAYBACK_RATE = 1.3f;
    public static final long DEFAULT_MIN_DRIFT_MS = 500;
    public static final long DEFAULT_TARGET_LATENCY_MS = 3000L;

    public static final class Builder {
        private float catchupPlaybackRate;
        private long maxDriftMs;
        private long targetLatencyMs;

        public Builder(float catchupPlaybackRate, long maxDriftMs, long targetLatencyMs) {
            this.catchupPlaybackRate = catchupPlaybackRate;
            this.maxDriftMs = maxDriftMs;
            this.targetLatencyMs = targetLatencyMs;
        }

        public Builder() {
            this(DEFAULT_CATCHUP_PLAYBACK_RATE,
                 DEFAULT_MIN_DRIFT_MS,
                 DEFAULT_TARGET_LATENCY_MS);
        }

        public Builder setCatchupPlaybackRate(float catchupPlaybackRate) {
            this.catchupPlaybackRate = catchupPlaybackRate;
            return this;
        }

        public Builder setMaxDriftMs(long maxDriftMs) {
            this.maxDriftMs = maxDriftMs;
            return this;
        }

        public Builder setTargetLatencyMs(long targetLatencyMs) {
            this.targetLatencyMs = targetLatencyMs;
            return this;
        }

        public PlaybackRateController build() {
            return new MattesExperimentalPlaybackRateController(catchupPlaybackRate,
                    maxDriftMs,
                                                     targetLatencyMs);
        }
    }

    private final float catchupPlaybackRate;
    private final long maxDriftMs;
    private final long targetLatencyMs;

    private MattesExperimentalPlaybackRateController(float catchupPlaybackRate, long maxDriftMs, long targetLatencyMs) {
        this.catchupPlaybackRate = catchupPlaybackRate;
        this.maxDriftMs = maxDriftMs;
        this.targetLatencyMs = targetLatencyMs;
    }

    @Override
    public void onPositionsUpdated(SpeedHandler speedHandler, long positionUs, long liveTimeUs) {
        long latencyMs = C.usToMs(liveTimeUs - positionUs);
        float latencyErrorMs = latencyMs-targetLatencyMs;

        float severity = latencyErrorMs/maxDriftMs;
        severity = (severity < -1f) ? -1f : (severity > 1f ? 1f : severity);
        float chosenPlaybackRate = (float) Math.pow(catchupPlaybackRate, severity);
        speedHandler.setPlaybackSpeed(chosenPlaybackRate);
    }
}
