/**
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
package org.apache.hadoop.hive.druid.serde;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.hadoop.hive.druid.DruidStorageHandlerUtils;
import org.apache.hadoop.hive.ql.optimizer.calcite.druid.DruidTable;
import org.apache.hadoop.io.NullWritable;

import com.fasterxml.jackson.core.type.TypeReference;

import io.druid.query.Result;
import io.druid.query.timeseries.TimeseriesQuery;
import io.druid.query.timeseries.TimeseriesResultValue;

/**
 * Record reader for results for Druid TimeseriesQuery.
 */
public class DruidTimeseriesQueryRecordReader
        extends DruidQueryRecordReader<TimeseriesQuery, Result<TimeseriesResultValue>> {

  private Result<TimeseriesResultValue> current;

  @Override
  protected TimeseriesQuery createQuery(String content) throws IOException {
    return DruidStorageHandlerUtils.JSON_MAPPER.readValue(content, TimeseriesQuery.class);
  }

  @Override
  protected List<Result<TimeseriesResultValue>> createResultsList(InputStream content) throws IOException {
    return DruidStorageHandlerUtils.SMILE_MAPPER.readValue(content,
            new TypeReference<List<Result<TimeseriesResultValue>>>(){});
  }

  @Override
  public boolean nextKeyValue() {
    if (results.hasNext()) {
      current = results.next();
      return true;
    }
    return false;
  }

  @Override
  public NullWritable getCurrentKey() throws IOException, InterruptedException {
    return NullWritable.get();
  }

  @Override
  public DruidWritable getCurrentValue() throws IOException, InterruptedException {
    // Create new value
    DruidWritable value = new DruidWritable();
    value.getValue().put(DruidTable.DEFAULT_TIMESTAMP_COLUMN, current.getTimestamp().getMillis());
    value.getValue().putAll(current.getValue().getBaseObject());
    return value;
  }

  @Override
  public boolean next(NullWritable key, DruidWritable value) {
    if (nextKeyValue()) {
      // Update value
      value.getValue().clear();
      value.getValue().put(DruidTable.DEFAULT_TIMESTAMP_COLUMN, current.getTimestamp().getMillis());
      value.getValue().putAll(current.getValue().getBaseObject());
      return true;
    }
    return false;
  }

  @Override
  public float getProgress() throws IOException {
    return results.hasNext() ? 0 : 1;
  }

}
