@echo off
setlocal enabledelayedexpansion

rem 设置应用根目录
set "APP_HOME=%~dp0.."
set "PID_FILE=%APP_HOME%\application.pid"

rem 获取版本号
set "VERSION_FILE=%APP_HOME%\version.txt"
if exist "%VERSION_FILE%" (
    set /p APP_VERSION=<"%VERSION_FILE%"
) else (
    set "APP_VERSION=1.0"
)

rem ASCII 标题
echo.
echo   ____  _____  ____  ______  ______
echo  /    \/     \/    \/      \/      \
echo /_______/_____/_______/\      \      \
echo ^|  ***  ^|  ***  ^|  ***  ^|  ***  ^|  ***  ^|
echo ^|  ***  ^|  ***  ^|  ***  ^|  ***  ^|  ***  ^|
echo ^|_______|_______|_______|_______|_______|
echo.
echo    ____  __  __  ____  ______
echo   /    \/  \/  \/    \/      \
echo  /_______/\  /\_______/\      \
echo  ^|  ***  ^|  ^^^|^|  ^|  ***  ^|  ***  ^|
echo  ^|  ***  ^|  ^^^|^|  ^|  ***  ^|  ***  ^|
echo  ^|_______|__^^^|^^^|__|_______|_______|
echo.
echo === 秒查(MiaoCha)日志搜索系统 v%APP_VERSION% ===
echo.

rem 解析命令行参数
set FORCE=false
:parse_args
if "%~1"=="" goto :end_parse_args
if "%~1"=="-f" (
    set "FORCE=true"
    shift
    goto :parse_args
) else if "%~1"=="--force" (
    set "FORCE=true"
    shift
    goto :parse_args
) else if "%~1"=="-h" (
    echo 用法: %~nx0 [选项]
    echo 选项:
    echo   -f, --force    强制停止 (使用 taskkill /F)
    echo   -h, --help     显示此帮助消息
    exit /b 0
) else if "%~1"=="--help" (
    echo 用法: %~nx0 [选项]
    echo 选项:
    echo   -f, --force    强制停止 (使用 taskkill /F)
    echo   -h, --help     显示此帮助消息
    exit /b 0
) else (
    echo 错误: 未知选项: %~1
    exit /b 1
)
:end_parse_args

rem 首先尝试从PID文件获取PID
set PID=
if exist "%PID_FILE%" (
    set /p PID=<"%PID_FILE%"

    if "!PID!"=="" (
        echo 警告: PID文件为空
        set PID=
    ) else (
        tasklist /fi "PID eq !PID!" | find "!PID!" >nul
        if %ERRORLEVEL% NEQ 0 (
            echo 警告: PID文件中的进程 (!PID!) 不存在
            set PID=
        )
    )
)

rem 如果PID文件无效，尝试通过进程名查找
if "!PID!"=="" (
    for /f "tokens=1" %%p in ('wmic process where "commandline like '%%miaocha-server%%'" get processid ^| findstr /r "[0-9]"') do (
        set PID=%%p
    )

    if "!PID!"=="" (
        echo 警告: 应用未运行
        del "%PID_FILE%" 2>nul
        exit /b 0
    )
)

rem 停止应用
echo 信息: 正在停止应用 (PID: !PID!)...

if "%FORCE%"=="true" (
    rem 强制停止
    echo 警告: 强制终止进程...
    taskkill /F /PID !PID!
    set SIGNAL=SIGKILL
) else (
    rem 优雅停止
    taskkill /PID !PID!
    set SIGNAL=SIGTERM
)

rem 显示等待动画
echo 等待进程终止...
for /l %%i in (1,1,10) do (
    tasklist /fi "PID eq !PID!" | find "!PID!" >nul
    if %ERRORLEVEL% NEQ 0 (
        goto :stopped
    )
    timeout /t 1 /nobreak >nul
    echo .
)

rem 检查进程是否已终止
tasklist /fi "PID eq !PID!" | find "!PID!" >nul
if %ERRORLEVEL% EQU 0 (
    echo 错误: 无法停止应用，请尝试使用 --force 选项
    exit /b 1
) else (
    :stopped
    echo 成功: 应用已停止 (信号: !SIGNAL!)
    rem 删除PID文件
    del "%PID_FILE%" 2>nul
)

exit /b 0
