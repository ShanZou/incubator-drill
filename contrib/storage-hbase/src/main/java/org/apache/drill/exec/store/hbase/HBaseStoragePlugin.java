/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.hbase;

import java.io.IOException;
import java.util.ArrayList;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.hydromatic.optiq.Schema;
import net.hydromatic.optiq.SchemaPlus;
import org.apache.drill.common.logical.data.Scan;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.AbstractStoragePlugin;

import com.google.common.base.Preconditions;

public class HBaseStoragePlugin extends AbstractStoragePlugin {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HBaseStoragePlugin.class);

  private final DrillbitContext context;
  private final HBaseStoragePluginConfig engineConfig;
  private final HBaseSchemaFactory schemaFactory;
  private final String name;

  public HBaseStoragePlugin(HBaseStoragePluginConfig configuration, DrillbitContext context, String name)
      throws IOException {
    this.context = context;
    this.schemaFactory = new HBaseSchemaFactory(configuration, name);
    this.engineConfig = configuration;
    this.name = name;
  }

  public HBaseStoragePluginConfig getEngineConfig() {
    return engineConfig;
  }

  public DrillbitContext getContext() {
    return this.context;
  }

  @Override
  public boolean supportsRead() {
    return true;
  }

  @Override
  public HBaseGroupScan getPhysicalScan(Scan scan) throws IOException {
    HTableReadEntry readEntry = scan.getSelection().getListWith(new ObjectMapper(),
        new TypeReference<HTableReadEntry>() {});

    return new HBaseGroupScan(readEntry.getTableName(), this, scan.getOutputReference(), null);
  }

  @Override
  public Schema createAndAddSchema(SchemaPlus parent) {
    return schemaFactory.add(parent);
  }

}