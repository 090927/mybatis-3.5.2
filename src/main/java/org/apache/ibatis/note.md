#### mybatis-config.xml 解析
- 解析入口 `SqlSessionFactoryBuilder#buil()`



#### Configuration
- `XMlConfiguration # parse()`,创建 Configuration 对象。

##### 反射模块
- `ObjectFactory` 是 MyBatis 中的反射工厂。
    - create() 方法创建指定类型的对象。
- 为提升 Reflector 的初始化速度，提供 `ReflectorFactory` 这个工厂接口，对 `Reflector` 对象进行缓存。
- `Reflector` 要使用反射模块操作一个Class，都会先将该Class 封装成一个 `Reflector` 对象，

###### 元数据
- MetaClass 中封装 Class 元信息。
- ObjectWrapper 封装的则是 对象元信息。
- MetaObject 属于类级别的方法。

##### 属性解析工具
- PropertyTokenizer 负责解析由 . 和[] 构成的表达式。
- PropertyCopier 一个属性拷贝的工具类。提供与 Spring 中 BeanUtils.copyProperties() 类似的功能

##### 类型处理
- TypeHandler 
- TypeHandlerRegistry 管理 `TypeHandler`.
- TypeAliasRegistry 维护别名配置的核心

##### 代理 (使用 JDK 动态代理) 实现日志功能 
- `BaseJdbcLogger` （抽象类）
  - 对获取 Connection
  - PreparedStatement
  - Statement
  - ResultSet 进行拦截

##### 数据源（DataSource）
- `DataSourceFactory` （工厂模式）进行创建
- `PooledConnection` 是 Mybatis 中定义一个 InvocationHandler 接口实现（封装真正 Connection 对象以及相关代理对象）
- `PoolState` 负责管理连接池中所有 PooledConnection 对象的状态，

##### 事务（Transaction）
- TransactionFactory 是用于创建 Transaction 的工厂接口，`最核心的方法（newTransaction ）`


##### binding (Mapper 接口 和 Mapper.xml 之间的映射功能)
- MapperRegistry 是MyBatis 初始化过程中构造的一个对象，主要作用就是维护 Mapper 接口以及这些 Mapper 的代理工厂
  - 使用 MyBatis 会先从 `MapperRegistry` 中获取 `xxxMapper` 接口的代理对象，（MapperProxyFactory） 工厂对象。
- `MapperProxyFactory` （核心功能 创建 Mapper 接口的代理对象）
- `MapperProxy` 是生成 Mapper 接口代理对象的关键，（实现 InvocationHandler 接口）
- `MapperMethod` 根据方法签名执行相应的 SQL 语句。


##### 动态SQL
- `SqlNode` 用于描述 mapper Sql 配置的SQL 节点，是MyBatis 实现动态SQL的基石。
    - 区分标签。
- `SqlSource`
  - `DynamicSqlSource` 当 SQL 语句中包含动态 SQL 的时候，会使用 DynamicSqlSource 对象。
  - `RawSqlSource`：当 SQL 语句中只包含静态 SQL 的时候，会使用 RawSqlSource 对象。
  - `StaticSqlSource`：DynamicSqlSource 和 RawSqlSource 经过一系列解析之后，会得到最终可提交到数据库的 SQL 语句，这个时候就可以通过 StaticSqlSource 进行封装了。
  

##### `StatementHandler` （完成SQL 语句执行中最核心）

##### `Executor` 核心接口

##### 缓存
- Cache。顶层抽象，定义MyBatis 缓存最核心、最基础的行为。
- `BlockingCache` 添加阻塞线程的特性
- 每个 `namespace` 开启二级缓存，同时还会将 namespace 与关联的二级缓存 Cache 对象记录到 Configuration.cache 集合中。
  - 在解析标签 `XMLMapperBuilder#cacheElement`


##### ResultSet
- `ResultSetHandler` 对结果集的处理。
- `DefaultMapResultHandler` 底层使用 Map<k,V> 存储映射得到的Java 对象。

#### 日志 log （采用适配器模式，兼容三方日志框架）
- Log 接口
- 采用工厂方法，`LogFactory` （负责创建 Log 对象）
  - 静态代码块，首先会检测 `logConstuctor` 字段是否为空，
      - 如果不为空，则表示已经成功确定当前使用的日志框架，直接返回。
      - 如果为空，则在当前线程中执行传入 `Runnalbe.run()` 尝试确定当前使用的日志框架
 
##### 生成主键 `KeyGenerator`
- Jdbc3KeyGenerator 用于获取数据库生成的自增ID，并记录到用户传入的实参对象的对应属性中。
- SelectkeyGenerator 查询主键信息，并记录到用户传入的实参对象的对应属性中。


#### 工具类
- `ResolverUtil` 查询