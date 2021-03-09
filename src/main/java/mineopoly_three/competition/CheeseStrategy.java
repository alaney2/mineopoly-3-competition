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

public class CheeseStrategy implements MinePlayerStrategy {
    public int boardSize;
    private List<InventoryItem> inventory;
    private boolean isRedPlayer;
    private Point currentLocation;
    private Point otherPlayerLocation;
    private int currentScore;
    private Map<TileType, Integer> turnsToMineResource;
    private PlayerBoardView currentBoard;
    private Economy economy;
    private int currentCharge;
    private Set<ItemType> typesOfGems;
    private Point cheese;
    private int otherPlayerScore;
    private int numTurns;
    private int winningScore;
    private int maxCharge;
    private int maxInventorySize;
    private Point previousLocation;
    private int stagnantTime;
    private Map<Point, List<InventoryItem>> itemsOnGround;
    private Map<ItemType, Integer> gemIncreasePerTurn;

    public CheeseStrategy() { }

    @Override
    public void initialize(int boardSize, int maxInventorySize, int maxCharge, int winningScore, PlayerBoardView startingBoard, Point startTileLocation, boolean isRedPlayer, Random random) {
        this.boardSize = boardSize;
        gemIncreasePerTurn = new HashMap<>();
        gemIncreasePerTurn.put(ItemType.RUBY, 3);
        gemIncreasePerTurn.put(ItemType.EMERALD, 4);
        gemIncreasePerTurn.put(ItemType.DIAMOND, 5);
        this.previousLocation = new Point();
        this.stagnantTime = 0;
        this.numTurns = 0;
        this.maxInventorySize = maxInventorySize;
        this.maxCharge = maxCharge;
        this.winningScore = winningScore;
        this.inventory = new ArrayList<>();
        this.isRedPlayer = isRedPlayer;
        this.currentScore = 0;
        this.otherPlayerScore = startingBoard.getOtherPlayerScore();
        this.currentBoard = startingBoard;
        this.currentLocation = startTileLocation;
        this.otherPlayerLocation = startingBoard.getOtherPlayerLocation();
        this.cheese = getCheeseResource();


        turnsToMineResource = new HashMap<>();
        typesOfGems = new HashSet<>(Arrays.asList(ItemType.RUBY, ItemType.EMERALD, ItemType.DIAMOND));
        turnsToMineResource.put(TileType.RESOURCE_RUBY, 1);
        turnsToMineResource.put(TileType.RESOURCE_EMERALD, 2);
        turnsToMineResource.put(TileType.RESOURCE_DIAMOND, 3);
    }

    @Override
    public TurnAction getTurnAction(PlayerBoardView boardView, Economy economy, int currentCharge, boolean isRedTurn) {
        this.numTurns += 1;
        this.previousLocation = this.currentLocation;
        this.otherPlayerScore = boardView.getOtherPlayerScore();
        this.currentBoard = boardView;
        this.economy = economy;
        this.itemsOnGround = boardView.getItemsOnGround();
        this.currentCharge = currentCharge;
        this.currentLocation = boardView.getYourLocation();
        this.otherPlayerLocation = boardView.getOtherPlayerLocation();

        if (currentScore == 0 && !currentLocation.equals(cheese) && inventory.size() == 0) {
            return moveTowardsPoint(cheese);
        }
        if (currentScore == 0 && inventory.size() > 0) {
            return moveTowardsPoint(getNearestMarketTile(currentLocation, isRedPlayer));
        }
        if (currentLocation.equals(cheese) && currentLocationIsResourceTile()) {
            return TurnAction.MINE;
        }
        if (currentLocation.equals(cheese) && currentLocationHasGem()) {
            return TurnAction.PICK_UP_RESOURCE;
        }
        if (currentScore > otherPlayerScore) {
            if (!currentLocation.equals(getNearestMarketTile(otherPlayerLocation, !isRedPlayer))) {
                return moveTowardsPoint(getNearestMarketTile(otherPlayerLocation, !isRedPlayer));
            }
            if (currentLocation.equals(getNearestMarketTile(otherPlayerLocation, !isRedPlayer))
                    && DistanceUtil.getManhattanDistance(currentLocation, otherPlayerLocation) > 3
                    && currentCharge > 15) {
                double rand = Math.random();
                if (rand < 0.5) {
                    return moveTowardsPoint(otherPlayerLocation);
                }
            }
            return null;
        } else {
            if (isRedPlayer) {
                if (DistanceUtil.getManhattanDistance(currentLocation, getNearestTilePoint(currentLocation, TileType.RED_MARKET)) >= 1000 - numTurns) {
                    return moveToNearestMarketTile();
                }
            } else {
                if (DistanceUtil.getManhattanDistance(currentLocation, getNearestTilePoint(currentLocation, TileType.BLUE_MARKET)) >= 1000 - numTurns) {
                    return moveToNearestMarketTile();
                }
            }

            if (getValueOfInventory() + currentScore >= winningScore) {
                return moveToNearestMarketTile();
            }

            TileType currentResource = calculateOptimalResource();

            if (currentLocationHasGem() && inventory.size() < maxInventorySize) {
                return TurnAction.PICK_UP_RESOURCE;
            }

            if (currentLocation.equals(previousLocation)) {
                stagnantTime += 1;
                if (stagnantTime >= 10 && currentScore <= otherPlayerScore) {
                    return actionIfOtherPlayerInWay();
                }
            } else {
                stagnantTime = 0;
            }

            if (DistanceUtil.getManhattanDistance(currentLocation, getNearestTilePoint(currentLocation, TileType.RECHARGE)) < boardSize / 4
                    && currentCharge < maxCharge/2) {
                return moveTowardsPoint(getNearestTilePoint(currentLocation, TileType.RECHARGE));
            }

            if (getNearestTilePoint(currentLocation, currentResource) != null) {
                if (!Utility.playerHasEnoughCharge(currentCharge, currentLocation,
                        getNearestTilePoint(currentLocation, currentResource), getNearestTilePoint(currentLocation, TileType.RECHARGE))) {
                    return moveTowardsPoint(getNearestTilePoint(currentLocation, TileType.RECHARGE));
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

                return moveTowardsPoint(gemVersusResource(nearestGem, nearestResource));
            }

            if (nearestGem != null && !currentLocation.equals(nearestGem)) {
                return moveTowardsPoint(nearestGem);
            }

            if (nearestResource != null && !currentLocation.equals(nearestResource)) {
                return moveTowardsPoint(nearestResource);
            }

            if (noResourcesOnBoard() && inventory.size() > 0) {
                return moveToNearestMarketTile();
            }
            return null;
        }
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
        return "Diamond Hands";
    }

    @Override
    public void endRound(int pointsScored, int opponentPointsScored) {
        inventory.clear();
        currentScore = 0;
    }

    public boolean noResourcesOnBoard() {
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                Point temp = new Point(col, row);
                if (currentBoard.getTileTypeAtLocation(temp).equals(TileType.RESOURCE_RUBY)
                        || currentBoard.getTileTypeAtLocation(temp).equals(TileType.RESOURCE_EMERALD)
                        || currentBoard.getTileTypeAtLocation(temp).equals(TileType.RESOURCE_DIAMOND)) {
                    return false;
                }
            }
        }

        return true;
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
        if (gemType == null) {
            return nearestResource;
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
        int potentialResourceValue = economy.getCurrentPrices().get(Utility.convertTileTypeToItemType(resourceTile))
                + resourceTurns * gemIncreasePerTurn.get(Utility.convertTileTypeToItemType(resourceTile));
        if (resourceTile.equals(TileType.RESOURCE_RUBY) && potentialResourceValue > 400) {
            potentialResourceValue = 400;
        } else if (resourceTile.equals(TileType.RESOURCE_EMERALD) && potentialResourceValue > 450) {
            potentialResourceValue = 450;
        } else if (resourceTile.equals(TileType.RESOURCE_DIAMOND) && potentialResourceValue > 500) {
            potentialResourceValue = 500;
        }
        if ((double) (potentialGemValue / gemTurns) > (double) (potentialResourceValue / resourceTurns)) {
            return nearestGem;
        }

        return nearestResource;
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

    public int getValueOfInventory() {
        int value = 0;
        for (InventoryItem item: inventory) {
            if (item.getItemType().equals(ItemType.RUBY)) {
                value += economy.getCurrentPrices().get(ItemType.RUBY);
            } else if (item.getItemType().equals(ItemType.EMERALD)) {
                value += economy.getCurrentPrices().get(ItemType.EMERALD);
            } else if (item.getItemType().equals(ItemType.DIAMOND)) {
                value += economy.getCurrentPrices().get(ItemType.DIAMOND);
            }
        }

        return value;
    }

    public TurnAction moveToNearestMarketTile() {
        if (isRedPlayer) {
            if (!otherPlayerLocation.equals(getNearestTilePoint(currentLocation, TileType.RED_MARKET))) {
                return moveTowardsPoint(getNearestTilePoint(currentLocation, TileType.RED_MARKET));
            }
            return moveTowardsPoint(getFarthestTilePoint(currentLocation, TileType.RED_MARKET));
        }
        if (!otherPlayerLocation.equals(getNearestTilePoint(currentLocation, TileType.BLUE_MARKET))) {
            return moveTowardsPoint(getNearestTilePoint(currentLocation, TileType.BLUE_MARKET));
        }
        return moveTowardsPoint(getFarthestTilePoint(currentLocation, TileType.BLUE_MARKET));
    }

    public Point getFarthestTilePoint(Point currentLocation, TileType tile) {
        Point farthestTile = getFirstInstanceOfTile(tile);
        if (farthestTile == null) {
            return null;
        }
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (currentBoard.getTileTypeAtLocation(col, row).equals(tile)
                        && Utility.compareManhattanDistance(currentLocation, farthestTile, new Point(col, row)) <= 0) {
                    farthestTile = new Point(col, row);
                }
            }
        }

        return farthestTile;
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

    public boolean currentLocationIsResourceTile() {
        return currentBoard.getTileTypeAtLocation(currentLocation).equals(TileType.RESOURCE_RUBY)
                || currentBoard.getTileTypeAtLocation(currentLocation).equals(TileType.RESOURCE_EMERALD)
                || currentBoard.getTileTypeAtLocation(currentLocation).equals(TileType.RESOURCE_DIAMOND);
    }

    public TurnAction moveTowardsPoint(Point point) {
        if (point == null) {
            return null;
        }
        int xdiff = Math.abs(currentLocation.x - point.x);
        int ydiff = Math.abs(currentLocation.y - point.y);
        if (xdiff == 0 && ydiff == 0) {
            return null;
        }
        if (xdiff >= ydiff) {
            if (currentLocation.x < point.x) {
                return TurnAction.MOVE_RIGHT;
            }
            return TurnAction.MOVE_LEFT;
        } else {
            if (currentLocation.y < point.y) {
                return TurnAction.MOVE_UP;
            }
            return TurnAction.MOVE_DOWN;
        }
    }

    public Point getNearestTilePoint(Point currentLocation, TileType tile) {
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

    public Point getCheeseResource() {
        int rubyTurns = 0;
        int emeraldTurns = 0;
        int diamondTurns = 0;
        Point rubyPoint = getNearestTilePoint(currentLocation, TileType.RESOURCE_RUBY);
        Point emeraldPoint = getNearestTilePoint(currentLocation, TileType.RESOURCE_EMERALD);
        Point diamondPoint = getNearestTilePoint(currentLocation, TileType.RESOURCE_DIAMOND);
        rubyTurns += DistanceUtil.getManhattanDistance(currentLocation, rubyPoint)
                + DistanceUtil.getManhattanDistance(currentLocation, getNearestMarketTile(otherPlayerLocation, !isRedPlayer)) + 1;
        emeraldTurns += DistanceUtil.getManhattanDistance(currentLocation, emeraldPoint)
                + DistanceUtil.getManhattanDistance(currentLocation, getNearestMarketTile(otherPlayerLocation, !isRedPlayer)) + 2;
        diamondTurns += DistanceUtil.getManhattanDistance(currentLocation, diamondPoint)
                + DistanceUtil.getManhattanDistance(currentLocation, getNearestMarketTile(otherPlayerLocation, !isRedPlayer)) + 3;
        if (rubyTurns <= emeraldTurns && rubyTurns <= diamondTurns) {
            return rubyPoint;
        } else if (emeraldTurns <= diamondTurns) {
            return emeraldPoint;
        } else {
            return diamondPoint;
        }
    }

    public Point getNearestMarketTile(Point currentLocation, boolean isRedPlayer) {
        TileType marketTile;
        if (isRedPlayer) {
            marketTile = TileType.RED_MARKET;
        } else {
            marketTile = TileType.BLUE_MARKET;
        }
        Point marketPoint = getFirstInstanceOfTile(marketTile);
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                Point temp = new Point(col, row);
                if (currentBoard.getTileTypeAtLocation(temp).equals(marketTile)
                        && DistanceUtil.getManhattanDistance(currentLocation, temp) < DistanceUtil.getManhattanDistance(currentLocation, marketPoint)) {
                    {
                        marketPoint = temp;
                    }
                }
            }
        }

        return marketPoint;
    }
}
