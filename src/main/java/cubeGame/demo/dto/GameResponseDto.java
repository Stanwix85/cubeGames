package cubeGame.demo.dto;

import fr.le_campus_numerique.square_games.engine.GameStatus;
import fr.le_campus_numerique.square_games.engine.Game;

import java.util.*;

public class GameResponseDto {

    private UUID id;
    private String factoryId;
    private GameStatus status;
    private int boardSize;
    private Collection<UUID> playerIds;
    private UUID currentPlayerId;

    public GameResponseDto() {
    }

    public GameResponseDto(UUID id, String factoryId, GameStatus status, int boardSize, Collection<UUID> playerIds, UUID currentPlayerId) {
        this.id = id;
        this.factoryId = factoryId;
        this.status = status;
        this.boardSize = boardSize;
        this.playerIds = playerIds;
        this.currentPlayerId = currentPlayerId;
    }
    public static GameResponseDto from(Game game) {
        return new GameResponseDto(
                game.getId(),
                game.getFactoryId(),
                game.getStatus(),
                game.getBoardSize(),
                game.getPlayerIds(),
                game.getCurrentPlayerId()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFactoryId() {
        return factoryId;
    }

    public void setFactoryId(String factoryId) {
        this.factoryId = factoryId;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public void setBoardSize(int boardSize) {
        this.boardSize = boardSize;
    }

    public Collection<UUID> getPlayerIds() {
        return playerIds;
    }

    public void setPlayerIds(Collection<UUID> playerIds) {
        this.playerIds = playerIds;
    }

    public UUID getCurrentPlayerId() {
        return currentPlayerId;
    }

    public void setCurrentPlayerId(UUID currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
    }
}
