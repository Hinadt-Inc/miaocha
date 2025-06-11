@echo off
setlocal enabledelayedexpansion

rem 设置应用根目录
set "APP_HOME=%~dp0.."
set "SCRIPTS_DIR=%APP_HOME%\bin"

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
set ARGS=
set FORCE=
:parse_args
if "%~1"=="" goto :end_parse_args
if "%~1"=="-e" (
    set "ARGS=!ARGS! -e %~2"
    shift
    shift
    goto :parse_args
) else if "%~1"=="--env" (
    set "ARGS=!ARGS! -e %~2"
    shift
    shift
    goto :parse_args
) else if "%~1"=="--profile" (
    set "ARGS=!ARGS! -e %~2"
    shift
    shift
    goto :parse_args
) else if "%~1"=="-d" (
    set "ARGS=!ARGS! -d"
    shift
    goto :parse_args
) else if "%~1"=="--debug" (
    set "ARGS=!ARGS! -d"
    shift
    goto :parse_args
) else if "%~1"=="-f" (
    set "FORCE=-f"
    shift
    goto :parse_args
) else if "%~1"=="--force" (
    set "FORCE=-f"
    shift
    goto :parse_args
) else if "%~1"=="-h" (
    echo 用法: %~nx0 [选项]
    echo 选项:
    echo   -e, --env, --profile PROFILE   设置活动环境
    echo   -d, --debug                    启用调试模式
    echo   -f, --force                    强制停止 (使用 taskkill /F)
    echo   -h, --help                     显示此帮助消息
    exit /b 0
) else if "%~1"=="--help" (
    echo 用法: %~nx0 [选项]
    echo 选项:
    echo   -e, --env, --profile PROFILE   设置活动环境
    echo   -d, --debug                    启用调试模式
    echo   -f, --force                    强制停止 (使用 taskkill /F)
    echo   -h, --help                     显示此帮助消息
    exit /b 0
) else (
    echo 错误: 未知选项: %~1
    exit /b 1
)
:end_parse_args

rem 停止应用
echo 信息: 正在重启应用...
echo 步骤 1/2: 停止应用

rem 执行停止脚本
call "%SCRIPTS_DIR%\stop.bat" %FORCE%

rem 启动应用
echo 步骤 2/2: 启动应用

rem 给进程一些时间完全停止
timeout /t 2 /nobreak > nul

rem 执行启动脚本
call "%SCRIPTS_DIR%\start.bat" %ARGS%

exit /b %ERRORLEVEL% 