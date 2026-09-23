package cubeGame.demo.DAO;

import cubeGame.demo.DAO.entities.GameEntity;
import cubeGame.demo.DAO.entities.GameTokenEntity;
import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.GameFactory;
import fr.le_campus_numerique.square_games.engine.GameStatus;
import fr.le_campus_numerique.square_games.engine.Token;
import fr.le_campus_numerique.square_games.engine.TokenPosition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository("jpaGameDao")
@Primary
public class JpaGameDao implements GameDao {

    private final GameEntityRepository repository;
    private final Map<UUID, Game> activeGamesCache = new ConcurrentHashMap<>();
    private final Map<String, GameFactory> factories = new HashMap<>();

    @Autowired
    public JpaGameDao(GameEntityRepository repository, List<GameFactory> factoryList) {
        this.repository = repository;
        if (factoryList != null) {
            for (GameFactory factory : factoryList) {
                this.factories.put(factory.getGameFactoryId().toLowerCase(), factory);
                if (factory.getGameFactoryId().equalsIgnoreCase("15 puzzle")) {
                    this.factories.put("taquin", factory);
                }
                if (factory.getGameFactoryId().equalsIgnoreCase("connect4")) {
                    this.factories.put("connectfour", factory);
                }
            }
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
        return repository.findById(id.toString()).map(this::toDomain);
    }

    @Override
    public List<Game> findAll() {
        return repository.findAll().stream()
                .map(this::toCachedOrDomain)
                .toList();
    }

    @Override
    public List<Game> findAll(int offset, int limit) {
        return repository.findAll().stream()
                .skip(Math.max(0, offset))
                .limit(Math.max(0, limit))
                .map(this::toCachedOrDomain)
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
        return repository.existsById(gameId.toString());
    }

    @Override
    public Game upsert(Game game) {
        if (game == null) {
            throw new IllegalArgumentException("Game must not be null");
        }
        GameEntity entity = toEntity(game);
        repository.save(entity);
        activeGamesCache.put(game.getId(), game);
        return game;
    }

    @Override
    public boolean delete(UUID gameId) {
        if (gameId == null) {
            return false;
        }
        activeGamesCache.remove(gameId);
        if (!repository.existsById(gameId.toString())) {
            return false;
        }
        repository.deleteById(gameId.toString());
        return true;
    }

    private Game toCachedOrDomain(GameEntity entity) {
        try {
            UUID id = UUID.fromString(entity.id);
            if (activeGamesCache.containsKey(id)) {
                return activeGamesCache.get(id);
            }
        } catch (IllegalArgumentException ignored) {
        }
        return toDomain(entity);
    }

    private GameEntity toEntity(Game game) {
        String id = game.getId().toString();
        String factoryId = game.getFactoryId();
        int boardSize = game.getBoardSize();
        GameStatus status = game.getStatus();
        String currentPlayerId = game.getCurrentPlayerId() != null ? game.getCurrentPlayerId().toString() : null;
        String playerIds = game.getPlayerIds() != null
                ? game.getPlayerIds().stream().map(UUID::toString).collect(Collectors.joining(","))
                : "";

        List<GameTokenEntity> tokenEntities = new ArrayList<>();

        // Board tokens
        if (game.getBoard() != null) {
            for (Map.Entry<CellPosition, Token> entry : game.getBoard().entrySet()) {
                CellPosition pos = entry.getKey();
                Token token = entry.getValue();
                String ownerId = token.getOwnerId().map(UUID::toString).orElse(null);
                tokenEntities.add(new GameTokenEntity(ownerId, token.getName(), false, pos.x(), pos.y()));
            }
        }

        // Remaining tokens
        if (game.getRemainingTokens() != null) {
            for (Token token : game.getRemainingTokens()) {
                String ownerId = token.getOwnerId().map(UUID::toString).orElse(null);
                tokenEntities.add(new GameTokenEntity(ownerId, token.getName(), false, null, null));
            }
        }

        // Removed tokens
        if (game.getRemovedTokens() != null) {
            for (Token token : game.getRemovedTokens()) {
                String ownerId = token.getOwnerId().map(UUID::toString).orElse(null);
                tokenEntities.add(new GameTokenEntity(ownerId, token.getName(), true, null, null));
            }
        }

        return new GameEntity(id, factoryId, boardSize, playerIds, status, currentPlayerId, tokenEntities);
    }

    private Game toDomain(GameEntity entity) {
        UUID gameId = UUID.fromString(entity.id);

        List<UUID> playersList = (entity.playerIds != null && !entity.playerIds.isBlank())
                ? Arrays.stream(entity.playerIds.split(","))
                .filter(s -> !s.isBlank())
                .map(UUID::fromString)
                .toList()
                : Collections.emptyList();

        List<TokenPosition<UUID>> boardTokens = new ArrayList<>();
        List<TokenPosition<UUID>> removedTokens = new ArrayList<>();

        if (entity.tokens != null) {
            for (GameTokenEntity tokenEntity : entity.tokens) {
                UUID ownerId = tokenEntity.ownerId != null ? UUID.fromString(tokenEntity.ownerId) : null;
                if (tokenEntity.removed) {
                    removedTokens.add(new TokenPosition<>(ownerId, tokenEntity.name, 0, 0));
                } else if (tokenEntity.x != null && tokenEntity.y != null) {
                    boardTokens.add(new TokenPosition<>(ownerId, tokenEntity.name, tokenEntity.x, tokenEntity.y));
                }
            }
        }

        String factoryKey = entity.factoryId != null ? entity.factoryId.trim().toLowerCase() : "tictactoe";
        GameFactory factory = factories.get(factoryKey);
        if (factory == null && factoryKey.contains("tictac")) {
            factory = factories.get("tictactoe");
        }

        if (factory != null && !playersList.isEmpty()) {
            try {
                Game restored = factory.createGameWithIds(gameId, entity.boardSize, playersList, boardTokens, removedTokens);
                activeGamesCache.put(gameId, restored);
                return restored;
            } catch (Exception ignored) {
            }
        }

        GameStatus status = entity.status != null ? entity.status : GameStatus.ONGOING;
        UUID currentPlayer = entity.currentPlayerId != null ? UUID.fromString(entity.currentPlayerId) : null;

        Set<UUID> players = new LinkedHashSet<>(playersList);

        Map<CellPosition, Token> board = new HashMap<>();
        List<Token> remaining = new ArrayList<>();
        List<Token> removed = new ArrayList<>();

        if (entity.tokens != null) {
            for (GameTokenEntity tokenEntity : entity.tokens) {
                UUID ownerId = tokenEntity.ownerId != null ? UUID.fromString(tokenEntity.ownerId) : null;
                CellPosition pos = (tokenEntity.x != null && tokenEntity.y != null)
                        ? new CellPosition(tokenEntity.x, tokenEntity.y)
                        : null;

                JpaRestoredToken token = new JpaRestoredToken(tokenEntity.name, ownerId, pos);

                if (tokenEntity.removed) {
                    removed.add(token);
                } else if (pos != null) {
                    board.put(pos, token);
                } else {
                    remaining.add(token);
                }
            }
        }

        return new JpaGameSnapshot(
                gameId,
                entity.factoryId,
                status,
                players,
                currentPlayer,
                entity.boardSize,
                board,
                remaining,
                removed
        );
    }

    private record JpaRestoredToken(
            String name,
            UUID ownerId,
            CellPosition position
    ) implements Token {

        @Override
        public Optional<UUID> getOwnerId() {
            return Optional.ofNullable(ownerId);
        }

        @Override
        public String getName() {
            return name != null ? name : "";
        }

        @Override
        public CellPosition getPosition() {
            return position;
        }

        @Override
        public boolean canMove() {
            return true;
        }

        @Override
        public Set<CellPosition> getAllowedMoves() {
            return Collections.emptySet();
        }

        @Override
        public void moveTo(CellPosition destination) {
            // Snapshot tokens loaded from DB without engine instance cannot perform rules calculation
            throw new UnsupportedOperationException("Moves on persisted snapshots must be executed via active engine session.");
        }
    }

    private record JpaGameSnapshot(
            UUID id,
            String factoryId,
            GameStatus status,
            Set<UUID> playerIds,
            UUID currentPlayerId,
            int boardSize,
            Map<CellPosition, Token> board,
            Collection<Token> remainingTokens,
            Collection<Token> removedTokens
    ) implements Game {

        @Override
        public UUID getId() {
            return id;
        }

        @Override
        public String getFactoryId() {
            return factoryId;
        }

        @Override
        public Set<UUID> getPlayerIds() {
            return playerIds;
        }

        @Override
        public GameStatus getStatus() {
            return status;
        }

        @Override
        public UUID getCurrentPlayerId() {
            return currentPlayerId;
        }

        @Override
        public int getBoardSize() {
            return boardSize;
        }

        @Override
        public Map<CellPosition, Token> getBoard() {
            return board;
        }

        @Override
        public Collection<Token> getRemainingTokens() {
            return remainingTokens;
        }

        @Override
        public Collection<Token> getRemovedTokens() {
            return removedTokens;
        }
    }
}
