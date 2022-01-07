/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.beam.sdk.io.kinesis;

import static org.apache.beam.sdk.io.kinesis.RateLimitPolicyFactory.withDefaultRateLimiter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.Duration.millis;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import java.util.concurrent.atomic.AtomicLong;
import org.apache.beam.sdk.io.kinesis.RateLimitPolicyFactory.DefaultRateLimiter;
import org.apache.beam.sdk.util.BackOff;
import org.apache.beam.sdk.util.Sleeper;
import org.apache.beam.vendor.guava.v26_0_jre.com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(RateLimitPolicyFactory.class)
public class RateLimitPolicyFactoryTest {

  @Test
  public void defaultRateLimiterShouldUseBackoffs() throws Exception {
    assertThat(withDefaultRateLimiter().getRateLimitPolicy())
        .isInstanceOf(DefaultRateLimiter.class);
    assertThat(withDefaultRateLimiter(millis(1), millis(1), millis(1)).getRateLimitPolicy())
        .isInstanceOf(DefaultRateLimiter.class);

    Sleeper sleeper = mock(Sleeper.class);
    BackOff emptySuccess = mock(BackOff.class);
    BackOff throttled = mock(BackOff.class);

    RateLimitPolicy policy = new DefaultRateLimiter(emptySuccess, throttled, sleeper);

    // reset emptySuccess after receiving at least 1 record, throttled is reset on any success
    policy.onSuccess(ImmutableList.of(mock(KinesisRecord.class)));

    verify(emptySuccess).reset();
    verify(throttled).reset();
    verifyNoInteractions(sleeper);
    clearInvocations(emptySuccess, throttled);

    when(emptySuccess.nextBackOffMillis()).thenReturn(88L, 99L);
    // throttle if no records received, throttled is reset again
    policy.onSuccess(ImmutableList.of());
    policy.onSuccess(ImmutableList.of());

    verify(emptySuccess, times(2)).nextBackOffMillis();
    verify(throttled, times(2)).reset();
    verify(sleeper).sleep(88L);
    verify(sleeper).sleep(99L);
    verifyNoMoreInteractions(sleeper, throttled, emptySuccess);
    clearInvocations(emptySuccess, throttled, sleeper);

    when(throttled.nextBackOffMillis()).thenReturn(111L, 222L);
    // throttle onThrottle
    policy.onThrottle(mock(KinesisClientThrottledException.class));
    policy.onThrottle(mock(KinesisClientThrottledException.class));

    verify(throttled, times(2)).nextBackOffMillis();
    verify(sleeper).sleep(111L);
    verify(sleeper).sleep(222L);
    verifyNoMoreInteractions(sleeper, throttled, emptySuccess);
  }

  @Test
  public void withoutLimiterShouldDoNothing() throws Exception {
    PowerMockito.spy(Thread.class);
    PowerMockito.doNothing().when(Thread.class);
    Thread.sleep(anyLong());
    RateLimitPolicy rateLimitPolicy = RateLimitPolicyFactory.withoutLimiter().getRateLimitPolicy();
    rateLimitPolicy.onSuccess(ImmutableList.of());
    verifyStatic(Thread.class, never());
    Thread.sleep(anyLong());
  }

  @Test
  public void shouldDelayDefaultInterval() throws Exception {
    PowerMockito.spy(Thread.class);
    PowerMockito.doNothing().when(Thread.class);
    Thread.sleep(anyLong());
    RateLimitPolicy rateLimitPolicy = RateLimitPolicyFactory.withFixedDelay().getRateLimitPolicy();
    rateLimitPolicy.onSuccess(ImmutableList.of());
    verifyStatic(Thread.class);
    Thread.sleep(eq(1000L));
  }

  @Test
  public void shouldDelayFixedInterval() throws Exception {
    PowerMockito.spy(Thread.class);
    PowerMockito.doNothing().when(Thread.class);
    Thread.sleep(anyLong());
    RateLimitPolicy rateLimitPolicy =
        RateLimitPolicyFactory.withFixedDelay(millis(500)).getRateLimitPolicy();
    rateLimitPolicy.onSuccess(ImmutableList.of());
    verifyStatic(Thread.class);
    Thread.sleep(eq(500L));
  }

  @Test
  public void shouldDelayDynamicInterval() throws Exception {
    PowerMockito.spy(Thread.class);
    PowerMockito.doNothing().when(Thread.class);
    Thread.sleep(anyLong());
    AtomicLong delay = new AtomicLong(0L);
    RateLimitPolicy rateLimitPolicy =
        RateLimitPolicyFactory.withDelay(() -> millis(delay.getAndUpdate(d -> d ^ 1)))
            .getRateLimitPolicy();
    rateLimitPolicy.onSuccess(ImmutableList.of());
    verifyStatic(Thread.class);
    Thread.sleep(eq(0L));
    Thread.sleep(eq(1L));
    Thread.sleep(eq(0L));
    Thread.sleep(eq(1L));
  }
}
