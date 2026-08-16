@echo off
chcp 65001 >nul
REM ============================================================
REM  彩票系统 - 前端启动脚本 (Windows)
REM  前端默认端口: 5173, 代理 /api -> http://localhost:8080
REM  请先启动后端!
REM ============================================================
setlocal
cd /d "%~dp0"

if not exist "node_modules" (
  echo 首次启动,正在安装依赖...
  call npm.cmd install --registry=https://registry.npmmirror.com
  if errorlevel 1 (
    echo npm install 失败,请检查网络 / Node 版本 (需 Node 18+)
    pause
    exit /b 1
  )
)

echo.
echo ============================================
echo   彩票系统前端启动中...
echo   前端地址: http://localhost:5173
echo   后端代理: /api -> http://localhost:8080
echo ============================================
echo.
call npm.cmd run dev
endlocal
