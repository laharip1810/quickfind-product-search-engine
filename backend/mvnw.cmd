@echo off
rem Minimal Maven wrapper for Windows: downloads the Maven version pinned in
rem .mvn\wrapper\maven-wrapper.properties on first use, then delegates to it.
setlocal
set "MVNW_DIR=%~dp0"
set "PROPS=%MVNW_DIR%.mvn\wrapper\maven-wrapper.properties"
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%PROPS%") do (
  if "%%A"=="distributionUrl" set "DIST_URL=%%B"
  if "%%A"=="distributionName" set "DIST_NAME=%%B"
)
if "%MAVEN_USER_HOME%"=="" set "MAVEN_USER_HOME=%USERPROFILE%\.m2"
set "WRAPPER_HOME=%MAVEN_USER_HOME%\wrapper\dists"
set "MAVEN_DIR=%WRAPPER_HOME%\%DIST_NAME%"

if not exist "%MAVEN_DIR%\bin\mvn.cmd" (
  echo Downloading %DIST_URL%
  if not exist "%WRAPPER_HOME%" mkdir "%WRAPPER_HOME%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%WRAPPER_HOME%\%DIST_NAME%.zip'; Expand-Archive -Force -Path '%WRAPPER_HOME%\%DIST_NAME%.zip' -DestinationPath '%WRAPPER_HOME%'; Remove-Item '%WRAPPER_HOME%\%DIST_NAME%.zip'"
  if errorlevel 1 (
    echo Failed to download Maven from %DIST_URL%
    exit /b 1
  )
)

call "%MAVEN_DIR%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%
