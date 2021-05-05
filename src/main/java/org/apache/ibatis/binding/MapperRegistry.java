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
package org.apache.ibatis.binding;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 * @author Lasse Voss
 *
 *  用于 Mapper 接口与 MapperProxyFactory（Mapper 的代理对象工厂） 之间的对应关系。
 *
 *   1、MapperRegistry 类是一个 Mapper 类注册工厂。把MapperProxyFactory 映射过的 Mapper 类添加到它的属性 `knownMappers`
 */
public class MapperRegistry {

  // 指向 MyBatis 全局唯一 Configuration 对象，（其中维护了解析之后的全部 MyBatis 配置信息）
  private final Configuration config;

  // 维护了所有解析到的 Mapper 接口对应 Class对象，和 MapperProxyFactory 工厂对象之间的映射关系
  private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<>();

  public MapperRegistry(Configuration config) {
    this.config = config;
  }

  /**
   * 根据 Mapper 接口 Class对象获取 Mapper 动态代理对象
   * @param type
   * @param sqlSession
   * @param <T>
   * @return
   */
  @SuppressWarnings("unchecked")
  public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
    final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
    if (mapperProxyFactory == null) {
      throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
    }
    try {

      /**
       *  “newInstance” {@link MapperProxyFactory#newInstance(SqlSession)}
       */
      return mapperProxyFactory.newInstance(sqlSession);
    } catch (Exception e) {
      throw new BindingException("Error getting mapper instance. Cause: " + e, e);
    }
  }

  public <T> boolean hasMapper(Class<T> type) {
    return knownMappers.containsKey(type);
  }


  /**
   *  【MyBatis 初始化】会读取全部 Mapper.xml 配置文件，还会扫描全部 Mapper 接口中注解信息，
   *
   * 根据 Mapper 接口 Class 对象创建 MapperProxyFactory 对象，并注册 knownMappers
   * @param type
   * @param <T>
   */
  public <T> void addMapper(Class<T> type) {

    // 是否是一个接口，且 knownMappers 集合没有加载过 type 类型。
    if (type.isInterface()) {
      if (hasMapper(type)) {
        throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
      }
      boolean loadCompleted = false;
      try {

        /**
         * mapper 和 MapperProxyFactory 进行映射。
         */
        knownMappers.put(type, new MapperProxyFactory<>(type));
        // It's important that the type is added before the parser is run
        // otherwise the binding may automatically be attempted by the
        // mapper parser. If the type is already known, it won't try.

        // Mapper 注解构造器
        MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);

        /**
         * 解析 {@link MapperAnnotationBuilder#parse()}
         */
        parser.parse();
        loadCompleted = true;
      } finally {

        // 解析失败
        if (!loadCompleted) {
          knownMappers.remove(type);
        }
      }
    }
  }

  /**
   * @since 3.2.2
   */
  public Collection<Class<?>> getMappers() {
    return Collections.unmodifiableCollection(knownMappers.keySet());
  }

  /**
   * @since 3.2.2
   */
  public void addMappers(String packageName, Class<?> superType) {
    ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<>();
    resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
    Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
    for (Class<?> mapperClass : mapperSet) {
      addMapper(mapperClass);
    }
  }

  /**
   * @since 3.2.2
   */
  public void addMappers(String packageName) {
    addMappers(packageName, Object.class);
  }

}
