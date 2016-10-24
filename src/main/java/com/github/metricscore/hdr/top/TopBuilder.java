/*
 *    Copyright 2016 Vladimir Bukhtoyarov
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.github.metricscore.hdr.top;

import com.github.metricscore.hdr.util.Clock;

import java.time.Duration;
import java.util.concurrent.Executor;

public class TopBuilder {

    public static final long MIN_CHUNK_RESETTING_INTERVAL_MILLIS = 1000;
    public static final int MAX_CHUNKS = 25;
    public static final int MIN_LENGTH_OF_QUERY_DESCRIPTION = 10;
    public static final int DEFAULT_MAX_LENGTH_OF_QUERY_DESCRIPTION = 1024;

    public static final Duration DEFAULT_SLOW_QUERY_THRESHOLD = Duration.ZERO;
    public static final Duration DEFAULT_SNAPSHOT_CACHING_DURATION = Duration.ZERO;

    private static final Executor DEFAULT_BACKGROUND_EXECUTOR = null;
    private static final TopFactory DEFAULT_TOP_FACTORY = TopFactory.UNIFORM;

    private int positionCount;
    private Duration slowQueryThreshold;
    private Duration snapshotCachingDuration;
    private int maxLengthOfQueryDescription;
    private Clock clock;
    private Executor backgroundExecutor;
    private TopFactory factory;

    private TopBuilder(int positionCount, Duration slowQueryThreshold, Duration snapshotCachingDuration, int maxLengthOfQueryDescription, Clock clock, Executor backgroundExecutor, TopFactory factory) {
        this.positionCount = positionCount;
        this.slowQueryThreshold = slowQueryThreshold;
        this.snapshotCachingDuration = snapshotCachingDuration;
        this.maxLengthOfQueryDescription = maxLengthOfQueryDescription;
        this.clock = clock;
        this.backgroundExecutor = backgroundExecutor;
        this.factory = factory;
    }

    public static TopBuilder newBuilder(int positionCount) {
        if (positionCount <= 1) {
            throw new IllegalArgumentException("positionCount should be >=1");
        }
        return new TopBuilder(positionCount, DEFAULT_SLOW_QUERY_THRESHOLD, DEFAULT_SNAPSHOT_CACHING_DURATION, DEFAULT_MAX_LENGTH_OF_QUERY_DESCRIPTION, Clock.defaultClock(), DEFAULT_BACKGROUND_EXECUTOR, DEFAULT_TOP_FACTORY);
    }

    @Override
    public TopBuilder clone() {
        return new TopBuilder(positionCount, slowQueryThreshold, snapshotCachingDuration, maxLengthOfQueryDescription, clock, backgroundExecutor, factory);
    }

    public TopBuilder withPositionCount(int positionCount) {
        if (positionCount <= 1) {
            throw new IllegalArgumentException("positionCount should be >=1");
        }
        this.positionCount = positionCount;
        return this;
    }

    public TopBuilder withSlowQueryThreshold(Duration slowQueryThreshold) {
        if (slowQueryThreshold == null) {
            throw new IllegalArgumentException("slowQueryThreshold should not be null");
        }
        if (slowQueryThreshold.isNegative()) {
            throw new IllegalArgumentException("slowQueryThreshold should not be negative");
        }
        this.slowQueryThreshold = slowQueryThreshold;
        return this;
    }

    public TopBuilder withSnapshotCachingDuration(Duration snapshotCachingDuration) {
        if (snapshotCachingDuration == null) {
            throw new IllegalArgumentException("snapshotCachingDuration should not be null");
        }
        if (snapshotCachingDuration.isNegative()) {
            throw new IllegalArgumentException("snapshotCachingDuration can not be negative");
        }
        this.snapshotCachingDuration = snapshotCachingDuration;
        return this;
    }

    public TopBuilder withMaxLengthOfQueryDescription(int maxLengthOfQueryDescription) {
        if (maxLengthOfQueryDescription < MIN_LENGTH_OF_QUERY_DESCRIPTION) {
            String msg = "The requested maxLengthOfQueryDescription=" + maxLengthOfQueryDescription + " is wrong " +
                    "because of maxLengthOfQueryDescription should be >=" + MIN_LENGTH_OF_QUERY_DESCRIPTION + "." +
                    "How do you plan to distinguish one query from another with so short description?";
            throw new IllegalArgumentException(msg);
        }
        this.maxLengthOfQueryDescription = maxLengthOfQueryDescription;
        return this;
    }

    public TopBuilder withClock(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("Clock should not be null");
        }
        this.clock = clock;
        return this;
    }

    public TopBuilder withBackgroundExecutor(Executor backgroundExecutor) {
        if (backgroundExecutor == null) {
            throw new IllegalArgumentException("Clock should not be null");
        }
        this.backgroundExecutor = backgroundExecutor;
        return this;
    }

    public TopBuilder neverResetPostions() {
        this.factory = TopFactory.UNIFORM;
        return this;
    }

    public TopBuilder createResetOnSnapshotTop() {
        this.factory = TopFactory.RESET_ON_SNAPSHOT;
        return this;
    }

    public TopBuilder resetAllPositionsPeriodically(Duration intervalBetweenResetting) {
        if (intervalBetweenResetting == null) {
            throw new IllegalArgumentException("intervalBetweenResetting should not be null");
        }
        if (intervalBetweenResetting.isNegative()) {
            throw new IllegalArgumentException("intervalBetweenResetting should not be negative");
        }
        long intervalBetweenResettingMillis = intervalBetweenResetting.toMillis();
        if (intervalBetweenResettingMillis < MIN_CHUNK_RESETTING_INTERVAL_MILLIS) {
            throw new IllegalArgumentException("");
        }
        this.factory = TopFactory.resetByChunks(intervalBetweenResettingMillis, 0);
        return this;
    }

    public TopBuilder resetAllPositionsPeriodicallyByChunks(Duration rollingWindow, int numberChunks) {
        if (numberChunks > MAX_CHUNKS) {
            throw new IllegalArgumentException("numberChunks should be <= " + MAX_CHUNKS);
        }
        if (rollingWindow == null) {
            throw new IllegalArgumentException("rollingWindow should not be null");
        }
        if (rollingWindow.isNegative()) {
            throw new IllegalArgumentException("rollingWindow should not be negative");
        }
        if (numberChunks < 2) {
            throw new IllegalArgumentException("numberChunks should be >= 1");
        }

        long intervalBetweenResettingMillis = rollingWindow.toMillis() / numberChunks;
        if (intervalBetweenResettingMillis < MIN_CHUNK_RESETTING_INTERVAL_MILLIS) {
            String msg = "interval between resetting one chunk should be >= " + MIN_CHUNK_RESETTING_INTERVAL_MILLIS + " millis";
            throw new IllegalArgumentException(msg);
        }
        return this;
    }


    private interface TopFactory {

        Top create(int positionCount, Duration slowQueryThreshold, int maxLengthOfQueryDescription, Clock clock, Executor backgroundExecutor);

        TopFactory UNIFORM = new TopFactory() {
            @Override
            public Top create(int positionCount, Duration slowQueryThreshold, int maxLengthOfQueryDescription, Clock clock, Executor backgroundExecutor) {
                return new UniformTop(positionCount, slowQueryThreshold);
            }
        };

        TopFactory RESET_ON_SNAPSHOT = new TopFactory() {
            @Override
            public Top create(int positionCount, Duration slowQueryThreshold, int maxLengthOfQueryDescription, Clock clock, Executor backgroundExecutor) {
                return new ResetOnSnapshotTop(positionCount, slowQueryThreshold);
            }
        };

        static TopFactory resetByChunks(final long intervalBetweenResettingMillis, int numberOfHistoryChunks) {
            return new TopFactory() {
                @Override
                public Top create(int positionCount, Duration slowQueryThreshold, int maxLengthOfQueryDescription, Clock clock, Executor backgroundExecutor) {
                    return new ResetByChunksTop(positionCount, slowQueryThreshold, intervalBetweenResettingMillis, numberOfHistoryChunks, clock, backgroundExecutor);
                }
            };
        }

    }

}
