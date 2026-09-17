package cubeGame.demo.DAO;

import fr.le_campus_numerique.square_games.engine.Game;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameDao {
    Optional<Game> findById(UUID gameId);

    default Optional<Game> findById(String gameId) {
        if (gameId == null) {
            return Optional.empty();
        }
        try {
            return findById(UUID.fromString(gameId));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    List<Game> findAll();

    List<Game> findAll(int offset, int limit);

    boolean existsById(UUID gameId);

    Game upsert(Game game);

    boolean delete(UUID gameId);
}

