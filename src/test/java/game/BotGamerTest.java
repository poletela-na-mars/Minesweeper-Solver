package game;

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
            model.getBoard().set(firstRandomClick.getKey(), firstRandomClick.getValue());

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
        Model.Board board = new Model.Board(2, 2, (cell, asBoomCell) -> {
        });
        Model.BotGamer botGamer = new Model.BotGamer(board, emptyFlagManager);

        // OPENED  CLOSED
        // CLOSED  CLOSED

        board.getCells()[0][0].open();
//        board.getCells()[0][1].setBomb(true);
//        board.getCells()[1][0].setBomb(true);

        botGamer.addGroup(board.getCells()[0][0]);

        // 0,0 ни в какой группе не состоит
        Assertions.assertTrue(botGamer.getGroups(board.getCells()[0][0].x(), board.getCells()[0][0].y()).isEmpty());
        // 0,1 состоит в одной группе (от 0,0)
        Assertions.assertEquals(1, botGamer.getGroups(board.getCells()[0][1].x(), board.getCells()[0][1].y()).size());
        // в группе 0,0 есть три cells
        Assertions.assertEquals(3, new ArrayList<>(botGamer.getGroups(board.getCells()[0][1].x(), board.getCells()[0][1].y())).get(0).getCells().size());

        // 1,0 состоит в одной группе (от 0,0)
        Assertions.assertEquals(1, botGamer.getGroups(board.getCells()[1][0].x(), board.getCells()[1][0].y()).size());
        Assertions.assertEquals(3, new ArrayList<>(botGamer.getGroups(board.getCells()[1][0].x(), board.getCells()[1][0].y())).get(0).getCells().size());

        // 1,1 состоит в одной группе (от 0,0)
        Assertions.assertEquals(1, botGamer.getGroups(board.getCells()[1][1].x(), board.getCells()[1][1].y()).size());
        Assertions.assertEquals(3, new ArrayList<>(botGamer.getGroups(board.getCells()[1][1].x(), board.getCells()[1][1].y())).get(0).getCells().size());
    }

    @Test
    public void getFirstContainsGroupOrNullAndRestructureGroupsTest() {
        Model.Board board = new Model.Board(3, 2, (cell, asBoomCell) -> {
        });
        Model.BotGamer botGamer = new Model.BotGamer(board, emptyFlagManager);

        //  !2   *(B)   *
        //   2   *(B)   2
        //   *     1   !1

        // ! - создали группы имнно для этих ячеек, для наглядности работы алгоритма
        // В группе от 0,0 содержится две ячейки, а в группе от 2,2 - 1. Однако группа от 0,0 содержит в себе группу от 2,2.
        // Поэтому метод вернет пару от GroupCell, где первый элемент пары - больший GroupCell (где больше Set от cells).

        board.getCells()[0][0].neighbourBombs = 2;
        board.getCells()[0][0].open();
        board.getCells()[1][0].neighbourBombs = 2;
        board.getCells()[1][0].open();

        board.getCells()[2][2].neighbourBombs = 1;
        board.getCells()[2][2].open();
        board.getCells()[2][1].neighbourBombs = 1;
        board.getCells()[2][1].open();
        board.getCells()[1][2].neighbourBombs = 2;
        board.getCells()[1][2].open();

        botGamer.addGroup(board.getCells()[0][0]);
        botGamer.addGroup(board.getCells()[2][2]);

        Set<Model.GroupCell> theorySet = botGamer.getGroups(1, 1);

        Model.GroupCell max = theorySet.stream().findFirst().get();
        int maxSize = max.cells.size();
        Model.GroupCell maxGroupCell = theorySet.stream().filter(it -> it.cells.size() >= maxSize).findFirst().get();
        theorySet.remove(maxGroupCell);

        Model.GroupCell secondGroupCell = theorySet.stream().findFirst().get();

        Pair<Model.GroupCell, Model.GroupCell> theoryPair = new Pair(maxGroupCell, secondGroupCell);
        Pair<Model.GroupCell, Model.GroupCell> result = botGamer.getFirstContainsGroupOrNull();
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
        Model.Board board = new Model.Board(3, 3, (cell, asBoomCell) -> {
        });
        Model.BotGamer botGamer = new Model.BotGamer(board, emptyFlagManager);

        //  0    1    *
        //  1    3    2
        //  *    *    *

        // Тест на однозначность нахождения бомб

        //  0     1     *
        //  1     3     2
        //  *    !0     *

        board.getCells()[1][1].neighbourBombs = 3;
        board.getCells()[1][1].open();
        board.getCells()[1][2].neighbourBombs = 2;
        board.getCells()[1][2].open();
        board.getCells()[1][0].neighbourBombs = 1;
        board.getCells()[1][0].open();
        board.getCells()[0][1].neighbourBombs = 1;
        board.getCells()[0][1].open();
        board.getCells()[0][0].neighbourBombs = 0;
        board.getCells()[0][0].open();

        board.getCells()[0][2].setBomb(true);
        board.getCells()[2][0].setBomb(true);
        board.getCells()[2][2].setBomb(true);

        botGamer.addGroup(board.getCells()[1][1]);
        botGamer.addGroup(board.getCells()[0][0]);
        botGamer.addGroup(board.getCells()[0][1]);
        botGamer.addGroup(board.getCells()[1][0]);
        botGamer.addGroup(board.getCells()[1][2]);

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
            model.getBoard().set(firstRandomClick.getKey(), firstRandomClick.getValue());

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
