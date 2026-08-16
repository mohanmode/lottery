@echo off
chcp 65001 >nul
REM ============================================================
REM  彩票系统 - 后端启动脚本 (Windows)
REM  启动前请确保:
REM   1. MySQL 已启动,并创建 lottery_db 数据库:
REM      CREATE DATABASE IF NOT EXISTS lottery_db DEFAULT CHARSET utf8mb4;
REM   2. application.yml 中的用户名密码正确 (默认 root/123456)
REM   3. JDK 17+ 已安装 (JAVA_HOME 已配置)
REM ============================================================
setlocal
cd /d "%~dp0"

echo.
echo ============================================
echo   彩票系统后端启动中...
echo   后端地址: http://localhost:8080
echo   健康检查: http://localhost:8080/api/system/health
echo   自动导入历史开奖数据 (1591 期)
echo ============================================
echo.

REM 首次启动前先确保数据库表存在: 会自动执行 schema.sql (需 spring.sql.init=true)
REM 直接通过 maven 启动
call mvn.cmd spring-boot:run -Dspring-boot.run.profiles=dev
if errorlevel 1 (
  echo.
  echo [错误] 启动失败,请检查日志。
  pause
)
endlocal
