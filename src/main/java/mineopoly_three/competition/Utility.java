package mineopoly_three.competition;

import mineopoly_three.action.TurnAction;
import mineopoly_three.game.Economy;
import mineopoly_three.item.InventoryItem;
import mineopoly_three.item.ItemType;
import mineopoly_three.strategy.PlayerBoardView;
import mineopoly_three.tiles.Tile;
import mineopoly_three.tiles.TileType;
import mineopoly_three.util.DistanceUtil;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class Utility {
    public static Point getFirstInstanceOfItem(ItemType item, Map<Point, List<InventoryItem>> itemsOnGround) {
        for (Point point: itemsOnGround.keySet()) {
            for (InventoryItem inventoryItem: itemsOnGround.get(point)) {
                if (inventoryItem.getItemType().equals(item)) {
                    return point;
                }
            }
        }

        return null;
    }

    public static boolean tileInBoard(Point point, int boardSize) {
        if (point.x < 0 || point.y < 0) {
            return false;
        } else return point.x < boardSize && point.y < boardSize;
    }

    public static Point getNearestAutominer(Point currentLocation, Map<Point, List<InventoryItem>> itemsOnGround) {
        Point autominer = getFirstInstanceOfItem(ItemType.AUTOMINER, itemsOnGround);
        if (autominer == null) {
            return null;
        }
        for (Point point: itemsOnGround.keySet()) {
            for (InventoryItem item: itemsOnGround.get(point)) {
                if (item.getItemType().equals(ItemType.AUTOMINER) && compareManhattanDistance(currentLocation, autominer, point) > 0) {
                    autominer = point;
                }
            }
        }

        return autominer;
    }

    public static TileType determineMostExpensiveResourceNotDiamond(Economy economy) {
        ItemType mostExpensiveResource = ItemType.RUBY;
        for (ItemType item: economy.getCurrentPrices().keySet()) {
            if (item.equals(ItemType.DIAMOND)) {
                continue;
            }
            if (economy.getCurrentPrices().get(item) > economy.getCurrentPrices().get(mostExpensiveResource)) {
                mostExpensiveResource = item;
            }
        }
        return convertItemTypeToTileType(mostExpensiveResource);
    }

    public static TileType determineMostExpensiveResource(Economy economy) {
        ItemType mostExpensiveResource = ItemType.RUBY;
        for (ItemType item: economy.getCurrentPrices().keySet()) {
            if (economy.getCurrentPrices().get(item) > economy.getCurrentPrices().get(mostExpensiveResource)) {
                mostExpensiveResource = item;
            }
        }
        return convertItemTypeToTileType(mostExpensiveResource);
    }

    public static TileType convertItemTypeToTileType(ItemType item) {
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

    public static ItemType convertTileTypeToItemType(TileType tile) {
        if (tile.equals(TileType.RESOURCE_RUBY)) {
            return ItemType.RUBY;
        } else if (tile.equals(TileType.RESOURCE_EMERALD)) {
            return ItemType.EMERALD;
        } else if (tile.equals(TileType.RESOURCE_DIAMOND)) {
            return ItemType.DIAMOND;
        } else {
            return ItemType.AUTOMINER;
        }
    }

    public static boolean tileExists(TileType tile, PlayerBoardView currentBoard, int boardSize) {
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (currentBoard.getTileTypeAtLocation(col, row).equals(tile)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean playerHasEnoughCharge(int currentCharge, Point currentLocation, Point destination, Point chargerLocation) {
        int distanceToNearestCharger = DistanceUtil.getManhattanDistance(currentLocation, chargerLocation);
        int distanceToDestination = DistanceUtil.getManhattanDistance(currentLocation, destination);
        return currentCharge > distanceToNearestCharger + distanceToDestination;
    }

//    public static TurnAction moveTowardsPoint(Point currentLocation, Point point) {
//        if (point == null) {
//            return null;
//        }
//        if (currentLocation.x < point.x) {
//            return TurnAction.MOVE_RIGHT;
//        } else if (currentLocation.y < point.y) {
//            return TurnAction.MOVE_UP;
//        } else if (currentLocation.x > point.x) {
//            return TurnAction.MOVE_LEFT;
//        } else if (currentLocation.y > point.y){
//            return TurnAction.MOVE_DOWN;
//        } else {
//            return null;
//        }
//    }

    public static TurnAction moveTowardsPoint(Point currentLocation, Point point) {
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

    public static int compareManhattanDistance(Point start, Point p1, Point p2) {
        return DistanceUtil.getManhattanDistance(start, p1) - DistanceUtil.getManhattanDistance(start, p2);
    }

    public static boolean isOtherPlayerInWay(TurnAction nextAction, Point currentLocation, Point otherPlayerLocation) {
        switch (nextAction) {
            case MOVE_UP:
                if (currentLocation.y == otherPlayerLocation.y - 1 && currentLocation.x == otherPlayerLocation.x) {
                    return true;
                }

            case MOVE_DOWN:
                if (currentLocation.y == otherPlayerLocation.y + 1 && currentLocation.x == otherPlayerLocation.x) {
                    return true;
                }

            case MOVE_LEFT:
                if (currentLocation.x == otherPlayerLocation.x - 1 && currentLocation.y == otherPlayerLocation.y) {
                    return true;
                }

            case MOVE_RIGHT:
                if (currentLocation.x == otherPlayerLocation.x + 1 && currentLocation.y == otherPlayerLocation.y) {
                    return true;
                }

            default:
                return false;
        }
    }
}
