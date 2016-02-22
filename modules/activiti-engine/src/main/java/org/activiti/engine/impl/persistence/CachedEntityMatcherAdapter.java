package org.activiti.engine.impl.persistence;

import java.util.Collection;

import org.activiti.engine.impl.db.Entity;
import org.activiti.engine.impl.persistence.cache.CachedEntity;

/**
 * @author Joram Barrez
 */
public abstract class CachedEntityMatcherAdapter<EntityImpl extends Entity> implements CachedEntityMatcher<EntityImpl> {
  
  @Override
  public void preProcess(Collection<EntityImpl> databaseEntities, Collection<CachedEntity> cachedEntities) {
    
  }
  
  @Override
  public boolean isRetained(EntityImpl entity, Object param) {
    return false;
  }
  
}
 

