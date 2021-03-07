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


    @Override
    public void initialize(int boardSize, int maxInventorySize, int maxCharge, int winningScore, PlayerBoardView startingBoard, Point startTileLocation, boolean isRedPlayer, Random random) {
        this.boardSize = boardSize;
        this.inventory = new ArrayList<>();
        this.isRedPlayer = isRedPlayer;
        this.currentScore = 0;
        this.currentBoard = startingBoard;
        this.currentLocation = startTileLocation;
        this.otherPlayerLocation = startingBoard.getOtherPlayerLocation();
        this.cheese = getCheeseResource();

        turnsToMineResource = new HashMap<>();
        typesOfGems = new HashSet<>(Arrays.asList(ItemType.RUBY, ItemType.EMERALD, ItemType.DIAMOND));
        turnsToMineResource.put(TileType.RESOURCE_RUBY, 1);
        turnsToMineResource.put(TileType.RESOURCE_EMERALD, 2);
        turnsToMineResource.put(TileType.RESOURCE_DIAMOND, 3);
        System.out.println(cheese);
    }

    @Override
    public TurnAction getTurnAction(PlayerBoardView boardView, Economy economy, int currentCharge, boolean isRedTurn) {
        this.currentBoard = boardView;
        this.economy = economy;
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
        if (!currentLocation.equals(getNearestMarketTile(otherPlayerLocation, !isRedPlayer))) {
            return moveTowardsPoint(getNearestMarketTile(otherPlayerLocation, !isRedPlayer));
        }
        if (currentLocation.equals(getNearestMarketTile(otherPlayerLocation, !isRedPlayer))
                && DistanceUtil.getManhattanDistance(currentLocation, otherPlayerLocation) > 2
                && currentCharge > 1) {
            return moveTowardsPoint(otherPlayerLocation);
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
        return "Cheese";
    }

    @Override
    public void endRound(int pointsScored, int opponentPointsScored) {
        inventory.clear();
        currentScore = 0;
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
