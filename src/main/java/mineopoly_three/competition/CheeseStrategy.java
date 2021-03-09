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
    private List<InventoryItem> otherPlayerInventory;
    private boolean isRedPlayer;
    private Point currentLocation;
    private Point otherPlayerLocation;
    private int currentScore;
    private Map<TileType, Integer> turnsToMineResource;
    private PlayerBoardView currentBoard;
    private Economy economy;
    private int currentCharge;
    private int otherPlayerCharge;
    private Set<ItemType> typesOfGems;
    private Point cheese;
    private int otherPlayerScore;
    private int numTurns;
    private int winningScore;
    private int maxCharge;
    private int maxInventorySize;
    private Point previousLocation;
    private Point otherPlayerPreviousLocation;
    private int stagnantTime;
    private Map<Point, List<InventoryItem>> itemsOnGround;
    private Map<ItemType, Integer> gemIncreasePerTurn;
    private int lossCount = 0;
    private int winCount = 0;
    private int autominerCount;
    private List<TurnAction> commandStack;

    public CheeseStrategy() { }

    @Override
    public void initialize(int boardSize, int maxInventorySize, int maxCharge, int winningScore, PlayerBoardView startingBoard, Point startTileLocation, boolean isRedPlayer, Random random) {
        this.boardSize = boardSize;
        gemIncreasePerTurn = new HashMap<>();
        gemIncreasePerTurn.put(ItemType.RUBY, 3);
        gemIncreasePerTurn.put(ItemType.EMERALD, 4);
        gemIncreasePerTurn.put(ItemType.DIAMOND, 5);
        this.commandStack = new ArrayList<>();
        this.previousLocation = new Point();
        this.otherPlayerPreviousLocation = new Point();
        this.stagnantTime = 0;
        this.numTurns = 0;
        this.otherPlayerCharge = maxCharge;
        this.maxInventorySize = maxInventorySize;
        this.maxCharge = maxCharge;
        this.winningScore = winningScore;
        this.inventory = new ArrayList<>();
        this.otherPlayerInventory = new ArrayList<>();
        this.isRedPlayer = isRedPlayer;
        this.currentScore = 0;
        this.otherPlayerScore = startingBoard.getOtherPlayerScore();
        this.currentBoard = startingBoard;
        this.currentLocation = startTileLocation;
        this.otherPlayerLocation = startingBoard.getOtherPlayerLocation();
        this.cheese = getCheeseResource();
        this.autominerCount = 0;

        turnsToMineResource = new HashMap<>();
        typesOfGems = new HashSet<>(Arrays.asList(ItemType.RUBY, ItemType.EMERALD, ItemType.DIAMOND));
        turnsToMineResource.put(TileType.RESOURCE_RUBY, 1);
        turnsToMineResource.put(TileType.RESOURCE_EMERALD, 2);
        turnsToMineResource.put(TileType.RESOURCE_DIAMOND, 3);
        if (winCount + lossCount == 30) {
            winCount = 0;
            lossCount = 0;
        }
    }

    @Override
    public TurnAction getTurnAction(PlayerBoardView boardView, Economy economy, int currentCharge, boolean isRedTurn) {
        this.numTurns += 1;
        this.previousLocation = this.currentLocation;
        this.otherPlayerPreviousLocation = this.otherPlayerLocation;
        this.otherPlayerScore = boardView.getOtherPlayerScore();
        this.currentBoard = boardView;
        this.economy = economy;
        this.itemsOnGround = boardView.getItemsOnGround();
        this.currentCharge = currentCharge;
        this.currentLocation = boardView.getYourLocation();
        this.otherPlayerLocation = boardView.getOtherPlayerLocation();

        if (otherPlayerPreviousLocation.equals(otherPlayerLocation) && otherPlayerCharge > 0) {
            if (currentBoard.getTileTypeAtLocation(otherPlayerLocation).equals(TileType.RESOURCE_RUBY)) {
                otherPlayerInventory.add(new InventoryItem(ItemType.RUBY));
            }
            if (currentBoard.getTileTypeAtLocation(otherPlayerLocation).equals(TileType.RESOURCE_EMERALD)) {
                otherPlayerInventory.add(new InventoryItem(ItemType.EMERALD));
            }
            if (currentBoard.getTileTypeAtLocation(otherPlayerLocation).equals(TileType.RESOURCE_DIAMOND)) {
                otherPlayerInventory.add(new InventoryItem(ItemType.DIAMOND));
            }
        } else {
            this.otherPlayerCharge -= 1;
        }

        if (!isRedPlayer) {
            if (currentBoard.getTileTypeAtLocation(otherPlayerLocation).equals(TileType.RED_MARKET)) {
                otherPlayerInventory.clear();
            }
        } else {
            if (currentBoard.getTileTypeAtLocation(otherPlayerLocation).equals(TileType.BLUE_MARKET)) {
                otherPlayerInventory.clear();
            }
        }

        if (currentBoard.getTileTypeAtLocation(otherPlayerLocation).equals(TileType.RECHARGE)) {
            otherPlayerCharge = maxCharge;
        }

        if (lossCount < 4) {

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
            if (currentBoard.getTileTypeAtLocation(currentLocation).equals(TileType.RECHARGE) && currentCharge < maxCharge) {
                return null;
            }
            if (currentScore > otherPlayerScore && otherPlayerScore == 0) {
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

                if (noResourcesOnBoard() && inventory.size() > 0) {
                    return moveToNearestMarketTile();
                }

                if (getValueOfInventory() + currentScore >= winningScore) {
                    return moveToNearestMarketTile();
                }

//                if (!commandStack.isEmpty()) {
//                    return commandStack.remove(commandStack.size() - 1);
//                }

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

                if (inventory.size() == maxInventorySize && currentCharge > DistanceUtil.getManhattanDistance
                        (getNearestMarketTile(currentLocation, isRedPlayer), currentLocation)
                        + DistanceUtil.getManhattanDistance(getNearestMarketTile(currentLocation, isRedPlayer), getNearestTilePoint(currentLocation, TileType.RECHARGE))) {
                    return moveToNearestMarketTile();
                }
                if (inventory.size() == maxInventorySize && boardSize >= 28) {
                    return moveToNearestMarketTile();
                }

                if (DistanceUtil.getManhattanDistance(currentLocation, getNearestTilePoint(currentLocation, TileType.RECHARGE)) < boardSize * 0.2
                        && currentCharge < maxCharge * 0.5) {
                    return moveTowardsPoint(getNearestTilePoint(currentLocation, TileType.RECHARGE));
                }

                if (currentCharge == 0) {
                    return moveTowardsPoint(getNearestTilePoint(currentLocation, TileType.RECHARGE));
                }

                if (getNearestTilePoint(currentLocation, currentResource) != null) {
                    if (!Utility.playerHasEnoughCharge(currentCharge, currentLocation,
                            getNearestTilePoint(currentLocation, currentResource), getNearestTilePoint(currentLocation, TileType.RECHARGE))) {
                        return moveTowardsPoint(getNearestTilePoint(currentLocation, TileType.RECHARGE));
                    }
                }

                if (currentLocation.equals(getNearestTilePoint(currentLocation, TileType.RECHARGE))) {
                    if (currentScore > otherPlayerScore + getValueOfOtherPlayerInventory() && otherPlayerInventory.size() == maxInventorySize) {
                        return blockCharger();
                    }
                }

                if (inventory.size() == maxInventorySize && boardSize < 28) {
                    return moveToNearestMarketTile();
                }

                if (currentBoard.getTileTypeAtLocation(currentLocation).equals(TileType.RECHARGE) && currentCharge == maxCharge) {
                    return moveToBetterQuadrant();
                }

                if (currentLocation.equals(getNearestTilePoint(currentLocation, TileType.RECHARGE)) && currentCharge < maxCharge) {
                    return null;
                }

                Point nearestResource = getNearestTilePoint(currentLocation, currentResource);
                Point nearestGem = getNearestAvailableGem();

                if (currentLocation.equals(nearestResource) && inventory.size() < maxInventorySize) {
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

                return null;
            }
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

            if (noResourcesOnBoard() && inventory.size() > 0) {
                return moveToNearestMarketTile();
            }

            if (getValueOfInventory() + currentScore >= winningScore) {
                return moveToNearestMarketTile();
            }

            if (autominerCount == 0 && otherPlayerHasAutominer()) {
                Point autominer = Utility.getNearestAutominer(currentLocation, boardView.getItemsOnGround());
                if (currentLocation.equals(autominer)) {
                    autominerCount += 1;
                    return TurnAction.PICK_UP_AUTOMINER;
                }
                if (autominer != null) {
                    return Utility.moveTowardsPoint(currentLocation, autominer);
                }
            }

            if (autominerInInventory() && currentScore > winningScore * 0.5) {
                inventory.clear();
                return TurnAction.PLACE_AUTOMINER;
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

            if (inventory.size() == maxInventorySize && currentCharge > DistanceUtil.getManhattanDistance
                    (getNearestMarketTile(currentLocation, isRedPlayer), currentLocation)
                    + DistanceUtil.getManhattanDistance(getNearestMarketTile(currentLocation, isRedPlayer), getNearestTilePoint(currentLocation, TileType.RECHARGE))) {
                return moveToNearestMarketTile();
            }
            if (inventory.size() == maxInventorySize && boardSize >= 28) {
                return moveToNearestMarketTile();
            }

            if (DistanceUtil.getManhattanDistance(currentLocation, getNearestTilePoint(currentLocation, TileType.RECHARGE)) < boardSize * 0.2
                    && currentCharge < maxCharge * 0.5) {
                return moveTowardsPoint(getNearestTilePoint(currentLocation, TileType.RECHARGE));
            }

            if (currentCharge == 0) {
                return moveTowardsPoint(getNearestTilePoint(currentLocation, TileType.RECHARGE));
            }

            if (getNearestTilePoint(currentLocation, currentResource) != null) {
                if (!Utility.playerHasEnoughCharge(currentCharge, currentLocation,
                        getNearestTilePoint(currentLocation, currentResource), getNearestTilePoint(currentLocation, TileType.RECHARGE))) {
                    return moveTowardsPoint(getNearestTilePoint(currentLocation, TileType.RECHARGE));
                }
            }

            if (currentLocation.equals(getNearestTilePoint(currentLocation, TileType.RECHARGE))) {
                if (currentScore > otherPlayerScore + getValueOfOtherPlayerInventory() && otherPlayerInventory.size() == maxInventorySize) {
                    return blockCharger();
                }
            }

            if (inventory.size() == maxInventorySize && boardSize < 28) {
                return moveToNearestMarketTile();
            }

            if (currentBoard.getTileTypeAtLocation(currentLocation).equals(TileType.RECHARGE) && currentCharge == maxCharge) {
                return moveToBetterQuadrant();
            }

            if (currentLocation.equals(getNearestTilePoint(currentLocation, TileType.RECHARGE)) && currentCharge < maxCharge) {
                return null;
            }

            Point nearestResource = getNearestTilePoint(currentLocation, currentResource);
            Point nearestGem = getNearestAvailableGem();

            if (currentLocation.equals(nearestResource) && inventory.size() < maxInventorySize) {
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
        if (opponentPointsScored > pointsScored) {
            lossCount++;
        } else {
            winCount++;
        }

    }

    public TurnAction moveToBetterQuadrant() {
        int quadrant;
        if (currentLocation.x >= boardSize/2 && currentLocation.y >= boardSize/2) {
            quadrant = 1;
        } else if (currentLocation.x <= boardSize/2 && currentLocation.y >= boardSize/2) {
            quadrant = 2;
        } else if (currentLocation.x <= boardSize/2 && currentLocation.y <= boardSize/2) {
            quadrant = 3;
        } else {
            quadrant = 4;
        }

        if (isRedPlayer) {
            if (currentBoard.getTileTypeAtLocation(currentLocation.x + 1, currentLocation.y).equals(TileType.RECHARGE)
                    && currentBoard.getTileTypeAtLocation(currentLocation.x, currentLocation.y - 1).equals(TileType.RECHARGE)) {
                if (numberOfTilesInQuadrant(1) > numberOfTilesInQuadrant(3) + 2) {
                    return TurnAction.MOVE_DOWN;
                } else {
                    return TurnAction.MOVE_UP;
                }
            } else if (currentBoard.getTileTypeAtLocation(currentLocation.x + 1, currentLocation.y).equals(TileType.RECHARGE)
                    && currentBoard.getTileTypeAtLocation(currentLocation.x, currentLocation.y + 1).equals(TileType.RECHARGE)) {
                if (numberOfTilesInQuadrant(1) > numberOfTilesInQuadrant(3) + 2) {
                    return TurnAction.MOVE_RIGHT;
                } else {
                    return TurnAction.MOVE_UP;
                }
            } else if (currentBoard.getTileTypeAtLocation(currentLocation.x - 1, currentLocation.y).equals(TileType.RECHARGE)
                    && currentBoard.getTileTypeAtLocation(currentLocation.x, currentLocation.y - 1).equals(TileType.RECHARGE)) {
                if (numberOfTilesInQuadrant(1) > numberOfTilesInQuadrant(3) + 2) {
                    return TurnAction.MOVE_DOWN;
                } else {
                    return TurnAction.MOVE_LEFT;
                }
            } else if (currentBoard.getTileTypeAtLocation(currentLocation.x - 1, currentLocation.y).equals(TileType.RECHARGE)
                    && currentBoard.getTileTypeAtLocation(currentLocation.x, currentLocation.y + 1).equals(TileType.RECHARGE)) {
                if (numberOfTilesInQuadrant(1) > numberOfTilesInQuadrant(3) + 2) {
                    return TurnAction.MOVE_DOWN;
                } else {
                    return TurnAction.MOVE_UP;
                }
            }
        } else {
            if (currentBoard.getTileTypeAtLocation(currentLocation.x + 1, currentLocation.y).equals(TileType.RECHARGE)
                    && currentBoard.getTileTypeAtLocation(currentLocation.x, currentLocation.y - 1).equals(TileType.RECHARGE)) {
                if (numberOfTilesInQuadrant(2) > numberOfTilesInQuadrant(4) + 2) {
                    return TurnAction.MOVE_DOWN;
                } else {
                    return TurnAction.MOVE_RIGHT;
                }
            } else if (currentBoard.getTileTypeAtLocation(currentLocation.x + 1, currentLocation.y).equals(TileType.RECHARGE)
                    && currentBoard.getTileTypeAtLocation(currentLocation.x, currentLocation.y + 1).equals(TileType.RECHARGE)) {
                if (numberOfTilesInQuadrant(2) > numberOfTilesInQuadrant(4) + 2) {
                    return TurnAction.MOVE_DOWN;
                } else {
                    return TurnAction.MOVE_UP;
                }
            } else if (currentBoard.getTileTypeAtLocation(currentLocation.x - 1, currentLocation.y).equals(TileType.RECHARGE)
                    && currentBoard.getTileTypeAtLocation(currentLocation.x, currentLocation.y - 1).equals(TileType.RECHARGE)) {
                if (numberOfTilesInQuadrant(2) > numberOfTilesInQuadrant(4) + 2) {
                    return TurnAction.MOVE_DOWN;
                } else {
                    return TurnAction.MOVE_UP;
                }
            } else if (currentBoard.getTileTypeAtLocation(currentLocation.x - 1, currentLocation.y).equals(TileType.RECHARGE)
                    && currentBoard.getTileTypeAtLocation(currentLocation.x, currentLocation.y + 1).equals(TileType.RECHARGE)) {
                if (numberOfTilesInQuadrant(2) > numberOfTilesInQuadrant(4) + 2) {
                    return TurnAction.MOVE_LEFT;
                } else {
                    return TurnAction.MOVE_UP;
                }
            }
        }
        return moveToNearestMarketTile();
    }

    //            if (quadrant == 3 && numberOfTilesInQuadrant(quadrant) < numberOfTilesInQuadrant(1)) {
//                return moveToFarthestCharger();
//            } else if (quadrant == 1 && numberOfTilesInQuadrant(quadrant) < numberOfTilesInQuadrant(3)) {
//                return moveToFarthestCharger();
//            } else {
//                return moveToNearestMarketTile();
//            }
//        } else {
//            if (quadrant == 4 && numberOfTilesInQuadrant(quadrant) < numberOfTilesInQuadrant(2)) {
//                return moveToFarthestCharger();
//            } else if (quadrant == 2 && numberOfTilesInQuadrant(quadrant) < numberOfTilesInQuadrant(4)) {
//                return moveToFarthestCharger();
//            } else {
//                return moveToNearestMarketTile();
//            }
//        }
//        if (isRedPlayer) {
//            if (quadrant == 3 && numberOfTilesInQuadrant(quadrant) < numberOfTilesInQuadrant(1)) {
//                commandStack.add(TurnAction.MOVE_DOWN);
//                commandStack.add(TurnAction.MOVE_DOWN);
//                commandStack.add(TurnAction.MOVE_RIGHT);
//                commandStack.add(TurnAction.MOVE_RIGHT);
//            } else if (quadrant == 1 && numberOfTilesInQuadrant(quadrant) < numberOfTilesInQuadrant(3)) {
//                commandStack.add(TurnAction.MOVE_LEFT);
//                commandStack.add(TurnAction.MOVE_LEFT);
//                commandStack.add(TurnAction.MOVE_UP);
//                commandStack.add(TurnAction.MOVE_UP);
//            }
//        } else {
//            if (quadrant == 4 && numberOfTilesInQuadrant(quadrant) < numberOfTilesInQuadrant(2)) {
//                commandStack.add(TurnAction.MOVE_DOWN);
//                commandStack.add(TurnAction.MOVE_DOWN);
//                commandStack.add(TurnAction.MOVE_LEFT);
//                commandStack.add(TurnAction.MOVE_LEFT);
//            } else if (quadrant == 2 && numberOfTilesInQuadrant(quadrant) < numberOfTilesInQuadrant(4)) {
//                commandStack.add(TurnAction.MOVE_UP);
//                commandStack.add(TurnAction.MOVE_UP);
//                commandStack.add(TurnAction.MOVE_RIGHT);
//                commandStack.add(TurnAction.MOVE_RIGHT);
//            }
//        }

    public TurnAction moveToFarthestCharger() {
        Point farthestCharger = getFirstInstanceOfTile(TileType.RECHARGE);
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                Point temp = new Point(col, row);
                if (currentBoard.getTileTypeAtLocation(temp).equals(TileType.RECHARGE)) {
                    if (Utility.compareManhattanDistance(currentLocation, temp, farthestCharger) > 0) {
                        farthestCharger = temp;
                    }
                }
            }
        }
        return moveTowardsPoint(farthestCharger);
    }

    public int numberOfTilesInQuadrant(int quadrant) {
        int count = 0;
        if (quadrant == 1) {
            for (int row = 0; row <= boardSize/2; row++) {
                for (int col = boardSize/2; col < boardSize; col++) {
                    Point temp = new Point(col, row);
                    if (currentBoard.getTileTypeAtLocation(temp).equals(TileType.RESOURCE_RUBY)
                            || currentBoard.getTileTypeAtLocation(temp).equals(TileType.RESOURCE_EMERALD)
                            || currentBoard.getTileTypeAtLocation(temp).equals(TileType.RESOURCE_DIAMOND)) {
                        count++;
                    }
                }
            }
        } else if (quadrant == 2) {
            for (int row = 0; row <= boardSize/2; row++) {
                for (int col = 0; col <= boardSize/2; col++) {
                    Point temp = new Point(col, row);
                    if (currentBoard.getTileTypeAtLocation(temp).equals(TileType.RESOURCE_RUBY)
                            || currentBoard.getTileTypeAtLocation(temp).equals(TileType.RESOURCE_EMERALD)
                            || currentBoard.getTileTypeAtLocation(temp).equals(TileType.RESOURCE_DIAMOND)) {
                        count++;
                    }
                }
            }
        } else if (quadrant == 3) {
            for (int row = boardSize/2; row < boardSize; row++) {
                for (int col = 0; col < boardSize/2; col++) {
                    Point temp = new Point(col, row);
                    if (currentBoard.getTileTypeAtLocation(temp).equals(TileType.RESOURCE_RUBY)
                            || currentBoard.getTileTypeAtLocation(temp).equals(TileType.RESOURCE_EMERALD)
                            || currentBoard.getTileTypeAtLocation(temp).equals(TileType.RESOURCE_DIAMOND)) {
                        count++;
                    }
                }
            }
        } else {
            for (int row = boardSize/2; row < boardSize; row++) {
                for (int col = boardSize/2; col < boardSize; col++) {
                    Point temp = new Point(col, row);
                    if (currentBoard.getTileTypeAtLocation(temp).equals(TileType.RESOURCE_RUBY)
                            || currentBoard.getTileTypeAtLocation(temp).equals(TileType.RESOURCE_EMERALD)
                            || currentBoard.getTileTypeAtLocation(temp).equals(TileType.RESOURCE_DIAMOND)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public boolean autominerInInventory() {
        for (InventoryItem item: inventory) {
            if (item.getItemType().equals(ItemType.AUTOMINER)) {
                return true;
            }
        }

        return false;
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
        return !(inventoryAutominerCount + mapAutominerCount == 2);
    }

    public TurnAction blockCharger() {
        if (currentLocation.y < otherPlayerLocation.y) {
            if (currentBoard.getTileTypeAtLocation(currentLocation.x, currentLocation.y + 1).equals(TileType.RECHARGE)) {
                return TurnAction.MOVE_UP;
            }
        } else if (currentLocation.y > otherPlayerLocation.y) {
            if (currentBoard.getTileTypeAtLocation(currentLocation.x, currentLocation.y - 1).equals(TileType.RECHARGE)) {
                return TurnAction.MOVE_DOWN;
            }
        } else if (currentLocation.x > otherPlayerLocation.x) {
            if (currentBoard.getTileTypeAtLocation(currentLocation.x - 1, currentLocation.y).equals(TileType.RECHARGE)) {
                return TurnAction.MOVE_LEFT;
            }
        } else if (currentLocation.x < otherPlayerLocation.x) {
            if (currentBoard.getTileTypeAtLocation(currentLocation.x + 1, currentLocation.y).equals(TileType.RECHARGE)) {
                return TurnAction.MOVE_RIGHT;
            }
        }

        return null;
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
            rubyPoint = new Point(Integer.MAX_VALUE/3, Integer.MAX_VALUE/3);
        }
        if (emeraldPoint == null) {
            emeraldPoint = new Point(Integer.MAX_VALUE/3, Integer.MAX_VALUE/3);
        }
        if (diamondPoint == null) {
            diamondPoint = new Point(Integer.MAX_VALUE/3, Integer.MAX_VALUE/3);
        }

        int rubyDistance = DistanceUtil.getManhattanDistance(currentLocation, rubyPoint);
        int emeraldDistance = DistanceUtil.getManhattanDistance(currentLocation, emeraldPoint);
        int diamondDistance = DistanceUtil.getManhattanDistance(currentLocation, diamondPoint);
        if (inventory.size() == maxInventorySize - 1) {
            rubyDistance += DistanceUtil.getManhattanDistance(rubyPoint, getNearestMarketTile(rubyPoint, isRedPlayer));
            emeraldDistance += DistanceUtil.getManhattanDistance(emeraldPoint, getNearestMarketTile(emeraldPoint, isRedPlayer));
            diamondDistance += DistanceUtil.getManhattanDistance(diamondPoint, getNearestMarketTile(diamondPoint, isRedPlayer));
        } else {
            rubyDistance += DistanceUtil.getManhattanDistance(rubyPoint, getNearestResource(rubyPoint));
            emeraldDistance += DistanceUtil.getManhattanDistance(emeraldPoint, getNearestResource(emeraldPoint));
            diamondDistance += DistanceUtil.getManhattanDistance(diamondPoint, getNearestResource(diamondPoint));
        }

//        if (inventory.size() + 1 == maxInventorySize) {
//            if (isRedPlayer) {
//                rubyDistance += DistanceUtil.getManhattanDistance(rubyPoint, getNearestTilePoint(rubyPoint, TileType.RED_MARKET));
//                emeraldDistance += DistanceUtil.getManhattanDistance(emeraldPoint, getNearestTilePoint(emeraldPoint, TileType.RED_MARKET));
//                diamondDistance += DistanceUtil.getManhattanDistance(diamondPoint, getNearestTilePoint(diamondPoint, TileType.RED_MARKET));
//            } else {
//                rubyDistance += DistanceUtil.getManhattanDistance(rubyPoint, getNearestTilePoint(rubyPoint, TileType.BLUE_MARKET));
//                emeraldDistance += DistanceUtil.getManhattanDistance(emeraldPoint, getNearestTilePoint(emeraldPoint, TileType.BLUE_MARKET));
//                diamondDistance += DistanceUtil.getManhattanDistance(diamondPoint, getNearestTilePoint(diamondPoint, TileType.BLUE_MARKET));
//            }
//        }
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

    public Point getNearestResource(Point exclude) {
        Point toReturn = new Point(Integer.MAX_VALUE/3, Integer.MAX_VALUE/3);
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                Point temp = new Point(col, row);
                if (currentBoard.getTileTypeAtLocation(temp).equals(TileType.RESOURCE_RUBY)
                        || currentBoard.getTileTypeAtLocation(temp).equals(TileType.RESOURCE_EMERALD)
                        || currentBoard.getTileTypeAtLocation(temp).equals(TileType.RESOURCE_DIAMOND)) {
                    if (Utility.compareManhattanDistance(exclude, toReturn, temp) > 0 && !temp.equals(exclude)) {
                        toReturn = temp;
                    }
                }
            }
        }
        return toReturn;
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

    public int getValueOfOtherPlayerInventory() {
        int value = 0;
        for (InventoryItem item: otherPlayerInventory) {
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
