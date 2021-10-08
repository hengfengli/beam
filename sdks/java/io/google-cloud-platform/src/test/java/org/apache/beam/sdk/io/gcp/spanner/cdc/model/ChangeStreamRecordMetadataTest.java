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
package org.apache.beam.sdk.io.gcp.spanner.cdc.model;

import static org.junit.Assert.assertEquals;

import com.google.cloud.Timestamp;
import org.junit.Test;

public class ChangeStreamRecordMetadataTest {

  @Test
  public void testBuilder() {
    final ChangeStreamRecordMetadata expected =
        new ChangeStreamRecordMetadata(
            "1",
            Timestamp.ofTimeMicroseconds(2L),
            Timestamp.ofTimeMicroseconds(3L),
            Timestamp.ofTimeMicroseconds(4L),
            Timestamp.ofTimeMicroseconds(5L),
            Timestamp.ofTimeMicroseconds(6L),
            Timestamp.ofTimeMicroseconds(7L),
            Timestamp.ofTimeMicroseconds(8L),
            Timestamp.ofTimeMicroseconds(9L),
            Timestamp.ofTimeMicroseconds(10L),
            Timestamp.ofTimeMicroseconds(11L),
            12L,
            13L);
    final ChangeStreamRecordMetadata actual =
        ChangeStreamRecordMetadata.newBuilder()
            .withPartitionToken("1")
            .withRecordTimestamp(Timestamp.ofTimeMicroseconds(2L))
            .withPartitionStartTimestamp(Timestamp.ofTimeMicroseconds(3L))
            .withPartitionEndTimestamp(Timestamp.ofTimeMicroseconds(4L))
            .withPartitionCreatedAt(Timestamp.ofTimeMicroseconds(5L))
            .withPartitionScheduledAt(Timestamp.ofTimeMicroseconds(6L))
            .withPartitionRunningAt(Timestamp.ofTimeMicroseconds(7L))
            .withQueryStartedAt(Timestamp.ofTimeMicroseconds(8L))
            .withRecordStreamStartedAt(Timestamp.ofTimeMicroseconds(9L))
            .withRecordStreamEndedAt(Timestamp.ofTimeMicroseconds(10L))
            .withRecordReadAt(Timestamp.ofTimeMicroseconds(11L))
            .withTotalStreamTimeMillis(12L)
            .withNumberOfRecordsRead(13L)
            .build();

    assertEquals(expected, actual);
  }
}
