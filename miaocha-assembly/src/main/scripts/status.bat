@echo off
setlocal enabledelayedexpansion

rem 设置应用根目录
set "APP_HOME=%~dp0.."
set "CONFIG_DIR=%APP_HOME%\config"
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
        exit /b 1
    )
)

rem 显示应用状态
echo 成功: 应用正在运行 (PID: !PID!)

rem 尝试获取应用当前的环境信息
set ACTIVE_PROFILE=
for /f "tokens=*" %%a in ('wmic process where "processid=!PID!" get commandline ^| findstr "spring.profiles.active"') do (
    for /f "tokens=1 delims= " %%b in ("%%a") do (
        set CMD_LINE=%%b
        if "!CMD_LINE:~0,22!"=="-Dspring.profiles.active" (
            set ACTIVE_PROFILE=!CMD_LINE:~23!
        )
    )
)

if "!ACTIVE_PROFILE!"=="" (
    set ACTIVE_PROFILE=default
)

rem 尝试获取应用的内存使用情况
echo 信息: 内存使用情况:
wmic process where "processid='!PID!'" get workingsetsize /format:list | findstr "="
for /f "tokens=2 delims==" %%m in ('wmic process where "processid='!PID!'" get workingsetsize /format:list ^| findstr "="') do (
    set /a MEM_MB=%%m / 1048576
    echo 内存使用: !MEM_MB! MB
)

rem 尝试获取应用的端口号
set PORT=
findstr /C:"server.port" "%CONFIG_DIR%\application-!ACTIVE_PROFILE!.yml" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    for /f "tokens=2 delims=: " %%p in ('findstr /C:"server.port" "%CONFIG_DIR%\application-!ACTIVE_PROFILE!.yml"') do set PORT=%%p
) else (
    findstr /C:"server.port" "%CONFIG_DIR%\application.yml" >nul 2>nul
    if %ERRORLEVEL% EQU 0 (
        for /f "tokens=2 delims=: " %%p in ('findstr /C:"server.port" "%CONFIG_DIR%\application.yml"') do set PORT=%%p
    )
)

if defined PORT (
    echo 信息: 应用端口: !PORT!

    rem 检查端口是否已被监听
    netstat -ano | findstr /C:":!PORT! " >nul
    if %ERRORLEVEL% EQU 0 (
        echo 成功: 端口 !PORT! 已经开放并正在监听
    ) else (
        echo 警告: 端口 !PORT! 未监听，应用可能尚未完全启动
    )

    rem 尝试检查健康检查接口
    where curl >nul 2>nul
    if %ERRORLEVEL% EQU 0 (
        for /f %%c in ('curl -s -o nul -w "%%{http_code}" http://localhost:!PORT!/actuator/health 2^>nul') do set HTTP_CODE=%%c
        if "!HTTP_CODE!"=="200" (
            echo 成功: 健康检查: 正常
        ) else (
            echo 警告: 健康检查: 异常 (HTTP状态码: !HTTP_CODE!)
        )
    )
)

rem 显示启动时间
for /f "tokens=* skip=1" %%t in ('wmic process where "processid='!PID!'" get creationdate ^| findstr /r "[0-9]"') do (
    set CREATION_DATE=%%t
    set YEAR=!CREATION_DATE:~0,4!
    set MONTH=!CREATION_DATE:~4,2!
    set DAY=!CREATION_DATE:~6,2!
    set HOUR=!CREATION_DATE:~8,2!
    set MINUTE=!CREATION_DATE:~10,2!
    set SECOND=!CREATION_DATE:~12,2!
    echo 信息: 启动时间: !YEAR!-!MONTH!-!DAY! !HOUR!:!MINUTE!:!SECOND!
)

rem 计算运行时间
for /f "tokens=1" %%u in ('wmic process where "processid='!PID!'" get workingsetsize ^| findstr /r "[0-9]"') do (
    set /a UPTIME_SEC=%time:~0,2%*3600 + %time:~3,2%*60 + %time:~6,2% - (!HOUR!*3600 + !MINUTE!*60 + !SECOND!)
    if !UPTIME_SEC! LSS 0 set /a UPTIME_SEC=!UPTIME_SEC! + 86400

    set /a UPTIME_DAYS=!UPTIME_SEC! / 86400
    set /a UPTIME_HOURS=(!UPTIME_SEC! %% 86400) / 3600
    set /a UPTIME_MINUTES=(!UPTIME_SEC! %% 3600) / 60
    set /a UPTIME_SECONDS=!UPTIME_SEC! %% 60

    echo 信息: 运行时间: !UPTIME_DAYS!天!UPTIME_HOURS!时!UPTIME_MINUTES!分!UPTIME_SECONDS!秒
)

rem 显示环境信息
echo 信息: 活动环境: !ACTIVE_PROFILE!

exit /b 0
