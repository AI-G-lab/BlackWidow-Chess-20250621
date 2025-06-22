# Contributing to BlackWidow Chess

Thank you for your interest in contributing to BlackWidow Chess! This document provides guidelines and information for contributors.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Making Changes](#making-changes)
- [Coding Standards](#coding-standards)
- [Testing](#testing)
- [Submitting Changes](#submitting-changes)
- [Issue Reporting](#issue-reporting)

## Code of Conduct

This project adheres to a code of conduct that we expect all contributors to follow:

- Be respectful and inclusive
- Focus on constructive feedback
- Help maintain a welcoming environment for all contributors
- Report any unacceptable behavior to the project maintainers

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/yourusername/BlackWidow-Chess.git
   cd BlackWidow-Chess
   ```
3. **Add the upstream repository**:
   ```bash
   git remote add upstream https://github.com/originalowner/BlackWidow-Chess.git
   ```

## Development Setup

### Prerequisites

- Java 8 or higher
- Apache Ant
- Git
- A Java IDE (IntelliJ IDEA, Eclipse, or VS Code recommended)

### Building the Project

1. Compile the source code:
   ```bash
   ant compile
   ```

2. Run tests:
   ```bash
   ant test
   ```

3. Create executable JAR:
   ```bash
   ant jar
   ```

4. Run the application:
   ```bash
   java -jar dist/BlackWidow.jar
   ```

## Making Changes

### Branch Strategy

1. **Create a feature branch** from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Keep your branch up to date**:
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

### Types of Contributions

We welcome various types of contributions:

- **Bug fixes**: Fix existing issues
- **New features**: Add new functionality
- **Performance improvements**: Optimize existing code
- **Documentation**: Improve README, comments, or add examples
- **Tests**: Add or improve test coverage
- **UI/UX improvements**: Enhance the user interface

## Coding Standards

### Java Code Style

- **Naming Conventions**:
  - Classes: `PascalCase` (e.g., `ChessBoard`, `MoveGenerator`)
  - Methods and variables: `camelCase` (e.g., `calculateLegalMoves`, `currentPlayer`)
  - Constants: `UPPER_SNAKE_CASE` (e.g., `MAX_SEARCH_DEPTH`, `BOARD_SIZE`)
  - Packages: `lowercase` (e.g., `com.chess.engine`)

- **Code Organization**:
  - Keep methods focused and under 50 lines when possible
  - Use meaningful variable and method names
  - Add JavaDoc comments for public methods and classes
  - Group related functionality in appropriate packages

- **Formatting**:
  - Use 4 spaces for indentation (no tabs)
  - Maximum line length: 120 characters
  - Place opening braces on the same line
  - Use blank lines to separate logical sections

### Example Code Style

```java
/**
 * Calculates all legal moves for the current player.
 * 
 * @param board the current board state
 * @return a collection of legal moves
 */
public Collection<Move> calculateLegalMoves(final Board board) {
    final List<Move> legalMoves = new ArrayList<>();
    
    for (final Piece piece : board.getCurrentPlayer().getActivePieces()) {
        legalMoves.addAll(piece.calculateLegalMoves(board));
    }
    
    return Collections.unmodifiableList(legalMoves);
}
```

### Architecture Guidelines

- **Immutability**: Prefer immutable objects, especially for core game state
- **Single Responsibility**: Each class should have one clear purpose
- **Dependency Injection**: Avoid tight coupling between components
- **Error Handling**: Use appropriate exceptions and handle edge cases
- **Performance**: Consider performance implications, especially in AI code

## Testing

### Writing Tests

- Write unit tests for new functionality
- Ensure tests are deterministic and repeatable
- Use descriptive test method names
- Test both positive and negative cases
- Aim for high code coverage on critical components

### Test Structure

```java
@Test
public void testKnightMovesFromCenter() {
    // Given
    final Board board = Board.createStandardBoard();
    final Knight knight = new Knight(Alliance.WHITE, 28);
    
    // When
    final Collection<Move> moves = knight.calculateLegalMoves(board);
    
    // Then
    assertEquals(8, moves.size());
    assertTrue(moves.contains(/* expected move */));
}
```

### Running Tests

```bash
# Run all tests
ant test

# Run specific test class
java -cp build:lib/* org.junit.runner.JUnitCore com.chess.tests.TestBoard
```

## Submitting Changes

### Pull Request Process

1. **Ensure your code follows the coding standards**
2. **Add or update tests** for your changes
3. **Update documentation** if necessary
4. **Commit your changes** with clear, descriptive messages:
   ```bash
   git commit -m "Add knight move validation for edge cases
   
   - Fix bug where knights could move off the board
   - Add comprehensive tests for boundary conditions
   - Update documentation for move validation"
   ```

5. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```

6. **Create a Pull Request** on GitHub with:
   - Clear title describing the change
   - Detailed description of what was changed and why
   - Reference to any related issues
   - Screenshots for UI changes

### Pull Request Template

```markdown
## Description
Brief description of changes made.

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Performance improvement
- [ ] Documentation update
- [ ] Other (please describe)

## Testing
- [ ] Tests pass locally
- [ ] New tests added for new functionality
- [ ] Manual testing completed

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No breaking changes (or clearly documented)
```

## Issue Reporting

### Before Creating an Issue

1. **Search existing issues** to avoid duplicates
2. **Check the latest version** to see if the issue is already fixed
3. **Gather information** about your environment

### Creating a Good Issue

Include the following information:

- **Clear title** summarizing the issue
- **Steps to reproduce** the problem
- **Expected behavior** vs actual behavior
- **Environment details**:
  - Java version
  - Operating system
  - Application version
- **Screenshots or logs** if applicable

### Issue Templates

**Bug Report:**
```markdown
**Describe the bug**
A clear description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior:
1. Go to '...'
2. Click on '....'
3. See error

**Expected behavior**
What you expected to happen.

**Environment:**
- OS: [e.g. Windows 10, macOS 12.0]
- Java Version: [e.g. Java 11]
- Application Version: [e.g. 1.0.0]
```

**Feature Request:**
```markdown
**Is your feature request related to a problem?**
A clear description of what the problem is.

**Describe the solution you'd like**
A clear description of what you want to happen.

**Additional context**
Any other context or screenshots about the feature request.
```

## Development Guidelines

### Chess Engine Development

- **Move Generation**: Ensure all moves are legal and complete
- **Board Representation**: Maintain consistency in board state
- **AI Algorithm**: Optimize for both strength and performance
- **Game Rules**: Implement all standard chess rules correctly

### GUI Development

- **Responsiveness**: Ensure UI remains responsive during AI thinking
- **Accessibility**: Consider users with different abilities
- **Cross-platform**: Test on different operating systems
- **User Experience**: Keep the interface intuitive and clean

### Performance Considerations

- **AI Search**: Profile and optimize search algorithms
- **Memory Usage**: Avoid memory leaks in long-running games
- **Startup Time**: Keep application startup fast
- **Move Generation**: Optimize critical path operations

## Getting Help

If you need help with development:

1. **Check the documentation** in the README and code comments
2. **Look at existing code** for patterns and examples
3. **Ask questions** in GitHub issues or discussions
4. **Join the community** through project communication channels

## Recognition

Contributors will be recognized in:
- The project's contributor list
- Release notes for significant contributions
- Special recognition for major features or fixes

Thank you for contributing to BlackWidow Chess!
