#!/bin/bash

echo "Starting BlackWidow Chess..."
echo

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in PATH"
    echo "Please install Java 8 or higher and try again"
    echo
    exit 1
fi

# Display Java version
echo "Java version:"
java -version
echo

# Check if JAR file exists
if [ ! -f "dist/BlackWidow.jar" ]; then
    echo "ERROR: BlackWidow.jar not found in dist directory"
    echo "Please build the project first using: ant jar"
    echo "Or compile manually and create the JAR file"
    echo
    exit 1
fi

# Run the application
echo "Running BlackWidow Chess..."
java -jar dist/BlackWidow.jar

# Check exit status
if [ $? -ne 0 ]; then
    echo
    echo "Application exited with error code $?"
    read -p "Press Enter to continue..."
fi
