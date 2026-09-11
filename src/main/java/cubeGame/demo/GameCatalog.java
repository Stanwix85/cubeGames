package cubeGame.demo;

import java.util.Collection;
import java.util.Locale;

public interface GameCatalog {
    Collection<String> getAvailableGameIds();

    default String getGameName(String gameId, Locale locale) {
        return gameId;
    }
}
