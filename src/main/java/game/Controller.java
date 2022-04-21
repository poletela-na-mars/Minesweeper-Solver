package game;


import javafx.scene.shape.Rectangle;


public class Controller extends Rectangle {

    private Model logicModel;

    private boolean isFirstClick = true;
    private int numOfGuesses = 0;

    private boolean gameEnded = false;

    private final View view;

    public Controller(View view) {
        this.view = view;

        view.enableHint(false);
    }

    public void onInitialParametersSet(int size, int numOfBombs) {
        this.logicModel = new Model(size, numOfBombs);

        view.createContent(logicModel.getBoard().getReadableCells());
    }

    public void onTileFlagClick(int x, int y) {
        boolean wasFlagged = logicModel.getBoard().getCells()[x][y].hasFlag();
        if (wasFlagged) {
            view.removeFlag(x, y);
        } else {
            view.setFlag(x, y);
        }

        logicModel.getBoard().getCells()[x][y].setFlag(!wasFlagged);
    }

    public void onHintAsked() {
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

    public void onTileClick(int x, int y) {
        if (gameEnded) {
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

        guess(x, y);
        numOfGuesses++;

        if (logicModel.getBoard().gameFinished()) {
            gameEnded = true;
            view.enableHint(false);

            openAll();
            if (logicModel.getBoard().lostTheGame()) {
                view.makeBoom(x, y);
            }

            if (logicModel.getBoard().lostTheGame()) {
                System.out.printf("DEFEAT in %s attempts", numOfGuesses);
                System.out.println();
            }
            if (logicModel.getBoard().wonTheGame()) {
                System.out.printf("VICTORY in %s attempts", numOfGuesses);
                System.out.println();
            }

            view.onGameFinished(logicModel.getBoard().wonTheGame(), numOfGuesses);
        }
    }

    private void guess(int x, int y) {
        logicModel.getBoard().guess(x, y);

        for (Model.Cell[] row : logicModel.getBoard().getCells()) {
            for (Model.Cell cell : row) {
                if (cell.isOpen()) {
                    view.openTile(cell.x, cell.y, cell.neighbourBombs);
                }
            }
        }
    }

    private void openAll() {
        for (int i = 0; i < logicModel.size; i++) {
            for (int j = 0; j < logicModel.size; j++) {
                Model.Cell cell = logicModel.getBoard().getCells()[i][j];
                if (cell.hasBomb()) {
                    view.openTileAsBomb(i, j);
                } else {
                    view.openTile(i, j, cell.neighbourBombs);
                }
            }
        }
    }

}
