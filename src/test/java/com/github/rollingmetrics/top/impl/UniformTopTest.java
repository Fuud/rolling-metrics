/*
 *
 *  Copyright 2016 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.top.impl;

import com.github.rollingmetrics.top.Top;
import com.github.rollingmetrics.top.TestData;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;


public class UniformTopTest {

    @Test
    public void testCommonAspects() {
        for (int i = 1; i <= 2; i++) {
            Top top = Top.builder(i)
                    .neverResetPositions()
                    .withSnapshotCachingDuration(Duration.ZERO)
                    .withLatencyThreshold(Duration.ofMillis(100))
                    .withMaxLengthOfQueryDescription(1000)
                    .build();
            TopTestUtil.testCommonScenarios(i, top, Duration.ofMillis(100).toNanos(), 1000);
        }
    }

    @Test
    public void test_size_1() throws Exception {
        Top top = Top.builder(1)
                .neverResetPositions()
                .withSnapshotCachingDuration(Duration.ZERO)
                .build();

        TopTestUtil.assertEmpty(top);

        TopTestUtil.update(top, TestData.first);
        TopTestUtil.checkOrder(top, TestData.first);

        TopTestUtil.update(top, TestData.second);
        TopTestUtil.checkOrder(top, TestData.second);

        TopTestUtil.update(top, TestData.first);
        TopTestUtil.checkOrder(top, TestData.second);
    }

    @Test
    public void test_size_3() throws Exception {
        Top top = Top.builder(3)
                .neverResetPositions()
                .withSnapshotCachingDuration(Duration.ZERO)
                .build();

        TopTestUtil.assertEmpty(top);

        TopTestUtil.update(top, TestData.first);
        TopTestUtil.checkOrder(top, TestData.first);

        TopTestUtil.update(top, TestData.second);
        TopTestUtil.checkOrder(top, TestData.second, TestData.first);

        TopTestUtil.update(top, TestData.third);
        TopTestUtil.checkOrder(top, TestData.third, TestData.second, TestData.first);

        TopTestUtil.update(top, TestData.fourth);
        TopTestUtil.checkOrder(top, TestData.fourth, TestData.third, TestData.second);

        TopTestUtil.update(top, TestData.fifth);
        TopTestUtil.checkOrder(top, TestData.fifth, TestData.fourth, TestData.third);

        TopTestUtil.update(top, TestData.first);
        TopTestUtil.checkOrder(top, TestData.fifth, TestData.fourth, TestData.third);

        TopTestUtil.update(top, TestData.fifth);
        TopTestUtil.checkOrder(top, TestData.fifth, TestData.fourth, TestData.third);
    }

    @Test
    public void testToString() {
        for (int i = 1; i <= 2; i++) {
            System.out.println(Top.builder(i)
                    .neverResetPositions()
                    .build());
        }
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHung() throws InterruptedException {
        Top top = Top.builder(1)
                .neverResetPositions()
                .withSnapshotCachingDuration(Duration.ZERO)
                .build();
        TopTestUtil.runInParallel(top, TimeUnit.SECONDS.toMillis(30), 0, 10_000);
    }

}