package mineopoly_three.strategy;

import mineopoly_three.action.TurnAction;
import mineopoly_three.game.Economy;
import mineopoly_three.item.InventoryItem;
import mineopoly_three.tiles.Tile;
import mineopoly_three.tiles.TileType;
import mineopoly_three.util.DistanceUtil;

import java.awt.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Diamond hands
public class CustomStrategy implements MinePlayerStrategy {
    private int boardSize;
    private int maxInventorySize;
    private int maxCharge;
    private int winningScore;
    private boolean isRedPlayer;
    private PlayerBoardView startingBoard;
    private Point currentLocation;

    private int itemCount = 0;
    private int score;
    private List<TurnAction> commandStack = new ArrayList<>();


    @Override
    public void initialize(int boardSize, int maxInventorySize, int maxCharge, int winningScore, PlayerBoardView startingBoard, Point startTileLocation, boolean isRedPlayer, Random random) {
        this.boardSize = boardSize;
        this.maxInventorySize = maxInventorySize;
        this.maxCharge = maxCharge;
        this.winningScore = winningScore;
        this.startingBoard = startingBoard;
        this.isRedPlayer = isRedPlayer;
        this.currentLocation = startTileLocation;
    }

    @Override
    public TurnAction getTurnAction(PlayerBoardView boardView, Economy economy, int currentCharge, boolean isRedTurn) {
        while (score < winningScore) {
            currentLocation = boardView.getYourLocation();

            if (!commandStack.isEmpty()) {

                return commandStack.remove(commandStack.size() - 1);
            }

            if (currentLocation.equals(TileType.RED_MARKET)) {
                itemCount = 0;
            }

            if (itemCount == maxInventorySize) {

                return moveTowardsTile(getNearestTile(TileType.RED_MARKET));
            }

            Point rubyPoint = getNearestTile(TileType.RESOURCE_RUBY);
            if (!currentLocation.equals(rubyPoint)) {

                return moveTowardsTile(rubyPoint);
            }

            if (boardView.getTileTypeAtLocation(currentLocation).equals(TileType.RESOURCE_RUBY)) {
                commandStack.add(TurnAction.PICK_UP_RESOURCE);
                commandStack.add(TurnAction.MINE);
            }
        }
        return null;
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
        return "FUCK";
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
