package mineopoly_three.competition;

import mineopoly_three.action.TurnAction;
import mineopoly_three.game.Economy;
import mineopoly_three.item.InventoryItem;
import mineopoly_three.item.ItemType;
import mineopoly_three.strategy.MinePlayerStrategy;
import mineopoly_three.strategy.PlayerBoardView;
import mineopoly_three.tiles.TileType;
import mineopoly_three.util.DistanceUtil;

import java.awt.*;
import java.util.*;
import java.util.List;

public class CompetitionStrategy implements MinePlayerStrategy {
    public static int boardSize;
    public static int maxInventorySize;
    public static int maxCharge;
    public static int winningScore;
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
    private Map<ItemType, Integer> gemIncreasePerTurn = new HashMap<>();
    private int pointsScored;
    private int opponentPointsScored;
    private int currentScore = 0;
    private Map<Point, List<InventoryItem>> itemsOnGround;
    public Set<ItemType> typesOfGems = new HashSet<>(Arrays.asList(ItemType.RUBY, ItemType.EMERALD, ItemType.DIAMOND));
    private int numberOfTurns;
    private int autominerCount = 0;

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
        gemIncreasePerTurn.put(ItemType.RUBY, 3);
        gemIncreasePerTurn.put(ItemType.EMERALD, 4);
        gemIncreasePerTurn.put(ItemType.DIAMOND, 5);
    }

    @Override
    public TurnAction getTurnAction(PlayerBoardView boardView, Economy economy, int currentCharge, boolean isRedTurn) {
        numberOfTurns += 1;
//        System.out.println(numberOfTurns);
        this.previousLocation = this.currentLocation;
        this.currentBoard = boardView;
        this.economy = economy;
        this.currentCharge = currentCharge;
        this.isRedTurn = isRedTurn;
        this.currentLocation = boardView.getYourLocation();
        this.otherPlayerLocation = boardView.getOtherPlayerLocation();
        this.itemsOnGround = currentBoard.getItemsOnGround();

        if (autominerCount == 0 && otherPlayerHasAutominer()) {
            Point autominer = mineopoly_three.competition.Utility.getNearestAutominer(currentLocation, boardView.getItemsOnGround());
            if (currentLocation.equals(autominer)) {
                autominerCount += 1;
                return TurnAction.PICK_UP_AUTOMINER;
            }
            if (autominer != null) {
                return mineopoly_three.competition.Utility.moveTowardsPoint(currentLocation, autominer);
            }
        }

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

        if (getCurrentInventoryValue() + currentScore >= winningScore) {
            return moveToNearestMarketTile();
        }

        if (DistanceUtil.getManhattanDistance(currentLocation, getNearestTilePoint(currentLocation, TileType.RECHARGE)) < boardSize / 4
                && currentCharge < maxCharge/2) {
            return mineopoly_three.competition.Utility.moveTowardsPoint(currentLocation, getNearestTilePoint(currentLocation, TileType.RECHARGE));
        }

        if (!mineopoly_three.competition.Utility.playerHasEnoughCharge(currentCharge, currentLocation,
                getNearestTilePoint(currentLocation, currentResource), getNearestTilePoint(currentLocation, TileType.RECHARGE))) {
            return mineopoly_three.competition.Utility.moveTowardsPoint(currentLocation, getNearestTilePoint(currentLocation, TileType.RECHARGE));
        }

        if (currentLocation.equals(getNearestTilePoint(currentLocation, TileType.RECHARGE)) && currentCharge < maxCharge) {
            return null;
        }

        if (inventory.size() == maxInventorySize) {
            return moveToNearestMarketTile();
        }

        Point nearestResource = getNearestTilePoint(currentLocation, currentResource);
        Point nearestGem = getNearestAvailableGem();
//        System.out.println(nearestGem);

        if (nearestResource != null && currentLocation.equals(nearestResource)) {
            return TurnAction.MINE;
        }

        if (nearestGem != null && nearestResource != null &&
                !currentLocation.equals(nearestGem) && !currentLocation.equals(nearestResource)) {

            return mineopoly_three.competition.Utility.moveTowardsPoint(currentLocation, gemVersusResource(nearestGem, nearestResource));
        }

        if (nearestGem != null && !currentLocation.equals(nearestGem)) {
            TurnAction nextAction = mineopoly_three.competition.Utility.moveTowardsPoint(currentLocation, nearestGem);

            return nextAction;
        }

        if (nearestResource != null && !currentLocation.equals(nearestResource)) {
            TurnAction nextAction = mineopoly_three.competition.Utility.moveTowardsPoint(currentLocation, nearestResource);

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
        for (int i = 0; i < inventory.size(); i++) {
            if (!inventory.get(i).getItemType().equals(ItemType.AUTOMINER)) {
                inventory.remove(i);
                i--;
            }
        }
    }

    @Override
    public String getName() {
        return "Diamond Hands";
    }

    @Override
    public void endRound(int pointsScored, int opponentPointsScored) {
        this.pointsScored = pointsScored;
        this.opponentPointsScored = opponentPointsScored;
    }

    public Point gemVersusResource(Point nearestGem, Point nearestResource) {
        if (Utility.compareManhattanDistance(nearestGem, currentLocation, otherPlayerLocation) > 0) {
            return nearestResource;
        }
        TileType resourceTile = currentBoard.getTileTypeAtLocation(nearestResource);
        ItemType gemType = null;

        for (InventoryItem item: currentBoard.getItemsOnGround().get(nearestGem)) {
            if (!item.getItemType().equals(ItemType.AUTOMINER)) {
                gemType = item.getItemType();
            }
        }
        int gemTurns = DistanceUtil.getManhattanDistance(currentLocation, nearestGem);
        int resourceTurns = DistanceUtil.getManhattanDistance(currentLocation, nearestResource)
                + turnsToMineResource.get(resourceTile);
        int potentialGemValue = economy.getCurrentPrices().get(gemType) + gemTurns * gemIncreasePerTurn.get(gemType);
        if (gemType.equals(ItemType.RUBY) && potentialGemValue > 400) {
            potentialGemValue = 400;
        } else if (gemType.equals(ItemType.EMERALD) && potentialGemValue > 450) {
            potentialGemValue = 450;
        } else if (gemType.equals(ItemType.DIAMOND) && potentialGemValue > 500) {
            potentialGemValue = 500;
        }
        int potentialResourceValue = economy.getCurrentPrices().get(mineopoly_three.competition.Utility.convertTileTypeToItemType(resourceTile))
                + resourceTurns * gemIncreasePerTurn.get(mineopoly_three.competition.Utility.convertTileTypeToItemType(resourceTile));
        if (resourceTile.equals(TileType.RESOURCE_RUBY) && potentialResourceValue > 400) {
            potentialResourceValue = 400;
        } else if (resourceTile.equals(TileType.RESOURCE_EMERALD) && potentialResourceValue > 450) {
            potentialResourceValue = 450;
        } else if (resourceTile.equals(TileType.RESOURCE_DIAMOND) && potentialResourceValue > 500) {
            potentialResourceValue = 500;
        }
        if ((double) (potentialGemValue / gemTurns) > (double) (potentialResourceValue / resourceTurns)) {
            System.out.println("HERE");
            return nearestGem;
        }
        return nearestResource;
    }

    public boolean otherPlayerHasAutominer() {
        int mapAutominerCount = 0;
        for (Point point: currentBoard.getItemsOnGround().keySet()) {
            for (InventoryItem item: currentBoard.getItemsOnGround().get(point)) {
                if (item.getItemType().equals(ItemType.AUTOMINER)) {
                    mapAutominerCount += 1;
                }
            }
        }
        if (mapAutominerCount == 2) {
            return false;
        }

        int inventoryAutominerCount = 0;
        for (InventoryItem item: inventory) {
            if (item.getItemType().equals(ItemType.AUTOMINER)) {
                inventoryAutominerCount += 1;
            }
        }
        if (inventoryAutominerCount + mapAutominerCount == 2) {
            return false;
        }

        return true;
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

    public Point determineAutominerPlacement() {
        double maxRatio = 0;
        Point bestPoint = new Point();
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                Point tempPoint = new Point(col, row);
                int distanceThere = DistanceUtil.getManhattanDistance(currentLocation, tempPoint);
                if ((double) calculateThreeByThreeValue(tempPoint) / distanceThere > maxRatio) {
                    maxRatio = (double) calculateThreeByThreeValue(tempPoint) / distanceThere;
                    if (mineopoly_three.competition.Utility.tileInBoard(new Point(tempPoint.x + 1, tempPoint.y + 1), boardSize)) {
                        bestPoint = new Point(tempPoint.x + 1, tempPoint.y + 1);
                    }
                }
            }
        }
        return bestPoint;
    }

    public int calculateThreeByThreeValue(Point topLeftCorner) {
        int value = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Point tempPoint = new Point(topLeftCorner.x + col, topLeftCorner.y + row);
                if (!mineopoly_three.competition.Utility.tileInBoard(tempPoint, boardSize)) {
                    continue;
                }
                if (currentBoard.getTileTypeAtLocation(tempPoint)
                        .equals(TileType.RESOURCE_RUBY)) {
                    value += economy.getCurrentPrices().get(ItemType.RUBY);
                } else if (currentBoard.getTileTypeAtLocation(tempPoint)
                        .equals(TileType.RESOURCE_EMERALD)) {
                    value += economy.getCurrentPrices().get(ItemType.EMERALD);
                } else if (currentBoard.getTileTypeAtLocation(tempPoint)
                        .equals(TileType.RESOURCE_DIAMOND)) {
                    value += economy.getCurrentPrices().get(ItemType.DIAMOND);
                }
            }
        }
        return value;
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
        Point nearestItem = getFirstInstanceOfItem(item);
        if (nearestItem == null) {
            return null;
        }
        for (Point point: itemsOnGround.keySet()) {
            if (itemsOnGround.get(point).contains(item)
                    && mineopoly_three.competition.Utility.compareManhattanDistance(currentLocation, nearestItem, point) > 0) {
                nearestItem = point;
            }
        }

        return nearestItem;
    }

    public Point getNearestAvailableGem() {
        Point nearestGem = getFirstInstanceOfItem(ItemType.RUBY);
        if (nearestGem == null) {
            nearestGem = getFirstInstanceOfItem(ItemType.EMERALD);
        }
        if (nearestGem == null) {
            nearestGem = getFirstInstanceOfItem(ItemType.DIAMOND);
        }
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
            for (InventoryItem inventoryItem: itemsOnGround.get(point)) {
                if (inventoryItem.getItemType().equals(item)) {
                    return point;
                }
            }
        }

        return null;
    }
}