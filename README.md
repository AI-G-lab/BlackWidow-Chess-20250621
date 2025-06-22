# BlackWidow Chess

A comprehensive Java-based chess engine and GUI application featuring AI opponents, multiple piece sets, and advanced chess analysis tools.

## Features

- **Complete Chess Engine**: Full implementation of chess rules including castling, en passant, and pawn promotion
- **AI Opponents**: Multiple difficulty levels with configurable search depth
- **Multiple Piece Sets**: Choose from various artistic chess piece designs (Holy Warriors, Fancy, Simple, etc.)
- **Game Analysis**: Built-in board evaluation and move analysis tools
- **PGN Support**: Load and save games in standard PGN format
- **FEN Support**: Import/export board positions using FEN notation
- **Network Play**: Client-server architecture for online games
- **Move History**: Complete game history with undo functionality
- **Customizable Interface**: Adjustable colors and board orientation

## Screenshots

The game features multiple chess piece themes and a clean, intuitive interface with move highlighting and game analysis panels.

## Requirements

- Java 8 or higher
- **Apache Ant is NOT required** - the project includes standalone build scripts

## Installation

### Option 1: Download Pre-built JAR
1. Download the latest release from the [Releases](../../releases) page
2. Run with: `java -jar BlackWidow.jar`

### Option 2: Build from Source
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/BlackWidow-Chess.git
   cd BlackWidow-Chess
   ```

2. Build using the provided scripts (no Ant required):
   
   **Windows:**
   ```bash
   build.bat
   ```
   
   **Linux/macOS:**
   ```bash
   ./build.sh
   ```
   
   **Or using Apache Ant (optional):**
   ```bash
   ant compile
   ant jar
   ```

3. Run the application:
   ```bash
   java -jar dist/BlackWidow.jar
   ```
   
   **Or use the run scripts:**
   - Windows: `run.bat`
   - Linux/macOS: `./run.sh`

## Usage

### Starting a Game
1. Launch the application
2. Use the "Options" menu to set up a new game
3. Configure AI difficulty and player types
4. Start playing by clicking and dragging pieces

### Game Controls
- **Left Click**: Select and move pieces
- **Right Click**: Deselect current piece
- **File Menu**: Load/save games, import FEN positions
- **Options Menu**: New game, board evaluation, undo moves
- **Preferences Menu**: Customize appearance and enable features

### AI Configuration
- Access "Setup Game" from the Options menu
- Choose between Human vs Human, Human vs AI, or AI vs AI
- Adjust AI search depth (higher = stronger but slower)
- Enable opening book for stronger early game play

### File Formats
- **PGN Files**: Standard chess game notation for sharing games
- **FEN Strings**: Board position notation for specific positions

## Architecture

The project follows a modular architecture:

```
src/com/chess/
├── engine/           # Core chess engine
│   ├── classic/      # Standard chess implementation
│   └── bitboards/    # Alternative bitboard implementation
├── gui/              # Swing-based user interface
├── pgn/              # PGN file handling and game persistence
└── network/          # Network play functionality
```

### Key Components

- **Board**: Immutable board representation with move generation
- **Pieces**: Individual piece classes with legal move calculation
- **Players**: Human and AI player implementations
- **AI Engine**: Alpha-beta pruning with configurable evaluation
- **GUI**: Swing-based interface with drag-and-drop functionality

## AI Features

- **Alpha-Beta Pruning**: Efficient minimax search algorithm
- **Board Evaluation**: Comprehensive position evaluation including:
  - Material balance
  - Piece positioning
  - King safety analysis
  - Pawn structure evaluation
  - Rook structure analysis
- **Opening Book**: Database of strong opening moves
- **Configurable Depth**: Adjustable search depth for different skill levels

## Development

### Project Structure
```
BlackWidow-Chess/
├── src/              # Source code
├── tests/            # Unit tests
├── art/              # Chess piece graphics
├── lib/              # External libraries
├── build/            # Compiled classes
├── dist/             # Distribution files
└── build.xml         # Ant build configuration
```

### Building
The project provides multiple build options:

**Standalone Scripts (Recommended - No Ant Required):**
- Windows: `build.bat`
- Linux/macOS: `./build.sh`

**Apache Ant (Optional):**
- `ant compile`: Compile source code
- `ant jar`: Create executable JAR file
- `ant clean`: Clean build artifacts

The standalone build scripts use standard Java tools (javac, jar) and do not require Apache Ant installation.

### Testing
Run the test suite:
```bash
java -cp build:lib/* com.chess.tests.ChessTestSuite
```

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature-name`
3. Make your changes and add tests
4. Commit your changes: `git commit -am 'Add feature'`
5. Push to the branch: `git push origin feature-name`
6. Submit a pull request

### Code Style
- Follow Java naming conventions
- Add JavaDoc comments for public methods
- Include unit tests for new features
- Maintain immutable board representation

## License

This project is licensed under the GNU Lesser General Public License v2.1 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Chess piece artwork from various open-source collections
- Alpha-beta pruning algorithm implementation
- PGN parsing and FEN utilities
- Swing GUI framework

## Project Videos

For development tutorials and gameplay demonstrations, see the [project video playlist](https://www.youtube.com/playlist?list=PLOJzCFLZdG4zk5d-1_ah2B4kqZSeIlWtt).

## Support

If you encounter any issues or have questions:
1. Check the [Issues](../../issues) page for existing problems
2. Create a new issue with detailed information about the problem
3. Include your Java version and operating system details

## Roadmap

- [ ] Enhanced AI evaluation functions
- [ ] Tournament mode with multiple games
- [ ] Chess960 (Fischer Random) support
- [ ] Online multiplayer improvements
- [ ] Mobile-friendly interface
- [ ] Chess puzzle solver
- [ ] Game database integration

---

**Note**: This is a legacy project originally developed over 10 years ago and has been updated to meet modern GitHub standards while preserving its original functionality.
