@echo off
echo Building BlackWidow Chess...
echo.

REM Check if Java is installed
javac -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java compiler (javac) is not installed or not in PATH
    echo Please install JDK 17 or higher and try again
    echo.
    pause
    exit /b 1
)

REM Create build directories
echo Creating build directories...
if not exist "build" mkdir build
if not exist "dist" mkdir dist

REM Clean previous build
echo Cleaning previous build...
if exist "build\*" del /q /s build\* >nul 2>&1
if exist "dist\BlackWidow.jar" del dist\BlackWidow.jar >nul 2>&1

REM Compile source files
echo Compiling source files...
javac -source 17 -target 17 -cp "build;lib/*" -d build -sourcepath src -encoding UTF-8 src/com/chess/BlackWidow.java
if %errorlevel% neq 0 (
    echo ERROR: Compilation failed
    pause
    exit /b 1
)

REM Compile network files
echo Compiling network files...
javac -source 17 -target 17 -cp "build;lib/*" -d build -sourcepath src -encoding UTF-8 src/network/*.java
if %errorlevel% neq 0 (
    echo ERROR: Network compilation failed
    pause
    exit /b 1
)

REM Create JAR file
echo Creating JAR file...
jar cfm dist\BlackWidow.jar src\META-INF\MANIFEST.MF -C build . -C art .
if %errorlevel% neq 0 (
    echo ERROR: JAR creation failed
    pause
    exit /b 1
)

echo.
echo Build completed successfully!
echo JAR file created: dist\BlackWidow.jar
echo.
echo To run the application, use: java -jar dist\BlackWidow.jar
echo Or simply run: run.bat
echo.
pause
