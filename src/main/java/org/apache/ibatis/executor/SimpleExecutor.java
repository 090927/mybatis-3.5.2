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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

/**
 * @author Clinton Begin
 *
 *  可以完成基本的增删改查操作。
 */
public class SimpleExecutor extends BaseExecutor {

  public SimpleExecutor(Configuration configuration, Transaction transaction) {
    super(configuration, transaction);
  }

  @Override
  public int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
    Statement stmt = null;
    try {
      Configuration configuration = ms.getConfiguration();
      StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, RowBounds.DEFAULT, null, null);
      stmt = prepareStatement(handler, ms.getStatementLog());
      return handler.update(stmt);
    } finally {
      closeStatement(stmt);
    }
  }

  @Override
  public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
    Statement stmt = null;
    try {
      Configuration configuration = ms.getConfiguration();

      /**
       * 获取 StatementHandler 对象。{@link Configuration#newStatementHandler(Executor, MappedStatement, Object, RowBounds, ResultHandler, BoundSql)}
       *
       *  返回 `{@link org.apache.ibatis.executor.statement.RoutingStatementHandler#RoutingStatementHandler(Executor, MappedStatement, Object, RowBounds, ResultHandler, BoundSql)} `
       *    1、会根据  Mapper 的 statementType
       */
      StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);

      /**
       * 创建 Statement 对象, 还依赖 “ParameterHandler” 填充 SQL 占位符 {@link #prepareStatement(StatementHandler, Log)}
       */
      stmt = prepareStatement(handler, ms.getStatementLog());

      /**
       * 调用 StatementHandler 对象 query
       *  并通过 ResultSetHandler 完成结果集的映射 {@link org.apache.ibatis.executor.statement.SimpleStatementHandler#query(Statement, ResultHandler)}
       */
      return handler.query(stmt, resultHandler);
    } finally {
      closeStatement(stmt);
    }
  }

  @Override
  protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql) throws SQLException {
    Configuration configuration = ms.getConfiguration();
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, null, boundSql);
    Statement stmt = prepareStatement(handler, ms.getStatementLog());
    stmt.closeOnCompletion();
    return handler.queryCursor(stmt);
  }

  @Override
  public List<BatchResult> doFlushStatements(boolean isRollback) {
    return Collections.emptyList();
  }

  private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
    Statement stmt;

    // 获取 JDBC Connection 对象。
    Connection connection = getConnection(statementLog);

    /**
     * 调用 StatementHandler prepare 创建 Statement 对象。
     *   “创建 Statement 对象” {@link org.apache.ibatis.executor.statement.PreparedStatementHandler#prepare(Connection, Integer)}
     */
    stmt = handler.prepare(connection, transaction.getTimeout());

    /**
     * 调用 StatementHandler parameterize 设置参数
     *  解析 SQL 语句中 包含的 “?” 占位符。 {@link org.apache.ibatis.executor.statement.PreparedStatementHandler#parameterize(Statement)}
      */
    handler.parameterize(stmt);
    return stmt;
  }

}
