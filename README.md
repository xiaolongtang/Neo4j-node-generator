# Neo4j Node Generator

这是一个自动生成Neo4j实体和仓库类的工具。

## 使用说明

1. 确保Neo4j数据库已经启动并可以连接
2. 在`application.properties`中配置Neo4j连接信息
3. 运行应用程序，它将自动生成实体类和仓库接口

## 关于APOC插件

本项目最初使用了APOC插件的`apoc.meta.type`函数来获取属性类型，但现在已经修改为使用Neo4j内置函数。如果你想使用APOC插件的功能，请按照以下步骤安装和配置：

### 安装APOC插件

1. 下载APOC插件JAR文件，确保版本与你的Neo4j版本兼容
   - 从[Neo4j APOC Releases](https://github.com/neo4j-contrib/neo4j-apoc-procedures/releases)下载
2. 将JAR文件放入Neo4j安装目录的`plugins`文件夹中
3. 修改Neo4j配置文件`neo4j.conf`，添加以下配置：
   ```
   dbms.security.procedures.unrestricted=apoc.*
   dbms.security.procedures.allowlist=apoc.*
   ```
4. 重启Neo4j服务器

### 验证APOC安装

在Neo4j浏览器中运行以下查询来验证APOC是否正确安装：

```cypher
CALL apoc.help("apoc")
```

如果返回结果，则表示APOC已成功安装。

## 项目依赖

- Java 17
- Spring Boot 3.1.5
- Neo4j Java Driver 5.14.0
- Lombok 1.18.30

## Docker command for neo4j with apoc plugin
```dockerfile
docker run \
    --restart always \
    -p 7474:7474 -p 7687:7687 \
    -e NEO4J_AUTH=neo4j/neo4j123456 \
    -e NEO4J_apoc_export_file_enabled=true \
    -e NEO4J_apoc_import_file_enabled=true \
    -e NEO4J_apoc_import_file_use__neo4j__config=true \
    -e NEO4J_PLUGINS=\[\"apoc-extended\"\] \
    neo4j:5.16
```


