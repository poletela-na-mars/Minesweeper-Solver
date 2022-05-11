package game;

import javafx.scene.shape.Rectangle;
import javafx.util.Pair;

import java.util.Random;

public class Controller extends Rectangle implements Model.FlagManager {

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
        this.logicModel = new Model(size, numOfBombs, this::openTile);  // передадим ссылку на функцию openTile

        view.createContent();
    }

    @Override
    public void setFlag(int x, int y) {
        logicModel.getBoard().getCells()[x][y].setFlag(true);
        view.setFlag(x, y);
    }

    @Override
    public void removeFlag(int x, int y) {
        logicModel.getBoard().getCells()[x][y].setFlag(false);
        view.removeFlag(x, y);
    }

    public void onTileFlagClick(int x, int y) {
        if (botGame || logicModel.getBoard().getCells()[x][y].isOpen()) {
            return;
        }
        boolean wasFlagged = logicModel.getBoard().getCells()[x][y].hasFlag();
        if (wasFlagged) {
            removeFlag(x, y);
        } else {
            setFlag(x, y);
        }
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
        }

        checkFinish();
        if (gameEnded) {
            return;
        }

        new Thread(() -> {
            logicModel.solveWithBot(this);
            botGame = false;
            checkFinish();
        }).start();
    }

    public void onTileClick(int x, int y) {
        if (botGame || gameEnded || logicModel.getBoard().getCells()[x][y].isOpen() ||
                logicModel.getBoard().getCells()[x][y].hasFlag()) {
            return;
        }

        if (isFirstClick) {
            isFirstClick = false;
            logicModel.getBoard().set(x, y);
            view.enableHint(true);
        }
        else {
            logicModel.getBoard().guess(x, y);
        }

        checkFinish();
    }

    private void checkFinish() {
        if (logicModel.getBoard().gameFinished()) {
            gameEnded = true;
            view.enableHint(false);

            // подождем 3 сек, чтобы визуально запомнить и сравнить поле проигрыша с открытым полностью
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            openAll();
            logicModel.getBoard().printBoardStatus();

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
        if (cell.isMine()) {
            if (asBoom) {
                view.detonateBomb(cell.x(), cell.y());
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
