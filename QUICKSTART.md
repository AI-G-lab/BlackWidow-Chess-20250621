# Quick Start Guide

This guide will help you get BlackWidow Chess running quickly on your system.

## Prerequisites

- Java 8 or higher installed on your system
- For building from source: JDK (Java Development Kit)

## Option 1: Quick Run (Recommended)

If you just want to play chess immediately:

### Windows
1. Double-click `run.bat`
2. The application will start automatically

### Linux/macOS
1. Open terminal in the project directory
2. Run: `./run.sh`
3. The application will start automatically

## Option 2: Build from Source

If you want to build the project yourself:

### Windows
1. Double-click `build.bat`
2. Wait for compilation to complete
3. Double-click `run.bat` to start the game

### Linux/macOS
1. Open terminal in the project directory
2. Run: `./build.sh`
3. Run: `./run.sh` to start the game

### Using Apache Ant (Advanced)
If you have Apache Ant installed:
```bash
ant compile    # Compile the source code
ant jar        # Create the JAR file
ant run        # Run the application
ant all        # Clean, compile, test, and build
```

## First Time Setup

1. **Choose your piece set**: Go to Preferences → Choose Chess Men Image Set
2. **Set up a game**: Go to Options → Setup Game
3. **Configure AI difficulty**: Adjust the search depth (higher = stronger but slower)
4. **Customize colors**: Go to Preferences → Choose Colors

## Game Controls

- **Left click**: Select and move pieces
- **Right click**: Deselect current piece
- **Drag and drop**: Move pieces by dragging

## Menu Overview

### File Menu
- **Load PGN File**: Import chess games
- **Load FEN File**: Import specific board positions
- **Save Game**: Export your game to PGN format
- **Exit**: Close the application

### Options Menu
- **New Game**: Start a fresh game
- **Setup Game**: Configure players (Human vs AI, etc.)
- **Evaluate Board**: See the AI's assessment of the position
- **Undo last move**: Take back the previous move

### Preferences Menu
- **Choose Colors**: Customize board colors
- **Choose Chess Men Image Set**: Select piece artwork
- **Flip board**: Rotate the board view
- **Highlight Legal Moves**: Show possible moves
- **Use Book Moves**: Enable opening book

## Troubleshooting

### "Java is not installed" error
- Download and install Java from [java.com](https://java.com)
- Make sure Java is in your system PATH

### "JAR file not found" error
- Run the build script first (`build.bat` or `./build.sh`)
- Make sure the build completed successfully

### Application won't start
- Check that you have Java 8 or higher: `java -version`
- Try running from command line: `java -jar dist/BlackWidow.jar`
- Check the console output for error messages

### Performance issues
- Lower the AI search depth in game setup
- Close other applications to free up memory
- Make sure you're using a recent Java version

## Game Modes

1. **Human vs Human**: Two players on the same computer
2. **Human vs AI**: Play against the computer
3. **AI vs AI**: Watch two AI players compete

## Tips for New Players

- Start with AI search depth 3-4 for moderate difficulty
- Enable "Highlight Legal Moves" to see possible moves
- Use the "Evaluate Board" feature to understand positions
- Try loading PGN files to study master games
- Experiment with different piece sets to find your favorite

## Advanced Features

- **PGN Support**: Load and save games in standard notation
- **FEN Support**: Import specific board positions
- **Game Analysis**: Built-in position evaluation
- **Multiple AI Levels**: Adjustable difficulty from beginner to expert
- **Opening Book**: Database of strong opening moves

## Getting Help

- Check the main [README.md](README.md) for detailed information
- Review the [CONTRIBUTING.md](CONTRIBUTING.md) if you want to help improve the project
- Report bugs using the GitHub issue tracker
- Join the community discussions

Enjoy playing BlackWidow Chess!
