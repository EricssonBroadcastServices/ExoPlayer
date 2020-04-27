package com.google.android.exoplayer2.playback;

import com.google.android.exoplayer2.C;

public final class DefaultPlaybackRateController implements PlaybackRateController {
    public static final float DEFAULT_CATCHUP_PLAYBACK_RATE = 1.3f;
    public static final long DEFAULT_MIN_DRIFT_MS = 50;

    public static final class Builder {
        private float catchupPlaybackRate;
        private long maxDriftMs;

        public Builder(float catchupPlaybackRate, long maxDriftMs) {
            this.catchupPlaybackRate = catchupPlaybackRate;
            this.maxDriftMs = maxDriftMs;
        }

        public Builder() {
            this(DEFAULT_CATCHUP_PLAYBACK_RATE,
                 DEFAULT_MIN_DRIFT_MS);
        }

        public Builder setCatchupPlaybackRate(float catchupPlaybackRate) {
            this.catchupPlaybackRate = catchupPlaybackRate;
            return this;
        }

        public Builder setMaxDriftMs(long maxDriftMs) {
            this.maxDriftMs = maxDriftMs;
            return this;
        }

        public PlaybackRateController build() {
            return new DefaultPlaybackRateController(catchupPlaybackRate, maxDriftMs);
        }
    }

    private static final int STATE_INACTIVE = 0;
    private static final int STATE_SPEED_UP = 1;
    private static final int STATE_SLOW_DOWN = 2;

    private final float catchupPlaybackRate;
    private final long maxDriftMs;

    private int state = STATE_INACTIVE;

    private DefaultPlaybackRateController(float catchupPlaybackRate, long maxDriftMs) {
        //TODO MATTE assert parameters are in range
        this.catchupPlaybackRate = catchupPlaybackRate;
        this.maxDriftMs = maxDriftMs;
    }

    @Override
    public void onPositionsUpdated(SpeedHandler speedHandler, long positionUs, long liveTimeUs) {
        if (state == STATE_INACTIVE) {
            if (positionUs != C.TIME_UNSET && liveTimeUs != C.TIME_UNSET) {
                long latencyMs = C.usToMs(liveTimeUs - positionUs);
                long latencyErrorMs = latencyMs;

                if (latencyErrorMs > maxDriftMs) {
                    speedUp(speedHandler);
                    return;
                } else if(latencyErrorMs < -maxDriftMs) {
                    slowDown(speedHandler);
                    return;
                }
            }
        } else if(state == STATE_SPEED_UP) {
            if (positionUs != C.TIME_UNSET && liveTimeUs != C.TIME_UNSET) {
                long latencyMs = C.usToMs(liveTimeUs - positionUs);
                long latencyErrorMs = latencyMs;

                if (latencyErrorMs <= maxDriftMs) {
                    inactivate(speedHandler);
                    return;
                } else {
                    speedUp(speedHandler);
                    return;
                }
            } else {
                inactivate(speedHandler);
                return;
            }
        } else if(state == STATE_SLOW_DOWN) {
            if (positionUs != C.TIME_UNSET && liveTimeUs != C.TIME_UNSET) {
                long latencyMs = C.usToMs(liveTimeUs - positionUs);
                long latencyErrorMs = latencyMs;

                if (latencyErrorMs >= -maxDriftMs) {
                    inactivate(speedHandler);
                    return;
                } else {
                    slowDown(speedHandler);
                    return;
                }
            } else {
                inactivate(speedHandler);
                return;
            }
        }
    }

    private void inactivate(SpeedHandler speedHandler) {
        state = STATE_INACTIVE;
        speedHandler.setPlaybackSpeed(1f);
    }

    private void speedUp(SpeedHandler speedHandler) {
        state = STATE_SPEED_UP;
        speedHandler.setPlaybackSpeed(catchupPlaybackRate);
    }

    private void slowDown(SpeedHandler speedHandler) {
        state = STATE_SLOW_DOWN;
        speedHandler.setPlaybackSpeed(1f/catchupPlaybackRate);
    }
}
