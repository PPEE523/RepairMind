# 设备检修知识检索与作业辅助系统

## 产品使用说明

本系统用于设备检修知识检索、作业辅助、知识库管理及相关智能分析。系统采用前后端分离架构，交付包已包含可直接运行的前端页面和后端程序。

本文档面向系统管理员和现场运维人员，说明系统的安装、启动、停止、升级和故障排查方法。除非交付人员另有说明，客户无需安装 Node.js、JDK 或 Maven，也无需从源代码构建系统。

---

## 1. 交付内容

系统交付目录如下：

```text
20270731/
└─ 安装文件/
   └─ deploy/
      └─ app/
         ├─ artifacts/                 # 系统程序文件
         │  ├─ backend/app.jar         # 后端程序
         │  └─ frontend/dist/          # 前端页面
         ├─ config/                    # 系统配置
         ├─ docker/                    # 容器运行配置
         ├─ logs/                      # 系统日志
         ├─ volumes/uploads/           # 用户上传文件
         └─ docker-compose.yml         # 服务编排文件
```

请勿随意删除或改名 `artifacts`、`config`、`logs` 和 `volumes` 目录。

---

## 2. 运行环境要求

| 项目 | 要求 |
|---|---|
| 操作系统 | Windows 10 22H2 或 Windows 11 |
| 容器运行环境 | Docker Desktop 稳定版，启用 Linux containers |
| 数据库 | PostgreSQL 14 或更高版本 |
| 缓存服务 | Redis 6 或更高版本 |
| 智能知识服务 | 已部署并可访问的 Dify 服务 |
| 网络 | 系统服务器能够访问 PostgreSQL、Redis 和 Dify |

安装 Docker Desktop 时建议启用 WSL 2。安装完成后启动 Docker Desktop，并确认界面显示 **Engine running**。

如需将系统提供给其他电脑访问，还需在服务器防火墙中允许系统访问端口，并由客户网络管理员分配访问地址。

---

## 3. 首次部署

首次部署由系统管理员或交付人员完成。已经完成首次配置后，日常使用请直接参阅[第 4 节：日常启动与停止](#4-日常启动与停止)。

### 3.1 准备 PostgreSQL

本节只在首次部署时执行。需要准备一个 PostgreSQL 数据库，并在该数据库中创建系统所需的数据表。

#### 方式一：使用 pgAdmin（推荐）

1. 打开 **pgAdmin**，连接 PostgreSQL 服务器。连接时使用安装 PostgreSQL 时设置的服务器地址、端口、用户名和密码。默认端口为 `5432`，默认管理员用户名通常为 `postgres`。
2. 在左侧展开 `Servers` → 对应的 PostgreSQL 服务器，右键点击 `Databases`，选择 **Create → Database**。
3. 将数据库名称填写为 `repair_db`，所有者选择 `postgres`（或客户指定的数据库用户），点击 **Save**。
4. 在左侧选中刚创建的 `repair_db`，右键选择 **Query Tool**。
5. 在 Query Tool 中打开并执行以下初始化脚本：

   ```text
   源代码/后台/backend/src/main/resources/sql/init-full.sql
   ```

   可以在 Query Tool 中选择 **File → Open** 打开该文件，也可以用文本编辑器打开脚本后复制全部内容到 Query Tool，再点击执行按钮。
6. 执行完成后，在 Query Tool 中运行以下语句验证数据表是否已创建：

   ```sql
   SELECT table_name
   FROM information_schema.tables
   WHERE table_schema = 'public'
   ORDER BY table_name;
   ```

   查询结果应能看到 `sys_user`、`sys_role`、`equipment_info` 等系统数据表。

#### 方式二：使用 PostgreSQL 命令行

如果已安装 `psql`，可在 PowerShell 中执行以下命令。请将路径替换为实际项目路径，并按提示输入 PostgreSQL 密码：

```powershell
psql -h localhost -p 5432 -U postgres -d postgres -c "CREATE DATABASE repair_db;"
psql -h localhost -p 5432 -U postgres -d repair_db -f "D:\TJU\软件杯\20270731\源代码\后台\backend\src\main\resources\sql\init-full.sql"
```

如果提示数据库 `repair_db` 已存在，说明第一条命令无需再次执行，可以直接执行第二条命令。

> **重要：不要重复执行初始化脚本。** 当前脚本包含 `DROP TABLE IF EXISTS` 语句，重新执行可能会删除已有业务表及其数据。数据库已经投入使用时，不要自行重新初始化，请先联系系统管理员或技术支持。

#### 检查 PostgreSQL 是否可访问

在部署服务器的 PowerShell 中执行：

```powershell
Test-NetConnection localhost -Port 5432
```

如果 PostgreSQL 安装在本机，后续 `.env` 中使用 `host.docker.internal` 作为数据库主机；这是 Docker Desktop 容器访问 Windows 宿主机的固定地址。如果 PostgreSQL 安装在其他服务器，则将 `.env` 中的数据库主机改为该服务器的 IP 地址或域名。

测试结果中 `TcpTestSucceeded` 必须为 `True`。如果为 `False`，请先启动 PostgreSQL 服务，并检查 PostgreSQL 服务端口、防火墙及网络访问权限，确认无误后再启动系统。

### 3.2 准备 Redis

确认 Redis 已启动并监听端口 `6379`。可以在 PowerShell 中检查：

```powershell
Test-NetConnection localhost -Port 6379
```

如果 Redis 设置了密码，须在后续配置中填写对应密码。

### 3.3 配置系统

进入部署目录：

```powershell
Set-Location "D:\TJU\软件杯\20270731\安装文件\deploy\app"
```

在 `docker-compose.yml` 所在目录创建 `.env` 文件。请按照客户实际环境填写以下配置：

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/repair_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=请填写数据库密码

SPRING_DATA_REDIS_HOST=host.docker.internal
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=
SPRING_DATA_REDIS_DATABASE=4

SERVER_BASE_URL=http://localhost
```

当 PostgreSQL 或 Redis 位于其他服务器时，将 `host.docker.internal` 替换为实际服务器地址。

Dify 地址、知识库和应用配置位于：

```text
安装文件/deploy/app/config/application-prod.yml
```

请由系统管理员根据交付信息填写 Dify 地址及授权信息。真实密码、API Key、JWT 密钥等敏感信息不得通过公开渠道传播，也不得提交到代码仓库。

### 3.4 首次启动

首次启动或交付包中的镜像发生变化时执行：

```powershell
docker compose up -d --build
```

检查服务状态：

```powershell
docker compose ps
```

当 `biz-backend` 和 `biz-frontend` 均处于 `Up` 或 `running` 状态时，打开浏览器访问：

```text
http://localhost
```

---

## 4. 日常启动与停止

### 4.1 启动系统

启动 Docker Desktop 后，以 PowerShell 执行：

```powershell
Set-Location "D:\TJU\软件杯\20270731\安装文件\deploy\app"
docker compose up -d
```

日常启动不需要重新构建镜像。

### 4.2 停止系统

```powershell
docker compose down
```

该命令只停止并删除容器，不会删除数据库、上传文件或日志。

### 4.3 重启系统

修改配置后，或系统需要重新加载时执行：

```powershell
docker compose restart
```

修改 Dockerfile、更新程序包或更新前端页面后，应执行：

```powershell
docker compose down
docker compose up -d --build
```

### 4.4 访问系统

默认访问地址：

```text
http://localhost
```

如需从其他电脑访问，请将 `localhost` 替换为部署服务器的 IP 地址或域名，例如：

```text
http://192.168.1.100
```

---

## 5. 日志与数据管理

### 5.1 查看服务状态

```powershell
docker compose ps
```

### 5.2 查看日志

查看全部服务日志：

```powershell
docker compose logs --tail=200
```

持续查看后端日志：

```powershell
docker compose logs -f backend
```

持续查看前端日志：

```powershell
docker compose logs -f frontend
```

### 5.3 数据和文件位置

| 内容 | 位置 | 说明 |
|---|---|---|
| 用户上传文件 | `安装文件/deploy/app/volumes/uploads/` | 不得随意删除 |
| 后端日志 | `安装文件/deploy/app/logs/backend/` | 用于故障定位 |
| 系统配置 | `安装文件/deploy/app/config/` | 修改前应先备份 |
| 业务数据 | PostgreSQL 数据库 `repair_db` | 应纳入客户备份策略 |
| 缓存数据 | Redis | 用于缓存和会话，按交付方案维护 |

建议定期备份 PostgreSQL 数据库、`volumes/uploads` 目录和 `config` 目录。

---

## 6. 常见问题处理

### 6.1 页面无法打开

依次确认：

1. Docker Desktop 已启动并显示 **Engine running**；
2. 前端容器处于运行状态；
3. 访问地址和端口正确；
4. 服务器防火墙未拦截访问端口。

检查状态：

```powershell
docker compose ps
```

如果前端容器未运行，查看日志：

```powershell
docker compose logs --tail=200 frontend
```

### 6.2 页面可以打开，但功能请求失败

查看后端日志：

```powershell
docker compose logs --tail=200 backend
```

重点检查 PostgreSQL、Redis 和 Dify 的地址、端口、账号、密码及授权信息。

### 6.3 后端容器启动后退出

确认以下内容：

- `artifacts/backend/app.jar` 文件存在；
- PostgreSQL 已启动且数据库名称为 `repair_db`；
- 数据库用户名和密码正确；
- Redis 服务可访问；
- `config/application-prod.yml` 格式正确；
- Dify 地址和授权信息有效。

修改配置后重新启动：

```powershell
docker compose down
docker compose up -d
```

### 6.4 端口被占用

默认端口如下：

| 服务 | 端口 |
|---|---:|
| 系统前端 | 80 |
| 系统后端 | 8080 |
| PostgreSQL | 5432 |
| Redis | 6379 |

检查端口：

```powershell
Get-NetTCPConnection -LocalPort 80,8080,5432,6379 -ErrorAction SilentlyContinue
```

如前端 80 端口已被其他程序占用，可在 `docker-compose.yml` 中将：

```yaml
ports:
  - "80:80"
```

改为：

```yaml
ports:
  - "8088:80"
```

然后重新启动，访问地址改为：

```text
http://localhost:8088
```

端口调整应由系统管理员完成，并同步通知系统使用人员。

### 6.5 上传文件或历史数据不见

不要删除或移动以下目录：

```text
安装文件/deploy/app/volumes/uploads/
```

如果数据库数据异常，请先停止系统并保留现场日志，再联系系统交付或运维人员。不要直接删除数据库或重新执行初始化脚本。

---

## 7. 安全要求

正式使用前必须由系统管理员完成以下工作：

- 修改数据库默认密码；
- 设置 Redis 访问密码（如客户安全策略要求）；
- 配置独立且足够复杂的 JWT 密钥；
- 配置正式的 Dify API Key、知识库和应用授权信息；
- 限制 PostgreSQL、Redis 管理端口的网络访问范围；
- 不将 `.env`、生产配置文件或包含密钥的日志发送给无关人员；
- 定期备份业务数据库、上传文件和系统配置；
- 生产环境建议通过 HTTPS 或客户现有网关提供访问。

如果怀疑密码或 API Key 泄露，应立即更换对应凭据，并重新启动后端服务使配置生效。

---

## 8. 技术支持信息

联系技术支持时，请一并提供以下信息：

- 系统访问地址；
- 问题发生时间；
- `docker compose ps` 输出；
- 相关服务最近 200 行日志；
- 问题现象和可复现步骤。

请先对日志和配置中的密码、Token、API Key 等敏感信息进行脱敏，再发送给技术支持人员。
