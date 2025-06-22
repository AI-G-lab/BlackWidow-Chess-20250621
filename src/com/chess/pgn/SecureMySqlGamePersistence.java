package com.chess.pgn;

import com.chess.engine.classic.board.Board;
import com.chess.engine.classic.board.Move;
import com.chess.engine.classic.player.Player;
import com.chess.security.SecureConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

public class SecureMySqlGamePersistence {

    private final Connection dbConnection;
    private static final Logger LOGGER = Logger.getLogger(SecureMySqlGamePersistence.class.getName());
    private final SecureConfig config;

    public SecureMySqlGamePersistence() {
        this.config = SecureConfig.getInstance();
        this.dbConnection = createSecureConnection();
        initializeTables();
    }

    private Connection createSecureConnection() {
        final String url = config.getDatabaseUrl();
        final String user = config.getDatabaseUsername();
        final String pass = config.getDatabasePassword();
        
        try {
            // Load the appropriate driver
            Class.forName(config.getDatabaseDriver());
            
            Connection conn = DriverManager.getConnection(url, user, pass);
            
            // Set connection properties for security
            conn.setAutoCommit(true);
            conn.setReadOnly(false);
            
            // Set timeout
            Statement stmt = conn.createStatement();
            stmt.setQueryTimeout(30); // 30 seconds timeout
            stmt.close();
            
            LOGGER.info("Database connection established successfully");
            return conn;
            
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Database driver not found", e);
            throw new RuntimeException("Database driver not found", e);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to connect to database", e);
            throw new RuntimeException("Unable to connect to database", e);
        }
    }

    private void initializeTables() {
        createGameTable();
        createMovesTable();
        createIndexes();
    }

    private void createGameTable() {
        final String sql = "CREATE TABLE IF NOT EXISTS games (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "event TEXT," +
                "site TEXT," +
                "date TEXT," +
                "round TEXT," +
                "white TEXT," +
                "black TEXT," +
                "result TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        
        executeUpdate(sql, "Failed to create games table");
    }

    private void createMovesTable() {
        final String sql = "CREATE TABLE IF NOT EXISTS moves (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "game_id INTEGER NOT NULL, " +
                "move_number INTEGER NOT NULL, " +
                "player TEXT NOT NULL, " +
                "san TEXT NOT NULL, " +
                "fen_before TEXT NOT NULL, " +
                "fen_after TEXT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE" +
                ")";
        
        executeUpdate(sql, "Failed to create moves table");
    }

    private void createIndexes() {
        createIndex("moves", "idx_moves_game_id_move_number", "game_id", "move_number");
        createIndex("moves", "idx_moves_fen_before", "fen_before");
        createIndex("games", "idx_games_white_black", "white", "black");
        createIndex("games", "idx_games_date", "date");
    }

    private void createIndex(final String tableName,
                             final String indexName,
                             final String... columns) {
        if (columns == null || columns.length == 0) {
            throw new IllegalArgumentException("At least one column must be specified for indexing.");
        }
        
        final String columnList = String.join(",", columns);
        final String sql = "CREATE INDEX IF NOT EXISTS " + indexName + " ON " + tableName + "(" + columnList + ")";
        
        executeUpdate(sql, "Failed to create index " + indexName);
    }

    private void executeUpdate(final String sql, final String errorMessage) {
        try (final PreparedStatement ps = dbConnection.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    public void persistGames(final List<Game> games) {
        if (games == null || games.isEmpty()) {
            return;
        }
        
        try {
            dbConnection.setAutoCommit(false); // Begin transaction
            
            int successCount = 0;
            for (final Game game : games) {
                try {
                    if (game.isValid()) {
                        game.saveGame(this);
                        successCount++;
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to save game: " + game, e);
                    // Continue with other games
                }
            }
            
            dbConnection.commit();
            dbConnection.setAutoCommit(true);
            
            LOGGER.info("Successfully persisted " + successCount + " out of " + games.size() + " games");
            
        } catch (SQLException e) {
            try { 
                dbConnection.rollback(); 
                dbConnection.setAutoCommit(true);
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Failed to rollback transaction", ex);
            }
            throw new RuntimeException("Failed to persist games batch", e);
        }
    }

    public Move getNextBestMove(Board board, Player player) {
        final String fen = FenUtilities.createFENFromGame(board);
        final String sql = "SELECT san, COUNT(*) AS times_played " +
                "FROM moves " +
                "WHERE fen_before = ? " +
                "GROUP BY san " +
                "HAVING times_played > ? " +
                "ORDER BY times_played DESC " +
                "LIMIT 1";

        try (final PreparedStatement ps = dbConnection.prepareStatement(sql)) {
            ps.setString(1, fen);
            ps.setInt(2, 20); // Minimum occurrences
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final String bestSAN = rs.getString("san");
                    final int timesPlayed = rs.getInt("times_played");
                    
                    LOGGER.info("Found book move: " + bestSAN + " (played " + timesPlayed + " times)");
                    
                    // Find matching move from legal moves
                    for (final Move move : player.getLegalMoves()) {
                        String moveSan = move.toString();
                        if (moveSan.equals(bestSAN)) {
                            return move;
                        }
                    }
                }
            }
        } catch (final SQLException e) {
            LOGGER.log(Level.WARNING, "Error querying book moves", e);
        }
        
        return Move.MoveFactory.getNullMove();
    }

    // Save a ValidGame (tags and moves) with input validation
    public void saveValidGame(Game game) throws SQLException {
        if (game == null || !game.isValid()) {
            throw new IllegalArgumentException("Invalid game provided");
        }
        
        final String insertGameSql = "INSERT INTO games (event, site, date, round, white, black, result) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = dbConnection.prepareStatement(insertGameSql, Statement.RETURN_GENERATED_KEYS)) {
            PGNGameTags tags = game.getTags();
            
            // Validate and sanitize input data
            ps.setString(1, sanitizeString(tags.getTagOrDefault("Event", "")));
            ps.setString(2, sanitizeString(tags.getTagOrDefault("Site", "")));
            ps.setString(3, sanitizeString(tags.getTagOrDefault("Date", "")));
            ps.setString(4, sanitizeString(tags.getTagOrDefault("Round", "")));
            ps.setString(5, sanitizeString(tags.getTagOrDefault("White", "")));
            ps.setString(6, sanitizeString(tags.getTagOrDefault("Black", "")));
            ps.setString(7, sanitizeString(tags.getTagOrDefault("Result", "")));
            
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long gameId = rs.getLong(1);
                    saveMoves(gameId, game.getMoves());
                } else {
                    throw new SQLException("Could not retrieve generated game ID.");
                }
            }
        }
    }

    // Save moves for a game with validation
    private void saveMoves(long gameId, List<MoveRecord> moves) throws SQLException {
        if (moves == null || moves.isEmpty()) {
            return;
        }
        
        final String insertMoveSql = "INSERT INTO moves (game_id, move_number, player, san, fen_before, fen_after) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = dbConnection.prepareStatement(insertMoveSql)) {
            for (MoveRecord move : moves) {
                if (move == null) continue;
                
                ps.setLong(1, gameId);
                ps.setInt(2, move.getMoveNumber());
                ps.setString(3, sanitizeString(move.getPlayer()));
                ps.setString(4, sanitizeString(move.getSan()));
                ps.setString(5, sanitizeString(move.getFenBefore()));
                ps.setString(6, sanitizeString(move.getFenAfter()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // Load a ValidGame with proper error handling
    public ValidGame loadValidGame(long gameId) throws SQLException {
        if (gameId <= 0) {
            throw new IllegalArgumentException("Invalid game ID: " + gameId);
        }
        
        final String selectGameSql = "SELECT * FROM games WHERE id = ?";
        PGNGameTags tags;
        
        try (PreparedStatement ps = dbConnection.prepareStatement(selectGameSql)) {
            ps.setLong(1, gameId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PGNGameTags.TagsBuilder builder = new PGNGameTags.TagsBuilder();
                    builder.addTag("Event", rs.getString("event"));
                    builder.addTag("Site", rs.getString("site"));
                    builder.addTag("Date", rs.getString("date"));
                    builder.addTag("Round", rs.getString("round"));
                    builder.addTag("White", rs.getString("white"));
                    builder.addTag("Black", rs.getString("black"));
                    builder.addTag("Result", rs.getString("result"));
                    tags = builder.build();
                } else {
                    throw new SQLException("Game not found with id: " + gameId);
                }
            }
        }
        
        List<MoveRecord> moves = loadMoves(gameId);
        return new ValidGame(tags, moves);
    }

    // Load all moves for a game
    private List<MoveRecord> loadMoves(long gameId) throws SQLException {
        final String selectMovesSql = "SELECT * FROM moves WHERE game_id = ? ORDER BY move_number ASC";
        List<MoveRecord> moves = new ArrayList<>();
        
        try (PreparedStatement ps = dbConnection.prepareStatement(selectMovesSql)) {
            ps.setLong(1, gameId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int moveNumber = rs.getInt("move_number");
                    String player = rs.getString("player");
                    String san = rs.getString("san");
                    String fenBefore = rs.getString("fen_before");
                    String fenAfter = rs.getString("fen_after");
                    
                    MoveRecord move = new MoveRecord(moveNumber, player, san, fenBefore, fenAfter);
                    moves.add(move);
                }
            }
        }
        
        return moves;
    }
    
    // Sanitize string input to prevent injection attacks
    private String sanitizeString(String input) {
        if (input == null) {
            return "";
        }
        
        // Limit length and remove potentially dangerous characters
        return input.substring(0, Math.min(input.length(), 255))
                   .replaceAll("[<>\"'&;]", "");
    }
    
    // Get database statistics
    public DatabaseStats getStats() throws SQLException {
        final String gameCountSql = "SELECT COUNT(*) as game_count FROM games";
        final String moveCountSql = "SELECT COUNT(*) as move_count FROM moves";
        
        int gameCount = 0;
        int moveCount = 0;
        
        try (PreparedStatement ps = dbConnection.prepareStatement(gameCountSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                gameCount = rs.getInt("game_count");
            }
        }
        
        try (PreparedStatement ps = dbConnection.prepareStatement(moveCountSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                moveCount = rs.getInt("move_count");
            }
        }
        
        return new DatabaseStats(gameCount, moveCount);
    }
    
    // Close connection properly
    public void close() {
        try {
            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close();
                LOGGER.info("Database connection closed");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error closing database connection", e);
        }
    }
    
    // Inner class for database statistics
    public static class DatabaseStats {
        private final int gameCount;
        private final int moveCount;
        
        public DatabaseStats(int gameCount, int moveCount) {
            this.gameCount = gameCount;
            this.moveCount = moveCount;
        }
        
        public int getGameCount() { return gameCount; }
        public int getMoveCount() { return moveCount; }
        
        @Override
        public String toString() {
            return String.format("Database Stats: %d games, %d moves", gameCount, moveCount);
        }
    }
}
