package game;


import javafx.application.Platform;
import javafx.scene.shape.Rectangle;
import javafx.util.Pair;

import java.util.Random;


public class Controller extends Rectangle {

    private Model logicModel;

    private boolean isFirstClick = true;
    private boolean botGame = false;


    private boolean gameEnded = false;

    private final View view;

    public Controller(View view) {
        this.view = view;

        view.enableHint(false);
    }

    public void onInitialParametersSet(int size, int numOfBombs) {
        this.logicModel = new Model(size, numOfBombs, this::openTile);

        view.createContent(logicModel.getBoard().getReadableCells());
    }

    public void onTileFlagClick(int x, int y) {
        if (botGame || logicModel.getBoard().getCells()[x][y].isOpen()) {
            return;
        }
        boolean wasFlagged = logicModel.getBoard().getCells()[x][y].hasFlag();
        if (wasFlagged) {
            view.removeFlag(x, y);
        } else {
            view.setFlag(x, y);
        }

        logicModel.getBoard().getCells()[x][y].setFlag(!wasFlagged);
    }

    public void onHintAsked() {
        if (botGame) {
            return;
        }

        label:
        for (int i = 0; i < logicModel.size; i++) {
            for (int j = 0; j < logicModel.size; j++) {
                Model.Cell cell = logicModel.getBoard().getCells()[i][j];
                if (cell.hasBomb() && !cell.hasFlag()) {
                    cell.setFlag(true);
                    view.setFlag(i, j);
                    break label;
                }
            }
        }
    }

    public void onSolverClick() {
        botGame = true;

        if (isFirstClick) {
            isFirstClick = false;
            Pair<Integer, Integer> firstRandomClick = new Pair<>(
                    new Random().nextInt(logicModel.size),
                    new Random().nextInt(logicModel.size)
            );
            logicModel.getBoard().set(firstRandomClick.getKey(), firstRandomClick.getValue());
            logicModel.getBoard().guess(firstRandomClick.getKey(), firstRandomClick.getValue());
        }

        checkFinish();
        if (gameEnded) {
            return;
        }

        logicModel.solveWithBot(unused -> {
            botGame = false;
            Platform.runLater(this::checkFinish);
            return null;
        });
    }

    public void onTileClick(int x, int y) {
        if (botGame || gameEnded || logicModel.getBoard().getCells()[x][y].isOpen()) {
            return;
        }

        if (logicModel.getBoard().getCells()[x][y].hasFlag()) {
            return;
        }

        if (isFirstClick) {
            isFirstClick = false;
            logicModel.getBoard().set(x, y);
            view.enableHint(true);
        }

        logicModel.getBoard().guess(x, y);

        checkFinish();
    }

    private void checkFinish() {
        if (logicModel.getBoard().gameFinished()) {
            gameEnded = true;
            view.enableHint(false);

            openAll();
            logicModel.getBoard().printProcess();

            if (logicModel.getBoard().lostTheGame()) {
                System.out.printf("DEFEAT in %s attempts", logicModel.getNumOfGuesses());
                System.out.println();
            }
            if (logicModel.getBoard().wonTheGame()) {
                System.out.printf("VICTORY in %s attempts", logicModel.getNumOfGuesses());
                System.out.println();
            }

            view.onGameFinished(logicModel.getBoard().wonTheGame(), logicModel.getNumOfGuesses());
        }
    }


    private void openTile(Model.ReadableCell cell, boolean asBoom) {
//        System.out.println("pls open x=" + cell.x() + ", y=" + cell.y() + ", which is bomb=" + cell.hasBomb());
        if (cell.hasBomb()) {
            if (asBoom) {
                view.makeBoom(cell.x(), cell.y());
            } else {
                view.openTileAsBomb(cell.x(), cell.y());
            }
        } else {
            view.openTile(cell.x(), cell.y(), cell.getNeighbourBombs());
        }
    }


    private void openAll() {
        for (int i = 0; i < logicModel.size; i++) {
            for (int j = 0; j < logicModel.size; j++) {
                Model.Cell cell = logicModel.getBoard().getCells()[i][j];

                if (!cell.isOpen()) {
                    cell.open();
                    if (cell.hasBomb()) {
                        view.openTileAsBomb(i, j);
                    } else {
                        view.openTile(i, j, cell.neighbourBombs);
                    }
                }

            }
        }
    }

}
