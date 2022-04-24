package game;

import game.Model;
import javafx.util.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import game.Controller;
import game.View;
import game.*;

public class BotGamerTest {

    @Test
    public void test() {
        int won = 0;
        int lost = 0;
        for (int i = 0; i < 1000; i++) {
            Model model = new Model(5, 5, (cell, asBoomCell) -> {
            });

            Pair<Integer, Integer> firstRandomClick = new Pair<>(
                    new Random().nextInt(model.size),
                    new Random().nextInt(model.size)
            );
            model.getBoard().set(firstRandomClick.getKey(), firstRandomClick.getValue());

            model.solveWithBot();

            if (model.getBoard().wonTheGame()) won++;
            else lost++;
        }
        boolean check = won > lost;
        System.out.println(won + " " + lost);
        Assertions.assertTrue(check);
        //Assertions.assertEquals(1, won);
    }
}
