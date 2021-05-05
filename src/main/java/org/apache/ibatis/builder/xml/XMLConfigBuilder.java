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
package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;
import javax.sql.DataSource;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.AutoMappingUnknownColumnBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.type.JdbcType;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class XMLConfigBuilder extends BaseBuilder {

  // 状态标识 ，字段（记录，当前XMLConfigBuilder 对象是否已经成功解析完 mybatis-config.xml）
  private boolean parsed;

  // XML 解析器
  private final XPathParser parser;

  // 标签定义的环境名称
  private String environment;

  // ReflectorFactory 接口的核心功能，是实现对 Reflector 对象的创建和缓存。
  private final ReflectorFactory localReflectorFactory = new DefaultReflectorFactory();

  public XMLConfigBuilder(Reader reader) {
    this(reader, null, null);
  }

  public XMLConfigBuilder(Reader reader, String environment) {
    this(reader, environment, null);
  }

  public XMLConfigBuilder(Reader reader, String environment, Properties props) {
    this(new XPathParser(reader, true, props, new XMLMapperEntityResolver()), environment, props);
  }

  public XMLConfigBuilder(InputStream inputStream) {
    this(inputStream, null, null);
  }

  public XMLConfigBuilder(InputStream inputStream, String environment) {
    this(inputStream, environment, null);
  }

  public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
    this(new XPathParser(inputStream, true, props, new XMLMapperEntityResolver()), environment, props);
  }

  private XMLConfigBuilder(XPathParser parser, String environment, Properties props) {

    /**
     *  注册 TypeHandler {@link Configuration#Configuration()}
     */
    super(new Configuration());
    ErrorContext.instance().resource("SQL Mapper Configuration");

    /**
     *  setVariables {@link Configuration#setVariables(Properties)}
     */
    this.configuration.setVariables(props);
    this.parsed = false;
    this.environment = environment;
    this.parser = parser;
  }

  public Configuration parse() {

    // 防止一个实例多次调用
    if (parsed) {
      throw new BuilderException("Each XMLConfigBuilder can only be used once.");
    }
    parsed = true;

    /**
     * 解析 <configuration> 节点。 {@link #parseConfiguration(XNode)}
     */
    parseConfiguration(parser.evalNode("/configuration"));
    return configuration;
  }

  /**
   * 对 <configuration> 子标签进行处理。
   *
   *  解析 mybatis-config.xml 配置文件的完整流程。
   *    1、properties
   *    2、settings
   *
   *    处理日志相关组件
   *
   *    typeAliases 标签
   *    plugins 标签
   *    objectFactory 标签
   *    objectWrapperFactory 标签
   *    reflectorFactory 标签
   *    environments 标签
   *    databaseIdProvider 标签
   *    typeHandlers 标签
   *    mappers 标签
   *
   *
   * @param root
   */
  private void parseConfiguration(XNode root) {
    try {
      //issue #117 read properties first

      /**
       *  解析 <properties> {@link #propertiesElement(XNode)}
       */
      propertiesElement(root.evalNode("properties"));

      /**
       *  处理 “setting” 标签 {@link #settingsAsProperties(XNode)}
       *
       */
      Properties settings = settingsAsProperties(root.evalNode("settings"));
      loadCustomVfs(settings);
      loadCustomLogImpl(settings);

      /**
       * <typeAliases> 别名解析 {@link #typeAliasesElement(XNode)}
       */
      typeAliasesElement(root.evalNode("typeAliases"));

      /**
       * <plugins>  解析插件 {@link #pluginElement(XNode)}
       */
      pluginElement(root.evalNode("plugins"));
      objectFactoryElement(root.evalNode("objectFactory"));
      objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
      reflectorFactoryElement(root.evalNode("reflectorFactory"));
      settingsElement(settings);
      // read it after objectFactory and objectWrapperFactory issue #631

      /**
       * 处理<environments>标签 {@link #environmentsElement(XNode)}
       */
      environmentsElement(root.evalNode("environments"));
      databaseIdProviderElement(root.evalNode("databaseIdProvider"));

      /**
       *  处理 “typeHandler” 标签 {@link #typeHandlerElement(XNode)}
       */
      typeHandlerElement(root.evalNode("typeHandlers"));

      /**
       *  <mappers/> 标签解析过程 {@link #mapperElement(XNode)}
       */
      mapperElement(root.evalNode("mappers"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
    }
  }

  /**
   * 处理 “setting” 标签
   *
   * @param context
   * @return
   */
  private Properties settingsAsProperties(XNode context) {
    if (context == null) {
      return new Properties();
    }

    /*
     * 处理 <settings> 标签的所有子标签，也就是 <setting>
     *   将其 name 属性 和 value 属性 整理到 Properties 对象中保存
     */
    Properties props = context.getChildrenAsProperties();
    // Check that all settings are known to the configuration class

    // 创建 Configuration 对应的 MetaClass 对象。
    MetaClass metaConfig = MetaClass.forClass(Configuration.class, localReflectorFactory);

    // 检测 configuration 对象中是否包含每个配置项的 setter 方法。
    for (Object key : props.keySet()) {
      if (!metaConfig.hasSetter(String.valueOf(key))) {
        throw new BuilderException("The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive).");
      }
    }
    return props;
  }

  private void loadCustomVfs(Properties props) throws ClassNotFoundException {
    String value = props.getProperty("vfsImpl");
    if (value != null) {
      String[] clazzes = value.split(",");
      for (String clazz : clazzes) {
        if (!clazz.isEmpty()) {
          @SuppressWarnings("unchecked")
          Class<? extends VFS> vfsImpl = (Class<? extends VFS>)Resources.classForName(clazz);
          configuration.setVfsImpl(vfsImpl);
        }
      }
    }
  }

  private void loadCustomLogImpl(Properties props) {
    Class<? extends Log> logImpl = resolveClass(props.getProperty("logImpl"));
    configuration.setLogImpl(logImpl);
  }

  /**
   *  别名解析
   *
   * @param parent
   */
  private void typeAliasesElement(XNode parent) {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {
        if ("package".equals(child.getName())) {
          String typeAliasPackage = child.getStringAttribute("name");
          configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
        } else {
          String alias = child.getStringAttribute("alias");
          String type = child.getStringAttribute("type");
          try {
            Class<?> clazz = Resources.classForName(type);
            if (alias == null) {
              typeAliasRegistry.registerAlias(clazz);
            } else {
              typeAliasRegistry.registerAlias(alias, clazz);
            }
          } catch (ClassNotFoundException e) {
            throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
          }
        }
      }
    }
  }

  private void pluginElement(XNode parent) throws Exception {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {

        // 获取 `interceptor` 属性。
        String interceptor = child.getStringAttribute("interceptor");

        // 获取拦截器属性，转换为 Properties 对象。
        Properties properties = child.getChildrenAsProperties();

        // 初始化 interceptor 属性指定的自定义插件。
        Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).newInstance();
        // 设置拦截器实例属性信息
        interceptorInstance.setProperties(properties);

        /**
         * 将拦截器实例添加到拦截器链中 {@link Configuration#addInterceptor(Interceptor)}
         */
        configuration.addInterceptor(interceptorInstance);
      }
    }
  }

  /**
   * 处理 “ObjectFactory” 标签
   * @param context
   * @throws Exception
   */
  private void objectFactoryElement(XNode context) throws Exception {
    if (context != null) {
      // 获取<objectFactory>标签的type属性
      String type = context.getStringAttribute("type");
      Properties properties = context.getChildrenAsProperties();

      // 根据type属性值，初始化自定义的ObjectFactory实现
      ObjectFactory factory = (ObjectFactory) resolveClass(type).newInstance();

      // 初始化ObjectFactory对象的配置
      factory.setProperties(properties);

      // 将ObjectFactory对象记录到Configuration这个全局配置对象中
      configuration.setObjectFactory(factory);
    }
  }

  private void objectWrapperFactoryElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      ObjectWrapperFactory factory = (ObjectWrapperFactory) resolveClass(type).newInstance();
      configuration.setObjectWrapperFactory(factory);
    }
  }

  private void reflectorFactoryElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      ReflectorFactory factory = (ReflectorFactory) resolveClass(type).newInstance();
      configuration.setReflectorFactory(factory);
    }
  }

  /**
   *  核心逻辑，解析 mybatis-config.xml 配置文件 <properties></properties> 标签
   *
   * @param context
   * @throws Exception
   */
  private void propertiesElement(XNode context) throws Exception {
    if (context != null) {
      Properties defaults = context.getChildrenAsProperties();
      String resource = context.getStringAttribute("resource");
      String url = context.getStringAttribute("url");
      if (resource != null && url != null) {
        throw new BuilderException("The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
      }

      /*
       * 解析出来的 KV 信息，被记录在 Properties 对象
       *    （ Configuration 全局配置对象 variables 字段 ）
       */

      if (resource != null) {
        defaults.putAll(Resources.getResourceAsProperties(resource));
      } else if (url != null) {
        defaults.putAll(Resources.getUrlAsProperties(url));
      }
      Properties vars = configuration.getVariables();
      if (vars != null) {
        defaults.putAll(vars);
      }
      parser.setVariables(defaults);
      configuration.setVariables(defaults);
    }
  }

  private void settingsElement(Properties props) {
    configuration.setAutoMappingBehavior(AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));
    configuration.setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior.valueOf(props.getProperty("autoMappingUnknownColumnBehavior", "NONE")));
    configuration.setCacheEnabled(booleanValueOf(props.getProperty("cacheEnabled"), true));
    configuration.setProxyFactory((ProxyFactory) createInstance(props.getProperty("proxyFactory")));
    configuration.setLazyLoadingEnabled(booleanValueOf(props.getProperty("lazyLoadingEnabled"), false));
    configuration.setAggressiveLazyLoading(booleanValueOf(props.getProperty("aggressiveLazyLoading"), false));
    configuration.setMultipleResultSetsEnabled(booleanValueOf(props.getProperty("multipleResultSetsEnabled"), true));
    configuration.setUseColumnLabel(booleanValueOf(props.getProperty("useColumnLabel"), true));
    configuration.setUseGeneratedKeys(booleanValueOf(props.getProperty("useGeneratedKeys"), false));
    configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));
    configuration.setDefaultStatementTimeout(integerValueOf(props.getProperty("defaultStatementTimeout"), null));
    configuration.setDefaultFetchSize(integerValueOf(props.getProperty("defaultFetchSize"), null));
    configuration.setDefaultResultSetType(resolveResultSetType(props.getProperty("defaultResultSetType")));
    configuration.setMapUnderscoreToCamelCase(booleanValueOf(props.getProperty("mapUnderscoreToCamelCase"), false));
    configuration.setSafeRowBoundsEnabled(booleanValueOf(props.getProperty("safeRowBoundsEnabled"), false));
    configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));
    configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));
    configuration.setLazyLoadTriggerMethods(stringSetValueOf(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));
    configuration.setSafeResultHandlerEnabled(booleanValueOf(props.getProperty("safeResultHandlerEnabled"), true));
    configuration.setDefaultScriptingLanguage(resolveClass(props.getProperty("defaultScriptingLanguage")));
    configuration.setDefaultEnumTypeHandler(resolveClass(props.getProperty("defaultEnumTypeHandler")));
    configuration.setCallSettersOnNulls(booleanValueOf(props.getProperty("callSettersOnNulls"), false));
    configuration.setUseActualParamName(booleanValueOf(props.getProperty("useActualParamName"), true));
    configuration.setReturnInstanceForEmptyRow(booleanValueOf(props.getProperty("returnInstanceForEmptyRow"), false));
    configuration.setLogPrefix(props.getProperty("logPrefix"));
    configuration.setConfigurationFactory(resolveClass(props.getProperty("configurationFactory")));
  }

  /**
   * 处理<environments>标签
   * @param context
   * @throws Exception
   */
  private void environmentsElement(XNode context) throws Exception {
    if (context != null) {

      // 未指定使用的环境id，默认获取default值.
      if (environment == null) {
        environment = context.getStringAttribute("default");
      }

      // 获取<environment>标签下的所有配置
      for (XNode child : context.getChildren()) {
        String id = child.getStringAttribute("id");
        if (isSpecifiedEnvironment(id)) {

          // 获取<transactionManager>、<dataSource>等标签，并进行解析，其中会根据配置信息初始化相应的TransactionFactory对象和DataSource对象
          TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
          DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
          DataSource dataSource = dsFactory.getDataSource();

          // 创建Environment对象，并关联创建好的TransactionFactory和DataSource
          Environment.Builder environmentBuilder = new Environment.Builder(id)
              .transactionFactory(txFactory)
              .dataSource(dataSource);

          // 将Environment对象记录到Configuration中，等待后续使用
          configuration.setEnvironment(environmentBuilder.build());
        }
      }
    }
  }

  /**
   * 处理<databaseIdProvider>标签
   * @param context
   * @throws Exception
   */
  private void databaseIdProviderElement(XNode context) throws Exception {
    DatabaseIdProvider databaseIdProvider = null;
    if (context != null) {

      // 获取type属性值
      String type = context.getStringAttribute("type");
      // awful patch to keep backward compatibility
      if ("VENDOR".equals(type)) {
        type = "DB_VENDOR";
      }

      // 初始化DatabaseIdProvider
      Properties properties = context.getChildrenAsProperties();
      databaseIdProvider = (DatabaseIdProvider) resolveClass(type).newInstance();
      databaseIdProvider.setProperties(properties);
    }
    Environment environment = configuration.getEnvironment();
    if (environment != null && databaseIdProvider != null) {

      /**
       * 通过DataSource获取DatabaseId，{@link org.apache.ibatis.mapping.VendorDatabaseIdProvider#getDatabaseId(DataSource)}
       *  并保存到Configuration中，
       */
      String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
      configuration.setDatabaseId(databaseId);
    }
  }

  private TransactionFactory transactionManagerElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      Properties props = context.getChildrenAsProperties();
      TransactionFactory factory = (TransactionFactory) resolveClass(type).newInstance();
      factory.setProperties(props);
      return factory;
    }
    throw new BuilderException("Environment declaration requires a TransactionFactory.");
  }

  private DataSourceFactory dataSourceElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      Properties props = context.getChildrenAsProperties();
      DataSourceFactory factory = (DataSourceFactory) resolveClass(type).newInstance();
      factory.setProperties(props);
      return factory;
    }
    throw new BuilderException("Environment declaration requires a DataSourceFactory.");
  }

  /**
   *  处理 typeHandler 标签
   *
   * @param parent
   */
  private void typeHandlerElement(XNode parent) {
    if (parent != null) {

      // 处理全部 typeHandler 子标签
      for (XNode child : parent.getChildren()) {
        if ("package".equals(child.getName())) {

          /*
           * 如果指定了 package 属性，则扫描指定包中所有的类。
           * 并解析 @MapperType 注解，完成 TypeHandler 的注册。
           */
          String typeHandlerPackage = child.getStringAttribute("name");

          /**
           * 扫描指定包下的 typeHandler {@link org.apache.ibatis.type.TypeHandlerRegistry#register(String)}
           */
          typeHandlerRegistry.register(typeHandlerPackage);
        } else {
          String javaTypeName = child.getStringAttribute("javaType");
          String jdbcTypeName = child.getStringAttribute("jdbcType");
          String handlerTypeName = child.getStringAttribute("handler");

          // 根据属性确定 TypeHandler 类型以及它能处理的数据库类型 和 Java 类型。
          Class<?> javaTypeClass = resolveClass(javaTypeName);
          JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
          Class<?> typeHandlerClass = resolveClass(handlerTypeName);

          // 调用 typeHandlerRegister.register 方法注册 TypeHandler.
          if (javaTypeClass != null) {
            if (jdbcType == null) {
              typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
            } else {
              typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
            }
          } else {
            typeHandlerRegistry.register(typeHandlerClass);
          }
        }
      }
    }
  }

  /**
   * <Mappers/> 标签解析过程
   * @param parent
   * @throws Exception
   */
  private void mapperElement(XNode parent) throws Exception {
    if (parent != null) {

      // 遍历解析 mappers 下的节点
      for (XNode child : parent.getChildren()) {

        // 通过 package 标签指定包。
        if ("package".equals(child.getName())) {
          String mapperPackage = child.getStringAttribute("name");

          /**
           *  扫描指定包内全部 Java 类型 {@link Configuration#addMappers(String)}
           */
          configuration.addMappers(mapperPackage);
        } else {

          /**
           * 如果不存在 package 节点，那么扫描 mapper 节点。
           *
           *  resource、url、mapperClass 三个值是有值的
           */
          String resource = child.getStringAttribute("resource");
          String url = child.getStringAttribute("url");
          String mapperClass = child.getStringAttribute("class");

          // 通过 resource 属性指定 XML 文件路径。
          if (resource != null && url == null && mapperClass == null) {

            // 通过URL 属性指定 XML 文件路径。
            ErrorContext.instance().resource(resource);
            InputStream inputStream = Resources.getResourceAsStream(resource);
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());

            /**
             * Mapper 解析 {@link XMLMapperBuilder#parse()}
             */
            mapperParser.parse();
          } else if (resource == null && url != null && mapperClass == null) {
            ErrorContext.instance().resource(url);
            InputStream inputStream = Resources.getUrlAsStream(url);
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url, configuration.getSqlFragments());

            /**
             *  Mapper 解析 {@link XMLMapperBuilder#parse()}
             */
            mapperParser.parse();
          } else if (resource == null && url == null && mapperClass != null) {

            // 通过类加载构造 configuration
            Class<?> mapperInterface = Resources.classForName(mapperClass);

            /**
             *  注册 Mapper {@link Configuration#addMapper(Class)}
             */
            configuration.addMapper(mapperInterface);
          } else {

            //
            throw new BuilderException("A mapper element may only specify a url, resource or class, but not more than one.");
          }
        }
      }
    }
  }

  private boolean isSpecifiedEnvironment(String id) {
    if (environment == null) {
      throw new BuilderException("No environment specified.");
    } else if (id == null) {
      throw new BuilderException("Environment requires an id attribute.");
    } else if (environment.equals(id)) {
      return true;
    }
    return false;
  }

}
