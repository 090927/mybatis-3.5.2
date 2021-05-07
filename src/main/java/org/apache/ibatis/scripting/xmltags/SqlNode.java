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
package org.apache.ibatis.scripting.xmltags;

/**
 * @author Clinton Begin
 *
 *  用于描述 mapper Sql 配置的SQL 节点，是MyBatis 实现动态SQL的基石。
 *
 *
 *   {@link TextSqlNode#apply(DynamicContext)} 解析包含 “${}” 占位符
 */
public interface SqlNode {

  /**
   * 用于解析 SQL 节点，根据参数信息生成静态SQL 内容
   *
   *   apply 方法会根据用户传入的 实参，解析该 sqlNode 所表示的动态SQL 内容并将解析后SQL 片段
   *   追加到 {@link DynamicContext#sqlBuilder} 暂存。
   *
   *   当 SQL 语句中全部的动态SQL 片段都解析完成之后，可以从 “sqlBuilder” 取出，得到一条完整的、可用的SQL 语句。
   * @param context
   * @return
   */
  boolean apply(DynamicContext context);
}
