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

#### 日志 log （采用适配器模式）
- Log 接口
- 采用工厂方法，`LogFactory` （负责创建 Log 对象）
  - 静态代码块，首先会检测 `logConstuctor` 字段是否为空，
      - 如果不为空，则表示已经成功确定当前使用的日志框架，直接返回。
      - 如果为空，则在当前线程中执行传入 `Runnalbe.run()` 尝试确定当前使用的日志框架
  