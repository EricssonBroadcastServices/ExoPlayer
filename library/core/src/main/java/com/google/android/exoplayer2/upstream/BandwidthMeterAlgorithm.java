package com.google.android.exoplayer2.upstream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.EventDispatcher;

import java.util.concurrent.CopyOnWriteArrayList;

public interface BandwidthMeterAlgorithm {
    /**
     * A listener of {@link BandwidthMeterAlgorithm} events.
     */
    interface Listener {
        /**
         * Called when the {@link BandwidthMeterAlgorithm}
         * provides a new bandwidth estimate.
         *
         * @param bitrateEstimate
         */
        void onBandwidthEstimate(long bitrateEstimate);

        void onBandwidthSample(long bytesTransferred);
    }

    /**
     * Event dispatcher for {@link Listener}.
     */
    class ListenerEventDispatcher implements Listener {
        /** The list of listeners */
        private final CopyOnWriteArrayList<ListenerContainer> listeners = new CopyOnWriteArrayList<>();

        @Override
        public void onBandwidthEstimate(long bitrateEstimate) {
            dispatchEvent(listener -> listener.onBandwidthEstimate(bitrateEstimate));
        }

        @Override
        public void onBandwidthSample(long bytesTransferred) {
            dispatchEvent(listener -> listener.onBandwidthSample(bytesTransferred));
        }

        private void dispatchEvent(EventDispatcher.Event<Listener> event) {
            for(ListenerContainer listenerLink : listeners) {
                synchronized (listenerLink) {
                    if(!listenerLink.removed) {
                        event.sendTo(listenerLink.listener);
                    }
                }
            }
        }

        public boolean addListener(Listener listener) {
            Assertions.checkArgument(listener!= null);
            removeListener(listener);
            return listeners.add(new ListenerContainer(listener));
        }

        public boolean removeListener(Listener listener) {
            boolean listenerRemoved = false;
            for(ListenerContainer listenerLink : listeners) {
                synchronized (listenerLink) {
                    if(listenerLink.listener == listener) {
                        listenerLink.removed = true;
                        listeners.remove(listenerLink);
                        listenerRemoved = true;
                    }
                }
            }
            return listenerRemoved;
        }

        private static class ListenerContainer {
            private final Listener listener;

            private boolean removed = false;

            public ListenerContainer(Listener listener) {
                this.listener = listener;
            }
        }
    }

    interface Builder<T extends BandwidthMeterAlgorithm> {
        @NonNull T build();
    }

    @Nullable
    TransferListener getTransferListener();

    void onNetworkTypeChanged(@C.NetworkType int networkType);

    boolean addListener(Listener listener);

    boolean removeListener(Listener listener);
}
