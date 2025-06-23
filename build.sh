#!/bin/bash

echo "Building BlackWidow Chess..."
echo

# Check if Java compiler is installed
if ! command -v javac &> /dev/null; then
    echo "ERROR: Java compiler (javac) is not installed or not in PATH"
    echo "Please install JDK 17 or higher and try again"
    echo
    exit 1
fi

# Display Java compiler version
echo "Java compiler version:"
javac -version
echo

# Create build directories
echo "Creating build directories..."
mkdir -p build
mkdir -p dist

# Clean previous build
echo "Cleaning previous build..."
rm -rf build/*
rm -f dist/BlackWidow.jar

# Compile source files
echo "Compiling source files..."
javac -source 17 -target 17 -cp "build:lib/*" -d build -sourcepath src -encoding UTF-8 src/com/chess/BlackWidow.java
if [ $? -ne 0 ]; then
    echo "ERROR: Compilation failed"
    exit 1
fi

# Compile network files
echo "Compiling network files..."
javac -source 17 -target 17 -cp "build:lib/*" -d build -sourcepath src -encoding UTF-8 src/network/*.java
if [ $? -ne 0 ]; then
    echo "ERROR: Network compilation failed"
    exit 1
fi

# Create JAR file
echo "Creating JAR file..."
jar cfm dist/BlackWidow.jar src/META-INF/MANIFEST.MF -C build . -C art .
if [ $? -ne 0 ]; then
    echo "ERROR: JAR creation failed"
    exit 1
fi

echo
echo "Build completed successfully!"
echo "JAR file created: dist/BlackWidow.jar"
echo
echo "To run the application, use: java -jar dist/BlackWidow.jar"
echo "Or simply run: ./run.sh"
echo

# Make run script executable
chmod +x run.sh
echo "Made run.sh executable"
echo
