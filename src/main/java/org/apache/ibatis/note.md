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


#### 日志 log 
> 采用工厂方法，`LogFactory` 