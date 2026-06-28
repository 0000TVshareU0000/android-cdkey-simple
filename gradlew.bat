@echo off
rem Copyright 2015 the original author.
rem
rem Licensed under the Apache License, Version 2.0 (the "License");
rem you may not use this file except in compliance with the License.
rem You may obtain a copy of the License at
rem
rem      https://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.

set DIR=%~dp0
set APP_BASE_NAME=%~n0
set APP_HOME=%DIR%

set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

set WRAPPER_JAR="%APP_HOME%\gradle\wrapper\gradle-wrapper.jar"
set WRAPPER_PROPERTIES="%APP_HOME%\gradle\wrapper\gradle-wrapper.properties"
set MAX_FD=maximum

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%"=="0" goto execute

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.
exit /b 1

:execute
set CLASSPATH=%WRAPPER_JAR%
set WRAPPER_PROPERTIES="-Dgradle-wrapper.properties=%WRAPPER_PROPERTIES%"

"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %WRAPPER_PROPERTIES% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set GRADLE_EXIT_CONSOLE=1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
