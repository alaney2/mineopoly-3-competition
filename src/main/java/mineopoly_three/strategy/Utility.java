package mineopoly_three.strategy;

import mineopoly_three.action.TurnAction;
import mineopoly_three.game.Economy;
import mineopoly_three.item.ItemType;
import mineopoly_three.tiles.TileType;
import mineopoly_three.util.DistanceUtil;

import java.awt.*;

public class Utility {
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

    public static boolean playerHasEnoughCharge(int currentCharge, Point currentLocation, Point chargerLocation) {
        int distanceToNearestCharger = DistanceUtil.getManhattanDistance(currentLocation, chargerLocation);
        if (currentCharge <= distanceToNearestCharger) {
            return false;
        }

        return true;
    }

    public static TurnAction moveTowardsPoint(Point currentLocation, Point point) {
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
}
