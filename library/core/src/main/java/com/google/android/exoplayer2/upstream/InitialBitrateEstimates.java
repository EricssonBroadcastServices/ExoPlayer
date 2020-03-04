package com.google.android.exoplayer2.upstream;

import android.util.SparseArray;

import com.google.android.exoplayer2.C;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class InitialBitrateEstimates {
    private final SparseArray<Long> estimates;

    public InitialBitrateEstimates() {
        this.estimates =  new SparseArray<>();
    }

    public InitialBitrateEstimates(String countryCode) {
        int[] groupIndices = getCountryGroupIndices(countryCode);
        this.estimates = new SparseArray<>(/* initialCapacity= */ 6);
        this.estimates.append(C.NETWORK_TYPE_UNKNOWN, DEFAULT_INITIAL_BITRATE_ESTIMATE);
        this.estimates.append(C.NETWORK_TYPE_WIFI, DEFAULT_INITIAL_BITRATE_ESTIMATES_WIFI[groupIndices[0]]);
        this.estimates.append(C.NETWORK_TYPE_2G, DEFAULT_INITIAL_BITRATE_ESTIMATES_2G[groupIndices[1]]);
        this.estimates.append(C.NETWORK_TYPE_3G, DEFAULT_INITIAL_BITRATE_ESTIMATES_3G[groupIndices[2]]);
        this.estimates.append(C.NETWORK_TYPE_4G, DEFAULT_INITIAL_BITRATE_ESTIMATES_4G[groupIndices[3]]);
        // Assume default Wifi bitrate for Ethernet to prevent using the slower fallback bitrate.
        this.estimates.append(
                C.NETWORK_TYPE_ETHERNET, DEFAULT_INITIAL_BITRATE_ESTIMATES_WIFI[groupIndices[0]]);
    }

    public void setAllEstimates(long bitrateEstimate) {
        for (int i = 0; i < estimates.size(); i++) {
            estimates.setValueAt(i, bitrateEstimate);
        }
    }

    public void setEstimate(int networkType, long bitrateEstimate) {
        estimates.put(networkType, bitrateEstimate);
    }

    public long getForNetworkType(@C.NetworkType int networkType) {
        Long initialBitrateEstimate = estimates.get(networkType);
        if (initialBitrateEstimate == null) {
            initialBitrateEstimate = estimates.get(C.NETWORK_TYPE_UNKNOWN);
        }
        if (initialBitrateEstimate == null) {
            initialBitrateEstimate = DEFAULT_INITIAL_BITRATE_ESTIMATE;
        }
        return initialBitrateEstimate;
    }

    /**
     * Country groups used to determine the default initial bitrate estimate. The group assignment for
     * each country is an array of group indices for [Wifi, 2G, 3G, 4G].
     */
    public static final Map<String, int[]> DEFAULT_INITIAL_BITRATE_COUNTRY_GROUPS =
            createInitialBitrateCountryGroupAssignment();

    /** Default initial Wifi bitrate estimate in bits per second. */
    public static final long[] DEFAULT_INITIAL_BITRATE_ESTIMATES_WIFI =
            new long[] {5_700_000, 3_500_000, 2_000_000, 1_100_000, 470_000};

    /** Default initial 2G bitrate estimates in bits per second. */
    public static final long[] DEFAULT_INITIAL_BITRATE_ESTIMATES_2G =
            new long[] {200_000, 148_000, 132_000, 115_000, 95_000};

    /** Default initial 3G bitrate estimates in bits per second. */
    public static final long[] DEFAULT_INITIAL_BITRATE_ESTIMATES_3G =
            new long[] {2_200_000, 1_300_000, 970_000, 810_000, 490_000};

    /** Default initial 4G bitrate estimates in bits per second. */
    public static final long[] DEFAULT_INITIAL_BITRATE_ESTIMATES_4G =
            new long[] {5_300_000, 3_200_000, 2_000_000, 1_400_000, 690_000};

    /**
     * Default initial bitrate estimate used when the device is offline or the network type cannot be
     * determined, in bits per second.
     */
    public static final long DEFAULT_INITIAL_BITRATE_ESTIMATE = 1_000_000;

    private static int[] getCountryGroupIndices(String countryCode) {
        int[] groupIndices = DEFAULT_INITIAL_BITRATE_COUNTRY_GROUPS.get(countryCode);
        // Assume median group if not found.
        return groupIndices == null ? new int[] {2, 2, 2, 2} : groupIndices;
    }

    private static Map<String, int[]> createInitialBitrateCountryGroupAssignment() {
        HashMap<String, int[]> countryGroupAssignment = new HashMap<>();
        countryGroupAssignment.put("AD", new int[] {1, 1, 0, 0});
        countryGroupAssignment.put("AE", new int[] {1, 4, 4, 4});
        countryGroupAssignment.put("AF", new int[] {4, 4, 3, 3});
        countryGroupAssignment.put("AG", new int[] {3, 1, 0, 1});
        countryGroupAssignment.put("AI", new int[] {1, 0, 0, 3});
        countryGroupAssignment.put("AL", new int[] {1, 2, 0, 1});
        countryGroupAssignment.put("AM", new int[] {2, 2, 2, 2});
        countryGroupAssignment.put("AO", new int[] {3, 4, 2, 0});
        countryGroupAssignment.put("AR", new int[] {2, 3, 2, 2});
        countryGroupAssignment.put("AS", new int[] {3, 0, 4, 2});
        countryGroupAssignment.put("AT", new int[] {0, 3, 0, 0});
        countryGroupAssignment.put("AU", new int[] {0, 3, 0, 1});
        countryGroupAssignment.put("AW", new int[] {1, 1, 0, 3});
        countryGroupAssignment.put("AX", new int[] {0, 3, 0, 2});
        countryGroupAssignment.put("AZ", new int[] {3, 3, 3, 3});
        countryGroupAssignment.put("BA", new int[] {1, 1, 0, 1});
        countryGroupAssignment.put("BB", new int[] {0, 2, 0, 0});
        countryGroupAssignment.put("BD", new int[] {2, 1, 3, 3});
        countryGroupAssignment.put("BE", new int[] {0, 0, 0, 1});
        countryGroupAssignment.put("BF", new int[] {4, 4, 4, 1});
        countryGroupAssignment.put("BG", new int[] {0, 1, 0, 0});
        countryGroupAssignment.put("BH", new int[] {2, 1, 3, 4});
        countryGroupAssignment.put("BI", new int[] {4, 4, 4, 4});
        countryGroupAssignment.put("BJ", new int[] {4, 4, 4, 4});
        countryGroupAssignment.put("BL", new int[] {1, 0, 2, 2});
        countryGroupAssignment.put("BM", new int[] {1, 2, 0, 0});
        countryGroupAssignment.put("BN", new int[] {4, 1, 3, 2});
        countryGroupAssignment.put("BO", new int[] {1, 2, 3, 2});
        countryGroupAssignment.put("BQ", new int[] {1, 1, 2, 4});
        countryGroupAssignment.put("BR", new int[] {2, 3, 3, 2});
        countryGroupAssignment.put("BS", new int[] {2, 1, 1, 4});
        countryGroupAssignment.put("BT", new int[] {3, 0, 3, 1});
        countryGroupAssignment.put("BW", new int[] {4, 4, 1, 2});
        countryGroupAssignment.put("BY", new int[] {0, 1, 1, 2});
        countryGroupAssignment.put("BZ", new int[] {2, 2, 2, 1});
        countryGroupAssignment.put("CA", new int[] {0, 3, 1, 3});
        countryGroupAssignment.put("CD", new int[] {4, 4, 2, 2});
        countryGroupAssignment.put("CF", new int[] {4, 4, 3, 0});
        countryGroupAssignment.put("CG", new int[] {3, 4, 2, 4});
        countryGroupAssignment.put("CH", new int[] {0, 0, 1, 0});
        countryGroupAssignment.put("CI", new int[] {3, 4, 3, 3});
        countryGroupAssignment.put("CK", new int[] {2, 4, 1, 0});
        countryGroupAssignment.put("CL", new int[] {1, 2, 2, 3});
        countryGroupAssignment.put("CM", new int[] {3, 4, 3, 1});
        countryGroupAssignment.put("CN", new int[] {2, 0, 2, 3});
        countryGroupAssignment.put("CO", new int[] {2, 3, 2, 2});
        countryGroupAssignment.put("CR", new int[] {2, 3, 4, 4});
        countryGroupAssignment.put("CU", new int[] {4, 4, 3, 1});
        countryGroupAssignment.put("CV", new int[] {2, 3, 1, 2});
        countryGroupAssignment.put("CW", new int[] {1, 1, 0, 0});
        countryGroupAssignment.put("CY", new int[] {1, 1, 0, 0});
        countryGroupAssignment.put("CZ", new int[] {0, 1, 0, 0});
        countryGroupAssignment.put("DE", new int[] {0, 1, 1, 3});
        countryGroupAssignment.put("DJ", new int[] {4, 3, 4, 1});
        countryGroupAssignment.put("DK", new int[] {0, 0, 1, 1});
        countryGroupAssignment.put("DM", new int[] {1, 0, 1, 3});
        countryGroupAssignment.put("DO", new int[] {3, 3, 4, 4});
        countryGroupAssignment.put("DZ", new int[] {3, 3, 4, 4});
        countryGroupAssignment.put("EC", new int[] {2, 3, 4, 3});
        countryGroupAssignment.put("EE", new int[] {0, 1, 0, 0});
        countryGroupAssignment.put("EG", new int[] {3, 4, 2, 2});
        countryGroupAssignment.put("EH", new int[] {2, 0, 3, 3});
        countryGroupAssignment.put("ER", new int[] {4, 2, 2, 0});
        countryGroupAssignment.put("ES", new int[] {0, 1, 1, 1});
        countryGroupAssignment.put("ET", new int[] {4, 4, 4, 0});
        countryGroupAssignment.put("FI", new int[] {0, 0, 1, 0});
        countryGroupAssignment.put("FJ", new int[] {3, 0, 3, 3});
        countryGroupAssignment.put("FK", new int[] {3, 4, 2, 2});
        countryGroupAssignment.put("FM", new int[] {4, 0, 4, 0});
        countryGroupAssignment.put("FO", new int[] {0, 0, 0, 0});
        countryGroupAssignment.put("FR", new int[] {1, 0, 3, 1});
        countryGroupAssignment.put("GA", new int[] {3, 3, 2, 2});
        countryGroupAssignment.put("GB", new int[] {0, 1, 3, 3});
        countryGroupAssignment.put("GD", new int[] {2, 0, 4, 4});
        countryGroupAssignment.put("GE", new int[] {1, 1, 1, 4});
        countryGroupAssignment.put("GF", new int[] {2, 3, 4, 4});
        countryGroupAssignment.put("GG", new int[] {0, 1, 0, 0});
        countryGroupAssignment.put("GH", new int[] {3, 3, 2, 2});
        countryGroupAssignment.put("GI", new int[] {0, 0, 0, 1});
        countryGroupAssignment.put("GL", new int[] {2, 2, 0, 2});
        countryGroupAssignment.put("GM", new int[] {4, 4, 3, 4});
        countryGroupAssignment.put("GN", new int[] {3, 4, 4, 2});
        countryGroupAssignment.put("GP", new int[] {2, 1, 1, 4});
        countryGroupAssignment.put("GQ", new int[] {4, 4, 3, 0});
        countryGroupAssignment.put("GR", new int[] {1, 1, 0, 2});
        countryGroupAssignment.put("GT", new int[] {3, 3, 3, 3});
        countryGroupAssignment.put("GU", new int[] {1, 2, 4, 4});
        countryGroupAssignment.put("GW", new int[] {4, 4, 4, 1});
        countryGroupAssignment.put("GY", new int[] {3, 2, 1, 1});
        countryGroupAssignment.put("HK", new int[] {0, 2, 3, 4});
        countryGroupAssignment.put("HN", new int[] {3, 2, 3, 2});
        countryGroupAssignment.put("HR", new int[] {1, 1, 0, 1});
        countryGroupAssignment.put("HT", new int[] {4, 4, 4, 4});
        countryGroupAssignment.put("HU", new int[] {0, 1, 0, 0});
        countryGroupAssignment.put("ID", new int[] {3, 2, 3, 4});
        countryGroupAssignment.put("IE", new int[] {1, 0, 1, 1});
        countryGroupAssignment.put("IL", new int[] {0, 0, 2, 3});
        countryGroupAssignment.put("IM", new int[] {0, 0, 0, 1});
        countryGroupAssignment.put("IN", new int[] {2, 2, 4, 4});
        countryGroupAssignment.put("IO", new int[] {4, 2, 2, 2});
        countryGroupAssignment.put("IQ", new int[] {3, 3, 4, 2});
        countryGroupAssignment.put("IR", new int[] {3, 0, 2, 2});
        countryGroupAssignment.put("IS", new int[] {0, 1, 0, 0});
        countryGroupAssignment.put("IT", new int[] {1, 0, 1, 2});
        countryGroupAssignment.put("JE", new int[] {1, 0, 0, 1});
        countryGroupAssignment.put("JM", new int[] {2, 3, 3, 1});
        countryGroupAssignment.put("JO", new int[] {1, 2, 1, 2});
        countryGroupAssignment.put("JP", new int[] {0, 2, 1, 1});
        countryGroupAssignment.put("KE", new int[] {3, 4, 4, 3});
        countryGroupAssignment.put("KG", new int[] {1, 1, 2, 2});
        countryGroupAssignment.put("KH", new int[] {1, 0, 4, 4});
        countryGroupAssignment.put("KI", new int[] {4, 4, 4, 4});
        countryGroupAssignment.put("KM", new int[] {4, 3, 2, 3});
        countryGroupAssignment.put("KN", new int[] {1, 0, 1, 3});
        countryGroupAssignment.put("KP", new int[] {4, 2, 4, 2});
        countryGroupAssignment.put("KR", new int[] {0, 1, 1, 1});
        countryGroupAssignment.put("KW", new int[] {2, 3, 1, 1});
        countryGroupAssignment.put("KY", new int[] {1, 1, 0, 1});
        countryGroupAssignment.put("KZ", new int[] {1, 2, 2, 3});
        countryGroupAssignment.put("LA", new int[] {2, 2, 1, 1});
        countryGroupAssignment.put("LB", new int[] {3, 2, 0, 0});
        countryGroupAssignment.put("LC", new int[] {1, 1, 0, 0});
        countryGroupAssignment.put("LI", new int[] {0, 0, 2, 4});
        countryGroupAssignment.put("LK", new int[] {2, 1, 2, 3});
        countryGroupAssignment.put("LR", new int[] {3, 4, 3, 1});
        countryGroupAssignment.put("LS", new int[] {3, 3, 2, 0});
        countryGroupAssignment.put("LT", new int[] {0, 0, 0, 0});
        countryGroupAssignment.put("LU", new int[] {0, 0, 0, 0});
        countryGroupAssignment.put("LV", new int[] {0, 0, 0, 0});
        countryGroupAssignment.put("LY", new int[] {4, 4, 4, 4});
        countryGroupAssignment.put("MA", new int[] {2, 1, 2, 1});
        countryGroupAssignment.put("MC", new int[] {0, 0, 0, 1});
        countryGroupAssignment.put("MD", new int[] {1, 1, 0, 0});
        countryGroupAssignment.put("ME", new int[] {1, 2, 1, 2});
        countryGroupAssignment.put("MF", new int[] {1, 1, 1, 1});
        countryGroupAssignment.put("MG", new int[] {3, 4, 2, 2});
        countryGroupAssignment.put("MH", new int[] {4, 0, 2, 4});
        countryGroupAssignment.put("MK", new int[] {1, 0, 0, 0});
        countryGroupAssignment.put("ML", new int[] {4, 4, 2, 0});
        countryGroupAssignment.put("MM", new int[] {3, 3, 1, 2});
        countryGroupAssignment.put("MN", new int[] {2, 3, 2, 3});
        countryGroupAssignment.put("MO", new int[] {0, 0, 4, 4});
        countryGroupAssignment.put("MP", new int[] {0, 2, 4, 4});
        countryGroupAssignment.put("MQ", new int[] {2, 1, 1, 4});
        countryGroupAssignment.put("MR", new int[] {4, 2, 4, 2});
        countryGroupAssignment.put("MS", new int[] {1, 2, 3, 3});
        countryGroupAssignment.put("MT", new int[] {0, 1, 0, 0});
        countryGroupAssignment.put("MU", new int[] {2, 2, 3, 4});
        countryGroupAssignment.put("MV", new int[] {4, 3, 0, 2});
        countryGroupAssignment.put("MW", new int[] {3, 2, 1, 0});
        countryGroupAssignment.put("MX", new int[] {2, 4, 4, 3});
        countryGroupAssignment.put("MY", new int[] {2, 2, 3, 3});
        countryGroupAssignment.put("MZ", new int[] {3, 3, 2, 1});
        countryGroupAssignment.put("NA", new int[] {3, 3, 2, 1});
        countryGroupAssignment.put("NC", new int[] {2, 0, 3, 3});
        countryGroupAssignment.put("NE", new int[] {4, 4, 4, 3});
        countryGroupAssignment.put("NF", new int[] {1, 2, 2, 2});
        countryGroupAssignment.put("NG", new int[] {3, 4, 3, 1});
        countryGroupAssignment.put("NI", new int[] {3, 3, 4, 4});
        countryGroupAssignment.put("NL", new int[] {0, 2, 3, 3});
        countryGroupAssignment.put("NO", new int[] {0, 1, 1, 0});
        countryGroupAssignment.put("NP", new int[] {2, 2, 2, 2});
        countryGroupAssignment.put("NR", new int[] {4, 0, 3, 1});
        countryGroupAssignment.put("NZ", new int[] {0, 0, 1, 2});
        countryGroupAssignment.put("OM", new int[] {3, 2, 1, 3});
        countryGroupAssignment.put("PA", new int[] {1, 3, 3, 4});
        countryGroupAssignment.put("PE", new int[] {2, 3, 4, 4});
        countryGroupAssignment.put("PF", new int[] {2, 2, 0, 1});
        countryGroupAssignment.put("PG", new int[] {4, 3, 3, 1});
        countryGroupAssignment.put("PH", new int[] {3, 0, 3, 4});
        countryGroupAssignment.put("PK", new int[] {3, 3, 3, 3});
        countryGroupAssignment.put("PL", new int[] {1, 0, 1, 3});
        countryGroupAssignment.put("PM", new int[] {0, 2, 2, 0});
        countryGroupAssignment.put("PR", new int[] {1, 2, 3, 3});
        countryGroupAssignment.put("PS", new int[] {3, 3, 2, 4});
        countryGroupAssignment.put("PT", new int[] {1, 1, 0, 0});
        countryGroupAssignment.put("PW", new int[] {2, 1, 2, 0});
        countryGroupAssignment.put("PY", new int[] {2, 0, 2, 3});
        countryGroupAssignment.put("QA", new int[] {2, 2, 1, 2});
        countryGroupAssignment.put("RE", new int[] {1, 0, 2, 2});
        countryGroupAssignment.put("RO", new int[] {0, 1, 1, 2});
        countryGroupAssignment.put("RS", new int[] {1, 2, 0, 0});
        countryGroupAssignment.put("RU", new int[] {0, 1, 1, 1});
        countryGroupAssignment.put("RW", new int[] {4, 4, 2, 4});
        countryGroupAssignment.put("SA", new int[] {2, 2, 2, 1});
        countryGroupAssignment.put("SB", new int[] {4, 4, 3, 0});
        countryGroupAssignment.put("SC", new int[] {4, 2, 0, 1});
        countryGroupAssignment.put("SD", new int[] {4, 4, 4, 3});
        countryGroupAssignment.put("SE", new int[] {0, 1, 0, 0});
        countryGroupAssignment.put("SG", new int[] {0, 2, 3, 3});
        countryGroupAssignment.put("SH", new int[] {4, 4, 2, 3});
        countryGroupAssignment.put("SI", new int[] {0, 0, 0, 0});
        countryGroupAssignment.put("SJ", new int[] {2, 0, 2, 4});
        countryGroupAssignment.put("SK", new int[] {0, 1, 0, 0});
        countryGroupAssignment.put("SL", new int[] {4, 3, 3, 3});
        countryGroupAssignment.put("SM", new int[] {0, 0, 2, 4});
        countryGroupAssignment.put("SN", new int[] {3, 4, 4, 2});
        countryGroupAssignment.put("SO", new int[] {3, 4, 4, 3});
        countryGroupAssignment.put("SR", new int[] {2, 2, 1, 0});
        countryGroupAssignment.put("SS", new int[] {4, 3, 4, 3});
        countryGroupAssignment.put("ST", new int[] {3, 4, 2, 2});
        countryGroupAssignment.put("SV", new int[] {2, 3, 3, 4});
        countryGroupAssignment.put("SX", new int[] {2, 4, 1, 0});
        countryGroupAssignment.put("SY", new int[] {4, 3, 2, 1});
        countryGroupAssignment.put("SZ", new int[] {4, 4, 3, 4});
        countryGroupAssignment.put("TC", new int[] {1, 2, 1, 1});
        countryGroupAssignment.put("TD", new int[] {4, 4, 4, 2});
        countryGroupAssignment.put("TG", new int[] {3, 3, 1, 0});
        countryGroupAssignment.put("TH", new int[] {1, 3, 4, 4});
        countryGroupAssignment.put("TJ", new int[] {4, 4, 4, 4});
        countryGroupAssignment.put("TL", new int[] {4, 2, 4, 4});
        countryGroupAssignment.put("TM", new int[] {4, 1, 2, 2});
        countryGroupAssignment.put("TN", new int[] {2, 2, 1, 2});
        countryGroupAssignment.put("TO", new int[] {3, 3, 3, 1});
        countryGroupAssignment.put("TR", new int[] {2, 2, 1, 2});
        countryGroupAssignment.put("TT", new int[] {1, 3, 1, 2});
        countryGroupAssignment.put("TV", new int[] {4, 2, 2, 4});
        countryGroupAssignment.put("TW", new int[] {0, 0, 0, 0});
        countryGroupAssignment.put("TZ", new int[] {3, 3, 4, 3});
        countryGroupAssignment.put("UA", new int[] {0, 2, 1, 2});
        countryGroupAssignment.put("UG", new int[] {4, 3, 3, 2});
        countryGroupAssignment.put("US", new int[] {1, 1, 3, 3});
        countryGroupAssignment.put("UY", new int[] {2, 2, 1, 1});
        countryGroupAssignment.put("UZ", new int[] {2, 2, 2, 2});
        countryGroupAssignment.put("VA", new int[] {1, 2, 4, 2});
        countryGroupAssignment.put("VC", new int[] {2, 0, 2, 4});
        countryGroupAssignment.put("VE", new int[] {4, 4, 4, 3});
        countryGroupAssignment.put("VG", new int[] {3, 0, 1, 3});
        countryGroupAssignment.put("VI", new int[] {1, 1, 4, 4});
        countryGroupAssignment.put("VN", new int[] {0, 2, 4, 4});
        countryGroupAssignment.put("VU", new int[] {4, 1, 3, 1});
        countryGroupAssignment.put("WS", new int[] {3, 3, 3, 2});
        countryGroupAssignment.put("XK", new int[] {1, 2, 1, 0});
        countryGroupAssignment.put("YE", new int[] {4, 4, 4, 3});
        countryGroupAssignment.put("YT", new int[] {2, 2, 2, 3});
        countryGroupAssignment.put("ZA", new int[] {2, 4, 2, 2});
        countryGroupAssignment.put("ZM", new int[] {3, 2, 2, 1});
        countryGroupAssignment.put("ZW", new int[] {3, 3, 2, 1});
        return Collections.unmodifiableMap(countryGroupAssignment);
    }
}
