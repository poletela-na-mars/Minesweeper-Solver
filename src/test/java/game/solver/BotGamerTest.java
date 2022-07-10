package game.solver;

import game.Model;
import game.TestBoard;
import javafx.util.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BotGamerTest {

    private final Model.FlagManager emptyFlagManager = new Model.FlagManager() {
        @Override
        public void setFlag(int x, int y) {
        }

        @Override
        public void removeFlag(int x, int y) {
        }
    };

    @RepeatedTest(100)
    public void testNotThrows() {
        AtomicInteger won = new AtomicInteger();
        AtomicInteger lost = new AtomicInteger();

        Assertions.assertDoesNotThrow(() -> {
            Model model = new Model(10, 16, (cell, asBoomCell) -> {
            });

            Pair<Integer, Integer> firstRandomClick = new Pair<>(
                    new Random().nextInt(model.size),
                    new Random().nextInt(model.size)
            );
            model.getBoard().initField(firstRandomClick.getKey(), firstRandomClick.getValue());

            model.solveWithBot(emptyFlagManager);

            System.out.println("-----------------------------------------------");
            if (model.getBoard().wonTheGame()) {
                won.getAndIncrement();
                System.out.println("VICTORY in " + model.getNumOfGuesses() + " attempts");
            } else {
                lost.getAndIncrement();
                System.out.println("DEFEAT in " + model.getNumOfGuesses() + " attempts");
            }
            System.out.println("-----------------------------------------------");
        });
    }


    @Test
    public void addGroupAndGetGroupsTest() {
        TestBoard board = new TestBoard(2, 2);
        BotGamer botGamer = new BotGamer(board, emptyFlagManager);

        // OPENED  CLOSED
        // CLOSED  CLOSED

        board.openCell(0, 0);

        botGamer.addGroup(board.getCell(0, 0));

        // 0,0 ни в какой группе не состоит
        Assertions.assertTrue(botGamer.getGroups(board.getCell(0, 0).x, board.getCell(0, 0).y).isEmpty());
        // 0,1 состоит в одной группе (от 0,0)
        Assertions.assertEquals(1, botGamer.getGroups(board.getCell(0, 1).x, board.getCell(0, 1).y).size());
        // в группе 0,0 есть три cells
        Assertions.assertEquals(3, new ArrayList<>(botGamer.getGroups(board.getCell(0, 1).x, board.getCell(0, 1).y)).get(0).getCells().size());

        // 1,0 состоит в одной группе (от 0,0)
        Assertions.assertEquals(1, botGamer.getGroups(board.getCell(1, 0).x, board.getCell(1, 0).y).size());
        Assertions.assertEquals(3, new ArrayList<>(botGamer.getGroups(board.getCell(1, 0).x, board.getCell(1, 0).y)).get(0).getCells().size());

        // 1,1 состоит в одной группе (от 0,0)
        Assertions.assertEquals(1, botGamer.getGroups(board.getCell(1, 1).x, board.getCell(1, 1).y).size());
        Assertions.assertEquals(3, new ArrayList<>(botGamer.getGroups(board.getCell(1,1).x, board.getCell(1, 1).y)).get(0).getCells().size());
    }

    @Test
    public void getFirstContainsGroupOrNullAndRestructureGroupsTest() {
        TestBoard board = new TestBoard(3, 2);
        BotGamer botGamer = new BotGamer(board, emptyFlagManager);

        // !2  2  *
        //  *  *  1
        //  *  2 !1

        // ! - создали группы именно для этих ячеек, для наглядности работы алгоритма
        // В группе от 0,0 содержится две ячейки, а в группе от 2,2 - 1. Однако группа от 0,0 содержит в себе группу от 2,2.
        // Поэтому метод вернет пару от GroupCell, где первый элемент пары - больший GroupCell (где больше Set от cells).

        board.setNeighbourBombs(0, 0, 2);
        board.openCell(0, 0);
        board.setNeighbourBombs(1, 0, 2);
        board.openCell(1, 0);

        board.setNeighbourBombs(2, 2, 1);
        board.openCell(2, 2);
        board.setNeighbourBombs(2, 1, 1);
        board.openCell(2, 1);
        board.setNeighbourBombs(1, 2, 2);
        board.openCell(1, 2);

        botGamer.addGroup(board.getCell(0, 0));
        botGamer.addGroup(board.getCell(2, 2));

        Set<GroupCell> theorySet = botGamer.getGroups(1, 1);

        GroupCell max = theorySet.stream().findFirst().get();
        int maxSize = max.cells.size();
        GroupCell maxGroupCell = theorySet.stream().filter(it -> it.cells.size() >= maxSize).findFirst().get();
        theorySet.remove(maxGroupCell);

        GroupCell secondGroupCell = theorySet.stream().findFirst().get();

        Pair<GroupCell, GroupCell> theoryPair = new Pair(maxGroupCell, secondGroupCell);
        Pair<GroupCell, GroupCell> result = botGamer.getFirstContainsGroupOrNull();
        Assertions.assertEquals(theoryPair, result);
        Assertions.assertTrue(maxGroupCell.contains(secondGroupCell));

        // Тест на restructureGroups. Вычтем из большей группы маленькую. Теперь они не пересекаются,
        // в большей группе осталась одна ячейка и в маленькой осталась одна ячейка.

        botGamer.restructureGroups();
        Assertions.assertFalse(maxGroupCell.contains(secondGroupCell));
        int actualSizeOfBiggerGroup = maxGroupCell.cells.size();
        int actualSizeOfSmallerGroup = secondGroupCell.cells.size();
        Assertions.assertEquals(1, actualSizeOfBiggerGroup);
        Assertions.assertEquals(1, actualSizeOfSmallerGroup);
    }

    @Test
    public void evaluateTest() {
        TestBoard board = new TestBoard(3, 3);
        BotGamer botGamer = new BotGamer(board, emptyFlagManager);

        //   0   1  (B)
        //   1   3   *
        //  (B)  2  (B)

        // Тест на однозначность нахождения бомб

        board.setNeighbourBombs(1, 1, 3);
        board.openCell(1, 1);
        board.setNeighbourBombs(1, 2, 2);
        board.openCell(1, 2);
        board.setNeighbourBombs(1, 0, 1);
        board.openCell(1, 0);
        board.setNeighbourBombs(0, 1, 1);
        board.openCell(0, 1);
        board.setNeighbourBombs(0, 0, 0);
        board.openCell(0, 0);

        board.getCell(0, 2).setBomb(true);
        board.getCell(2, 0).setBomb(true);
        board.getCell(2, 2).setBomb(true);

        botGamer.addGroup(board.getCell(1, 1));
        botGamer.addGroup(board.getCell(0, 0));
        botGamer.addGroup(board.getCell(0, 1));
        botGamer.addGroup(board.getCell(1, 0));
        botGamer.addGroup(board.getCell(1, 2));

        botGamer.restructureGroups();
        botGamer.evaluateClearGroupsProbabilities();
        Assertions.assertEquals(0, botGamer.cells[2][1].getMineProbability());
        Assertions.assertEquals(1, botGamer.cells[2][0].getMineProbability());
        Assertions.assertEquals(1, botGamer.cells[2][2].getMineProbability());
        Assertions.assertEquals(1, botGamer.cells[0][2].getMineProbability());
    }

    @Test
    public void ratioTest() {
        int won = 0;
        int lost = 0;
        for (int i = 0; i < 100; i++) {


            Model model = new Model(10, 16, (cell, asBoomCell) -> {
            });

            Pair<Integer, Integer> firstRandomClick = new Pair<>(
                    new Random().nextInt(model.size),
                    new Random().nextInt(model.size)
            );
            model.getBoard().initField(firstRandomClick.getKey(), firstRandomClick.getValue());

            model.solveWithBot(emptyFlagManager);

            System.out.println("-----------------------------------------------");
            if (model.getBoard().wonTheGame()) {
                won++;
                System.out.println("VICTORY in " + model.getNumOfGuesses() + " attempts");
            } else {
                lost++;
                System.out.println("DEFEAT in " + model.getNumOfGuesses() + " attempts");
            }
            System.out.println("-----------------------------------------------");
        }
        boolean check = won > lost;
        System.out.println(won + " " + lost);
        Assertions.assertTrue(check);
    }
}
