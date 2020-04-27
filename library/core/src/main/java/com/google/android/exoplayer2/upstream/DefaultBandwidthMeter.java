/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.upstream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.remotedebugging.RemoteLogging;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.util.Clock;
import com.google.android.exoplayer2.util.EventDispatcher;
import com.google.android.exoplayer2.util.SlidingPercentile;
import com.google.android.exoplayer2.util.Util;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Estimates bandwidth by listening to data transfers.
 *
 * <p>The bandwidth estimate is calculated using a {@link SlidingPercentile} and is updated each
 * time a transfer ends. The initial estimate is based on the current operator's network country
 * code or the locale of the user, as well as the network connection type. This can be configured in
 * the {@link Builder}.
 */
public final class DefaultBandwidthMeter implements BandwidthMeter, BandwidthMeterAlgorithm.Listener {
  /** Builder for a bandwidth meter. */
  public static final class Builder {

    @Nullable private final Context context;

    private InitialBitrateEstimates initialBitrateEstimates;
    private Clock clock;
    private boolean resetOnNetworkTypeChange;
    private BandwidthMeterAlgorithmProvider bandwidthMeterAlgorithmProvider;

    /**
     * Creates a builder with default parameters and without listener.
     *
     * @param context A context.
     */
    public Builder(Context context) {
      // Handling of null is for backward compatibility only.
      this.context = context == null ? null : context.getApplicationContext();
      this.initialBitrateEstimates = new InitialBitrateEstimates(Util.getCountryCode(context));
      clock = Clock.DEFAULT;
      resetOnNetworkTypeChange = true;
      bandwidthMeterAlgorithmProvider = new DefaultBandwidthMeterAlgorithmProvider(context);
    }

    /**
     * Sets the initial bitrate estimate in bits per second that should be assumed when a bandwidth
     * estimate is unavailable.
     *
     * @param initialBitrateEstimate The initial bitrate estimate in bits per second.
     * @return This builder.
     */
    public Builder setInitialBitrateEstimate(long initialBitrateEstimate) {
      initialBitrateEstimates.setAllEstimates(initialBitrateEstimate);
      return this;
    }

    /**
     * Sets the initial bitrate estimate in bits per second that should be assumed when a bandwidth
     * estimate is unavailable and the current network connection is of the specified type.
     *
     * @param networkType The {@link C.NetworkType} this initial estimate is for.
     * @param initialBitrateEstimate The initial bitrate estimate in bits per second.
     * @return This builder.
     */
    public Builder setInitialBitrateEstimate(
        @C.NetworkType int networkType, long initialBitrateEstimate) {
      initialBitrateEstimates.setEstimate(networkType, initialBitrateEstimate);
      return this;
    }

    /**
     * Sets the clock to use to calculate elapsed time when reporting bandwidth samples to
     * {@link com.google.android.exoplayer2.upstream.BandwidthMeter.EventListener}s.
     *
     * @param clock The clock to use.
     * @return This builder.
     */
    public Builder setClock(Clock clock) {
      this.clock = clock;
      return this;
    }

    /**
     * Sets the initial bitrate estimates to the default values of the specified country. The
     * initial estimates are used when a bandwidth estimate is unavailable.
     *
     * @param countryCode The ISO 3166-1 alpha-2 country code of the country whose default bitrate
     *     estimates should be used.
     * @return This builder.
     */
    public Builder setInitialBitrateEstimate(String countryCode) {
      this.initialBitrateEstimates = new InitialBitrateEstimates(Util.toUpperInvariant(countryCode));
      return this;
    }


    /**
     * Sets whether to reset if the network type changes. The default value is {@code true}.
     *
     * @param resetOnNetworkTypeChange Whether to reset if the network type changes.
     * @return This builder.
     */
    public Builder setResetOnNetworkTypeChange(boolean resetOnNetworkTypeChange) {
      this.resetOnNetworkTypeChange = resetOnNetworkTypeChange;
      return this;
    }

    /**
     * Sets the {@link BandwidthMeterAlgorithmProvider} to use for selecting {@link BandwidthMeterAlgorithm}.
     *
     * @param bandwidthMeterAlgorithmProvider
     * @return This builder.
     */
    public Builder setBandwidthMeterAlgorithmProvider(BandwidthMeterAlgorithmProvider bandwidthMeterAlgorithmProvider) {
      this.bandwidthMeterAlgorithmProvider = bandwidthMeterAlgorithmProvider;
      return this;
    }

    /**
     * Builds the bandwidth meter.
     *
     * @return A bandwidth meter with the configured properties.
     */
    public DefaultBandwidthMeter build() {
      return new DefaultBandwidthMeter(
          context,
          initialBitrateEstimates,
          clock,
          resetOnNetworkTypeChange,
          bandwidthMeterAlgorithmProvider);
    }
  }

  @Nullable private final Context context;
  private final InitialBitrateEstimates initialBitrateEstimates;
  private final EventDispatcher<EventListener> eventDispatcher;
  private final Clock clock;
  private final BandwidthMeterAlgorithmProvider bandwidthMeterAlgorithmProvider;
  private final ForwardingTransferListener forwardingTransferListener = new ForwardingTransferListener();

  @GuardedBy("this")
  @MonotonicNonNull
  private BandwidthMeterAlgorithm bandwidthMeterAlgorithm;

  @C.NetworkType private int networkType;
  private long bitrateEstimate;
  private long lastBandwidthSampleDispatch = C.TIME_UNSET;
  private long lastReportedBitrateEstimate;

  private boolean networkTypeOverrideSet;
  @C.NetworkType private int networkTypeOverride;

  /** @deprecated Use {@link Builder} instead. */
  @Deprecated
  public DefaultBandwidthMeter() {
    this(
        /* context= */ null,
        /* initialBitrateEstimates= */ new InitialBitrateEstimates(),
        Clock.DEFAULT,
        /* resetOnNetworkTypeChange= */ false,
        /* bandwidthMeterAlgorithmProvider */ new DefaultBandwidthMeterAlgorithmProvider(null));
  }

  private DefaultBandwidthMeter(
      @Nullable Context context,
      InitialBitrateEstimates initialBitrateEstimates,
      Clock clock,
      boolean resetOnNetworkTypeChange,
      BandwidthMeterAlgorithmProvider bandwidthMeterAlgorithmProvider) {
    this.context = context == null ? null : context.getApplicationContext();
    this.initialBitrateEstimates = initialBitrateEstimates;
    this.eventDispatcher = new EventDispatcher<>();
    this.clock = clock;
    this.bandwidthMeterAlgorithmProvider = bandwidthMeterAlgorithmProvider;
    // Set the initial network type and bitrate estimate
    networkType = context == null ? C.NETWORK_TYPE_UNKNOWN : Util.getNetworkType(context);
    bitrateEstimate = initialBitrateEstimates.getForNetworkType(networkType);
    RemoteLogging.getBandwidthMetering().log("INITIAL_ESTIMATE", "bitrateEstimate: "+bitrateEstimate);

    // Initialize initial BandwidthMeterAlgorithm
    setAlgorithm(bandwidthMeterAlgorithmProvider.getInitialAlgorithm());

    // Register to receive connectivity actions if possible.
    if (context != null && resetOnNetworkTypeChange) {
      ConnectivityActionReceiver connectivityActionReceiver =
          ConnectivityActionReceiver.getInstance(context);
      connectivityActionReceiver.register(/* bandwidthMeter= */ this);
    }
  }

  /**
   * Overrides the network type. Handled in the same way as if the meter had detected a change from
   * the current network type to the specified network type internally.
   *
   * <p>Applications should not normally call this method. It is intended for testing purposes.
   *
   * @param networkType The overriding network type.
   */
  public synchronized void setNetworkTypeOverride(@C.NetworkType int networkType) {
    networkTypeOverride = networkType;
    networkTypeOverrideSet = true;
    onConnectivityAction();
  }

  @Override
  public synchronized long getBitrateEstimate() {
    return bitrateEstimate;
  }

  @Override
  @Nullable
  public TransferListener getTransferListener() {
    return forwardingTransferListener;
  }

  @Override
  public void addEventListener(Handler eventHandler, EventListener eventListener) {
    eventDispatcher.addListener(eventHandler, eventListener);
  }

  @Override
  public void removeEventListener(EventListener eventListener) {
    eventDispatcher.removeListener(eventListener);
  }

  private synchronized void setAlgorithm(@NonNull BandwidthMeterAlgorithm bandwidthMeterAlgorithm) {
    if(objectsEquals(this.bandwidthMeterAlgorithm, bandwidthMeterAlgorithm)) {
      return;
    }
    BandwidthMeterAlgorithm oldBandwidthMeterAlgorithm = this.bandwidthMeterAlgorithm;
    this.bandwidthMeterAlgorithm = bandwidthMeterAlgorithm;

    if(oldBandwidthMeterAlgorithm != null) {
      oldBandwidthMeterAlgorithm.removeListener(this);
    }
    forwardingTransferListener.setTransferListener(bandwidthMeterAlgorithm.getTransferListener());
    bandwidthMeterAlgorithm.addListener(this);
  }

  @Override
  public void onMediaSourceChanged(MediaSource mediaSource) {
    BandwidthMeterAlgorithmSelector algorithmSelector = mediaSource.getBandwidthMeterAlgorithmSelector();
    BandwidthMeterAlgorithm selectedAlgorithm = algorithmSelector.selectBandwidthMeterAlgorithm(bandwidthMeterAlgorithmProvider);
    setAlgorithm(selectedAlgorithm);
  }

  private synchronized void onConnectivityAction() {
    int networkType =
        networkTypeOverrideSet
            ? networkTypeOverride
            : (context == null ? C.NETWORK_TYPE_UNKNOWN : Util.getNetworkType(context));
    if (this.networkType == networkType) {
      return;
    }

    this.networkType = networkType;
    bandwidthMeterAlgorithm.onNetworkTypeChanged(networkType);
  }

  private void maybeNotifyBandwidthSample(long bytesTransferred, long bitrateEstimate) {
    long nowMs = clock.elapsedRealtime();
    int elapsedMs = lastBandwidthSampleDispatch != C.TIME_UNSET ? (int) (nowMs - lastBandwidthSampleDispatch) : 0;

    if ((lastBandwidthSampleDispatch != C.TIME_UNSET && elapsedMs <= 0) && bytesTransferred == 0 && bitrateEstimate == lastReportedBitrateEstimate) {
      return;
    }
    lastReportedBitrateEstimate = bitrateEstimate;
    lastBandwidthSampleDispatch = nowMs;
    eventDispatcher.dispatch(
        listener -> listener.onBandwidthSample(elapsedMs, bytesTransferred, bitrateEstimate));
  }

  @Override
  public synchronized void onBandwidthEstimate(long bitrateEstimate) {
    RemoteLogging.getBandwidthMetering().log("ESTIMATE", "bitrateEstimate: "+bitrateEstimate);
    this.bitrateEstimate = bitrateEstimate;
  }

  @Override
  public void onBandwidthSample(long bytesTransferred) {
    maybeNotifyBandwidthSample(bytesTransferred, bitrateEstimate);
  }

  /**
   * Reimplementation of {@code Objects.equals(...)} that is supported by api 16.
   * @return
   */
  private static boolean objectsEquals(Object a, Object b) {
    return (a == b) || (a != null && a.equals(b));
  }

  /**
   * Forwards transfer-events to active algorithm
   */
  private class ForwardingTransferListener implements TransferListener {
    private TransferListener transferListener = null;

    public synchronized void setTransferListener(TransferListener transferListener) {
      this.transferListener = transferListener;
    }

    @Override
    public synchronized void onTransferInitializing(DataSource source, DataSpec dataSpec, boolean isNetwork) {
      if(transferListener != null) {
        transferListener.onTransferInitializing(source, dataSpec, isNetwork);
      }
    }

    @Override
    public synchronized void onTransferStart(DataSource source, DataSpec dataSpec, boolean isNetwork) {
      if(transferListener != null) {
        transferListener.onTransferStart(source, dataSpec, isNetwork);
      }
    }

    @Override
    public synchronized void onBytesTransferred(DataSource source, DataSpec dataSpec, boolean isNetwork, int bytesTransferred) {
      if(transferListener != null) {
        transferListener.onBytesTransferred(source, dataSpec, isNetwork, bytesTransferred);
      }
    }

    @Override
    public synchronized void onTransferEnd(DataSource source, DataSpec dataSpec, boolean isNetwork) {
      if(transferListener != null) {
        transferListener.onTransferEnd(source, dataSpec, isNetwork);
      }
    }
  }

  /*
   * Note: This class only holds a weak reference to DefaultBandwidthMeter instances. It should not
   * be made non-static, since doing so adds a strong reference (i.e. DefaultBandwidthMeter.this).
   */
  private static class ConnectivityActionReceiver extends BroadcastReceiver {

    private static @MonotonicNonNull ConnectivityActionReceiver staticInstance;

    private final Handler mainHandler;
    private final ArrayList<WeakReference<DefaultBandwidthMeter>> bandwidthMeters;

    public static synchronized ConnectivityActionReceiver getInstance(Context context) {
      if (staticInstance == null) {
        staticInstance = new ConnectivityActionReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(staticInstance, filter);
      }
      return staticInstance;
    }

    private ConnectivityActionReceiver() {
      mainHandler = new Handler(Looper.getMainLooper());
      bandwidthMeters = new ArrayList<>();
    }

    public synchronized void register(DefaultBandwidthMeter bandwidthMeter) {
      removeClearedReferences();
      bandwidthMeters.add(new WeakReference<>(bandwidthMeter));
      // Simulate an initial update on the main thread (like the sticky broadcast we'd receive if
      // we were to register a separate broadcast receiver for each bandwidth meter).
      mainHandler.post(() -> updateBandwidthMeter(bandwidthMeter));
    }

    @Override
    public synchronized void onReceive(Context context, Intent intent) {
      if (isInitialStickyBroadcast()) {
        return;
      }
      removeClearedReferences();
      for (int i = 0; i < bandwidthMeters.size(); i++) {
        WeakReference<DefaultBandwidthMeter> bandwidthMeterReference = bandwidthMeters.get(i);
        DefaultBandwidthMeter bandwidthMeter = bandwidthMeterReference.get();
        if (bandwidthMeter != null) {
          updateBandwidthMeter(bandwidthMeter);
        }
      }
    }

    private void updateBandwidthMeter(DefaultBandwidthMeter bandwidthMeter) {
      bandwidthMeter.onConnectivityAction();
    }

    private void removeClearedReferences() {
      for (int i = bandwidthMeters.size() - 1; i >= 0; i--) {
        WeakReference<DefaultBandwidthMeter> bandwidthMeterReference = bandwidthMeters.get(i);
        DefaultBandwidthMeter bandwidthMeter = bandwidthMeterReference.get();
        if (bandwidthMeter == null) {
          bandwidthMeters.remove(i);
        }
      }
    }
  }
}
