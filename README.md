# Spring Boot + MyBatis-Plus 实现 MySQL 主从复制动态数据源切换


>MySQL 主从复制是一种常见的数据库架构，它可以提高数据库的性能和可用性。**动态数据源切换则可以根据业务需求，在不同场景下使用不同的数据源，比如在读多写少的场景下，可以通过切换到从库来分担主库的压力**。
>
>在本文中，我们将介绍如何在 Spring Boot 中实现 MySQL 主从复制和动态数据源切换，使用 MyBatis-Plus 进行数据库操作

```
#代码地址
https://github.com/bangbangzhou/spring-boot-dynamic-master-slave.git
```

**公众号地址:**

![](https://files.mdnice.com/user/7954/63cacb3d-39c2-406e-86fd-edbcdcf5665e.png)



那么接下来我们开始项目实现，项目结构如下

![](https://files.mdnice.com/user/7954/cf2de763-8b4a-4303-920d-40b4b6bca3e8.png)


## 1.引入依赖

在项目的的`pom.xml`文件中引入Spring Boot和MyBatis-Plus的相关依赖

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <artifactId>spring-boot-starter-parent</artifactId>
        <groupId>org.springframework.boot</groupId>
        <version>2.7.15</version>
    </parent>

    <groupId>com.zbbmeta</groupId>
    <artifactId>spring-boot-dynamic-master-slave</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.30</version>
        </dependency>

        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.5.3</version>
        </dependency>

        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>5.8.20</version>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>


        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## 2. 配置数据源

在`application.yml`文件中配置主从数据源信息。**注意这里我们要搭建主从数据库，只是在一个mysql实例中创建两个库，里面存在相同表**

```yml

server:
  port: 8082

spring:
  datasource:
    master:
      username: root
      password: root
      url: jdbc:mysql://localhost:3306/shiro_db?useUnicode=true&characterEncoding=utf8
      driver-class-name: com.mysql.cj.jdbc.Driver
    slave:
      username: root
      password: root
      url: jdbc:mysql://localhost:3306/backend_db?useUnicode=true&characterEncoding=utf8
      driver-class-name: com.mysql.cj.jdbc.Driver


mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
```

## 3. 创建DatabaseType 枚举类型

创建DatabaseType 枚举类型,用于切换数据源时，确定连接的是那个数据源

**在`com.zbbmeta.config`包下创建DatabaseType枚举类型**

```java
// 定义一个枚举类型 DatabaseType，表示系统中的数据库类型
public enum DatabaseType {
    MASTER,  // 主数据库类型
    SLAVE    // 从数据库类型
}
```

## 4. 配置数据源上下文

在`com.zbbmeta.holder`包下创建一个`DataSourceContextHolder`类用于保存和获取当前线程使用的数据源类型

```java
public class DatabaseContextHolder {

    private static final ThreadLocal<DatabaseType> contextHolder = new ThreadLocal<>();

    public static void setDatabaseType(DatabaseType databaseType) {
        contextHolder.set(databaseType);
    }

    public static DatabaseType getDatabaseType() {
        return contextHolder.get();
    }

    public static void clearDatabaseType() {
        contextHolder.remove();
    }
}
```

## 5. 配置动态数据源

我们创建了一个 `DynamicDataSource` 类，继承 `AbstractRoutingDataSource`，用于实现动态数据源的切换。

>**AbstractRoutingDataSource** 是 Spring Framework 提供的一个**抽象数据源类**，用于**实现动态数据源切换**。<font color="red" size="4">它允许应用程序在运行时动态地切换到不同的数据源</font>，从而支持多数据源的场景，比如数据库读写分离、主从复制等

 `AbstractRoutingDataSource`介绍：

 - **动态数据源切换**： AbstractRoutingDataSource 的核心思想是根据某个键值（lookup key）来决定使用哪个具体的数据源。这个键值是通过 **determineCurrentLookupKey()** 方法提供

 - **抽象类**： AbstractRoutingDataSource 是一个抽象类，它提供了模板方法 determineCurrentLookupKey()，需要由子类实现
 - **实现 javax.sql.DataSource 接口**： AbstractRoutingDataSource 实现了 javax.sql.DataSource 接口，因此可以像常规数据源一样被用于与数据库的交互。

- **在 Spring 配置中使用**： 在 Spring 的配置中，我们可以将 `AbstractRoutingDataSource` 配置为数据源 bean，并将真实的数据源作为其目标数据源。在需要切换数据源的时候，调用 determineCurrentLookupKey() 方法，它将返回用于切换数据源的键值。

在`com.zbbmeta.config`包下创建`DynamicDataSource`类

```java
public class DynamicDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSourceType();
    }
}
```

DynamicDataSource类中重写`determineCurrentLookupKey()`方法： 在这个方法中，我们通过调用 `DataSourceContextHolder.getDataSourceType()` 来获取当前线程持有的数据源类型。这个方法的返回值将被用作数据源的 lookup key，从而实现动态切换。

## 6. 添加DataSource注解类

在·`com.zbbmeta.annotation`包下创建`DataSource`注解类，这是一个自定义注解，用于标记在类或方法上，以指定数据源的类型。下面是对这段代码的注解说明

```java
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataSource {

    DatabaseType type() default DatabaseType.SLAVE;

}

```

**注解说明**：

- **@interface DataSource**： 这是一个注解的声明，用于创建名为 DataSource 的自定义注解。

- **@Target({ElementType.METHOD, ElementType.TYPE})**： `@Target` 注解表示此注解可以用于类和方法。在这里，`DataSource` 注解可以标注在**类和方法**上。

- `@Retention(RetentionPolicy.RUNTIME)`： @Retention 注解表示这个注解的生命周期，即在运行时仍然可用。这是因为我们希望在运行时通过反射获取注解信息。

- `DatabaseType type() default DatabaseType.SLAVE`： 这是 DataSource 注解的一个成员变量。它是一个枚举类型的变量，表示数据库类型，默认值为 SLAVE。通过这个成员变量，我们可以在使用 DataSource 注解时指定使用的数据源类型

## 7. 配置数据源切换切面

在`com.zbbmeta.aspect`报下创建一个切面类`DataSourceAspect`，用于在执行数据库操作前动态切换数据源。

```java
@Aspect
@Component
@EnableAspectJAutoProxy
public class DataSourceAspect {
// 定义切点，匹配使用了 @DataSource 注解的方法
    @Pointcut("@annotation(com.zbbmeta.annotation.DataSource)")
    public void dataSourcePointCut() {}

    // 环绕通知，在方法执行前后切换数据源
    @Around("dataSourcePointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        // 获取方法上的 @DataSource 注解
        DataSource dataSource = method.getAnnotation(DataSource.class);
        if (dataSource != null) {
            // 切换数据源类型
            DatabaseContextHolder.setDatabaseType(dataSource.type());
        }

        try {
            // 执行目标方法
            return point.proceed();
        } finally {
            // 清除数据源类型，确保线程安全
            DatabaseContextHolder.clearDatabaseType();
        }
    }
}

```



## 8. 创建DataSourceConfig

在`com.zbbmeta.config`包下创建`DataSourceConfig`,用于配置主从两个数据源

```java
@Configuration
@Data
public class DataSourceConfig {
    @Value("${spring.datasource.master.url}")
    private String dbUrl;
    @Value("${spring.datasource.master.username}")
    private String username;
    @Value("${spring.datasource.master.password}")
    private String password;
    @Value("${spring.datasource.master.driver-class-name}")
    private String driverClassName;


    @Value("${spring.datasource.slave.url}")
    private String slaveDbUrl;
    @Value("${spring.datasource.slave.username}")
    private String slaveUsername;
    @Value("${spring.datasource.slave.password}")
    private String slavePassword;
    @Value("${spring.datasource.slave.driver-class-name}")
    private String slaveDriverClassName;


    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create()
                .driverClassName(driverClassName)
                .url(dbUrl)
                .username(username)
                .password(password)
                .build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.slave")
    public DataSource slaveDataSource() {

        return DataSourceBuilder.create()
                .driverClassName(slaveDriverClassName)
                .url(slaveDbUrl)
                .username(slaveUsername)
                .password(slavePassword)
                .build();

    }
}
```


## 9 创建DataSourceConfig

在`com.zbbmeta.config`包下创建DynamicDataSourceConfig类中配置MyBatis-Plus的相关内容。

```java
@Configuration
@MapperScan("com.zbbmeta.mapper")
public class DynamicDataSourceConfig {
    @Autowired
    private DataSource masterDataSource;

    @Autowired
    private DataSource slaveDataSource;

    // 配置动态数据源
    @Bean
    @Primary
    public DataSource dynamicDataSource() {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DatabaseType.MASTER, masterDataSource);
        targetDataSources.put(DatabaseType.SLAVE, slaveDataSource);

        DynamicRoutingDataSource dynamicDataSource = new DynamicRoutingDataSource();
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(masterDataSource); // 设置默认数据源
        return dynamicDataSource;
    }

    // 配置 MyBatis 的 SqlSessionFactory
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dynamicDataSource) throws Exception {
        MybatisSqlSessionFactoryBean sessionFactoryBean = new MybatisSqlSessionFactoryBean();
        sessionFactoryBean.setDataSource(dynamicDataSource);

        // 设置要扫描的 mapper 接口和 XML 文件路径
        sessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
        sessionFactoryBean.setTypeAliasesPackage("com.zbbmeta.entity");  // 设置实体类包路径

        return sessionFactoryBean.getObject();
    }

    // 配置 MyBatis 的 SqlSessionTemplate
    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}

```

## 10. 测试

使用MybatisX生成代码，并且创建`com.zbbmeta.controller`包下创建`TutorialController`类，并且在需要切换数据源的方法上使用 @DataSource 注解，切面将根据该注解的配置在方法执行前后进行数据源切换。
![](https://files.mdnice.com/user/7954/861537d3-4b5d-4e2b-bba8-c7ca65e4dfac.png)

![](https://files.mdnice.com/user/7954/554506a4-8d8d-4465-b906-52a0c3b0213d.png)

```java
@RestController
public class TutorialController {


    @Autowired
    private TutorialService tutorialService;


    @DataSource
    @GetMapping("/list")
    public List<Tutorial> list(){
        return tutorialService.list();

    }

    @DataSource(type = DatabaseType.MASTER)
    @GetMapping("/create")
    public Boolean create(){

        Tutorial tutorial = new Tutorial();
        tutorial.setTitle("master");
        tutorial.setDescription("master");

        return tutorialService.save(tutorial);
    }
}

```

使用POSTMAN发送请求

```
http://localhost:8082/list

http://localhost:8082/create
```

```
#代码地址
https://github.com/bangbangzhou/spring-boot-dynamic-master-slave.git
```

**公众号地址:**

![](https://files.mdnice.com/user/7954/63cacb3d-39c2-406e-86fd-edbcdcf5665e.png)
