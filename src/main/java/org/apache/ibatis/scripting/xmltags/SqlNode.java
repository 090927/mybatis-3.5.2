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
 */
public interface SqlNode {

  /**
   * 用于解析 SQL 节点，根据参数信息生成静态SQL 内容
   * @param context
   * @return
   */
  boolean apply(DynamicContext context);
}
