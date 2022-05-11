package game;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.regex.Pattern;

public class View extends Application {
    private Controller controller;

    public static final double MAIN_BOARD_SIZE = 750.0;

    public static final ImagePattern FLAG = new ImagePattern(new Image("flag.png"));

    public static final ImagePattern FLAG_SELECTED = new ImagePattern(new Image("flag_selected.png"));

    public static final ImagePattern FLAG_NOT_SELECTED = new ImagePattern(new Image("flag_not_selected.png"));

    private static final Image imageIcon = new Image("minesweeper_icon.jpg");

    public static final int DEFAULT_BOARD_SIZE = 10;

    private boolean setFlag = false;

    public static final int DEFAULT_NUM_OF_BOMBS = 15;

    String regex = "\\d+";

    private final Stage appStage = new Stage();
    private final Stage messageStage = new Stage();
    
    public static int boardSize = DEFAULT_BOARD_SIZE;

    public static double tileSize;

    private int numOfBombs = DEFAULT_NUM_OF_BOMBS;

    public static Group tileGroup = new Group();

    private final Button buttonHint = new Button("Hint");
    private final Button buttonSolver = new Button("Solver");
    private final Pane root = new Pane();
    private final Rectangle flag = new Rectangle();

    private final Font font = new Font("Arial", 15);

    private Stage primaryStage;

    public void createContent() {
        tileSize = tileSize();
        root.setStyle("-fx-background-color: blanchedalmond");
        root.setPrefSize(MAIN_BOARD_SIZE, MAIN_BOARD_SIZE + 50);
        flag.setWidth(60);
        flag.setHeight(60);
        flag.relocate(MAIN_BOARD_SIZE + 90, MAIN_BOARD_SIZE - 500);
        flag.setOnMouseClicked(event -> {
            setFlag = !setFlag;
            if (setFlag) {
                flag.setFill(FLAG_SELECTED);
            } else {
                flag.setFill(FLAG_NOT_SELECTED);
            }
        });
        flag.setFill(FLAG_NOT_SELECTED);

        buttonSolver.relocate(MAIN_BOARD_SIZE / 2.0 + 465, MAIN_BOARD_SIZE - 300); //
        buttonSolver.setStyle("-fx-background-color: orange; -fx-text-fill: white");
        buttonSolver.setFont(font);
        buttonHint.relocate(MAIN_BOARD_SIZE / 2.0 + 470, MAIN_BOARD_SIZE - 350); //
        buttonHint.setStyle("-fx-background-color: orange; -fx-text-fill: white");
        buttonHint.setFont(font);

        buttonSolver.setOnMouseClicked(event -> {
            buttonSolver.setDisable(true);
            controller.onSolverClick();
        });
        buttonHint.setOnMouseClicked(event -> controller.onHintAsked());
        root.getChildren().addAll(tileGroup, flag, buttonSolver, buttonHint);

        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                Tile tile = new Tile(i, j);
                tileGroup.getChildren().add(tile);
            }
        }
        bindHandlers();
    }

    private void showMessage(boolean won, int numOfGuesses) {
        Pane root = new Pane();
        String message;
        if (!won) {
            message = "You lost the game!";
        } else {
            message = "You won the game!";
        }
        Label labelWonOrLost = new Label(message);
        labelWonOrLost.relocate(25, 20);
        labelWonOrLost.setFont(font);
        labelWonOrLost.setStyle("-fx-text-fill: dimgrey");
        Label labelAttempts = new Label(String.format("Attempts' count: %d", numOfGuesses));
        labelAttempts.relocate(25, 50);
        labelAttempts.setFont(font);
        labelAttempts.setStyle("-fx-text-fill: dimgrey");
        root.getChildren().addAll(labelWonOrLost, labelAttempts);
        //root.setPrefSize(50.0, 100.0);
        root.setStyle("-fx-background-color: blanchedalmond");
        messageStage.getIcons().add(imageIcon);
        messageStage.setWidth(200);
        messageStage.setHeight(130);
        messageStage.centerOnScreen();
        messageStage.setScene(new Scene(root));
        messageStage.show();
    }

    private void initialiseBoard() {
        appStage.getIcons().add(imageIcon);
        appStage.setWidth(390);
        appStage.setHeight(400);
        appStage.setResizable(false);
        appStage.centerOnScreen();
        appStage.initStyle(StageStyle.UNDECORATED);
        appStage.setTitle("Initialise your board");
        Pane root = new Pane();
        root.setStyle("-fx-background-color: blanchedalmond");
        Label text  = new Label("Initialise your board");
        text.setFont(font);
        text.setStyle("-fx-text-fill: dimgrey");
        text.relocate(120, 100);
        TextField sizeOfField = new TextField(String.format("%d", DEFAULT_BOARD_SIZE));
        sizeOfField.relocate(250, 150);
        sizeOfField.setPrefSize(45, 25);
        sizeOfField.setFont(font);
        sizeOfField.setStyle("-fx-text-fill: dimgrey");
        Label labelSize = new Label("Size of field:");
        labelSize.setFont(font);
        labelSize.setStyle("-fx-text-fill: dimgrey");
        labelSize.relocate(85, 150);
        TextField bombsCount = new TextField(String.format("%d", DEFAULT_NUM_OF_BOMBS));
        bombsCount.relocate(250, 200);
        bombsCount.setPrefSize(45, 25);
        bombsCount.setFont(font);
        bombsCount.setStyle("-fx-text-fill: dimgrey");
        Label labelBombsCount = new Label("Bombs' count:");
        labelBombsCount.setFont(font);
        labelBombsCount.setStyle("-fx-text-fill: dimgrey");
        labelBombsCount.relocate(85, 200);
        Button buttonOK = new Button("OK");
        buttonOK.relocate(215, 255);
        buttonOK.setFont(font);
        buttonOK.setStyle("-fx-text-fill: white; -fx-background-color: orange");
        buttonOK.setOnMouseClicked(event -> {
            if (Pattern.matches(regex, sizeOfField.getText()) && Pattern.matches(regex, bombsCount.getText())) {
                boardSize = Integer.parseInt(sizeOfField.getText());
                numOfBombs = Integer.parseInt(bombsCount.getText());
                if ((boardSize * boardSize <= numOfBombs) || (boardSize > 25) || (boardSize < 2) || (numOfBombs < 1)) {
                    // если ввели бомб больше, чем поле,
                    // или бомб на все поле, или доска больше 25х25 (для улучшения быстроты отрисовки и отклика),
                    // или доска меньше 2x2, или бомб меньше одной,
                    // то стандартные условия
                    boardSize = DEFAULT_BOARD_SIZE;
                    numOfBombs = DEFAULT_NUM_OF_BOMBS;
                }
                appStage.close();
            }
        });
        Button quit = new Button("Quit");
        quit.setStyle("-fx-background-color: orange; -fx-text-fill: white");
        quit.relocate(120, 255);
        quit.setFont(font);
        quit.setOnMouseClicked(event -> System.exit(0));
        root.getChildren().addAll(sizeOfField, labelSize, bombsCount, labelBombsCount, buttonOK, text, quit);
        root.setPrefSize(200, 100);
        Scene beginScene = new Scene(root);
        appStage.setScene(beginScene);
        appStage.showAndWait();

        controller.onInitialParametersSet(boardSize, numOfBombs);
    }

    public void enableHint(boolean enabled) {
        buttonHint.setDisable(!enabled);
    }
    
    static class Tile extends Rectangle {

        int x;

        int y;

        private static Image getImageNumber(int num) {
            return new Image("image" + num + ".jpg");
        }

        public static final ImagePattern CLOSED_IMAGE = new ImagePattern(new Image("high quality/closed.jpg"));

        public static final Image IMAGE_BOMB = new Image("high quality/bomb.png");

        public static final Image IMAGE_BOOM = new Image("high quality/bomb_boom.png");


        public void open(int numOfNeighbours) {
            setFill(new ImagePattern(getImageNumber(numOfNeighbours)));
        }

        public void openAsBomb() {
            setFill(new ImagePattern(IMAGE_BOMB));
        }

        public Tile(int x, int y) {
            this.x = x;
            this.y = y;
            setWidth(View.tileSize);
            setHeight(View.tileSize);
            relocate(x * View.tileSize, y * View.tileSize);
            setFill(CLOSED_IMAGE);
        }

        public void detonateBomb() {
            setFill(new ImagePattern(IMAGE_BOOM));
        }

        public void setFlag(boolean hasFlag) {
            if (hasFlag) {
                setFill(View.FLAG);
            } else {
                setFill(CLOSED_IMAGE);
            }
        }
    }

    double tileSize() {
        return MAIN_BOARD_SIZE / boardSize;
    }

    private Tile getTile(int x, int y) {
        return (Tile) tileGroup.getChildren().get(x * View.boardSize + y);
    }

    private void bindHandlers() {
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
        scene.setOnMouseClicked(event -> {
            int x = (int) (event.getSceneX() / tileSize);
            int y = (int) (event.getSceneY() / tileSize);
            if (y >= boardSize || x >= boardSize) {
                return;
            }

            if (setFlag) {
                controller.onTileFlagClick(x, y);
                return;
            }

            controller.onTileClick(x, y);
        });
    }

    public void detonateBomb(int x, int y) {
        getTile(x, y).detonateBomb();
    }

    public void onGameFinished(boolean won, int numOfGuesses) {
        buttonSolver.setDisable(true);

        Platform.runLater(() -> showMessage(won, numOfGuesses)); // выполнение на UI-потоке
        // If you need to update a GUI component from a non-GUI thread,
        // you can use that to put your update in a queue and it will be handled
        // by the GUI thread as soon as possible.
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        controller = new Controller(this);
        primaryStage.setWidth(1000);
        primaryStage.setHeight(788);
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.setTitle("Minesweeper");
        primaryStage.getIcons().add(imageIcon);

        initialiseBoard();
    }

    public void setFlag(int x, int y) {
        getTile(x, y).setFlag(true);
    }

    public void removeFlag(int x, int y) {
        getTile(x, y).setFlag(false);
    }

    public void openTile(int x, int y, int numOfBombs) {
        Platform.runLater(() -> getTile(x, y).open(numOfBombs));    // выполнение на UI-потоке
    }

    public void openTileAsBomb(int x, int y) {
        Platform.runLater(() -> getTile(x, y).openAsBomb());    // выполнение на UI-потоке
    }

    public static void main(String[] args) {
        launch(args);
    }
}


