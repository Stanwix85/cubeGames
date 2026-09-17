package cubeGame.demo.DAO;

import fr.le_campus_numerique.square_games.engine.Game;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository("jdbcGameDao")
public class JdbcGameDao implements GameDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Map<UUID, Game> activeGamesCache = new ConcurrentHashMap<>();

    @Autowired
    public JdbcGameDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initTable() {
        Integer count = jdbcTemplate.getJdbcOperations().queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'games'",
                Integer.class
        );
        if (count == null || count == 0) {
            String sql = """
                    CREATE TABLE IF NOT EXISTS games (
                        id VARCHAR(36) PRIMARY KEY,
                        game_type VARCHAR(50) NOT NULL,
                        status VARCHAR(50) NOT NULL,
                        board_size INT NOT NULL
                    )
                    """;
            jdbcTemplate.getJdbcOperations().execute(sql);
        }
    }

    @Override
    public Optional<Game> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        if (activeGamesCache.containsKey(id)) {
            return Optional.of(activeGamesCache.get(id));
        }
        String sql = "SELECT id, game_type, status, board_size FROM games WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", id.toString());
        List<Game> games = jdbcTemplate.query(sql, params, new GameRowMapper());
        return games.stream().findFirst();
    }

    @Override
    public List<Game> findAll() {
        String sql = "SELECT id, game_type, status, board_size FROM games";
        List<Game> dbGames = jdbcTemplate.query(sql, new GameRowMapper());
        return dbGames.stream()
                .map(g -> activeGamesCache.getOrDefault(g.getId(), g))
                .toList();
    }

    @Override
    public List<Game> findAll(int offset, int limit) {
        String sql = "SELECT id, game_type, status, board_size FROM games LIMIT :limit OFFSET :offset";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("offset", offset);
        List<Game> dbGames = jdbcTemplate.query(sql, params, new GameRowMapper());
        return dbGames.stream()
                .map(g -> activeGamesCache.getOrDefault(g.getId(), g))
                .toList();
    }

    @Override
    public boolean existsById(UUID gameId) {
        if (gameId == null) {
            return false;
        }
        if (activeGamesCache.containsKey(gameId)) {
            return true;
        }
        String sql = "SELECT COUNT(*) FROM games WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", gameId.toString());
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    @Override
    public Game upsert(Game game) {
        if (game == null) {
            throw new IllegalArgumentException("Game must not be null");
        }
        String sql = """
                INSERT INTO games (id, game_type, status, board_size)
                VALUES (:id, :game_type, :status, :board_size)
                ON DUPLICATE KEY UPDATE
                    game_type = VALUES(game_type),
                    status = VALUES(status),
                    board_size = VALUES(board_size)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", game.getId().toString())
                .addValue("game_type", game.getFactoryId())
                .addValue("status", game.getStatus().name())
                .addValue("board_size", game.getBoardSize());

        jdbcTemplate.update(sql, params);
        activeGamesCache.put(game.getId(), game);
        return game;
    }

    @Override
    public boolean delete(UUID gameId) {
        if (gameId == null) {
            return false;
        }
        activeGamesCache.remove(gameId);
        String sql = "DELETE FROM games WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", gameId.toString());
        int rows = jdbcTemplate.update(sql, params);
        return rows > 0;
    }
}