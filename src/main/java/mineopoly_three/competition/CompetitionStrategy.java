package mineopoly_three.competition;

import mineopoly_three.action.TurnAction;
import mineopoly_three.game.Economy;
import mineopoly_three.item.InventoryItem;
import mineopoly_three.item.ItemType;
import mineopoly_three.strategy.MinePlayerStrategy;
import mineopoly_three.strategy.PlayerBoardView;
import mineopoly_three.tiles.Tile;
import mineopoly_three.tiles.TileType;
import mineopoly_three.util.DistanceUtil;

import java.awt.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.List;

// Diamond hands
public class CompetitionStrategy implements MinePlayerStrategy {
    private int boardSize;
    private int maxInventorySize;
    private int maxCharge;
    private int winningScore;
    private boolean isRedPlayer;
    private PlayerBoardView startingBoard;
    private Point currentLocation;
    private PlayerBoardView currentBoard;
    private Economy economy;
    private int currentCharge;
    private boolean isRedTurn;

    private int itemCount = 0;
    private int score;
    private List<TurnAction> commandStack;
    private List<TileType> resourcePriority;
    private int resourcePriorityIndex;
    private Map<TileType, Integer> movesToMineResource;
    private int pointsScored;
    private int opponentPointsScored;

    @Override
    public void initialize(int boardSize, int maxInventorySize, int maxCharge, int winningScore, PlayerBoardView startingBoard, Point startTileLocation, boolean isRedPlayer, Random random) {
        this.boardSize = boardSize;
        this.maxInventorySize = maxInventorySize;
        this.maxCharge = maxCharge;
        this.winningScore = winningScore;
        this.startingBoard = startingBoard;
        this.isRedPlayer = isRedPlayer;
        this.currentLocation = startTileLocation;

        commandStack = new ArrayList<>();
//        resourcePriority = new ArrayList<>(Arrays.asList(
//                TileType.RESOURCE_RUBY, TileType.RESOURCE_EMERALD, TileType.RESOURCE_DIAMOND));
//        resourcePriorityIndex = 0;
        movesToMineResource = new HashMap<>();
        movesToMineResource.put(TileType.RESOURCE_RUBY, 1);
        movesToMineResource.put(TileType.RESOURCE_EMERALD, 2);
        movesToMineResource.put(TileType.RESOURCE_DIAMOND, 3);
    }

    @Override
    public TurnAction getTurnAction(PlayerBoardView boardView, Economy economy, int currentCharge, boolean isRedTurn) {
        this.currentBoard = boardView;
        this.economy = economy;
        this.currentCharge = currentCharge;
        this.isRedTurn = isRedTurn;
        this.currentLocation = boardView.getYourLocation();

//        if (resourcePriorityIndex > resourcePriority.size() - 1) {
//            resourcePriorityIndex = 0;
//        }
//
//        if (!tileExists(resourcePriority.get(resourcePriorityIndex))) {
//            resourcePriorityIndex += 1;
//            if (resourcePriorityIndex < 0) {
//                resourcePriorityIndex = resourcePriority.size() - 1;
//            }
//        }
//
//        TileType currentResource = resourcePriority.get(resourcePriorityIndex);
        TileType currentResource = getMostExpensiveResource();
        if (!playerHasEnoughCharge()) {
            return moveTowardsTile(getNearestTile(TileType.RECHARGE));
        }

        if (currentLocation.equals(getNearestTile(TileType.RECHARGE)) && currentCharge < maxCharge) {
            return null;
        }

        if (!commandStack.isEmpty()) {
            return commandStack.remove(commandStack.size() - 1);
        }

        if (currentLocation.equals(getNearestTile(TileType.RED_MARKET))) {
            itemCount = 0;
            resourcePriorityIndex += 1;
        }

        if (itemCount == maxInventorySize) {
            return moveTowardsTile(getNearestTile(TileType.RED_MARKET));
        }

        Point resourcePoint = getNearestTile(currentResource);
        if (!currentLocation.equals(resourcePoint)) {
            return moveTowardsTile(resourcePoint);
        }

        if (boardView.getTileTypeAtLocation(currentLocation).equals(currentResource)) {
            commandStack.add(TurnAction.PICK_UP_RESOURCE);
            for (int move = 0; move < movesToMineResource.get(currentResource); move++) {
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
        this.pointsScored = pointsScored;
        this.opponentPointsScored = opponentPointsScored;
    }

    public TileType getMostExpensiveResource() {
        ItemType mostExpensiveResource = ItemType.RUBY;
        for (ItemType item: economy.getCurrentPrices().keySet()) {
            if (economy.getCurrentPrices().get(item) > economy.getCurrentPrices().get(mostExpensiveResource)) {
                mostExpensiveResource = item;
            }
        }
        return convertItemTypeToTileType(mostExpensiveResource);
    }

    public TileType convertItemTypeToTileType(ItemType item) {
        if (item.equals(ItemType.RUBY)) {
            return TileType.RESOURCE_RUBY;
        } else if (item.equals(ItemType.EMERALD)) {
            return TileType.RESOURCE_EMERALD;
        } else if (item.equals(ItemType.DIAMOND)) {
            return TileType.RESOURCE_DIAMOND;
        } else {
            return TileType.EMPTY;
        }
    }

    public boolean tileExists(TileType tile) {
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (currentBoard.getTileTypeAtLocation(col, row).equals(tile)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean playerHasEnoughCharge() {
        int distanceToNearestCharger = DistanceUtil.getManhattanDistance(currentLocation, getNearestTile(TileType.RECHARGE));
        if (currentCharge <= distanceToNearestCharger) {
            return false;
        }

        return true;
    }

    public TurnAction moveTowardsTile(Point point) {
        if (currentLocation.x < point.x) {
            return TurnAction.MOVE_RIGHT;
        } else if (currentLocation.y < point.y) {
            return TurnAction.MOVE_UP;
        } else if (currentLocation.x > point.x) {
            return TurnAction.MOVE_LEFT;
        } else if (currentLocation.y > point.y){
            return TurnAction.MOVE_DOWN;
        } else {
            return null;
        }
    }

    public Point getNearestTile(TileType tile) {
        Point nearestTile = getFirstInstanceOfTile(tile);
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (currentBoard.getTileTypeAtLocation(col, row).equals(tile)
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
                if (currentBoard.getTileTypeAtLocation(col, row).equals(tile)) {
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