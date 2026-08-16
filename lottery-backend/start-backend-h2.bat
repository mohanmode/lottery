@echo off
chcp 65001 >nul
echo ========================================
echo   彩票后端 - H2 内存数据库模式启动
echo   (无需安装 MySQL, 适合开发调试)
echo ========================================
echo.

cd /d "%~dp0"

set SPRING_PROFILES_ACTIVE=h2

echo [启动中] 正在编译并启动 Spring Boot (H2 模式)...
echo [提示] 首次启动约 20~40 秒, 会自动导入 1591 期历史数据
echo [提示] 数据存在内存中, 重启后自动重新导入
echo [验证] 启动后访问 http://localhost:8080/api/system/health
echo.

mvn spring-boot:run

pause
