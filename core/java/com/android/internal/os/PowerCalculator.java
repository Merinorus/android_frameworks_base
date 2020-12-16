/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.internal.os;

import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.UidBatteryConsumer;
import android.os.UserHandle;
import android.util.SparseArray;

import java.util.List;
import java.util.Locale;

/**
 * Calculates power use of a device subsystem for an app.
 */
public abstract class PowerCalculator {

    /**
     * Attributes the total amount of power used by this subsystem to various consumers such
     * as apps.
     *
     * @param sippers       A list of battery sippers that contains battery attribution data.
     *                      The calculator may modify the list.
     * @param batteryStats  The recorded battery stats.
     * @param rawRealtimeUs The raw system realtime in microseconds.
     * @param rawUptimeUs   The raw system uptime in microseconds.
     * @param statsType     The type of stats. As of {@link android.os.Build.VERSION_CODES#Q}, this
     *                      can only be {@link BatteryStats#STATS_SINCE_CHARGED}, since
     *                      {@link BatteryStats#STATS_CURRENT} and
     *                      {@link BatteryStats#STATS_SINCE_UNPLUGGED} are deprecated.
     * @param asUsers       An array of users for which the attribution is requested.  It may
     *                      contain {@link UserHandle#USER_ALL} to indicate that the attribution
     *                      should be performed for all users.
     */
    public void calculate(List<BatterySipper> sippers, BatteryStats batteryStats,
            long rawRealtimeUs, long rawUptimeUs, int statsType, SparseArray<UserHandle> asUsers) {
        for (int i = sippers.size() - 1; i >= 0; i--) {
            final BatterySipper app = sippers.get(i);
            if (app.drainType == BatterySipper.DrainType.APP) {
                calculateApp(app, app.uidObj, rawRealtimeUs, rawUptimeUs, statsType);
            }
        }
    }

    /**
     * Attributes the total amount of power used by this subsystem to various consumers such
     * as apps.
     *
     * @param builder       {@link BatteryUsageStats.Builder that contains a list of
     *                      per-UID battery consumer builders for attribution data.
     *                      The calculator may modify the builder and its constituent parts.
     * @param batteryStats  The recorded battery stats.
     * @param rawRealtimeUs The raw system realtime in microseconds.
     * @param rawUptimeUs   The raw system uptime in microseconds.
     * @param statsType     The type of stats. As of {@link android.os.Build.VERSION_CODES#Q}, this
     *                      can only be {@link BatteryStats#STATS_SINCE_CHARGED}, since
     *                      {@link BatteryStats#STATS_CURRENT} and
     *                      {@link BatteryStats#STATS_SINCE_UNPLUGGED} are deprecated.
     * @param asUsers       An array of users for which the attribution is requested.  It may
     *                      contain {@link UserHandle#USER_ALL} to indicate that the attribution
     *                      should be performed for all users.
     */
    public void calculate(BatteryUsageStats.Builder builder, BatteryStats batteryStats,
            long rawRealtimeUs, long rawUptimeUs, int statsType, SparseArray<UserHandle> asUsers) {
        final SparseArray<UidBatteryConsumer.Builder> uidBatteryConsumerBuilders =
                builder.getUidBatteryConsumerBuilders();
        for (int i = uidBatteryConsumerBuilders.size() - 1; i >= 0; i--) {
            final UidBatteryConsumer.Builder app = uidBatteryConsumerBuilders.valueAt(i);
            calculateApp(app, app.getBatteryStatsUid(), rawRealtimeUs, rawUptimeUs, statsType);
        }
    }

    /**
     * Calculate the amount of power an app used for this subsystem.
     * @param app The BatterySipper that represents the power use of an app.
     * @param u The recorded stats for the app.
     * @param rawRealtimeUs The raw system realtime in microseconds.
     * @param rawUptimeUs The raw system uptime in microseconds.
     * @param statsType The type of stats. As of {@link android.os.Build.VERSION_CODES#Q}, this can
     *                  only be {@link BatteryStats#STATS_SINCE_CHARGED}, since
     *                  {@link BatteryStats#STATS_CURRENT} and
     *                  {@link BatteryStats#STATS_SINCE_UNPLUGGED} are deprecated.
     */
    protected void calculateApp(BatterySipper app, BatteryStats.Uid u, long rawRealtimeUs,
                                      long rawUptimeUs, int statsType) {

        // TODO(b/175156498): Temporary code during the transition from BatterySippers to
        //  BatteryConsumers.
        UidBatteryConsumer.Builder builder = new UidBatteryConsumer.Builder(0, 0, u);
        calculateApp(builder, u, rawRealtimeUs, rawUptimeUs, statsType);
        final UidBatteryConsumer uidBatteryConsumer = builder.build();
        app.cpuPowerMah = uidBatteryConsumer.getConsumedPower(
                UidBatteryConsumer.POWER_COMPONENT_CPU);
        app.cpuTimeMs = uidBatteryConsumer.getUsageDurationMillis(
                UidBatteryConsumer.TIME_COMPONENT_CPU);
        app.cpuFgTimeMs = uidBatteryConsumer.getUsageDurationMillis(
                UidBatteryConsumer.TIME_COMPONENT_CPU_FOREGROUND);
        app.packageWithHighestDrain = uidBatteryConsumer.getPackageWithHighestDrain();
    }

    /**
     * Calculate the amount of power an app used for this subsystem.
     * @param app The UidBatteryConsumer.Builder that represents the power use of an app.
     * @param u The recorded stats for the app.
     * @param rawRealtimeUs The raw system realtime in microseconds.
     * @param rawUptimeUs The raw system uptime in microseconds.
     * @param statsType The type of stats. As of {@link android.os.Build.VERSION_CODES#Q}, this can
     *                  only be {@link BatteryStats#STATS_SINCE_CHARGED}, since
     *                  {@link BatteryStats#STATS_CURRENT} and
     *                  {@link BatteryStats#STATS_SINCE_UNPLUGGED} are deprecated.
     */
    protected void calculateApp(UidBatteryConsumer.Builder app, BatteryStats.Uid u,
            long rawRealtimeUs, long rawUptimeUs, int statsType) {
    }

    /**
     * Reset any state maintained in this calculator.
     */
    public void reset() {
    }

    /**
     * Converts charge in mAh to string.
     */
    public static String formatCharge(double power) {
        if (power == 0) return "0";

        final String format;
        if (power < .00001) {
            format = "%.8f";
        } else if (power < .0001) {
            format = "%.7f";
        } else if (power < .001) {
            format = "%.6f";
        } else if (power < .01) {
            format = "%.5f";
        } else if (power < .1) {
            format = "%.4f";
        } else if (power < 1) {
            format = "%.3f";
        } else if (power < 10) {
            format = "%.2f";
        } else if (power < 100) {
            format = "%.1f";
        } else {
            format = "%.0f";
        }

        // Use English locale because this is never used in UI (only in checkin and dump).
        return String.format(Locale.ENGLISH, format, power);
    }
}
