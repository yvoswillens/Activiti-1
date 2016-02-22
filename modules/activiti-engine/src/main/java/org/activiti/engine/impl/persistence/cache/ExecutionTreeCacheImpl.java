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
package org.activiti.engine.impl.persistence.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;

/**
 * @author Joram Barrez
 */
public class ExecutionTreeCacheImpl implements ExecutionTreeCache {
  
  protected Map<String, ExecutionEntity> cachedExecutions = new HashMap<String, ExecutionEntity>();
  
  @Override
  public void put(ExecutionEntity executionEntity) {
    addChildren(findRootProcessInstanceExecution(executionEntity));
  }
  
  protected ExecutionEntity findRootProcessInstanceExecution(ExecutionEntity executionEntity) {
    ExecutionEntity currentExecutionEntity = executionEntity;
    while (!currentExecutionEntity.getId().equals(executionEntity.getId())) {
      
      if (currentExecutionEntity.getProcessInstanceId() != null
          && !currentExecutionEntity.getId().equals(currentExecutionEntity.getProcessInstanceId())) {
        currentExecutionEntity = currentExecutionEntity.getProcessInstance();
      } else if (currentExecutionEntity.getSuperExecutionId() != null
          && !currentExecutionEntity.getId().equals(currentExecutionEntity.getSuperExecutionId())) {
        currentExecutionEntity = currentExecutionEntity.getSuperExecution();
      } else if (currentExecutionEntity.getParentId() != null){
        currentExecutionEntity = currentExecutionEntity.getParent();
      } else {
        throw new ActivitiException("Programmatic error: invalid execution : no process instance, super execution or parent id");
      }
    }
    
    return currentExecutionEntity;
  }
  
  protected void addChildren(ExecutionEntity executionEntity) {
    List<? extends ExecutionEntity> childExecutions = executionEntity.getExecutions();
    if (childExecutions != null) {
      for (ExecutionEntity childExecutionEntity : childExecutions) {
        cachedExecutions.put(childExecutionEntity.getId(), childExecutionEntity);
        addChildren(childExecutionEntity);
      }
    }
    
    ExecutionEntity subProcessInstance = executionEntity.getSubProcessInstance();
    if (subProcessInstance != null) {
      cachedExecutions.put(subProcessInstance.getId(), subProcessInstance);
      addChildren(subProcessInstance);
    }
  }
  
  @Override
  public ExecutionEntity get(String executionId) {
    return cachedExecutions.get(executionId);
  }

  @Override
  public void flush() {
    
  }

  @Override
  public void close() {
    
  }

}
