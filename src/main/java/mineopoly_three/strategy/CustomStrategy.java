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
    private PlayerBoardView currentBoard;
    private Economy economy;
    private int currentCharge;
    private boolean isRedTurn;

    private List<TurnAction> commandStack = new ArrayList<>();
    private List<InventoryItem> inventory = new ArrayList<>();
    private Map<TileType, Integer> movesToMineResource = new HashMap<>();
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
        this.isRedPlayer = isRedPlayer;

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

        TileType currentResource = Utility.determineMostExpensiveResource(economy);
        if (!Utility.playerHasEnoughCharge(currentCharge, currentLocation, getNearestTile(TileType.RECHARGE))) {
            return Utility.moveTowardsTile(currentLocation, getNearestTile(TileType.RECHARGE));
        }

        if (currentLocation.equals(getNearestTile(TileType.RECHARGE)) && currentCharge < maxCharge) {
            return null;
        }

        if (!commandStack.isEmpty()) {
            return commandStack.remove(commandStack.size() - 1);
        }

        if (currentLocation.equals(getNearestTile(TileType.RED_MARKET))) {
            inventory.clear();
        }

        if (inventory.size() == maxInventorySize) {
            return Utility.moveTowardsTile(currentLocation, getNearestTile(TileType.RED_MARKET));
        }

        Point nearestResource = getNearestTile(currentResource);
        Point nearestGem = getNearestAvailableGem();

        if (nearestGem != null && nearestResource != null &&
                !currentLocation.equals(nearestGem) && !currentLocation.equals(nearestResource)) {

            if (Utility.compareManhattanDistance(currentLocation, nearestGem, nearestResource) > 0) {
                return Utility.moveTowardsTile(currentLocation, nearestResource);
            }
            return Utility.moveTowardsTile(currentLocation, nearestGem);
        }

        if (nearestGem != null && !currentLocation.equals(nearestGem)) {
            return Utility.moveTowardsTile(currentLocation, nearestGem);
        }

        if (nearestResource != null && !currentLocation.equals(nearestResource)) {

            return Utility.moveTowardsTile(currentLocation, nearestResource);
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

        return getTurnAction(boardView, economy, currentCharge, !isRedTurn);
    }

    @Override
    public void onReceiveItem(InventoryItem itemReceived) {
        inventory.add(itemReceived);
    }

    @Override
    public void onSoldInventory(int totalSellPrice) { }

    @Override
    public String getName() {
        return "FUCK";
    }

    @Override
    public void endRound(int pointsScored, int opponentPointsScored) {
        this.pointsScored = pointsScored;
        this.opponentPointsScored = opponentPointsScored;
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
