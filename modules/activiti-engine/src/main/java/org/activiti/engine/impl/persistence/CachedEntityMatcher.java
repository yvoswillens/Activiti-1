package org.activiti.engine.impl.persistence;

import java.util.Collection;

import org.activiti.engine.impl.db.Entity;
import org.activiti.engine.impl.persistence.cache.CachedEntity;

/**
 * Interface to express a condition whether or not a cached entity should be used in the return result of a query.
 * 
 * @author Joram Barrez
 */
public interface CachedEntityMatcher<EntityImpl extends Entity> {

  /**
   * Allows to 'pre-process' this instance, using the database results and the cached entities.
   */
  void preProcess( Collection<EntityImpl> databaseEntities, Collection<CachedEntity> cachedEntities);
  
  /**
   * Return true in case the cached entity should be used instead of one from the database. 
   */
  boolean isRetained(EntityImpl entity, Object param);

}
