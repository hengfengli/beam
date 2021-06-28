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
package org.apache.beam.sdk.io.gcp.spanner.cdc;

import static org.apache.beam.sdk.io.gcp.spanner.cdc.dao.PartitionMetadataDao.COLUMN_CREATED_AT;
import static org.apache.beam.sdk.io.gcp.spanner.cdc.dao.PartitionMetadataDao.COLUMN_END_TIMESTAMP;
import static org.apache.beam.sdk.io.gcp.spanner.cdc.dao.PartitionMetadataDao.COLUMN_HEARTBEAT_MILLIS;
import static org.apache.beam.sdk.io.gcp.spanner.cdc.dao.PartitionMetadataDao.COLUMN_INCLUSIVE_END;
import static org.apache.beam.sdk.io.gcp.spanner.cdc.dao.PartitionMetadataDao.COLUMN_INCLUSIVE_START;
import static org.apache.beam.sdk.io.gcp.spanner.cdc.dao.PartitionMetadataDao.COLUMN_PARENT_TOKENS;
import static org.apache.beam.sdk.io.gcp.spanner.cdc.dao.PartitionMetadataDao.COLUMN_PARTITION_TOKEN;
import static org.apache.beam.sdk.io.gcp.spanner.cdc.dao.PartitionMetadataDao.COLUMN_START_TIMESTAMP;
import static org.apache.beam.sdk.io.gcp.spanner.cdc.dao.PartitionMetadataDao.COLUMN_STATE;
import static org.apache.beam.sdk.io.gcp.spanner.cdc.dao.PartitionMetadataDao.COLUMN_UPDATED_AT;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import org.apache.beam.sdk.io.gcp.spanner.cdc.dao.PartitionMetadataDao;
import org.apache.beam.sdk.io.gcp.spanner.cdc.model.PartitionMetadata;
import org.apache.beam.sdk.io.gcp.spanner.cdc.model.PartitionMetadata.State;

public class PipelineInitializer {

  private static final long DEFAULT_HEARTBEAT_MILLIS = 5000;

  // TODO: See if we can get away with not passing in the database id, but the generated table name
  // instead
  public static void initialize(
      DatabaseAdminClient databaseAdminClient,
      PartitionMetadataDao partitionMetadataDao,
      DatabaseId id,
      String partitionMetadataTableName,
      Timestamp inclusiveStartAt,
      @Nullable Timestamp exclusiveEndAt) {
    createMetadataTable(databaseAdminClient, id, partitionMetadataTableName);
    createFakeParentPartition(partitionMetadataDao, inclusiveStartAt, exclusiveEndAt);
  }

  private static void createMetadataTable(
      DatabaseAdminClient databaseAdminClient, DatabaseId id, String tableName) {
    final String metadataCreateStmt =
        "CREATE TABLE "
            + tableName
            + " ("
            + COLUMN_PARTITION_TOKEN
            + " STRING(MAX) NOT NULL,"
            + COLUMN_PARENT_TOKENS
            + " ARRAY<STRING(MAX)> NOT NULL,"
            + COLUMN_START_TIMESTAMP
            + " TIMESTAMP NOT NULL,"
            + COLUMN_INCLUSIVE_START
            + " BOOL NOT NULL, "
            + COLUMN_END_TIMESTAMP
            + " TIMESTAMP,"
            + COLUMN_INCLUSIVE_END
            + " BOOL,"
            + COLUMN_HEARTBEAT_MILLIS
            + " INT64 NOT NULL,"
            + COLUMN_STATE
            + " STRING(MAX) NOT NULL,"
            + COLUMN_CREATED_AT
            + " TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true),"
            + COLUMN_UPDATED_AT
            + " TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true)"
            + ") PRIMARY KEY (PartitionToken)";
    OperationFuture<Void, UpdateDatabaseDdlMetadata> op =
        databaseAdminClient.updateDatabaseDdl(
            id.getInstanceId().getInstance(),
            id.getDatabase(),
            Collections.singletonList(metadataCreateStmt),
            null);
    try {
      // Initiate the request which returns an OperationFuture.
      op.get();
    } catch (ExecutionException e) {
      // If the operation failed during execution, expose the cause.
      throw (SpannerException) e.getCause();
    } catch (InterruptedException e) {
      // Throw when a thread is waiting, sleeping, or otherwise occupied,
      // and the thread is interrupted, either before or during the activity.
      throw SpannerExceptionFactory.propagateInterrupt(e);
    }
  }

  private static void createFakeParentPartition(
      PartitionMetadataDao partitionMetadataDao,
      Timestamp inclusiveStartAt,
      @Nullable Timestamp exclusiveEndAt) {
    PartitionMetadata parentPartition =
        PartitionMetadata.newBuilder()
            .setPartitionToken(InitialPartition.PARTITION_TOKEN)
            .setParentTokens(InitialPartition.PARENT_TOKENS)
            .setStartTimestamp(inclusiveStartAt)
            .setEndTimestamp(exclusiveEndAt)
            .setHeartbeatMillis(DEFAULT_HEARTBEAT_MILLIS)
            .setState(State.CREATED)
            .build();
    partitionMetadataDao.insert(parentPartition);
  }
}
