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
package org.activiti.engine.impl.persistence.entity.data.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.ActivitiOptimisticLockingException;
import org.activiti.engine.impl.ExecutionQueryImpl;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.ProcessInstanceQueryImpl;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.persistence.CachedEntityMatcher;
import org.activiti.engine.impl.persistence.CachedEntityMatcherAdapter;
import org.activiti.engine.impl.persistence.cache.CachedEntity;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.activiti.engine.impl.persistence.entity.data.AbstractDataManager;
import org.activiti.engine.impl.persistence.entity.data.ExecutionDataManager;
import org.activiti.engine.impl.persistence.entity.data.impl.cache.ExecutionsByParentExecutionIdEntityMatcher;
import org.activiti.engine.impl.persistence.entity.data.impl.cache.ExecutionsByProcessInstanceIdEntityMatcher;
import org.activiti.engine.impl.persistence.entity.data.impl.cache.SubProcessInstanceExecutionBySuperExecutionIdMatcher;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;

/**
 * @author Joram Barrez
 */
public class MybatisExecutionDataManager extends AbstractDataManager<ExecutionEntity> implements ExecutionDataManager {

  protected boolean eagerlyFetchExecutionTree;
  
  protected CachedEntityMatcher<ExecutionEntity> executionsByParentIdMatcher = new ExecutionsByParentExecutionIdEntityMatcher();
  protected CachedEntityMatcher<ExecutionEntity> executionsByProcessInstanceIdMatcher = new ExecutionsByProcessInstanceIdEntityMatcher();
  protected CachedEntityMatcher<ExecutionEntity> subProcessInstanceBySuperExecutionIdMatcher = new SubProcessInstanceExecutionBySuperExecutionIdMatcher();
  
  public MybatisExecutionDataManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
    super(processEngineConfiguration);
    this.eagerlyFetchExecutionTree = processEngineConfiguration.isEagerlyFetchExecutionTree();
  }

  @Override
  public Class<? extends ExecutionEntity> getManagedEntityClass() {
    return ExecutionEntityImpl.class;
  }
  
  @Override
  public ExecutionEntity create() {
    ExecutionEntityImpl executionEntity = new ExecutionEntityImpl();
    executionEntity.setExecutions(new ArrayList<ExecutionEntityImpl>(1));
    return executionEntity;
  }
  
  @Override
  public ExecutionEntity findById(String entityId) {
    if (eagerlyFetchExecutionTree) {
      return findByIdAndFetchExecutionTree(entityId);
    } else {
      return super.findById(entityId);
    }
  }
  
  protected ExecutionEntity findByIdAndFetchExecutionTree(final String executionId) {
    
    // If it's in the cache, the tree must have been fetched before
    ExecutionEntity cachedEntity = getEntityCache().findInCache(getManagedEntityClass(), executionId);
    if (cachedEntity != null) {
      return cachedEntity;
    }
    
    // Fetches execution tree. This will store them in the cache.
    List<ExecutionEntity> executionEntities = getList("selectExecutionsWithSameRootProcessInstanceId", executionId, 
        new CachedEntityMatcher<ExecutionEntity>() {
      
          protected ExecutionEntity executionEntity;
      
          @Override
          public void preProcess(Collection<ExecutionEntity> databaseEntities, Collection<CachedEntity> cachedEntities) {
            
            // Doing some preprocessing here: we need to find the execution that matches the provided execution id,
            // as we need to match the root process instance id later on.
            
            if (cachedEntities != null) {
              for (CachedEntity cachedEntity : cachedEntities) {
                ExecutionEntity executionEntity = (ExecutionEntity) cachedEntity.getEntity();
                if (executionId.equals(executionEntity.getId())) {
                  this.executionEntity = executionEntity;
                  return;
                }
              }
            }
            
            if (databaseEntities != null) {
              for (ExecutionEntity databaseExecutionEntity : databaseEntities) {
                if (executionId.equals(databaseExecutionEntity.getId())) {
                  this.executionEntity = databaseExecutionEntity;
                  return;
                }
              }
            }
            
          }
          
          @Override
          public boolean isRetained(ExecutionEntity entity, Object parameter) {
            return(executionEntity.getRootProcessInstanceId() != null 
                && executionEntity.getRootProcessInstanceId().equals(entity.getRootProcessInstanceId()));
          }
          
    }, true);
    
    for (ExecutionEntity executionEntity : executionEntities) {
      if (executionId.equals(executionEntity.getId())) {
        return executionEntity;
      }
    }
    return null;
  }
  
  @Override
  public ExecutionEntity findSubProcessInstanceBySuperExecutionId(final String superExecutionId) {
    
    if (eagerlyFetchExecutionTree) {
      findByIdAndFetchExecutionTree(superExecutionId);
    }
    
    return getEntity("selectSubProcessInstanceBySuperExecutionId", 
        superExecutionId, 
        subProcessInstanceBySuperExecutionIdMatcher,
        !eagerlyFetchExecutionTree);
  }
  
  @Override
  public List<ExecutionEntity> findChildExecutionsByParentExecutionId(final String parentExecutionId) {
    if (eagerlyFetchExecutionTree) {
      findByIdAndFetchExecutionTree(parentExecutionId);
      return getListFromCache(executionsByParentIdMatcher, parentExecutionId);
    } else {
      return getList("selectExecutionsByParentExecutionId", parentExecutionId, executionsByParentIdMatcher, true);
    }
  }
  
  @Override
  public List<ExecutionEntity> findChildExecutionsByProcessInstanceId(final String processInstanceId) {
    if (eagerlyFetchExecutionTree) {
      findByIdAndFetchExecutionTree(processInstanceId);
      return getListFromCache(executionsByProcessInstanceIdMatcher, processInstanceId);
    } else {
      return getList("selectChildExecutionsByProcessInstanceId", processInstanceId, executionsByProcessInstanceIdMatcher, true);
    }
  }

  @Override
  public List<ExecutionEntity> findExecutionsByParentExecutionAndActivityIds(final String parentExecutionId, final Collection<String> activityIds) {
    
    Map<String, Object> parameters = new HashMap<String, Object>(2);
    parameters.put("parentExecutionId", parentExecutionId);
    parameters.put("activityIds", activityIds);
    
    return getList("selectExecutionsByParentExecutionAndActivityIds", parameters, new CachedEntityMatcherAdapter<ExecutionEntity>() {
      @Override
      public boolean isRetained(ExecutionEntity executionEntity, Object parameter) {
        return executionEntity.getParentId() != null && executionEntity.getParentId().equals(parentExecutionId)
            && executionEntity.getActivityId() != null && activityIds.contains(executionEntity.getActivityId());
      }
      
    }, true);
  }

  @Override
  public long findExecutionCountByQueryCriteria(ExecutionQueryImpl executionQuery) {
    return (Long) getDbSqlSession().selectOne("selectExecutionCountByQueryCriteria", executionQuery);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<ExecutionEntity> findExecutionsByQueryCriteria(ExecutionQueryImpl executionQuery, Page page) {
    return getDbSqlSession().selectList("selectExecutionsByQueryCriteria", executionQuery, page);
  }
  
  @Override
  public long findProcessInstanceCountByQueryCriteria(ProcessInstanceQueryImpl executionQuery) {
    return (Long) getDbSqlSession().selectOne("selectProcessInstanceCountByQueryCriteria", executionQuery);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<ProcessInstance> findProcessInstanceByQueryCriteria(ProcessInstanceQueryImpl executionQuery) {
    return getDbSqlSession().selectList("selectProcessInstanceByQueryCriteria", executionQuery);
  }
  
  @Override
  public List<ExecutionEntity> findExecutionsByRootProcessInstanceId(final String rootProcessInstanceId) {
    return getList("selectExecutionsByRootProcessInstanceId", rootProcessInstanceId, new CachedEntityMatcherAdapter<ExecutionEntity>() {
      @Override
      public boolean isRetained(ExecutionEntity entity, Object parameter) {
        return entity.getRootProcessInstanceId() != null && entity.getRootProcessInstanceId().equals(rootProcessInstanceId);
      }
    }, true); 
  }
  
  @Override
  public List<ExecutionEntity> findExecutionsByProcessInstanceId(final String processInstanceId) {
    return getList("selectExecutionsByProcessInstanceId", processInstanceId, new CachedEntityMatcherAdapter<ExecutionEntity>() {
      @Override
      public boolean isRetained(ExecutionEntity entity, Object parameter) {
        return entity.getProcessInstanceId() != null && entity.getProcessInstanceId().equals(processInstanceId);
      }
    }, true); 
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public List<ProcessInstance> findProcessInstanceAndVariablesByQueryCriteria(ProcessInstanceQueryImpl executionQuery) {
    // paging doesn't work for combining process instances and variables due
    // to an outer join, so doing it in-memory
    if (executionQuery.getFirstResult() < 0 || executionQuery.getMaxResults() <= 0) {
      return Collections.EMPTY_LIST;
    }

    int firstResult = executionQuery.getFirstResult();
    int maxResults = executionQuery.getMaxResults();

    // setting max results, limit to 20000 results for performance reasons
    executionQuery.setMaxResults(20000);
    executionQuery.setFirstResult(0);

    List<ProcessInstance> instanceList = getDbSqlSession().selectListWithRawParameterWithoutFilter("selectProcessInstanceWithVariablesByQueryCriteria", executionQuery,
        executionQuery.getFirstResult(), executionQuery.getMaxResults());

    if (instanceList != null && !instanceList.isEmpty()) {
      if (firstResult > 0) {
        if (firstResult <= instanceList.size()) {
          int toIndex = firstResult + Math.min(maxResults, instanceList.size() - firstResult);
          return instanceList.subList(firstResult, toIndex);
        } else {
          return Collections.EMPTY_LIST;
        }
      } else {
        int toIndex = Math.min(maxResults, instanceList.size());
        return instanceList.subList(0, toIndex);
      }
    }
    return Collections.EMPTY_LIST;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<ExecutionEntity> findEventScopeExecutionsByActivityId(String activityRef, String parentExecutionId) {
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("activityId", activityRef);
    parameters.put("parentExecutionId", parentExecutionId);

    return getDbSqlSession().selectList("selectExecutionsByParentExecutionId", parameters);
  }

  @Override
  public Collection<ExecutionEntity> findInactiveExecutionsByActivityId(final String activityId) {
    HashMap<String, Object> params = new HashMap<String, Object>(2);
    params.put("activityId", activityId);
    params.put("isActive", false);
    return getList("selectInactiveExecutionsInActivity", params, new CachedEntityMatcherAdapter<ExecutionEntity>() {
      public boolean isRetained(ExecutionEntity entity) {
        return !entity.isActive() && entity.getActivityId() != null && entity.getActivityId().equals(activityId);
      }
    }, true);
  }

  @Override
  public Collection<ExecutionEntity> findInactiveExecutionsByProcessInstanceId(final String processInstanceId) {
    HashMap<String, Object> params = new HashMap<String, Object>(2);
    params.put("processInstanceId", processInstanceId);
    params.put("isActive", false);
    return getList("selectInactiveExecutionsForProcessInstance", params, new CachedEntityMatcherAdapter<ExecutionEntity>() {
      public boolean isRetained(ExecutionEntity executionEntity) {
        return executionEntity.getProcessInstanceId() != null && executionEntity.getProcessInstanceId().equals(processInstanceId) && !executionEntity.isActive();
      }
    }, true);
  }
  
  @Override
  public Collection<ExecutionEntity> findInactiveExecutionsByActivityIdAndProcessInstanceId(final String activityId, final String processInstanceId) {
    HashMap<String, Object> params = new HashMap<String, Object>(3);
    params.put("activityId", activityId);
    params.put("processInstanceId", processInstanceId);
    params.put("isActive", false);
    return getList("selectInactiveExecutionsInActivityAndProcessInstance", params, new CachedEntityMatcherAdapter<ExecutionEntity>() {
      public boolean isRetained(ExecutionEntity executionEntity) {
        return executionEntity.getProcessInstanceId() != null && executionEntity.getProcessInstanceId().equals(processInstanceId) && !executionEntity.isActive() &&
            executionEntity.getActivityId() != null && executionEntity.getActivityId().equals(activityId);
      }
    }, true);
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public List<String> findProcessInstanceIdsByProcessDefinitionId(String processDefinitionId) {
    return getDbSqlSession().selectList("selectProcessInstanceIdsByProcessDefinitionId", processDefinitionId);
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public List<Execution> findExecutionsByNativeQuery(Map<String, Object> parameterMap, int firstResult, int maxResults) {
    return getDbSqlSession().selectListWithRawParameter("selectExecutionByNativeQuery", parameterMap, firstResult, maxResults);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<ProcessInstance> findProcessInstanceByNativeQuery(Map<String, Object> parameterMap, int firstResult, int maxResults) {
    return getDbSqlSession().selectListWithRawParameter("selectExecutionByNativeQuery", parameterMap, firstResult, maxResults);
  }

  @Override
  public long findExecutionCountByNativeQuery(Map<String, Object> parameterMap) {
    return (Long) getDbSqlSession().selectOne("selectExecutionCountByNativeQuery", parameterMap);
  }
  
  @Override
  public void updateExecutionTenantIdForDeployment(String deploymentId, String newTenantId) {
    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("deploymentId", deploymentId);
    params.put("tenantId", newTenantId);
    getDbSqlSession().update("updateExecutionTenantIdForDeployment", params);
  }
  
  @Override
  public void updateProcessInstanceLockTime(String processInstanceId, Date lockDate, Date expirationTime) {
    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("id", processInstanceId);
    params.put("lockTime", lockDate);
    params.put("expirationTime", expirationTime);

    int result = getDbSqlSession().update("updateProcessInstanceLockTime", params);
    if (result == 0) {
      throw new ActivitiOptimisticLockingException("Could not lock process instance");
    }
  }
  
  @Override
  public void clearProcessInstanceLockTime(String processInstanceId) {
    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("id", processInstanceId);
    getDbSqlSession().update("clearProcessInstanceLockTime", params);
  }
  
}
