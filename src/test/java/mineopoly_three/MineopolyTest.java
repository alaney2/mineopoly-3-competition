package mineopoly_three;

import mineopoly_three.action.TurnAction;
import mineopoly_three.game.Economy;
import mineopoly_three.item.ItemType;
import mineopoly_three.strategy.CustomStrategy;
import mineopoly_three.strategy.PlayerBoardView;
import mineopoly_three.tiles.TileType;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.util.HashMap;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class MineopolyTest {

    TileType[][] boardTileType;
    PlayerBoardView boardView;
    CustomStrategy strategy;
    Economy economy;

    @Before
    public void setUp() {
        strategy = new CustomStrategy();
        boardTileType = new TileType[][]{
                {TileType.RECHARGE, TileType.RED_MARKET, TileType.BLUE_MARKET, TileType.RESOURCE_RUBY},
                {TileType.EMPTY, TileType.RESOURCE_RUBY, TileType.RESOURCE_DIAMOND, TileType.RESOURCE_EMERALD},
                {TileType.RESOURCE_DIAMOND, TileType.EMPTY, TileType.RESOURCE_EMERALD, TileType.EMPTY},
                {TileType.RESOURCE_EMERALD, TileType.EMPTY, TileType.RESOURCE_DIAMOND, TileType.RESOURCE_RUBY}};
        boardView = new PlayerBoardView(boardTileType, new HashMap<>(), new Point(), new Point(), 0);
        economy = new Economy(new ItemType[] {});
        strategy.initialize(4, 5, 80, 1000, boardView, new Point(), true, new Random());
    }

    @Test
    public void moveToChargerWhenLowBattery() {
        boardView = new PlayerBoardView(boardTileType, new HashMap<>(), new Point(1,3), new Point(), 0);
        assertEquals(TurnAction.MOVE_LEFT, strategy.getTurnAction(boardView, economy, 1, true));
    }

    @Test
    public void stayOnChargerWhenBatteryNotFull() {
        boardView = new PlayerBoardView(boardTileType, new HashMap<>(), new Point(0,3), new Point(), 0);
        assertEquals(null, strategy.getTurnAction(boardView, economy, 1, true));
    }

    @Test
    public void moveToMarketWithFullInventory() {
        strategy.initialize(4, 0, 80, 1000, boardView, new Point(), true, new Random());
        boardView = new PlayerBoardView(boardTileType, new HashMap<>(), new Point(0,3), new Point(), 0);
        assertEquals(TurnAction.MOVE_RIGHT, strategy.getTurnAction(boardView, economy, 80, true));
    }

    @Test
    public void mineRubyWhenOnRubyAndInventoryNotFull() {
        strategy.initialize(4, 5, 80, 1000, boardView, new Point(), true, new Random());
        boardView = new PlayerBoardView(boardTileType, new HashMap<>(), new Point(3,0), new Point(), 0);
        assertEquals(TurnAction.MINE, strategy.getTurnAction(boardView, economy, 80, true));
    }

    @Test
    public void mineEmeraldWhenOnEmeraldAndInventoryNotFull() {
        strategy.initialize(4, 5, 80, 1000, boardView, new Point(), true, new Random());
        boardView = new PlayerBoardView(boardTileType, new HashMap<>(), new Point(0,0), new Point(), 0);
        assertEquals(TurnAction.MINE, strategy.getTurnAction(boardView, economy, 80, true));
    }

    @Test
    public void mineDiamondWhenOnDiamondAndInventoryNotFull() {
        strategy.initialize(4, 5, 80, 1000, boardView, new Point(), true, new Random());
        boardView = new PlayerBoardView(boardTileType, new HashMap<>(), new Point(0,1), new Point(), 0);
        assertEquals(TurnAction.MINE, strategy.getTurnAction(boardView, economy, 80, true));
    }
}
