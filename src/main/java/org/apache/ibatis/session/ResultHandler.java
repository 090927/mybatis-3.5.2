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
package org.apache.ibatis.session;

/**
 * @author Clinton Begin
 *
 *  将查询结果转换为 Java 对象。
 *
 *
 *   `DefaultResultContext` 对象，生命周期与 ResultSet 相同，
 *    每从 ResultSet 映射得到一个 Java 对象都会暂存在 {@link org.apache.ibatis.executor.result.DefaultResultContext
 */
public interface ResultHandler<T> {

  void handleResult(ResultContext<? extends T> resultContext);

}
