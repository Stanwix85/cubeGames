package cubeGame.demo.DAO;

import fr.le_campus_numerique.square_games.engine.Game;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository("inMemoryGameDao")
public class InMemoryGameDao implements GameDao {

    private final Map<UUID, Game> games = new ConcurrentHashMap<>();

    @Override
    public Optional<Game> findById(UUID gameId) {
        if (gameId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(games.get(gameId));
    }

    @Override
    public List<Game> findAll() {
        return new ArrayList<>(games.values());
    }

    @Override
    public List<Game> findAll(int offset, int limit) {
        return games.values().stream()
                .skip(Math.max(0, offset))
                .limit(Math.max(0, limit))
                .toList();
    }

    @Override
    public boolean existsById(UUID gameId) {
        return gameId != null && games.containsKey(gameId);
    }

    @Override
    public Game upsert(Game game) {
        if (game == null) {
            throw new IllegalArgumentException("Game must not be null");
        }
        games.put(game.getId(), game);
        return game;
    }

    @Override
    public boolean delete(UUID gameId) {
        if (gameId == null) {
            return false;
        }
        return games.remove(gameId) != null;
    }
}
