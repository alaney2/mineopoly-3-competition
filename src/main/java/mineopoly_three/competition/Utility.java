package mineopoly_three.competition;

import mineopoly_three.action.TurnAction;
import mineopoly_three.game.Economy;
import mineopoly_three.item.ItemType;
import mineopoly_three.strategy.PlayerBoardView;
import mineopoly_three.tiles.TileType;
import mineopoly_three.util.DistanceUtil;

import java.awt.*;

public class Utility {
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
        if (currentCharge <= distanceToNearestCharger + distanceToDestination) {
            return false;
        }

        return true;
    }

    public static TurnAction moveTowardsTile(Point currentLocation, Point point) {
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
