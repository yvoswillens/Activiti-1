/* Licensed under the Apache License, Version 2.0 (the "License");
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
package org.activiti.engine.impl.util;

import org.activiti.engine.impl.persistence.entity.ExecutionEntity;

/**
 * @author Joram Barrez
 */
public class ExecutionCachedEntityStateUtil {
  
  public static final int EVENT_SUBSCRIPTIONS_STATE_BIT = 1;
  public static final int TASKS_STATE_BIT = 2;
  public static final int JOBS_STATE_BIT = 3;
  public static final int VARIABLES_STATE_BIT = 4;
  
  public static boolean hasTasks(ExecutionEntity executionEntity) {
    return BitMaskUtil.isBitOn(executionEntity.getCachedEntityState(), TASKS_STATE_BIT);
  }
  
  public static void setHasTasks(ExecutionEntity executionEntity, boolean hasTasks) {
    BitMaskUtil.setBit(executionEntity.getCachedEntityState(), TASKS_STATE_BIT, hasTasks);
  }
  
  public static boolean hasJobs(ExecutionEntity executionEntity) {
    return BitMaskUtil.isBitOn(executionEntity.getCachedEntityState(), JOBS_STATE_BIT);
  }
  
  public static void setHasJobs(ExecutionEntity executionEntity, boolean hasJobs) {
    BitMaskUtil.setBit(executionEntity.getCachedEntityState(), JOBS_STATE_BIT, hasJobs);
  }
  
  public static boolean hasEventSubscriptions(ExecutionEntity executionEntity) {
    return BitMaskUtil.isBitOn(executionEntity.getCachedEntityState(), EVENT_SUBSCRIPTIONS_STATE_BIT);
  }
  
  public static void setHasEventSubscriptions(ExecutionEntity executionEntity, boolean hasEventSubscriptions) {
    BitMaskUtil.setBit(executionEntity.getCachedEntityState(), EVENT_SUBSCRIPTIONS_STATE_BIT, hasEventSubscriptions);
  }
  
  public static boolean hasVariables(ExecutionEntity executionEntity) {
    return BitMaskUtil.isBitOn(executionEntity.getCachedEntityState(), VARIABLES_STATE_BIT);
  }
  
  public static void setHasVariables(ExecutionEntity executionEntity, boolean hasVariables) {
    BitMaskUtil.setBit(executionEntity.getCachedEntityState(), VARIABLES_STATE_BIT, hasVariables);
  }

}
