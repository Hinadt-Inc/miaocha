@echo off
setlocal enabledelayedexpansion

rem 设置应用根目录
set "APP_HOME=%~dp0.."
set "LIB_DIR=%APP_HOME%\lib"
set "CONFIG_DIR=%APP_HOME%\config"
set "JAR_FILE=%APP_HOME%\miaocha-server.jar"
set "LOG_DIR=%APP_HOME%\logs"
set "PID_FILE=%APP_HOME%\application.pid"

rem 默认环境
set "ACTIVE_PROFILE=dev"

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
:parse_args
if "%~1"=="" goto :end_parse_args
if "%~1"=="-e" (
    set "ACTIVE_PROFILE=%~2"
    shift
    shift
    goto :parse_args
) else if "%~1"=="--env" (
    set "ACTIVE_PROFILE=%~2"
    shift
    shift
    goto :parse_args
) else if "%~1"=="--profile" (
    set "ACTIVE_PROFILE=%~2"
    shift
    shift
    goto :parse_args
) else if "%~1"=="-d" (
    set "DEBUG=true"
    shift
    goto :parse_args
) else if "%~1"=="--debug" (
    set "DEBUG=true"
    shift
    goto :parse_args
) else if "%~1"=="-h" (
    echo 用法: %~nx0 [选项]
    echo 选项:
    echo   -e, --env, --profile PROFILE   设置活动环境 (默认: dev)
    echo   -d, --debug                    启用调试模式
    echo   -h, --help                     显示此帮助消息
    exit /b 0
) else if "%~1"=="--help" (
    echo 用法: %~nx0 [选项]
    echo 选项:
    echo   -e, --env, --profile PROFILE   设置活动环境 (默认: dev)
    echo   -d, --debug                    启用调试模式
    echo   -h, --help                     显示此帮助消息
    exit /b 0
) else (
    echo 错误: 未知选项: %~1
    exit /b 1
)
:end_parse_args

rem 检查Java
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo 错误: 未找到Java，请安装JDK 17或更高版本
    exit /b 1
)

rem 检查JDK版本
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
)
set JAVA_VERSION=%JAVA_VERSION:"=%
for /f "delims=. tokens=1-3" %%v in ("%JAVA_VERSION%") do (
    if "%%v"=="1" (
        set JAVA_MAJOR_VERSION=%%w
    ) else (
        set JAVA_MAJOR_VERSION=%%v
    )
)

if %JAVA_MAJOR_VERSION% LSS 17 (
    echo 警告: 检测到Java版本 %JAVA_VERSION%, 建议使用JDK 17或更高版本
) else (
    echo 信息: 检测到Java版本: %JAVA_VERSION%
)

rem 检查JAR文件是否存在
if not exist "%JAR_FILE%" (
    echo 错误: 未找到主JAR文件 %JAR_FILE%
    exit /b 1
)

rem 确保日志目录存在
if not exist "%LOG_DIR%" (
    mkdir "%LOG_DIR%"
    echo 信息: 创建目录: %LOG_DIR%
)

rem 检查应用是否已经运行
set pid=
for /f "tokens=1" %%p in ('wmic process where "commandline like '%%miaocha-server%%'" get processid ^| findstr /r "[0-9]"') do (
    set pid=%%p
)

if defined pid (
    echo 警告: 应用已经在运行中，进程ID: !pid!
    echo 如需重启请先停止
    exit /b 0
)

rem 构建JVM参数
set "JAVA_OPTS=-Xms1g -Xmx2g"

rem 调试模式
if "%DEBUG%"=="true" (
    set "JAVA_OPTS=%JAVA_OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
    echo 信息: 已启用调试模式，端口: 5005
)

rem 附加系统属性
set "JAVA_OPTS=%JAVA_OPTS% -Dspring.profiles.active=%ACTIVE_PROFILE%"
set "JAVA_OPTS=%JAVA_OPTS% -Dspring.config.location=file:%CONFIG_DIR%/"
set "JAVA_OPTS=%JAVA_OPTS% -Dlogging.config=%CONFIG_DIR%\logback-spring.xml"
set "JAVA_OPTS=%JAVA_OPTS% -Dfile.encoding=UTF-8"

echo 信息: 启动环境: %ACTIVE_PROFILE%
echo 信息: 配置目录: %CONFIG_DIR%
echo 信息: JVM参数: %JAVA_OPTS%

rem 启动应用
echo 信息: 正在启动应用...

rem 创建启动批处理
set "STARTUP_BAT=%TEMP%\start_%RANDOM%.bat"
echo @echo off > "%STARTUP_BAT%"
echo cd /d "%APP_HOME%" >> "%STARTUP_BAT%"
rem 构建完整的类路径：config目录 + 主JAR + lib目录的所有JAR
set "CLASSPATH=%CONFIG_DIR%;%JAR_FILE%;%LIB_DIR%\*"
rem 使用主类启动，因为这不是fat jar
echo start "秒查系统" /b java %JAVA_OPTS% -cp "%CLASSPATH%" com.hinadt.miaocha.MiaoChaApp ^> "%LOG_DIR%\startup.log" 2^>^&1 >> "%STARTUP_BAT%"
echo for /f "tokens=2" %%%%p in ('tasklist /fi "IMAGENAME eq java.exe" /fi "WINDOWTITLE eq 秒查系统" /fo list ^| find "PID:"') do echo %%%%p ^> "%PID_FILE%" >> "%STARTUP_BAT%"

rem 执行启动批处理
call "%STARTUP_BAT%"
del "%STARTUP_BAT%"

rem 应用启动检查参数
set MAX_WAIT_SECONDS=30
echo 信息: 等待应用启动 (最多 %MAX_WAIT_SECONDS% 秒)...

rem 设置开始时间
set start_time=%time%
rem 去除时间前导空格
set start_time=%start_time: =%
rem 转换为秒数
for /f "tokens=1-4 delims=:." %%a in ("%start_time%") do (
    set /a start_seconds=((1%%a%%100*60+1%%b%%100)*60+1%%c%%100)
)

rem 设置结束时间
set /a end_seconds=start_seconds+MAX_WAIT_SECONDS

rem 动画字符
set "animation=- \ | /"
set animation_pos=0

:check_loop
rem 获取当前时间
set current_time=%time%
set current_time=%current_time: =%
for /f "tokens=1-4 delims=:." %%a in ("%current_time%") do (
    set /a current_seconds=((1%%a%%100*60+1%%b%%100)*60+1%%c%%100)
)

rem 检查是否超时
if %current_seconds% LSS %start_seconds% (
    rem 如果跨天，加上一天的秒数
    set /a current_seconds+=24*60*60
)
if %current_seconds% GTR %end_seconds% goto :timeout

rem 显示动画
for /f "tokens=%animation_pos% delims= " %%a in ("%animation%") do (
    echo | set /p="[ %%a ] 检查启动状态... "
)
set /a animation_pos+=1
if %animation_pos% GTR 4 set animation_pos=1

rem 检查PID文件
if not exist "%PID_FILE%" (
    rem PID文件不存在，等待一秒后重试
    ping -n 2 127.0.0.1 > nul
    echo.
    goto :check_loop
)

rem 读取PID
set /p PID=<"%PID_FILE%" 2>nul
if "%PID%"=="" (
    rem PID文件为空，等待一秒后重试
    ping -n 2 127.0.0.1 > nul
    echo.
    goto :check_loop
)

rem 检查进程
tasklist /fi "PID eq %PID%" | find "%PID%" > nul
if %ERRORLEVEL% NEQ 0 (
    echo [ X ] 进程已终止!
    echo 错误: 启动失败，进程已退出。请检查日志: %LOG_DIR%\startup.log
    type "%LOG_DIR%\startup.log"
    exit /b 1
)

rem 检查日志中的成功标志
findstr /C:"Started LogManageSystemApplication" "%LOG_DIR%\startup.log" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [ √ ] 应用启动成功!
    goto :startup_success
)

rem 等待一秒后重试
ping -n 2 127.0.0.1 > nul
echo.
goto :check_loop

:timeout
echo [ ! ] 等待超时，但进程仍在运行
echo 警告: 应用似乎仍在启动中，但已超过等待时间 (%MAX_WAIT_SECONDS%秒)
echo 警告: 请检查日志确认启动状态: %LOG_DIR%\startup.log
echo 警告: 最近的日志内容:
rem 显示最近的日志内容
powershell -Command "Get-Content '%LOG_DIR%\startup.log' -Tail 10"

rem 检查进程是否仍在运行
if exist "%PID_FILE%" (
    set /p PID=<"%PID_FILE%" 2>nul
    if not "%PID%"=="" (
        tasklist /fi "PID eq %PID%" | find "%PID%" > nul
        if %ERRORLEVEL% EQU 0 goto :startup_success
    )
)
goto :startup_failed

:startup_success
echo 成功: 应用启动成功! PID: %PID%
rem 获取启动时间
for /f "tokens=* usebackq" %%a in (`findstr /C:"Started LogManageSystemApplication in" "%LOG_DIR%\startup.log"`) do (
    set startup_message=%%a
)
if defined startup_message (
    for /f "tokens=6 delims= " %%t in ("%startup_message%") do (
        echo 信息: 启动用时: %%t 秒
    )
)
echo 查看完整日志: type "%LOG_DIR%\startup.log"

rem 尝试获取应用的端口号
set PORT=
findstr /C:"server.port" "%CONFIG_DIR%\application-%ACTIVE_PROFILE%.yml" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    for /f "tokens=2 delims=: " %%p in ('findstr /C:"server.port" "%CONFIG_DIR%\application-%ACTIVE_PROFILE%.yml"') do set PORT=%%p
) else (
    findstr /C:"server.port" "%CONFIG_DIR%\application.yml" >nul 2>nul
    if %ERRORLEVEL% EQU 0 (
        for /f "tokens=2 delims=: " %%p in ('findstr /C:"server.port" "%CONFIG_DIR%\application.yml"') do set PORT=%%p
    )
)

if defined PORT (
    echo 信息: 应用访问地址: http://localhost:!PORT!/
    echo 信息: API文档: http://localhost:!PORT!/swagger-ui
)

exit /b 0

:startup_failed
echo 错误: 应用启动失败，请检查日志: %LOG_DIR%\startup.log
exit /b 1
