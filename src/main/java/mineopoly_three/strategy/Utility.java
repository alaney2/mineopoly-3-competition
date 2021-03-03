package mineopoly_three.strategy;

import mineopoly_three.action.TurnAction;
import mineopoly_three.game.Economy;
import mineopoly_three.item.ItemType;
import mineopoly_three.tiles.TileType;
import mineopoly_three.util.DistanceUtil;

import java.awt.*;

public class Utility {
    /**
     * @param economy with current prices
     * @return The most expensive resource in the economy
     */
    public static TileType determineMostExpensiveResource(Economy economy) {
        ItemType mostExpensiveResource = ItemType.RUBY;
        for (ItemType item: economy.getCurrentPrices().keySet()) {
            if (economy.getCurrentPrices().get(item) > economy.getCurrentPrices().get(mostExpensiveResource)) {
                mostExpensiveResource = item;
            }
        }
        return convertItemTypeToTileType(mostExpensiveResource);
    }

    /**
     * @param item to convert
     * @return Equivalent TileType of the item
     */
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

    /**
     *
     * @param currentCharge of player
     * @param currentLocation of player
     * @param chargerLocation location of closest charger
     * @return If the player has enough charge to move to a charger
     */
    public static boolean playerHasEnoughCharge(int currentCharge, Point currentLocation, Point chargerLocation) {
        int distanceToNearestCharger = DistanceUtil.getManhattanDistance(currentLocation, chargerLocation);
        return currentCharge <= distanceToNearestCharger;
    }

    /**
     * @param currentLocation of player
     * @param point to go to
     * @return TurnAction to get to point
     */
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

    /**
     * @param start point to compare with two other points
     * @param p1 first point to compare with start
     * @param p2 second point to compare with start
     * @return Distance from start to p1 minus distance from start to p2
     */
    public static int compareManhattanDistance(Point start, Point p1, Point p2) {
        return DistanceUtil.getManhattanDistance(start, p1) - DistanceUtil.getManhattanDistance(start, p2);
    }
}
