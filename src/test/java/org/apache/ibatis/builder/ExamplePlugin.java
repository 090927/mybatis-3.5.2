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
package org.apache.ibatis.builder;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;

import java.util.Properties;

/**
 * 自定义 MyBatis 插件。
 */
@Intercepts({})
public class ExamplePlugin implements Interceptor {
  private Properties properties;

  @Override
  public Object intercept(Invocation invocation) throws Throwable {

    /**
     *  TODO 自定义拦截的逻辑
     *  执行目标方法 {@link Invocation#proceed()}
     */
    return invocation.proceed();
  }

  @Override
  public Object plugin(Object target) {
    /**
     * 创建动态代理对象 {@link Plugin#wrap(Object, Interceptor)}
     */
    return Plugin.wrap(target, this);
  }

  @Override
  public void setProperties(Properties properties) {
    // 设置插件的属性信息。
    this.properties = properties;
  }

  public Properties getProperties() {
    return properties;
  }

}
