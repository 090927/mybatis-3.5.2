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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.reflection.ExceptionUtil;

/**
 * @author Clinton Begin
 *
 *  采用JDK 内置动态代理方式创建对象。
 */
public class Plugin implements InvocationHandler {

  private final Object target;

  // 自定义拦截器实例
  private final Interceptor interceptor;

  // Interceptor 注解指定的方法。
  private final Map<Class<?>, Set<Method>> signatureMap;

  private Plugin(Object target, Interceptor interceptor, Map<Class<?>, Set<Method>> signatureMap) {
    this.target = target;
    this.interceptor = interceptor;
    this.signatureMap = signatureMap;
  }

  /**
   * 简化动态代理对象的创建
   *
   *  只有在匹配的情况下，才会生成代理对象，否则直接返回目标对象。
   *
   * @param target 即  Executor、ParameterHandler、ResultSetHandler、StatementHandler
   * @param interceptor 拦截器的实例
   * @return
   */
  public static Object wrap(Object target, Interceptor interceptor) {

    /**
     * 获取 “@Intercepts” 注解指定的要拦截的组件及方法集合 {@link #getSignatureMap(Interceptor)}
     *
     *  Key: Executor、ParameterHandler、ResultSetHandler、StatementHandler
     *  Value: 所有方法对应的 Method 对象
     */
    Map<Class<?>, Set<Method>> signatureMap = getSignatureMap(interceptor);
    Class<?> type = target.getClass();

    /**
     *  检查当前传入的 target 对象是否为 “@Signature” 注解要拦截的的类型（{@link #getAllInterfaces(Class, Map)}），如果是，就使用 JDK 动态代理创建代理对象。
     */
    Class<?>[] interfaces = getAllInterfaces(type, signatureMap);
    if (interfaces.length > 0) {

      // 创建一个动态代理对象。
      return Proxy.newProxyInstance(
          type.getClassLoader(),
          interfaces,
          // 使用 InvocationHandler 就是 Plugin 本身。
          new Plugin(target, interceptor, signatureMap));
    }
    return target;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    try {

      // 如果该方法是 Interceptors 注解指定的 方法，则调用拦截器实例的 intercept() 方法进行拦截逻辑。
      Set<Method> methods = signatureMap.get(method.getDeclaringClass());

      // 如果当前方法需要被代理，则执行intercept()方法进行拦截处理
      if (methods != null && methods.contains(method)) {

        /**
         *   TODO 可执行，用户自定义的拦截器（ MyBatis 插件 ），并把目标方法信息封装成 Invocation 对象作为 intercept() 方法的参数。
         *
         *  {@link org.apache.ibatis.builder.ExamplePlugin#intercept(Invocation)}
         */
        return interceptor.intercept(new Invocation(target, method, args));
      }

      // 如果当前方法不需要被代理，则调用target对象的相应方法
      return method.invoke(target, args);
    } catch (Exception e) {
      throw ExceptionUtil.unwrapThrowable(e);
    }
  }

  /**
   *  获取 interceptor 注解，要拦截的组件及方法。
   *
   * @param interceptor
   * @return
   */
  private static Map<Class<?>, Set<Method>> getSignatureMap(Interceptor interceptor) {

    // 获取 `Intercepts` 注解信息。
    Intercepts interceptsAnnotation = interceptor.getClass().getAnnotation(Intercepts.class);
    // issue #251
    if (interceptsAnnotation == null) {
      throw new PluginException("No @Intercepts annotation was found in interceptor " + interceptor.getClass().getName());
    }

    // 获取所有 Signature 注解信息。
    Signature[] sigs = interceptsAnnotation.value();
    Map<Class<?>, Set<Method>> signatureMap = new HashMap<>();

    //  把 Signature 注解 指定拦截器的组件，及方法添加到 Map 中。
    for (Signature sig : sigs) {
      Set<Method> methods = signatureMap.computeIfAbsent(sig.type(), k -> new HashSet<>());
      try {
        Method method = sig.type().getMethod(sig.method(), sig.args());
        methods.add(method);
      } catch (NoSuchMethodException e) {
        throw new PluginException("Could not find method on " + sig.type() + " named " + sig.method() + ". Cause: " + e, e);
      }
    }
    return signatureMap;
  }

  private static Class<?>[] getAllInterfaces(Class<?> type, Map<Class<?>, Set<Method>> signatureMap) {
    Set<Class<?>> interfaces = new HashSet<>();
    while (type != null) {
      for (Class<?> c : type.getInterfaces()) {
        if (signatureMap.containsKey(c)) {
          interfaces.add(c);
        }
      }
      type = type.getSuperclass();
    }
    return interfaces.toArray(new Class<?>[interfaces.size()]);
  }

}
