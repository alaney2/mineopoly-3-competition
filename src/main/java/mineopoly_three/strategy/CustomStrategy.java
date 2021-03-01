package mineopoly_three.strategy;

import mineopoly_three.action.TurnAction;
import mineopoly_three.game.Economy;
import mineopoly_three.item.InventoryItem;
import mineopoly_three.item.ItemType;
import mineopoly_three.tiles.Tile;
import mineopoly_three.tiles.TileType;
import mineopoly_three.util.DistanceUtil;

import java.awt.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.List;

// Diamond hands
public class CustomStrategy implements MinePlayerStrategy {
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
    private Map<Point, List<InventoryItem>> itemsOnGround;
    public Set<ItemType> typesOfGems = new HashSet<>(Arrays.asList(ItemType.RUBY, ItemType.EMERALD, ItemType.DIAMOND));
    private int numberOfTurns;


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
        movesToMineResource = new HashMap<>();
        movesToMineResource.put(TileType.RESOURCE_RUBY, 1);
        movesToMineResource.put(TileType.RESOURCE_EMERALD, 2);
        movesToMineResource.put(TileType.RESOURCE_DIAMOND, 3);
    }

    @Override
    public TurnAction getTurnAction(PlayerBoardView boardView, Economy economy, int currentCharge, boolean isRedTurn) {
        numberOfTurns += 1;
        System.out.println(numberOfTurns);
        this.currentBoard = boardView;
        this.economy = economy;
        this.currentCharge = currentCharge;
        this.isRedTurn = isRedTurn;
        this.currentLocation = boardView.getYourLocation();
        this.itemsOnGround = currentBoard.getItemsOnGround();

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
        }

        if (itemCount == maxInventorySize) {
            return moveTowardsTile(getNearestTile(TileType.RED_MARKET));
        }

        Point nearestResource = getNearestTile(currentResource);
        Point nearestGem = getNearestAvailableGem();
        if (nearestGem != null && nearestResource != null &&
                !currentLocation.equals(nearestGem) && !currentLocation.equals(nearestResource)) {

            if (compareManhattanDistance(currentLocation, nearestGem, nearestResource) > 0) {
                return moveTowardsTile(nearestResource);
            }
            return moveTowardsTile(nearestGem);
        }

        if (nearestGem != null && !currentLocation.equals(nearestGem)) {
            return moveTowardsTile(nearestGem);
        }

        if (nearestResource != null && !currentLocation.equals(nearestResource)) {

            return moveTowardsTile(nearestResource);
        }

        if (currentLocation.equals(nearestGem)) {

            return TurnAction.PICK_UP_RESOURCE;
        }

        if (currentLocation.equals(nearestResource)) {

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
        if (point == null) {
            return null;
        }
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
        if (nearestTile == null) {
            return null;
        }
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (currentBoard.getTileTypeAtLocation(col, row).equals(tile)
                        && compareManhattanDistance(currentLocation, nearestTile, new Point(col, row)) > 0) {
                    nearestTile = new Point(col, row);
                }
            }
        }

        return nearestTile;
    }

    public Point getNearestItemOnGround(ItemType item) {
        Point nearestItem = getFirstInstanceOfItem(item);
        if (nearestItem == null) {
            return null;
        }
        for (Point point: itemsOnGround.keySet()) {
            if (itemsOnGround.get(point).contains(item)
                    && compareManhattanDistance(currentLocation, nearestItem, point) > 0) {
                nearestItem = point;
            }
        }

        return nearestItem;
    }

    public Point getNearestAvailableGem() {
        Point nearestGem = getFirstInstanceOfItem(ItemType.RUBY);
        if (nearestGem == null) {
            return null;
        }
        for (Point point: itemsOnGround.keySet()) {
            for (InventoryItem item: itemsOnGround.get(point)) {
                if (typesOfGems.contains(item.getItemType()) && compareManhattanDistance(currentLocation, nearestGem, point) > 0) {
                    nearestGem = point;
                }
            }
        }

        return nearestGem;
    }

    public Point getFirstInstanceOfTile(TileType tile) {
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (currentBoard.getTileTypeAtLocation(col, row).equals(tile)) {
                    return new Point(col, row);
                }
            }
        }

        return null;
    }

    public Point getFirstInstanceOfItem(ItemType item) {

        for (Point point: itemsOnGround.keySet()) {
            if (itemsOnGround.get(point).contains(item)) {
                return point;
            }
        }

        return null;
    }

    public static int compareManhattanDistance(Point start, Point p1, Point p2) {
        return DistanceUtil.getManhattanDistance(start, p1) - DistanceUtil.getManhattanDistance(start, p2);
    }
}
