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
package org.apache.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.ResultHandler;

/**
 * @author Clinton Begin
 *
 *  封装了对jdbc Statement 对象的操作，比如为 Statement 对象设置参数、调用 Statement 接口提供的方法与数据库交互，等等。
 */
public interface StatementHandler {

  /**
   * 用于创建 jdbc Statement 对象，并完成 Statement 对象的属性设置。
   * @param connection
   * @param transactionTimeout
   * @return
   * @throws SQLException
   */
  Statement prepare(Connection connection, Integer transactionTimeout)
      throws SQLException;


  /**
   *
   *  使用 MyBatis 中的 ParameterHandler 组件为 PreparedStatement 和 CallableStatement 参数占位符设置值
   *
   * @param statement
   * @throws SQLException
   */
  void parameterize(Statement statement)
      throws SQLException;

  /**
   *  将 SQL 命令添加到批处理量执行列表中。
   *
   * @param statement
   * @throws SQLException
   */
  void batch(Statement statement)
      throws SQLException;

  int update(Statement statement)
      throws SQLException;

  <E> List<E> query(Statement statement, ResultHandler resultHandler)
      throws SQLException;

  /**
   *
   * 带游标的查询，返回 Cursor 对象，能够从 Iterator 动态从数据库中加载数据。
   *
   * @param statement
   * @param <E>
   * @return
   * @throws SQLException
   */
  <E> Cursor<E> queryCursor(Statement statement)
      throws SQLException;

  /**
   *
   *  获取 Mapper 中配置的 SQL 信息，BoundSql 封装动态SQL解析后的SQL 文本，和参数映射。
   *
   * @return
   */
  BoundSql getBoundSql();

  ParameterHandler getParameterHandler();

}
