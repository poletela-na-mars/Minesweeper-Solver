package game;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class View extends Application {
    private Controller controller;

    public static final double MAIN_BOARD_SIZE = 750.0;

    private boolean botGame = false;

    public static final ImagePattern FLAG = new ImagePattern(new Image("flag.png"));

    public static final ImagePattern FLAG_SELECTED = new ImagePattern(new Image("flag_selected.png"));

    public static final ImagePattern FLAG_NOT_SELECTED = new ImagePattern(new Image("flag_not_selected.png"));

    public static final int DEFAULT_BOARD_SIZE = 10;

    private boolean setFlag = false;

    public static final int DEFAULT_NUM_OF_BOMBS = 15;

    String regex = "\\d+";

    private Stage appStage = new Stage();
    
    public static int boardSize = DEFAULT_BOARD_SIZE;

    public static double tileSize;

    private int numOfBombs = DEFAULT_NUM_OF_BOMBS;

    public static Group tileGroup = new Group();

    private Button hint = new Button("Hint");
    private Button button = new Button("Solver");
    private Pane root = new Pane();
    private Rectangle flag = new Rectangle();


    private Stage primaryStage;


    public void createContent(Model.ReadableCell[][] cells) {
        tileSize = tileSize();

        root.setPrefSize(MAIN_BOARD_SIZE, MAIN_BOARD_SIZE + 50);//убрать
        flag.setWidth(60);//
        flag.setHeight(60);//
        flag.relocate(MAIN_BOARD_SIZE + 90, MAIN_BOARD_SIZE - 500);//
        flag.setOnMouseClicked(event -> {
            setFlag = !setFlag;
            if (setFlag) {
                flag.setFill(FLAG_SELECTED);
            } else {
                flag.setFill(FLAG_NOT_SELECTED);
            }
        });
        flag.setFill(FLAG_NOT_SELECTED);

        button.relocate(MAIN_BOARD_SIZE / 2.0 + 450, MAIN_BOARD_SIZE - 300); //
        hint.relocate(MAIN_BOARD_SIZE / 2.0 + 450, MAIN_BOARD_SIZE - 350); //

        button.setOnMouseClicked(event -> {
            button.setDisable(true);
            controller.onSolverClick();
        });
        hint.setOnMouseClicked(event -> {
            controller.onHintAsked();
        });
        root.getChildren().addAll(tileGroup, flag, button, hint);

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
        Label label1 = new Label(message);
        label1.relocate(10.0, 0.0);
        Label label2 = new Label(String.format("Attempts' count: %d", numOfGuesses));
        label2.relocate(10.0, 50.0);
        root.getChildren().addAll(label1, label2);
        root.setPrefSize(50.0, 100.0);
        appStage.setScene(new Scene(root));
        appStage.show();
    }

    private void initialise() {
        appStage.getIcons().add(Tile.IMAGE_BOMB);
        Pane root = new Pane();
        TextField textField1 = new TextField(String.format("%d", DEFAULT_BOARD_SIZE));
        textField1.relocate(100.0, 0.0);
        textField1.setPrefSize(40.0, 15.0);
        Label label1 = new Label("Size of field:");
        label1.relocate(10, 0);
        TextField textField2 = new TextField(String.format("%d", DEFAULT_NUM_OF_BOMBS));
        textField2.relocate(100, 30);
        textField2.setPrefSize(40, 15);
        Label label2 = new Label("Bombs' count:");
        label2.relocate(10, 30);
        Button button = new Button("OK");
        button.relocate(50, 60);
        button.setOnMouseClicked(event -> {
            if (Pattern.matches(regex, textField1.getText()) && Pattern.matches(regex, textField2.getText())) {
                boardSize = Integer.parseInt(textField1.getText());
//                textField2.clear();
//                Double d = (Math.ceil(boardSize * 0.165));
//                textField2.appendText(d.toString());
                numOfBombs = Integer.parseInt(textField2.getText());
                if ((boardSize * boardSize <= numOfBombs) || (boardSize > 30) || (boardSize < 2) || (numOfBombs < 1)) {  // если ввели бомб больше, чем поле,
                    // или бомб на все поле, или доска больше 30х30 (для улучшения быстроты отрисовки и отклика),
                    // то стандартные условия
                    boardSize = DEFAULT_BOARD_SIZE;
                    numOfBombs = DEFAULT_NUM_OF_BOMBS;
                }
                appStage.close();
            }
        });
        root.getChildren().addAll(textField1, label1, textField2, label2, button);
        root.setPrefSize(200, 100);
        Scene beginScene = new Scene(root);
        appStage.setScene(beginScene);
        appStage.showAndWait();

        controller.onInitialParametersSet(boardSize, numOfBombs);
    }

    public void enableHint(boolean enabled) {
        hint.setDisable(!enabled);
    }
    
    static class Tile extends Rectangle {

        int x;

        int y;

        private static Image getImageNumber(int num) {
            List<String> list = new ArrayList<>();
            list.add("image0.jpg");
            list.add("image1.jpg");
            list.add("image2.jpg");
            list.add("image3.jpg");
            list.add("image4.jpg");
            list.add("image5.jpg");
            list.add("image6.jpg");
            list.add("image7.jpg");
            list.add("image8.jpg");
            return new Image(list.get(num));
        }

        public static final ImagePattern CLOSED_IMAGE = new ImagePattern(new Image("closed.jpg"));

        public static final Image IMAGE_BOMB = new Image("bomb.png");

        public static final Image IMAGE_BOOM = new Image("bomb_boom.png");



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


        public void makeBoom() {
            setFill(new ImagePattern(IMAGE_BOOM));
        }


        public void setFlag(boolean hasFlag) {
            if (hasFlag) {
                setFill(View.FLAG);
            } else {
                setFill(CLOSED_IMAGE);
            }
//            this.hasFlag = hasFlag;
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

    public void makeBoom(int x, int y) {
        getTile(x, y).makeBoom();
    }

    public void onGameFinished(boolean won, int numOfGuesses) {
        button.setDisable(true);

        showMessage(won, numOfGuesses);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        controller = new Controller(this);
        primaryStage.setWidth(1000);
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.setTitle("Minesweeper");

        Image imageIcon = new Image("minesweeper_icon.jpg");
        primaryStage.getIcons().add(imageIcon);

        initialise();
    }

    public void setFlag(int x, int y) {
        getTile(x, y).setFlag(true);
    }

    public void removeFlag(int x, int y) {
        getTile(x, y).setFlag(false);
    }


    public void openTile(int x, int y, int numOfBombs) {
        getTile(x, y).open(numOfBombs);
    }

    public void openTileAsBomb(int x, int y) {
        getTile(x, y).openAsBomb();
    }

    public static void main(String[] args) {
        launch(args);
    }
}


