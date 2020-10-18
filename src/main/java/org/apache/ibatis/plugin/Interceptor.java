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
package org.apache.ibatis.plugin;

import java.util.Properties;

/**
 * @author Clinton Begin
 */
public interface Interceptor {

  /**
   * 定义拦截器逻辑，该方法会在目标方法调用时执行。
   * @param invocation
   * @return
   * @throws Throwable
   */
  Object intercept(Invocation invocation) throws Throwable;

  // 用于创建 Executor、ParameterHandler、ResultSetHandler、StatementHandler 的代理对象。
  default Object plugin(Object target) {

    /**
     * {@link Plugin#wrap(Object, Interceptor)}
     */
    return Plugin.wrap(target, this);
  }

  // 设置插件的属性值。
  default void setProperties(Properties properties) {
    // NOP
  }

}
