package org.activiti.engine.impl.persistence;

import java.util.Collection;

import org.activiti.engine.impl.db.Entity;
import org.activiti.engine.impl.persistence.cache.CachedEntity;

/**
 * Interface to express a condition whether or not a cached entity should be used in the return result of a query.
 * 
 * @author Joram Barrez
 */
public interface SingleCachedEntityMatcher<EntityImpl extends Entity> {

  boolean isRetained( EntityImpl entity, Object param);

}
