@echo off
:: Always run from the folder where this script lives
cd /d "%~dp0"

echo =============================
echo   MyDispatch Plugin Builder
echo =============================
echo.

:: Clean output folder
if exist out rmdir /s /q out
mkdir out

:: Check if we already have a valid API jar
echo Checking for DarkBot API...
for %%I in (lib\darkbot-common.jar) do set JARSIZE=%%~zI
if "%JARSIZE%"=="" set JARSIZE=0
if %JARSIZE% LSS 10000 (
    echo Downloading DarkBot API jar...
    powershell -Command "Invoke-WebRequest -Uri 'https://jitpack.io/eu/darkbot/DarkBotAPI/darkbot-common/0.9.8/darkbot-common-0.9.8.jar' -OutFile 'lib\darkbot-common.jar'"
    for %%I in (lib\darkbot-common.jar) do set JARSIZE=%%~zI
    if %JARSIZE% LSS 10000 (
        echo.
        echo ERROR: Could not download DarkBot API. Please check your internet connection.
        pause
        exit /b 1
    )
    echo Download successful!
)

echo API jar found.
echo.

:: Compile all Java files
echo Compiling...
javac --release 11 -cp lib\darkbot-common.jar -d out ^
  src\com\mydispatch\config\DispatchConfig.java ^
  src\com\mydispatch\tasks\DispatchTask.java

if errorlevel 1 (
    echo.
    echo ERROR: Compilation failed! See errors above.
    pause
    exit /b 1
)

echo Compilation successful!
echo.

:: Copy plugin.json into out folder
copy src\plugin.json out\plugin.json >nul

:: Package into jar
echo Packaging...
cd out
jar cf ..\MyDispatch.jar .
cd ..

echo.
echo =============================
echo  SUCCESS! MyDispatch.jar is ready.
echo  Copy it to your DarkBot plugins folder.
echo =============================
echo.
pause
