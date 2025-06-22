@echo off
echo Starting BlackWidow Chess...
echo.

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 8 or higher and try again
    echo.
    pause
    exit /b 1
)

REM Check if JAR file exists
if not exist "dist\BlackWidow.jar" (
    echo ERROR: BlackWidow.jar not found in dist directory
    echo Please build the project first using: ant jar
    echo Or compile manually and create the JAR file
    echo.
    pause
    exit /b 1
)

REM Run the application
echo Running BlackWidow Chess...
java -jar dist\BlackWidow.jar

REM Keep window open if there's an error
if %errorlevel% neq 0 (
    echo.
    echo Application exited with error code %errorlevel%
    pause
)
