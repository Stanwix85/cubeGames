package cubeGame.demo;

import cubeGame.demo.service.GamePlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Service
public class GameCatalogImpl implements GameCatalog {

    private final List<GamePlugin> gamePlugins;

    @Autowired
    public GameCatalogImpl(List<GamePlugin> gamePlugins) {
        this.gamePlugins = gamePlugins;
    }

    @Override
    public Collection<String> getAvailableGameIds() {
        return gamePlugins.stream()
                .map(GamePlugin::getId)
                .toList();
    }

    @Override
    public String getGameName(String gameId, Locale locale) {
        if (gameId == null) {
            return null;
        }
        return gamePlugins.stream()
                .filter(p -> p.getId().equalsIgnoreCase(gameId.trim()))
                .findFirst()
                .map(p -> p.getName(locale))
                .orElse(gameId);
    }
}
