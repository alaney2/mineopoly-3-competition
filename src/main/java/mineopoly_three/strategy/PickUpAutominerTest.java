package mineopoly_three.strategy;

import mineopoly_three.action.TurnAction;
import mineopoly_three.game.Economy;
import mineopoly_three.item.InventoryItem;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.List;

public class PickUpAutominerTest implements MinePlayerStrategy{
    private List<TurnAction> commandStack = new ArrayList<>();

    @Override
    public void initialize(int boardSize, int maxInventorySize, int maxCharge, int winningScore, PlayerBoardView startingBoard, Point startTileLocation, boolean isRedPlayer, Random random) {
        commandStack.add(TurnAction.PICK_UP_AUTOMINER);
        commandStack.add(TurnAction.MOVE_RIGHT);
        commandStack.add(TurnAction.MOVE_RIGHT);
        commandStack.add(TurnAction.MOVE_RIGHT);
        commandStack.add(TurnAction.MOVE_UP);
        commandStack.add(TurnAction.MOVE_UP);
        commandStack.add(TurnAction.MOVE_UP);
    }

    @Override
    public TurnAction getTurnAction(PlayerBoardView boardView, Economy economy, int currentCharge, boolean isRedTurn) {
        while (commandStack.size() > 0) {
            return commandStack.remove(commandStack.size() - 1);
        }
        return null;
    }

    @Override
    public void onReceiveItem(InventoryItem itemReceived) {

    }

    @Override
    public void onSoldInventory(int totalSellPrice) {

    }

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public void endRound(int pointsScored, int opponentPointsScored) {

    }
}
