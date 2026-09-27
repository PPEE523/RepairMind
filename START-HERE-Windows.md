# RepairMind：Windows 本地启动（给第一次接触项目的同学）

## 先弄清目标

这是一个“设备检修知识检索与作业辅助系统”：网页是 Vue 前端，Java/Spring Boot 后端处理登录和业务，PostgreSQL 保存用户与业务数据，Redis 保存登录令牌，Dify 提供 AI 问答与知识库能力。

组里说的“本地跑出来”，最低验收是：在自己的 Windows 电脑上启动这些服务，打开登录页，以管理员账号登录，进入数据看板，并能打开菜单。AI 检索另外依赖可访问且已授权的 Dify，不能只凭登录成功判断 AI 功能正常。

当前仓库中的 `安装文件/deploy/app/docker-compose.yml` 从 `源代码/后台/backend` 和 `源代码/前端` 构建镜像，**不需要另找 app.jar**；仓库中 `安装文件/deploy/app/docker/backend/Dockerfile` 的 `/data/app.jar` 启动方式并没有被上述 compose 文件使用。

## 零、先应用修复包（建数据库之前）

把本修复包的 `RepairMind-修复.patch` 复制到解压后的 `RepairMind-main` 根目录。如果电脑还没有 Git 命令，先安装 Git for Windows。打开 PowerShell，执行：

```powershell
Set-Location 'C:\RepairMind\RepairMind-main'
git apply --check '.\RepairMind-修复.patch'
git apply '.\RepairMind-修复.patch'
```

`git apply --check` 没输出且没有错误，才执行下一行。随后确认根目录出现 `START-HERE-Windows.md`，SQL 目录出现 `repair-schema-existing.sql`。如果你拿的是 Git 克隆的仓库，先运行 `git switch -c fix/windows-bootstrap-login` 创建个人分支，再应用补丁；直接下载的 ZIP 没有分支，可以先跑通，后面再在克隆的仓库中提交修复。

## 一、准备

1. 在 Windows 10/11 上安装并启动 Docker Desktop；使用 Linux containers，等界面显示 Engine running。打开 Windows 的 **PowerShell**（下文命令都在 PowerShell 执行）。
2. 将仓库解压到一个短路径，例如 `C:\RepairMind\RepairMind-main`，确认该目录内直接看得到 `README.md`、`安装文件`、`源代码`。以后所有命令都从此目录出发；实际路径不同就替换路径。
3. 在 PowerShell 执行：

   ```powershell
   Set-Location 'C:\RepairMind\RepairMind-main'
   docker version
   ```

   如果 `docker version` 连不上 Engine，先解决 Docker Desktop 启动问题。如果你已有本机 PostgreSQL 和 Redis，并且确认能使用 5432、6379 端口，可以跳过下文创建这两个容器的步骤。

## 二、建立本机数据库和 Redis（首次才做）

给本机测试数据库设置一个自己记得住的密码，不要发到群里。为避免 Docker Compose 把 `$` 等符号当作配置语法，本次本机测试先使用至少 12 位、只含英文字母和数字的密码。下面这条命令会要求你在 PowerShell 输入密码：

```powershell
$dbPassword = Read-Host '请设置本机 PostgreSQL 密码'
```

在**同一个 PowerShell 窗口**里执行：

```powershell
docker run -d --name repairmind-pg --restart unless-stopped -e "POSTGRES_PASSWORD=$dbPassword" -e "POSTGRES_DB=repair_db" -p "127.0.0.1:5432:5432" -v "repairmind-pgdata:/var/lib/postgresql/data" postgres:16
docker run -d --name repairmind-redis --restart unless-stopped -p "127.0.0.1:6379:6379" redis:7
docker exec repairmind-pg pg_isready -U postgres -d repair_db
```

最后一条命令返回 `accepting connections` 才继续。如果提示端口占用，先确认自己是否已有数据库/Redis；不要重复建同名容器。以后重启电脑后，可用 `docker start repairmind-pg repairmind-redis` 恢复两个容器。

### 新数据库：只初始化一次

**只在刚建立的空数据库执行以下两条命令。** `init-full.sql` 包含 `DROP TABLE`，重复执行会清除已有表的数据。本修复版的脚本已经包含看板和聊天所需的表，且默认管理员密码与登录页一致。

```powershell
docker cp '.\源代码\后台\backend\src\main\resources\sql\init-full.sql' repairmind-pg:/tmp/init-full.sql
docker exec repairmind-pg psql -v ON_ERROR_STOP=1 -U postgres -d repair_db -f /tmp/init-full.sql
```

验证表已经存在：

```powershell
docker exec repairmind-pg psql -U postgres -d repair_db -tAc "SELECT to_regclass('public.sys_user'), to_regclass('public.chat_history'), to_regclass('public.work_sop_flow');"
```

应看到 `sys_user`、`chat_history`、`work_sop_flow`。

### 已按旧 README 建过数据库：只补表，不重新初始化

如果你此前已经执行旧的 `init-full.sql`，保留原数据库，执行可重复的补表脚本：

```powershell
docker cp '.\源代码\后台\backend\src\main\resources\sql\repair-schema-existing.sql' repairmind-pg:/tmp/repair-schema-existing.sql
docker exec repairmind-pg psql -v ON_ERROR_STOP=1 -U postgres -d repair_db -f /tmp/repair-schema-existing.sql
```

若 PostgreSQL 是你在 Windows 上自行安装的，而不是 `repairmind-pg` 容器，请用 pgAdmin 的 `Query Tool` 打开并执行对应脚本，且仅在**新空库**运行 `init-full.sql`。

## 三、配置并启动前后端

在部署目录创建 `.env` 文件：

```powershell
Set-Location '.\安装文件\deploy\app'
notepad .env
```

把下面内容粘贴到记事本，把 `替换成刚才的数据库密码` 改成第一步设置的密码，保存；不要在密码两边加引号。

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/repair_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=替换成刚才的数据库密码
SPRING_DATA_REDIS_HOST=host.docker.internal
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=
SPRING_DATA_REDIS_DATABASE=4
SERVER_BASE_URL=http://localhost:8080
```

如果数据库/Redis 安装在其他机器上，地址和密码应改成实际值。在 Windows Docker Desktop 中，容器通过 `host.docker.internal` 访问本机发布的端口。`.env` 只留在自己电脑上，不提交到 Git。

然后仍在 `安装文件\deploy\app` 中执行：

```powershell
docker compose up -d --build
docker compose ps
docker compose logs --tail=80 backend
```

第一次构建要下载 Java、Node、Maven 与 npm 依赖，可能需要较长时间；这一套 Docker 构建无需在 Windows 另外安装 JDK、Maven、Node。两个服务运行后访问 `http://localhost/`。新空库且已应用修复版脚本时，管理员为 `admin` / `Admin@123456`，成功后应进入数据看板；请尽快修改默认密码。旧数据库保留旧哈希时，旧 `init-full.sql` 实际对应 `admin` / `123456`，登录后立即修改密码。

如网页打不开，先看 `docker compose ps`；后端重启就看 `docker compose logs --tail=150 backend`；登录成功但看板报数据库错，就检查 `chat_history` 是否存在、是否运行了补表脚本。若日志仍出现 `/data/app.jar`，说明启动的不是当前 compose 源码构建的后端，请检查所在目录并重建：`docker compose up -d --build --force-recreate`。

本地使用完毕，在部署目录执行 `docker compose down`；这不会删除 PostgreSQL 的持久卷。测试 Dify/AI 功能前须与组长核对服务地址和独立授权信息；仓库里现有的配置值不能当作有效授权的保证。

## 四、交个人分支的修复

如果你已经在克隆仓库的个人分支应用补丁并跑通，可提交自己的改动：

```powershell
git status
git add README.md START-HERE-Windows.md '源代码/后台/backend/src/main/resources/sql/init-full.sql' '源代码/后台/backend/src/main/resources/sql/repair-schema-existing.sql'
git commit -m 'Fix local database bootstrap and default login'
git push -u origin fix/windows-bootstrap-login
```

如果先用下载的 ZIP 跑通，再克隆仓库、建个人分支，并在克隆的目录重新应用同一个补丁，验证后提交。补丁基于本次提供的 ZIP；若组长更新了仓库，`git apply --check` 可能失败，先把报错与 `git status` 发给我，不要强行覆盖。修复内容包括：正确的管理员 BCrypt 哈希；新数据库完整建表；旧数据库安全补表；本指南与 README 的入口说明。提交后让组长评审。

代码包内的配置和初始化脚本包含类似 Dify API Key 的值。上传日志、截图和提交补丁时避免把它们贴出来；如果这些是仍在使用的真实密钥，请让组长更换。
