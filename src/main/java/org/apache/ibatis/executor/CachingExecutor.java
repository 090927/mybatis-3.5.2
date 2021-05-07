/**
 *    Copyright 2009-2020 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.executor;

import java.sql.SQLException;
import java.util.List;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.cache.TransactionalCacheManager;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 *
 *   “Executor” 装饰器。
 *
 *  当开启 二级缓存，CachingExecutor 对 SimpleExecutor、ReuseExecutor、BatchExecutor 进行装饰操作。
 *    为查询增加缓存功能。
 */
public class CachingExecutor implements Executor {

  private final Executor delegate;

  // 用于管理，所有二级缓存对象。
  private final TransactionalCacheManager tcm = new TransactionalCacheManager();

  public CachingExecutor(Executor delegate) {
    this.delegate = delegate;
    delegate.setExecutorWrapper(this);
  }

  @Override
  public Transaction getTransaction() {
    return delegate.getTransaction();
  }

  @Override
  public void close(boolean forceRollback) {
    try {
      //issues #499, #524 and #573
      if (forceRollback) {
        tcm.rollback();
      } else {
        tcm.commit();
      }
    } finally {
      delegate.close(forceRollback);
    }
  }

  @Override
  public boolean isClosed() {
    return delegate.isClosed();
  }

  @Override
  public int update(MappedStatement ms, Object parameterObject) throws SQLException {

    /**
     * 如果需要刷新，则更新缓存。 {@link #flushCacheIfRequired(MappedStatement)}
     */
    flushCacheIfRequired(ms);
    return delegate.update(ms, parameterObject);
  }

  @Override
  public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {
    // 获取BoundSql对象
    BoundSql boundSql = ms.getBoundSql(parameterObject);

    /**
     * 创建缓存 key {@link BaseExecutor#createCacheKey(MappedStatement, Object, RowBounds, BoundSql)}
     */
    CacheKey key = createCacheKey(ms, parameterObject, rowBounds, boundSql);

    /**
     * 核心查询 {@link #query(MappedStatement, Object, RowBounds, ResultHandler, CacheKey, BoundSql)}
     */
    return query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
  }

  @Override
  public <E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException {
    flushCacheIfRequired(ms);
    return delegate.queryCursor(ms, parameter, rowBounds);
  }

  @Override
  public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql)
      throws SQLException {

    // 获取 MappedStatement 对象中维护二级缓存对象。
    Cache cache = ms.getCache();
    if (cache != null) {

      /**
       * 根据 <select> 标签配置决定是否需要清空二级缓存 {@link #flushCacheIfRequired(MappedStatement)}
       */
      flushCacheIfRequired(ms);
      if (ms.isUseCache() && resultHandler == null) {

        // 是否包含输出参数
        ensureNoOutParams(ms, boundSql);
        @SuppressWarnings("unchecked")

        /**
         * 从 `MappedStatement` 对象，对应的二级缓存中获取数据。{@link TransactionalCacheManager#getObject(Cache, CacheKey)}
         */
          List<E> list = (List<E>) tcm.getObject(cache, key);
        if (list == null) {

          /**
           * 如果没有，则查询数据库 {@link BaseExecutor#query(MappedStatement, Object, RowBounds, ResultHandler, CacheKey, BoundSql)}
           */
          list = delegate.query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);

          /**
           * 然后将数据，存放到 `MappedStatement` 对应的二级缓存中。{@link TransactionalCacheManager#putObject(Cache, CacheKey, Object)}
           *
           * 将查询结果放入TransactionalCache.entriesToAddOnCommit集合中暂存
           */
          tcm.putObject(cache, key, list); // issue #578 and #116
        }
        return list;
      }
    }
    return delegate.query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
  }

  @Override
  public List<BatchResult> flushStatements() throws SQLException {
    return delegate.flushStatements();
  }

  @Override
  public void commit(boolean required) throws SQLException {
    delegate.commit(required);
    tcm.commit();
  }

  @Override
  public void rollback(boolean required) throws SQLException {
    try {
      delegate.rollback(required);
    } finally {
      if (required) {
        tcm.rollback();
      }
    }
  }

  private void ensureNoOutParams(MappedStatement ms, BoundSql boundSql) {
    if (ms.getStatementType() == StatementType.CALLABLE) {
      for (ParameterMapping parameterMapping : boundSql.getParameterMappings()) {
        if (parameterMapping.getMode() != ParameterMode.IN) {
          throw new ExecutorException("Caching stored procedures with OUT params is not supported.  Please configure useCache=false in " + ms.getId() + " statement.");
        }
      }
    }
  }

  @Override
  public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
    return delegate.createCacheKey(ms, parameterObject, rowBounds, boundSql);
  }

  @Override
  public boolean isCached(MappedStatement ms, CacheKey key) {
    return delegate.isCached(ms, key);
  }

  @Override
  public void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key, Class<?> targetType) {
    delegate.deferLoad(ms, resultObject, property, key, targetType);
  }

  @Override
  public void clearLocalCache() {
    delegate.clearLocalCache();
  }

  private void flushCacheIfRequired(MappedStatement ms) {
    Cache cache = ms.getCache();
    if (cache != null && ms.isFlushCacheRequired()) {
      tcm.clear(cache);
    }
  }

  @Override
  public void setExecutorWrapper(Executor executor) {
    throw new UnsupportedOperationException("This method should not be called");
  }

}
