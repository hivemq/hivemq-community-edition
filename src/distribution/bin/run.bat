rem  Copyright 2019-present HiveMQ GmbH
rem
rem  Licensed under the Apache License, Version 2.0 (the "License");
rem  you may not use this file except in compliance with the License.
rem  You may obtain a copy of the License at
rem
rem      http://www.apache.org/licenses/LICENSE-2.0
rem
rem  Unless required by applicable law or agreed to in writing, software
rem  distributed under the License is distributed on an "AS IS" BASIS,
rem  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem  See the License for the specific language governing permissions and
rem  limitations under the License.

@echo off
setlocal
echo -------------------------------------------------------------------------
echo.
echo                   _    _  _              __  __   ____
echo                  ^| ^|  ^| ^|(_)            ^|  \/  ^| / __ \
echo                  ^| ^|__^| ^| _ __   __ ___ ^| \  / ^|^| ^|  ^| ^|
echo                  ^|  __  ^|^| ^|\ \ / // _ \^| ^|\/^| ^|^| ^|  ^| ^|
echo                  ^| ^|  ^| ^|^| ^| \ V /^|  __/^| ^|  ^| ^|^| ^|__^| ^|
echo                  ^|_^|  ^|_^|^|_^|  \_/  \___^|^|_^|  ^|_^| \___\_\
echo.
echo -------------------------------------------------------------------------
echo.
echo   HiveMQ Start Script for Windows v1.7
echo.

call :isAdmin
if %errorlevel% == 0 (
    echo.
    echo    Running with admin rights.
    echo.
) else (
    echo -------------------------------------------------------------------------
    echo.
    echo    WARNING
    echo.
    echo    HiveMQ needs more runtime privileges,
    echo    please run again as admin. (right click, select 'Run as administrator'^)
    echo.
    echo -------------------------------------------------------------------------
    echo.
)

java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR! You do not have the Java Runtime Environment installed, please install Java JRE from https://adoptium.net/?variant=openjdk11 and try again.
    goto EXIT
)

for /f tokens^=2-2^ delims^=^" %%j in ('java -version 2^>^&1') do set "JAVA_VERSION=%%j"

for /f "tokens=1 delims=. " %%a in ("%JAVA_VERSION%") do set "JAVA_MAJOR_VERSION=%%a"

if %JAVA_MAJOR_VERSION% LSS 11 (
    echo ERROR! HiveMQ requires at least Java version 11.
    goto EXIT
)

rem ########### VARIABLES
set "JAVA_OPTS=%JAVA_OPTS% -Djava.net.preferIPv4Stack=true"

set "JAVA_OPTS=%JAVA_OPTS% --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/sun.security.provider=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED --add-exports java.base/jdk.internal.misc=ALL-UNNAMED"

rem JMX Monitoring
if not defined HIVEMQ_JMX_ENABLED set "HIVEMQ_JMX_ENABLED=true"
if not defined HIVEMQ_JMX_PORT set "HIVEMQ_JMX_PORT=9010"
if "%HIVEMQ_JMX_ENABLED%" == "true" (
    set "JAVA_OPTS=%JAVA_OPTS% -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=%HIVEMQ_JMX_PORT% -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
)

rem Disable Localization
set "JAVA_OPTS=%JAVA_OPTS% -Duser.language=en -Duser.region=US"

rem Uncomment for enabling diagnostic mode
rem set "JAVA_OPTS=%JAVA_OPTS% -DdiagnosticMode=true"

if not defined HIVEMQ_HOME (
    rem %0 is the current script, ~ removes quotes, d includes the drive letter, p includes the path without the filename
    call :RESOLVE "%~dp0\.." HIVEMQ_HOME
)

if not exist "%HIVEMQ_HOME%\" (
    echo ERROR! HiveMQ home folder not found or readable.
    goto EXIT
)

if not exist "%HIVEMQ_HOME%\bin\hivemq.jar" (
    echo ERROR! HiveMQ JAR not found.
    goto EXIT
)

if not defined HIVEMQ_HEAPDUMP_FOLDER set "HIVEMQ_HEAPDUMP_FOLDER=%HIVEMQ_HOME%"
set "JAVA_OPTS=%JAVA_OPTS% -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath="%HIVEMQ_HEAPDUMP_FOLDER%\heap-dump.hprof""

echo -------------------------------------------------------------------------
echo.
echo   HIVEMQ_HOME: %HIVEMQ_HOME%
echo.
echo   JAVA_OPTS: %JAVA_OPTS%
echo.
echo   JAVA_VERSION: %JAVA_VERSION%
echo.
echo -------------------------------------------------------------------------
echo.

set "JAVA_OPTS=-Dhivemq.home="%HIVEMQ_HOME%" %JAVA_OPTS%"

java %JAVA_OPTS% -jar "%HIVEMQ_HOME%\bin\hivemq.jar"

:EXIT
pause
goto :EOF


rem function that exits with 0 if running with admin rights
:isAdmin
fsutil dirty query %systemdrive% >nul
exit /b

rem function that resolves the path in arg 1 and stores it in the variable arg 2
:RESOLVE
rem ~ removes quotes, f includes the fully qualified path
set %2=%~f1
goto :EOF
