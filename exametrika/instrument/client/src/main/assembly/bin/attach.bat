@echo off
rem -------------------------------------------------------------------------
rem Attach utility launching script
rem -------------------------------------------------------------------------

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT" setlocal

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
) else (
  set DIRNAME=.\
)

pushd %DIRNAME%..
if "x%EXA_HOME%" == "x" (
  set "EXA_HOME=%CD%"
)
popd

set DIRNAME=

if "x%JAVA_HOME%" == "x" (
  set  JAVA=java
)  else (
  set "JAVA=%JAVA_HOME%\bin\java"
)

"%JAVA%" -Dcom.exametrika.home="%EXA_HOME%" -jar "%EXA_HOME%\lib\instrument.client.jar" %*
