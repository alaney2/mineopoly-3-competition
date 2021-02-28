package mineopoly_three.strategy;

import mineopoly_three.action.TurnAction;
import mineopoly_three.game.Economy;
import mineopoly_three.item.InventoryItem;
import mineopoly_three.tiles.Tile;
import mineopoly_three.tiles.TileType;
import mineopoly_three.util.DistanceUtil;

import java.awt.*;
import java.util.Random;

// Diamond hands
public class CustomStrategy implements MinePlayerStrategy {
    int boardSize;
    int winningScore;
    boolean isRedPlayer;
    int itemCount = 0;
    int score;

    PlayerBoardView startingBoard;
    Point currentLocation;

    @Override
    public void initialize(int boardSize, int maxInventorySize, int maxCharge, int winningScore, PlayerBoardView startingBoard, Point startTileLocation, boolean isRedPlayer, Random random) {
        this.boardSize = boardSize;
        this.winningScore = winningScore;
        this.startingBoard = startingBoard;
        this.isRedPlayer = isRedPlayer;
        this.currentLocation = startTileLocation;
    }

    @Override
    public TurnAction getTurnAction(PlayerBoardView boardView, Economy economy, int currentCharge, boolean isRedTurn) {
        currentLocation = boardView.getYourLocation();
        Point rubyPoint = getNearestTile(TileType.RESOURCE_RUBY);
        while (!currentLocation.equals(rubyPoint)) {

            return moveTowardsTile(rubyPoint);
        }

        if (boardView.getTileTypeAtLocation(currentLocation).equals(TileType.RESOURCE_RUBY)) {
            return TurnAction.MINE;
        }
        return TurnAction.PICK_UP_RESOURCE;
    }

    @Override
    public void onReceiveItem(InventoryItem itemReceived) {
        itemCount += 1;
    }

    @Override
    public void onSoldInventory(int totalSellPrice) {
        score += totalSellPrice;
    }

    @Override
    public String getName() {
        return "Fuck";
    }

    @Override
    public void endRound(int pointsScored, int opponentPointsScored) {
        return;
    }

    public TurnAction moveTowardsTile(Point point) {
        if (currentLocation.x < point.x) {
            currentLocation.x += 1;
            return TurnAction.MOVE_RIGHT;
        } else if (currentLocation.y < point.y) {
            currentLocation.y += 1;
            return TurnAction.MOVE_UP;
        } else if (currentLocation.x > point.x) {
            currentLocation.x -= 1;
            return TurnAction.MOVE_LEFT;
        } else if (currentLocation.y > point.y){
            currentLocation.y -= 1;
            return TurnAction.MOVE_DOWN;
        } else {
            return null;
        }
    }

    public Point getNearestTile(TileType tile) {
        Point nearestTile = getFirstInstanceOfTile(tile);
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (startingBoard.getTileTypeAtLocation(col, row).equals(tile)
                        && compareManhattanDistance(currentLocation, nearestTile, new Point(col, row)) > 0) {
                    nearestTile.x = col;
                    nearestTile.y = row;
                }
            }
        }

        return nearestTile;
    }

    public Point getFirstInstanceOfTile(TileType tile) {
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (startingBoard.getTileTypeAtLocation(col, row).equals(tile)) {
                    return new Point(col, row);
                }
            }
        }
        return new Point();
    }

    public static int compareManhattanDistance(Point start, Point p1, Point p2) {
        return DistanceUtil.getManhattanDistance(start, p1) - DistanceUtil.getManhattanDistance(start, p2);
    }
}
