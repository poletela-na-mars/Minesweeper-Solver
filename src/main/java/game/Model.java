package game;

import game.solver.BotGamer;

import java.util.*;

//Board + CellImpl (+ abstract Cell, OnOpenCellListener, FlagManager)
public class Model {

    private final Board board;

    private BotGamer bot;

    public final int size;
    public final int numOfBombs;

    public Model(int size, int numOfBombs, OnOpenCellListener onOpenCellListener) {
        this.size = size;
        this.numOfBombs = numOfBombs;
        this.board = new Board(size, numOfBombs);
        board.addOnOpenCellListener(onOpenCellListener);
    }

    public Board getBoard() {
        return this.board;
    }

    public void solveWithBot(FlagManager flagManager) {
        this.bot = new BotGamer(board, flagManager);
        bot.play();
    }

    public int getNumOfGuesses() {
        return board.numOfGuesses;
    }

    public static class Board implements FlagManager {
        private boolean lostTheGame = false;

        private boolean wonTheGame = false;

        protected final CellImpl[][] cells;

        public final int size;

        public final int numOfBombs;

        private final Set<OnOpenCellListener> onOpenCellListeners = new HashSet<>();

        private int numOfGuesses = 0;

        public void addOnOpenCellListener(OnOpenCellListener onOpenCellListener) {
            onOpenCellListeners.add(onOpenCellListener);
        }

        public void openCell(int x, int y) {
            cells[x][y].open();
        }

        @Override
        public void setFlag(int x, int y) {
            cells[x][y].setFlag(true);
        }

        @Override
        public void removeFlag(int x, int y) {
            cells[x][y].setFlag(false);
        }

        public Board(int size, int numOfBombs) {
            this.size = size;
            this.numOfBombs = numOfBombs;
            cells = new CellImpl[size][size];

            //Инициализируем
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    cells[i][j] = new CellImpl(i, j);
                }
            }
        }

        public void initField(int xClick, int yClick) {
            //Ставим бомбы
            for (int i = 0; i < numOfBombs; i++) {
                int x = (int) (Math.random() * size);
                int y = (int) (Math.random() * size);
                if (cells[x][y].hasBomb() || (x == xClick && y == yClick)) {
                    i--;
                } else {
                    cells[x][y].setBomb(true);
                }
            }
            //Счет соседей
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    for (int x = i - 1; x < i + 2; x++) {
                        if (x == -1 || x == size)
                            continue;
                        for (int y = j - 1; y < j + 2; y++) {
                            if (y == -1 || y == size)
                                continue;
                            if (cells[x][y].hasBomb()) {
                                cells[i][j].addNeighbourBomb();
                            }
                        }
                    }
                }
            }
            guess(xClick, yClick);
        }

        public void guess(int x, int y) {
            numOfGuesses++;
            System.out.printf("Open %d,%d%n", x, y);

            //Проиграл?
            if (cells[x][y].hasBomb()) {
                cells[x][y].open();
                onOpenCellListeners.forEach(it -> it.onOpenCell(cells[x][y], true)); //interface -> controller -> view
                lostTheGame = true;
                return;
            }
            //Открыть все не соседние с бомбами клетки
            openAllNulls(cells[x][y]);

            checkIfWon();
            printBoardStatus();
        }

        private void openAllNulls(CellImpl cell) {
            boolean[][] visited = new boolean[size][size];
            List<CellImpl> intendToVisit = new LinkedList<>();

            intendToVisit.add(cell);

            while (!intendToVisit.isEmpty()) {
                CellImpl current = intendToVisit.remove(0);
                //visited.add(current);
                visited[current.x][current.y] = true;
                current.open();
                onOpenCellListeners.forEach(it -> it.onOpenCell(current, false));

                if (current.neighbourBombs > 0) {
                    continue;
                }

//                if (current.x > 0 && !cells[current.x - 1][current.y].hasBomb() && !visited.contains(cells[current.x - 1][current.y]))
//                    intendToVisit.add(cells[current.x - 1][current.y]);
//                if (current.x < size - 1 && !cells[current.x + 1][current.y].hasBomb() && !visited.contains(cells[current.x + 1][current.y]))
//                    intendToVisit.add(cells[current.x + 1][current.y]);
//
//                if (current.y > 0 && !cells[current.x][current.y - 1].hasBomb() && !visited.contains(cells[current.x][current.y - 1]))
//                    intendToVisit.add(cells[current.x][current.y - 1]);
//                if (current.y < size - 1 && !cells[current.x][current.y + 1].hasBomb() && !visited.contains(cells[current.x][current.y + 1]))
//                    intendToVisit.add(cells[current.x][current.y + 1]);

                if (current.x > 0 && !cells[current.x - 1][current.y].hasBomb() && !visited[current.x - 1][current.y])
                    intendToVisit.add(0, cells[current.x - 1][current.y]);
                if (current.x < size - 1 && !cells[current.x + 1][current.y].hasBomb() && !visited[current.x + 1][current.y])
                    intendToVisit.add(0, cells[current.x + 1][current.y]);

                if (current.y > 0 && !cells[current.x][current.y - 1].hasBomb() && !visited[current.x][current.y - 1])
                    intendToVisit.add(0, cells[current.x][current.y - 1]);
                if (current.y < size - 1 && !cells[current.x][current.y + 1].hasBomb() && !visited[current.x][current.y + 1])
                    intendToVisit.add(0, cells[current.x][current.y + 1]);
            }
        }

        public void printBoardStatus() {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    Integer value = cells[j][i].isOpen()
                            ? (cells[j][i].hasBomb() ? -1
                            : cells[j][i].getNeighbourBombs()) : null;

                    System.out.printf("% 4d ", value);
                }
                System.out.println();
            }
            System.out.println();
        }

        public void checkIfWon() {
            int numOfNulls = 0;
            for (CellImpl[] cellRow : cells) {
                for (CellImpl cell : cellRow) {
                    if (!cell.isOpen()) {
                        numOfNulls++;
                    }
                }
            }
            if (numOfNulls == numOfBombs && !lostTheGame) {
                wonTheGame = true;
            }
        }

        /*private Cell[][] getCells() {
            return cells;
        }*/

        public boolean gameFinished() {
            return lostTheGame || wonTheGame;
        }

        public boolean lostTheGame() {
            return lostTheGame;
        }

        public boolean wonTheGame() {
            return wonTheGame;
        }

        public Cell[][] getCells() {
            return cells;
        }
    }

    public abstract static class Cell {
        public Cell(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public abstract int getNeighbourBombs();

        public abstract boolean isOpen();

        public abstract boolean hasFlag();

        public abstract boolean isMine() throws IllegalStateException;

        public final int x;
        public final int y;
    }

    public static class CellImpl extends Cell {

        private boolean isOpen = false;

        private boolean hasBomb = false;

        private boolean flagged = false;

        int neighbourBombs;

        public CellImpl(int x, int y) {
            super(x, y);
        }

        public void setBomb(boolean hasBomb) {
            this.hasBomb = hasBomb;
        }

        private boolean hasBomb() {
            return hasBomb;
        }

        @Override
        public boolean isMine() throws IllegalStateException {
            if (!isOpen) throw new IllegalStateException("Cell is closed!");
            return hasBomb;
        }

        @Override
        public boolean hasFlag() {
            return flagged;
        }

        public void setFlag(boolean set) {
            flagged = set;
        }

        public void addNeighbourBomb() {
            neighbourBombs++;
        }

        @Override
        public int getNeighbourBombs() {
            if (isOpen)
                return neighbourBombs;
            else return 0;
        }

        @Override
        public boolean isOpen() {
            return isOpen;
        }

        public void open() {
            isOpen = true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CellImpl cell = (CellImpl) o;

            if (x != cell.x) return false;
            return y == cell.y;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            return result;
        }
    }

    @FunctionalInterface
    public interface OnOpenCellListener {
        void onOpenCell(Cell cell, boolean asBoomCell); // в controller openTile()
    }

    public interface FlagManager {
        void setFlag(int x, int y);
        void removeFlag(int x, int y);
    }
}


