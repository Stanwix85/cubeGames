package cubeGame.demo.service;

import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.IntRange;

import java.util.Locale;

public interface GamePlugin {

    /**
     * Unique identifier for the game type (e.g. "tictactoe", "taquin", "connect4").
     */
    String getId();

    /**
     * Returns the localized, human-readable name of the game for the specified locale.
     *
     * @param locale the target locale
     * @return the translated game name
     */
    String getName(Locale locale);

    /**
     * Returns the default name of the game for the platform default locale.
     *
     * @return the default game name
     */
    default String getName() {
        return getName(Locale.getDefault());
    }

    /**
     * Returns the allowed range of players for this game.
     */
    IntRange getPlayerCountRange();

    /**
     * Returns the default player count when none is provided.
     */
    int getDefaultPlayerCount();

    /**
     * Returns the allowed board size range for the given player count.
     */
    IntRange getBoardSizeRange(int playerCount);

    /**
     * Returns the default board size for the given player count.
     */
    int getDefaultBoardSize(int playerCount);

    /**
     * Creates a new game instance without systematically requiring all parameters.
     * If playerCount or boardSize is null, plugin defaults will be used.
     *
     * @param playerCount optional player count (nullable)
     * @param boardSize   optional board size (nullable)
     * @return the newly instantiated Game
     */
    Game createGame(Integer playerCount, Integer boardSize);

    /**
     * Creates a new game instance with all default parameters.
     *
     * @return the newly instantiated Game
     */
    default Game createGame() {
        return createGame(null, null);
    }
}
