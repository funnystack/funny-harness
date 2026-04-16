## 构建命令

- 编译：`{build_command}`（如 `mvn compile` / `gradle build`）
- 打包：`{package_command}`（如 `mvn package -DskipTests`）
- 清理：`{clean_command}`（如 `mvn clean`）

## 测试命令

- 全量测试：`{test_command}`（如 `mvn test`）
- 单模块测试：`{module_test_command}`（如 `mvn test -pl {module_name}`）
- 跳过测试打包：`{skip_test_command}`（如 `mvn package -DskipTests`）

## 本地启动

- 启动方式：`{startup_command}`（如 `mvn spring-boot:run` / IDE 直接运行 `{main_class}`）
- 主启动类：`{main_class_full_name}`
- 本地配置文件：`{local_config_path}`（如 `src/main/resources/application-local.yml`）
- 健康检查：`{health_check_url}`（如 `http://localhost:{port}/actuator/health`）

## 部署

- 部署方式：`{deploy_method}`（如 Docker / K8s / JAR 直接部署）
- Docker 构建：`{docker_build_command}`
- 环境变量：`{env_vars_list}`（列出启动必需的环境变量名称，不含值）
