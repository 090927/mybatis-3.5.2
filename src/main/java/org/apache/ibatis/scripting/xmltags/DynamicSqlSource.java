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

import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;

import java.util.Map;

/**
 * @author Clinton Begin
 *
 *  用于描述 Mapper XML 文件中配置的SQL资源信息。这些SQL 通常包含动态SQL 配置或者 ${} 信息占位符。需要在 Mapper 调用时才能确定具体SQL 语句。
 */
public class DynamicSqlSource implements SqlSource {

  private final Configuration configuration;
  private final SqlNode rootSqlNode;

  public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode) {
    this.configuration = configuration;
    this.rootSqlNode = rootSqlNode;
  }

  @Override
  public BoundSql getBoundSql(Object parameterObject) {

    // 通过参数对象创建动态 SQL 上下文。
    DynamicContext context = new DynamicContext(configuration, parameterObject);

    /**
     *  对动态SQL 进行解析。
     */
    rootSqlNode.apply(context);

    // 创建 sqlSourceBuilder 对象。
    SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
    Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();

    /**
     * 对 SQL 内容进一步处理，生成 StaticSqlSource 对象 {@link SqlSourceBuilder#parse(String, Class, Map)}
     *
     *  1、获取动态 SQL 解析后的结果 {@link DynamicContext#getSql()}
     */
    SqlSource sqlSource = sqlSourceParser.parse(context.getSql(), parameterType, context.getBindings());

    /**
     * 获得 boundSql 实例 {@link DynamicSqlSource#getBoundSql(Object)}
     */
    BoundSql boundSql = sqlSource.getBoundSql(parameterObject);

    /**
     * 将 <bind> 标签绑定的参数添加到 BoundSql 对象中 {@link BoundSql#setAdditionalParameter(String, Object)}
     */
    context.getBindings().forEach(boundSql::setAdditionalParameter);
    return boundSql;
  }

}
