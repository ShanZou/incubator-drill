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
package org.apache.drill.exec.physical.impl.TopN;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import mockit.Injectable;
import mockit.NonStrictExpectations;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.util.FileUtils;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.memory.TopLevelAllocator;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.FragmentRoot;
import org.apache.drill.exec.physical.impl.ImplCreator;
import org.apache.drill.exec.physical.impl.OperatorCreatorRegistry;
import org.apache.drill.exec.physical.impl.SimpleRootExec;
import org.apache.drill.exec.planner.PhysicalPlanReader;
import org.apache.drill.exec.pop.PopUnitTestBase;
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.proto.CoordinationProtos;
import org.apache.drill.exec.proto.UserProtos;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.rpc.user.QueryResultBatch;
import org.apache.drill.exec.rpc.user.UserServer.UserClientConnection;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.RemoteServiceSet;
import org.apache.drill.exec.vector.BigIntVector;
import org.apache.drill.exec.vector.IntVector;
import org.junit.AfterClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestSimpleTopN extends PopUnitTestBase {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestSimpleTopN.class);
  DrillConfig c = DrillConfig.create();
  
  
  @Test
  public void sortOneKeyAscending() throws Throwable{
    RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();

    try(Drillbit bit1 = new Drillbit(CONFIG, serviceSet);
        Drillbit bit2 = new Drillbit(CONFIG, serviceSet);
        DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator());) {

      bit1.run();
      bit2.run();
      client.connect();
      List<QueryResultBatch> results = client.runQuery(UserProtos.QueryType.PHYSICAL,
              Files.toString(FileUtils.getResourceAsFile("/topN/one_key_sort.json"),
                      Charsets.UTF_8));
      int count = 0;
      for(QueryResultBatch b : results) {
        if (b.getHeader().getRowCount() != 0)
          count += b.getHeader().getRowCount();
      }
      assertEquals(100, count);

      long previousBigInt = Long.MIN_VALUE;

      int recordCount = 0;
      int batchCount = 0;

      for (QueryResultBatch b : results) {
        if (b.getHeader().getRowCount() == 0) break;
        batchCount++;
        RecordBatchLoader loader = new RecordBatchLoader(bit1.getContext().getAllocator());
        loader.load(b.getHeader().getDef(),b.getData());
        BigIntVector c1 = (BigIntVector) loader.getValueAccessorById(loader.getValueVectorId(new SchemaPath("blue", ExpressionPosition.UNKNOWN)).getFieldId(), BigIntVector.class).getValueVector();


        BigIntVector.Accessor a1 = c1.getAccessor();
//        IntVector.Accessor a2 = c2.getAccessor();

        for(int i =0; i < c1.getAccessor().getValueCount(); i++){
          recordCount++;
          assertTrue(previousBigInt <= a1.get(i));
          previousBigInt = a1.get(i);
        }
      }

      System.out.println(String.format("Sorted %,d records in %d batches.", recordCount, batchCount));

    }
    

  }
  

}
