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
  echo   HiveMQ Start Script for Windows v1.6
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
    echo    please run again as admin. (right click, select 'Run as administrator')
    echo.
    echo -------------------------------------------------------------------------
    echo.
  )

  java -version >nul 2>&1
  if errorlevel 1 goto NOJAVA

  for /f tokens^=2-2^ delims^=^" %%j in ('java -version 2^>^&1') do @set "java_version=%%j"

  for /F "tokens=1 delims=. " %%a in ("%java_version%") do (
    set java_version_start=%%a
  )

  rem ########### VARIABLES
  set "JAVA_OPTS=-Djava.net.preferIPv4Stack=true %JAVA_OPTS%"

  if %java_version_start% LEQ 10 (
    echo ERROR! HiveMQ requires at least Java 11
    GOTO EXIT
  )

  set "JAVA_OPTS=%JAVA_OPTS% --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/sun.security.provider=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED --add-exports java.base/jdk.internal.misc=ALL-UNNAMED"


  rem JMX Monitoring
  set "JAVA_OPTS=-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false %JAVA_OPTS%"

  rem Uncomment for enabling diagnostic mode
  rem set "JAVA_OPTS=-DdiagnosticMode=true %JAVA_OPTS%"


  IF "%HIVEMQ_HOME%" == "" GOTO NOPATH
  set HIVEMQ_FOLDER="%HIVEMQ_HOME%";
  GOTO CHECKPATH

:NOPATH
  SET ROOT=%~dp0
  rem Resolve is fetching the parent directory
  CALL :RESOLVE "%ROOT%\.." PARENT_ROOT
  set HIVEMQ_FOLDER="%PARENT_ROOT%"
:CHECKPATH
  set HIVEMQ_FOLDER=%HIVEMQ_FOLDER:;=%
  rem Check if directory exists
  FOR %%i IN (%HIVEMQ_FOLDER%) DO IF EXIST %%~si\NUL  GOTO CHECKFILE
  echo ERROR! HiveMQ Home Folder not found or readable.
  GOTO EXIT

:CHECKFILE
  rem Check if file exists
  FOR %%i IN (%HIVEMQ_FOLDER%/bin/hivemq.jar) DO IF EXIST %%~si  GOTO START
  echo ERROR! HiveMQ JAR not found.
  GOTO EXIT

:START
      set "JAVA_OPTS=-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=%HIVEMQ_FOLDER%/heap-dump.hprof %JAVA_OPTS%"

      echo -------------------------------------------------------------------------
      echo.
      echo   HIVEMQ_HOME: %HIVEMQ_FOLDER%
      echo.
      echo   JAVA_OPTS: %JAVA_OPTS%
      echo.
      echo   JAVA_VERSION: %java_version%
      echo.
      echo -------------------------------------------------------------------------
      echo.
    
  set "JAVA_OPTS=-Dhivemq.home=%HIVEMQ_FOLDER% %JAVA_OPTS%"
  
  java %JAVA_OPTS% -jar %HIVEMQ_FOLDER%/bin/hivemq.jar
  GOTO EXIT

:isAdmin
fsutil dirty query %systemdrive% >nul
exit /b

:NOJAVA
  echo You do not have the Java Runtime Environment installed, please install Java JRE from https://adoptopenjdk.net/?variant=openjdk11 and try again.

:EXIT
  pause

:EXIT_WO_PAUSE

GOTO :EOF

:RESOLVE
SET %2=%~f1
GOTO :EOF