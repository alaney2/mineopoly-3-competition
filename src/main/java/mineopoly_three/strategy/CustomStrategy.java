package mineopoly_three.strategy;

import mineopoly_three.action.TurnAction;
import mineopoly_three.game.Economy;
import mineopoly_three.item.InventoryItem;
import mineopoly_three.item.ItemType;
import mineopoly_three.tiles.TileType;

import java.awt.*;
import java.util.*;
import java.util.List;

public class CustomStrategy implements MinePlayerStrategy {
    public int boardSize;
    public int maxInventorySize;
    public int maxCharge;
    public int winningScore;
    private boolean isRedPlayer;
    private Point currentLocation;
    private PlayerBoardView currentBoard;
    private Economy economy;

    private final List<InventoryItem> inventory = new ArrayList<>();
    private final Map<TileType, Integer> movesToMineResource = new HashMap<>();
    private Map<Point, List<InventoryItem>> itemsOnGround;
    public Set<ItemType> typesOfGems = new HashSet<>(Arrays.asList(ItemType.RUBY, ItemType.EMERALD, ItemType.DIAMOND));

    public List<InventoryItem> getInventory() {
        return inventory;
    }

    /**
     * Initializes constant variables.
     *
     * @param boardSize         The length and width of the square game board
     * @param maxInventorySize  The maximum number of items that your player can carry at one time
     * @param maxCharge         The amount of charge your robot starts with (number of tile moves before needing to recharge)
     * @param winningScore      The first player to reach this score wins the round
     * @param startingBoard     A view of the GameBoard at the start of the game. You can use this to pre-compute fixed
     *                          information, like the locations of market or recharge tiles
     * @param startTileLocation A Point representing your starting location in (x, y) coordinates
     *                          (0, 0) is the bottom left and (boardSize - 1, boardSize - 1) is the top right
     * @param isRedPlayer       True if this strategy is the red player, false otherwise
     * @param random            A random number generator, if your strategy needs random numbers you should use this.
     */
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

    /**
     * Finds the most expensive resource in economy and mines it. If there is a gem on the way, pick it up.
     * If there isn't enough charge to reach the destination, move to a charger. If inventory is full,
     * move to the market and sell everything.
     *
     * @param boardView     A PlayerBoardView object representing all the information about the board and the other player
     *                      that your strategy is allowed to access
     * @param economy       The GameEngine's economy object which holds current prices for resources
     * @param currentCharge The amount of charge your robot has (number of tile moves before needing to recharge)
     * @param isRedTurn     For use when two players attempt to move to the same spot on the same turn
     *                      If true: The red player will move to the spot, and the blue player will do nothing
     *                      If false: The blue player will move to the spot, and the red player will do nothing
     * @return TurnAction to execute
     */
    @Override
    public TurnAction getTurnAction(PlayerBoardView boardView, Economy economy, int currentCharge, boolean isRedTurn) {
        this.currentBoard = boardView;
        this.economy = economy;
        this.currentLocation = boardView.getYourLocation();
        this.itemsOnGround = currentBoard.getItemsOnGround();

        if (currentLocationHasGem() && inventory.size() < maxInventorySize) {
            return TurnAction.PICK_UP_RESOURCE;
        }

        if (!Utility.playerHasEnoughCharge(currentCharge, currentLocation, getNearestTile(TileType.RECHARGE))) {
            return Utility.moveTowardsPoint(currentLocation, getNearestTile(TileType.RECHARGE));
        }

        if (currentLocation.equals(getNearestTile(TileType.RECHARGE)) && currentCharge < maxCharge) {
            return null;
        }

        if (inventory.size() == maxInventorySize) {
            return moveToNearestMarketTile();
        }

        TileType currentResource = Utility.determineMostExpensiveResource(economy);
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

            if (Utility.compareManhattanDistance(currentLocation, nearestGem, nearestResource) > 0) {
                return Utility.moveTowardsPoint(currentLocation, nearestResource);
            }

            return Utility.moveTowardsPoint(currentLocation, nearestGem);
        }

        if (nearestGem != null && !currentLocation.equals(nearestGem)) {
            return Utility.moveTowardsPoint(currentLocation, nearestGem);
        }

        if (nearestResource != null && !currentLocation.equals(nearestResource)) {
            return Utility.moveTowardsPoint(currentLocation, nearestResource);
        }

        return null;
    }

    /**
     * Adds the item received to a list.
     *
     * @param itemReceived The item received from the player's TurnAction on their last turn
     */
    @Override
    public void onReceiveItem(InventoryItem itemReceived) {
        inventory.add(itemReceived);
    }

    /**
     * Clears the inventory.
     *
     * @param totalSellPrice The combined sell price for all items in your strategy's inventory
     */
    @Override
    public void onSoldInventory(int totalSellPrice) {
        inventory.clear();
    }

    @Override
    public String getName() {
        return "CustomStrategy";
    }

    @Override
    public void endRound(int pointsScored, int opponentPointsScored) {
    }

    /**
     * Finds nearest market tile based on color.
     *
     * @return TurnAction in direction of nearest market
     */
    public TurnAction moveToNearestMarketTile() {
        if (isRedPlayer) {
            return Utility.moveTowardsPoint(currentLocation, getNearestTile(TileType.RED_MARKET));
        }

        return Utility.moveTowardsPoint(currentLocation, getNearestTile(TileType.BLUE_MARKET));
    }

    /**
     * @return Value of inventory
     */
    public int getCurrentInventoryValue() {
        int value = 0;
        for (InventoryItem item : inventory) {
            value += economy.getCurrentPrices().get(item.getItemType());
        }

        return value;
    }

    /**
     * @return If the current location has a gem on it
     */
    public boolean currentLocationHasGem() {
        if (currentBoard.getItemsOnGround().containsKey(currentLocation)) {
            for (InventoryItem item : currentBoard.getItemsOnGround().get(currentLocation)) {
                ItemType itemType = item.getItemType();
                if (itemType.equals(ItemType.RUBY) || itemType.equals(ItemType.EMERALD) || itemType.equals(ItemType.DIAMOND)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @param tile to find
     * @return Point of nearest TileType from current location; null if there isn't one
     */
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

    /**
     * @return Point of nearest gem from current location; null if there isn't one
     */
    public Point getNearestAvailableGem() {
        Point nearestGem = getFirstInstanceOfItem(ItemType.RUBY);
        if (nearestGem == null) {
            return null;
        }
        for (Point point : itemsOnGround.keySet()) {
            for (InventoryItem item : itemsOnGround.get(point)) {
                if (typesOfGems.contains(item.getItemType())
                        && Utility.compareManhattanDistance(currentLocation, nearestGem, point) > 0) {
                    nearestGem = point;
                }
            }
        }

        return nearestGem;
    }

    /**
     * @param tile to find
     * @return The first instance of tile; null if there isn't one
     */
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

    /**
     * @param item to find
     * @return The first instance of item; null if there isn't one
     */
    public Point getFirstInstanceOfItem(ItemType item) {
        for (Point point : itemsOnGround.keySet()) {
            for (InventoryItem inventoryItem : itemsOnGround.get(point)) {
                if (inventoryItem.getItemType().equals(item)) {
                    return point;
                }
            }
        }

        return null;
    }
}