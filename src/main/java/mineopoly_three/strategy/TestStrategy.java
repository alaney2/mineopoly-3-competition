package mineopoly_three.strategy;

import mineopoly_three.action.TurnAction;
import mineopoly_three.competition.Utility;
import mineopoly_three.game.Economy;
import mineopoly_three.item.InventoryItem;
import mineopoly_three.item.ItemType;
import mineopoly_three.tiles.TileType;
import mineopoly_three.util.DistanceUtil;

import java.awt.*;
import java.util.*;
import java.util.List;

// IGNORE THIS CLASS

public class TestStrategy implements MinePlayerStrategy {
    public int boardSize;
    public int maxInventorySize;
    public int maxCharge;
    public int winningScore;
    private boolean isRedPlayer;
    private int stagnantTime;
    private Point previousLocation;
    private Point currentLocation;
    private Point otherPlayerLocation;
    private PlayerBoardView currentBoard;
    private Economy economy;
    private int currentCharge;
    private boolean isRedTurn;

    private List<InventoryItem> inventory = new ArrayList<>();
    private Map<TileType, Integer> turnsToMineResource = new HashMap<>();
    private int pointsScored;
    private int opponentPointsScored;
    private Map<Point, List<InventoryItem>> itemsOnGround;
    public Set<ItemType> typesOfGems = new HashSet<>(Arrays.asList(ItemType.RUBY, ItemType.EMERALD, ItemType.DIAMOND));

    @Override
    public void initialize(int boardSize, int maxInventorySize, int maxCharge, int winningScore, PlayerBoardView startingBoard, Point startTileLocation, boolean isRedPlayer, Random random) {
        this.boardSize = boardSize;
        this.maxInventorySize = maxInventorySize;
        this.maxCharge = maxCharge;
        this.winningScore = winningScore;
        this.isRedPlayer = isRedPlayer;
        this.previousLocation = new Point();
        this.currentLocation = new Point();

        turnsToMineResource.put(TileType.RESOURCE_RUBY, 1);
        turnsToMineResource.put(TileType.RESOURCE_EMERALD, 2);
        turnsToMineResource.put(TileType.RESOURCE_DIAMOND, 3);
    }

    @Override
    public TurnAction getTurnAction(PlayerBoardView boardView, Economy economy, int currentCharge, boolean isRedTurn) {
        this.previousLocation = this.currentLocation;
        this.currentBoard = boardView;
        this.economy = economy;
        this.currentCharge = currentCharge;
        this.isRedTurn = isRedTurn;
        this.currentLocation = boardView.getYourLocation();
        this.otherPlayerLocation = boardView.getOtherPlayerLocation();
        this.itemsOnGround = currentBoard.getItemsOnGround();

        TileType currentResource = calculateOptimalResource();

        if (currentLocationHasGem() && inventory.size() < maxInventorySize) {
            return TurnAction.PICK_UP_RESOURCE;
        }

        if (currentLocation.equals(previousLocation)) {
            stagnantTime += 1;
            if (stagnantTime >= 10) {
                return actionIfOtherPlayerInWay();
            }
        } else {
            stagnantTime = 0;
        }

        if (DistanceUtil.getManhattanDistance(currentLocation, getNearestTilePoint(currentLocation, TileType.RECHARGE)) < boardSize / 4
                && currentCharge < maxCharge/2) {
            return mineopoly_three.competition.Utility.moveTowardsPoint(currentLocation, getNearestTilePoint(currentLocation, TileType.RECHARGE));
        }

        if (getNearestTilePoint(currentLocation, currentResource) != null) {
            if (!mineopoly_three.competition.Utility.playerHasEnoughCharge(currentCharge, currentLocation,
                    getNearestTilePoint(currentLocation, currentResource), getNearestTilePoint(currentLocation, TileType.RECHARGE))) {
                return mineopoly_three.competition.Utility.moveTowardsPoint(currentLocation, getNearestTilePoint(currentLocation, TileType.RECHARGE));
            }
        }

        if (currentLocation.equals(getNearestTilePoint(currentLocation, TileType.RECHARGE)) && currentCharge < maxCharge) {
            return null;
        }

        if (inventory.size() == maxInventorySize) {
            return moveToNearestMarketTile();
        }

        Point nearestResource = getNearestTilePoint(currentLocation, currentResource);
        Point nearestGem = getNearestAvailableGem();

        if (currentLocation.equals(nearestResource)) {
            return TurnAction.MINE;
        }

        if (nearestGem != null && nearestResource != null &&
                !currentLocation.equals(nearestGem) && !currentLocation.equals(nearestResource)) {
            TurnAction nextAction;
            if (mineopoly_three.competition.Utility.compareManhattanDistance(currentLocation, nearestGem, nearestResource) > 0) {
                nextAction = mineopoly_three.competition.Utility.moveTowardsPoint(currentLocation, nearestResource);
                return nextAction;
            }
            nextAction = mineopoly_three.competition.Utility.moveTowardsPoint(currentLocation, nearestGem);

            return nextAction;
        }

        if (nearestGem != null && !currentLocation.equals(nearestGem)) {
            return Utility.moveTowardsPoint(currentLocation, nearestGem);
        }

        if (nearestResource != null && !currentLocation.equals(nearestResource)) {
            return Utility.moveTowardsPoint(currentLocation, nearestResource);
        }

        return null;
    }

    @Override
    public void onReceiveItem(InventoryItem itemReceived) {
        inventory.add(itemReceived);
    }

    @Override
    public void onSoldInventory(int totalSellPrice) {
        for (int i = 0; i < inventory.size(); i++) {
            if (!inventory.get(i).getItemType().equals(ItemType.AUTOMINER)) {
                inventory.remove(i);
                i--;
            }
        }
    }

    @Override
    public String getName() {
        return "F";
    }

    @Override
    public void endRound(int pointsScored, int opponentPointsScored) {
        this.pointsScored = pointsScored;
        this.opponentPointsScored = opponentPointsScored;
    }

    public TurnAction actionIfOtherPlayerInWay() {
        if (currentLocation.x + 1 == otherPlayerLocation.x && currentLocation.y == otherPlayerLocation.y) {
            if (currentLocation.x < boardSize) {
                return TurnAction.MOVE_UP;
            }
            return TurnAction.MOVE_DOWN;
        } else if (currentLocation.x - 1 == otherPlayerLocation.x && currentLocation.y == otherPlayerLocation.y) {
            if (currentLocation.x < boardSize) {
                return TurnAction.MOVE_UP;
            }
            return TurnAction.MOVE_DOWN;
        } else if (currentLocation.y + 1 == otherPlayerLocation.y && currentLocation.x == otherPlayerLocation.x) {
            if (currentLocation.y < boardSize) {
                return TurnAction.MOVE_RIGHT;
            }
            return TurnAction.MOVE_LEFT;
        } else {
            if (currentLocation.y < boardSize) {
                return TurnAction.MOVE_RIGHT;
            }
            return TurnAction.MOVE_LEFT;
        }
    }

    public TileType calculateOptimalResource() {
        Point rubyPoint = getNearestTilePoint(currentLocation, TileType.RESOURCE_RUBY);
        Point emeraldPoint = getNearestTilePoint(currentLocation, TileType.RESOURCE_EMERALD);
        Point diamondPoint = getNearestTilePoint(currentLocation, TileType.RESOURCE_DIAMOND);

        // Checking if these resources exist
        if (rubyPoint == null) {
            rubyPoint = new Point(Integer.MAX_VALUE/2, Integer.MAX_VALUE/2);
        }
        if (emeraldPoint == null) {
            emeraldPoint = new Point(Integer.MAX_VALUE/2, Integer.MAX_VALUE/2);
        }
        if (diamondPoint == null) {
            diamondPoint = new Point(Integer.MAX_VALUE/2, Integer.MAX_VALUE/2);
        }

        int rubyDistance = DistanceUtil.getManhattanDistance(currentLocation, rubyPoint);
        int emeraldDistance = DistanceUtil.getManhattanDistance(currentLocation, emeraldPoint);
        int diamondDistance = DistanceUtil.getManhattanDistance(currentLocation, diamondPoint);

        if (inventory.size() + 1 == maxInventorySize) {
            if (isRedPlayer) {
                rubyDistance += DistanceUtil.getManhattanDistance(rubyPoint, getNearestTilePoint(rubyPoint, TileType.RED_MARKET));
                emeraldDistance += DistanceUtil.getManhattanDistance(emeraldPoint, getNearestTilePoint(emeraldPoint, TileType.RED_MARKET));
                diamondDistance += DistanceUtil.getManhattanDistance(diamondPoint, getNearestTilePoint(diamondPoint, TileType.RED_MARKET));
            } else {
                rubyDistance += DistanceUtil.getManhattanDistance(rubyPoint, getNearestTilePoint(rubyPoint, TileType.BLUE_MARKET));
                emeraldDistance += DistanceUtil.getManhattanDistance(emeraldPoint, getNearestTilePoint(emeraldPoint, TileType.BLUE_MARKET));
                diamondDistance += DistanceUtil.getManhattanDistance(diamondPoint, getNearestTilePoint(diamondPoint, TileType.BLUE_MARKET));
            }
        }
        // Adding current price + price increase per turn * turns
        int potentialRubyPrice = economy.getCurrentPrices().get(ItemType.RUBY) + 3 * rubyDistance;
        int potentialEmeraldPrice = economy.getCurrentPrices().get(ItemType.EMERALD) + 4 * emeraldDistance;
        int potentialDiamondPrice = economy.getCurrentPrices().get(ItemType.DIAMOND) + 5 * diamondDistance;

        // Capping the prices
        if (potentialRubyPrice > 400) {
            potentialRubyPrice = 400;
        }
        if (potentialEmeraldPrice > 450) {
            potentialEmeraldPrice = 450;
        }
        if (potentialDiamondPrice > 500) {
            potentialDiamondPrice = 500;
        }

        // Ratio = price / (distance * turns to mine)
        double rubyRatio = (double) potentialRubyPrice / (rubyDistance * turnsToMineResource.get(TileType.RESOURCE_RUBY));
        double emeraldRatio = (double) potentialEmeraldPrice / (emeraldDistance * turnsToMineResource.get(TileType.RESOURCE_EMERALD));
        double diamondRatio = (double) potentialDiamondPrice / (diamondDistance * turnsToMineResource.get(TileType.RESOURCE_DIAMOND));

        if (rubyRatio >= emeraldRatio && rubyRatio >= diamondRatio) {
            return TileType.RESOURCE_RUBY;
        } else if (emeraldRatio >= diamondRatio) {
            return TileType.RESOURCE_EMERALD;
        } else {
            return TileType.RESOURCE_DIAMOND;
        }
    }

    public TurnAction moveToNearestMarketTile() {
        if (isRedPlayer) {
            return mineopoly_three.competition.Utility.moveTowardsPoint(currentLocation, getNearestTilePoint(currentLocation, TileType.RED_MARKET));
        }

        return mineopoly_three.competition.Utility.moveTowardsPoint(currentLocation, getNearestTilePoint(currentLocation, TileType.BLUE_MARKET));
    }

    public int getCurrentInventoryValue() {
        int value = 0;
        for (InventoryItem item: inventory) {
            if (!item.getItemType().equals(ItemType.AUTOMINER)) {
                value += economy.getCurrentPrices().get(item.getItemType());
            }
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

    public Point getNearestTilePoint(Point currentLocation, TileType tile) {
        Point nearestTile = getFirstInstanceOfTile(tile);
        if (nearestTile == null) {
            return null;
        }
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (currentBoard.getTileTypeAtLocation(col, row).equals(tile)
                        && mineopoly_three.competition.Utility.compareManhattanDistance(currentLocation, nearestTile, new Point(col, row)) > 0) {
                    nearestTile = new Point(col, row);
                }
            }
        }

        return nearestTile;
    }

    public Point getNearestItemOnGround(ItemType item) {
        Point nearestItem = mineopoly_three.competition.Utility.getFirstInstanceOfItem(item, currentBoard.getItemsOnGround());
        if (nearestItem == null) {
            return null;
        }
        for (Point point: itemsOnGround.keySet()) {
            for (InventoryItem inventoryItem: itemsOnGround.get(point)) {
                if (inventoryItem.getItemType().equals(item)
                        && Utility.compareManhattanDistance(currentLocation, nearestItem, point) > 0) {
                    nearestItem = point;
                }
            }
        }

        return nearestItem;
    }

    public Point getNearestAvailableGem() {
        Point nearestGem = mineopoly_three.competition.Utility.getFirstInstanceOfItem(ItemType.RUBY, currentBoard.getItemsOnGround());
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
}