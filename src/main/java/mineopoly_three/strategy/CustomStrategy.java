package mineopoly_three.strategy;

import mineopoly_three.action.TurnAction;
import mineopoly_three.game.Economy;
import mineopoly_three.item.InventoryItem;
import mineopoly_three.item.ItemType;
import mineopoly_three.tiles.TileType;

import java.awt.*;
import java.util.*;
import java.util.List;

// Diamond hands
public class CustomStrategy implements MinePlayerStrategy {
    public static int boardSize;
    public static int maxInventorySize;
    public static int maxCharge;
    public static int winningScore;
    private boolean isRedPlayer;
    private Point currentLocation;
    private Point otherPlayerLocation;
    private PlayerBoardView currentBoard;
    private Economy economy;
    private int currentCharge;
    private boolean isRedTurn;

    private List<InventoryItem> inventory = new ArrayList<>();
    private Map<TileType, Integer> movesToMineResource = new HashMap<>();
    private int pointsScored;
    private int opponentPointsScored;
    private int currentScore = 0;
    private Map<Point, List<InventoryItem>> itemsOnGround;
    public Set<ItemType> typesOfGems = new HashSet<>(Arrays.asList(ItemType.RUBY, ItemType.EMERALD, ItemType.DIAMOND));
    private int numberOfTurns;


    @Override
    public void initialize(int boardSize, int maxInventorySize, int maxCharge, int winningScore, PlayerBoardView startingBoard, Point startTileLocation, boolean isRedPlayer, Random random) {
        this.boardSize = boardSize;
        this.maxInventorySize = maxInventorySize;
        this.maxCharge = maxCharge;
        this.winningScore = winningScore;
        this.isRedPlayer = isRedPlayer;

        movesToMineResource.put(TileType.RESOURCE_RUBY, 1);
        movesToMineResource.put(TileType.RESOURCE_EMERALD, 2);
        movesToMineResource.put(TileType.RESOURCE_DIAMOND, 3);
    }

    @Override
    public TurnAction getTurnAction(PlayerBoardView boardView, Economy economy, int currentCharge, boolean isRedTurn) {
        numberOfTurns += 1;
//        System.out.println(numberOfTurns);
        this.currentBoard = boardView;
        this.economy = economy;
        this.currentCharge = currentCharge;
        this.isRedTurn = isRedTurn;
        this.currentLocation = boardView.getYourLocation();
        this.otherPlayerLocation = boardView.getOtherPlayerLocation();
        this.itemsOnGround = currentBoard.getItemsOnGround();

        TileType currentResource = Utility.determineMostExpensiveResource(economy);

        if (currentLocationHasGem() && inventory.size() < maxInventorySize) {
            return TurnAction.PICK_UP_RESOURCE;
        }

        // Not sure why having this code screws up the tests.

//        if (getCurrentInventoryValue() + currentScore >= winningScore) {
//            return moveToNearestMarketTile();
//        }

        if (!Utility.playerHasEnoughCharge(currentCharge, currentLocation, getNearestTile(TileType.RECHARGE))) {
            return Utility.moveTowardsTile(currentLocation, getNearestTile(TileType.RECHARGE));
        }

        if (currentLocation.equals(getNearestTile(TileType.RECHARGE)) && currentCharge < maxCharge) {
            return null;
        }

        if (inventory.size() == maxInventorySize) {
            return moveToNearestMarketTile();
        }

        Point nearestResource = getNearestTile(currentResource);
        Point nearestGem = getNearestAvailableGem();

        if (currentLocation.equals(nearestGem)) {
            return TurnAction.PICK_UP_RESOURCE;
        }

        if (currentLocation.equals(nearestResource)) {
            return TurnAction.MINE;
        }

        if (nearestGem != null && nearestResource != null &&
                !currentLocation.equals(nearestGem) && !currentLocation.equals(nearestResource)) {
            TurnAction nextAction;
            if (Utility.compareManhattanDistance(currentLocation, nearestGem, nearestResource) > 0) {
                nextAction = Utility.moveTowardsTile(currentLocation, nearestResource);
                return nextAction;
            }
            nextAction = Utility.moveTowardsTile(currentLocation, nearestGem);
            return nextAction;
        }

        if (nearestGem != null && !currentLocation.equals(nearestGem)) {
            TurnAction nextAction = Utility.moveTowardsTile(currentLocation, nearestGem);
            return nextAction;
        }

        if (nearestResource != null && !currentLocation.equals(nearestResource)) {
            TurnAction nextAction = Utility.moveTowardsTile(currentLocation, nearestResource);
            return nextAction;
        }

        return null;
    }

    @Override
    public void onReceiveItem(InventoryItem itemReceived) {
        inventory.add(itemReceived);
    }

    @Override
    public void onSoldInventory(int totalSellPrice) {
        currentScore += totalSellPrice;
        inventory.clear();
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

    public TurnAction moveToNearestMarketTile() {
        if (isRedPlayer) {
            return Utility.moveTowardsTile(currentLocation, getNearestTile(TileType.RED_MARKET));
        }
        return Utility.moveTowardsTile(currentLocation, getNearestTile(TileType.BLUE_MARKET));
    }

    public int getCurrentInventoryValue() {
        int value = 0;
        for (InventoryItem item: inventory) {
            value += economy.getCurrentPrices().get(item.getItemType());
        }
        return value;
    }

    public boolean currentLocationHasGem() {
        if (currentBoard.getItemsOnGround().containsKey(currentLocation)) {
            for (InventoryItem item: currentBoard.getItemsOnGround().get(currentLocation)) {
                ItemType itemType = item.getItemType();
                if (itemType.equals(ItemType.RUBY) || itemType.equals(ItemType.EMERALD) || itemType.equals(ItemType.DIAMOND)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Point getNearestTile(TileType tile) {
        Point nearestTile = getFirstInstanceOfTile(tile);
        if (nearestTile == null) {
            return null;
        }
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (currentBoard.getTileTypeAtLocation(col, row).equals(tile)
                        && Utility.compareManhattanDistance(currentLocation, nearestTile, new Point(col, row)) > 0) {
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
                    && Utility.compareManhattanDistance(currentLocation, nearestItem, point) > 0) {
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
                if (typesOfGems.contains(item.getItemType()) && Utility.compareManhattanDistance(currentLocation, nearestGem, point) > 0) {
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
}