@echo off
setlocal enabledelayedexpansion

rem 设置应用根目录
set "ROOT_DIR=%~dp0..\..\"
set "POM_FILE=%ROOT_DIR%pom.xml"
set "VERSION_FILE=%ROOT_DIR%version.txt"

rem 从POM文件中提取版本号
if exist "%POM_FILE%" (
    for /f "tokens=1,2 delims=<>" %%a in ('findstr "<version>" "%POM_FILE%"') do (
        if "%%a"=="    <version" (
            set "VERSION=%%b"
            goto :writeVersion
        )
    )
)

rem 如果无法从POM文件中获取，则使用默认版本
set "VERSION=1.0"
echo 无法从POM文件中提取版本号，使用默认版本 1.0

:writeVersion
echo !VERSION! > "%VERSION_FILE%"
echo 版本文件已创建: %VERSION_FILE% (版本: !VERSION!)
exit /b 0 