/*
 *
 *  Copyright 2017 Vladimir Bukhtoyarov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.github.rollingmetrics.ranking.impl;


import com.github.rollingmetrics.ranking.Position;
import com.github.rollingmetrics.ranking.Ranking;
import com.github.rollingmetrics.util.CachingSupplier;
import com.github.rollingmetrics.util.Ticker;

import java.time.Duration;
import java.util.List;

public class SnapshotCachingRanking implements Ranking {

    private final Ranking target;
    private final CachingSupplier<List<Position>> cache;

    public SnapshotCachingRanking(Ranking target, Duration cachingDuration, Ticker ticker) {
        this.target = target;
        this.cache = new CachingSupplier<>(cachingDuration, ticker, target::getPositionsInDescendingOrder);
    }

    @Override
    public void update(long weight, Object identity) {
        target.update(weight, identity);
    }

    @Override
    public List<Position> getPositionsInDescendingOrder() {
        return cache.get();
    }

    @Override
    public int getSize() {
        return target.getSize();
    }

    @Override
    public String toString() {
        return "SnapshotCachingTop{" +
                "target=" + target +
                ", cache=" + cache +
                '}';
    }

}