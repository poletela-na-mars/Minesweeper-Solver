package game;

import javafx.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

//Board + Cell + SolverCell + BotGamer + GroupCell (+ interface ReadableCell, OnOpenCellListener, FlagManager)
public class Model {

    private final Board board;

    private BotGamer bot;

    public final int size;
    public final int numOfBombs;

    public Model(int size, int numOfBombs, OnOpenCellListener onOpenCellListener) {
        this.size = size;
        this.numOfBombs = numOfBombs;
        this.board = new Board(size, numOfBombs, onOpenCellListener);
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

    static class Board {
        private boolean lostTheGame = false;

        private boolean wonTheGame = false;

        private final Cell[][] cells;

        private final int size;

        private final int numOfBombs;

        private OnOpenCellListener onOpenCellListener;

        private int numOfGuesses = 0;

        public Board(int size, int numOfBombs, OnOpenCellListener onOpenCellListener) {
            this.size = size;
            this.numOfBombs = numOfBombs;
            this.onOpenCellListener = onOpenCellListener;
            cells = new Cell[size][size];

            //Инициализируем
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    cells[i][j] = new Cell(i, j);
                }
            }
        }

        public void set(int xClick, int yClick) {
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
            long start = System.currentTimeMillis();
            numOfGuesses++;
            System.out.printf("Open %d,%d%n", x, y);

            //Проиграл?
            if (cells[x][y].hasBomb()) {
                cells[x][y].open();
                onOpenCellListener.onOpenCell(cells[x][y], true);   //interface -> controller -> view
                lostTheGame = true;
                System.out.println("guess: " + (System.currentTimeMillis() - start));
                return;
            }
            //Открыть все не соседние с бомбами клетки
            openAllNulls(cells[x][y]);

            checkIfWon();
            printBoardStatus();
            System.out.println("guess: " + (System.currentTimeMillis() - start));
        }

        private void openAllNulls(Cell cell) {
            Set<Cell> visited = new HashSet<>();
            Queue<Cell> intendToVisit = new LinkedList<>();

            intendToVisit.add(cell);

            while (!intendToVisit.isEmpty()) {
                Cell current = intendToVisit.remove();
                visited.add(current);
                current.open();
                onOpenCellListener.onOpenCell(current, false);

                if (current.neighbourBombs > 0) {
                    continue;
                }

                if (current.x > 0 && !cells[current.x - 1][current.y].hasBomb() && !visited.contains(cells[current.x - 1][current.y]))
                    intendToVisit.add(cells[current.x - 1][current.y]);
                if (current.x < size - 1 && !cells[current.x + 1][current.y].hasBomb() && !visited.contains(cells[current.x + 1][current.y]))
                    intendToVisit.add(cells[current.x + 1][current.y]);

                if (current.y > 0 && !cells[current.x][current.y - 1].hasBomb() && !visited.contains(cells[current.x][current.y - 1]))
                    intendToVisit.add(cells[current.x][current.y - 1]);
                if (current.y < size - 1 && !cells[current.x][current.y + 1].hasBomb() && !visited.contains(cells[current.x][current.y + 1]))
                    intendToVisit.add(cells[current.x][current.y + 1]);
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
            for (Cell[] cellRow : cells) {
                for (Cell cell : cellRow) {
                    if (!cell.isOpen()) {
                        numOfNulls++;
                    }
                }
            }
            if (numOfNulls == numOfBombs && !lostTheGame) {
                wonTheGame = true;
            }
        }

        Cell[][] getCells() {
            return cells;
        }

        public boolean gameFinished() {
            return lostTheGame || wonTheGame;
        }

        public boolean lostTheGame() {
            return lostTheGame;
        }

        public boolean wonTheGame() {
            return wonTheGame;
        }

        ReadableCell[][] getReadableCells() {
            return cells;
        }
    }

    public interface ReadableCell {
        int getNeighbourBombs();

        boolean isOpen();

        boolean hasFlag();

        boolean isMine() throws IllegalStateException;

        int x();

        int y();
    }

    public static class Cell implements ReadableCell {

        public final int x;
        public final int y;

        private boolean isOpen = false;

        private boolean hasBomb = false;

        private boolean flagged = false;

        int neighbourBombs;

        public Cell(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void setBomb(boolean hasBomb) {
            this.hasBomb = hasBomb;
        }

        public boolean hasBomb() {
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
        public int x() {
            return x;
        }

        @Override
        public int y() {
            return y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Cell cell = (Cell) o;

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

    static class SolverCell implements ReadableCell {

        private final ReadableCell delegate;

        SolverCell(ReadableCell cell) {
            delegate = cell;
        }

        private boolean sureBomb = false;
        private boolean sureNotBomb = false;

        @Override
        public boolean isMine() throws IllegalStateException {
            return delegate.isMine();
        }

        private double mineProbability;

        public void correctProbabilityOnValue(double value) {
            mineProbability *= value;
        }

        @Override
        public int getNeighbourBombs() {
            return delegate.getNeighbourBombs();
        }

        @Override
        public boolean hasFlag() {
            return delegate.hasFlag();
        }

        @Override
        public boolean isOpen() {
            return delegate.isOpen();
        }

        @Override
        public int x() {
            return delegate.x();
        }

        @Override
        public int y() {
            return delegate.y();
        }

        public double getMineProbability() {
            if (sureBomb) return 1;
            if (sureNotBomb) return 0;

            return mineProbability;
        }

        void setMineProbability(double prob) {
            this.mineProbability = prob;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SolverCell that = (SolverCell) o;

            return x() == that.x() && y() == that.y();
        }

        @Override
        public int hashCode() {
            int result = x();
            result = 31 * result + y();
            return result;
        }

        @Override
        public String toString() {
            return "Cell(x=" + x() + ", y=" + y() + ", open=" + isOpen() + " " + (isOpen() ? getMineProbability() : "") + ")";
        }
    }

    static class BotGamer implements OnOpenCellListener, FlagManager {
        private final Board board;

        private int detectedBombs;

        private final Set<GroupCell> solverGroups = new HashSet<>();

        protected final SolverCell[][] cells;

        private final OnOpenCellListener originalOnOpenCellListener;
        private final FlagManager flagManager;

        public BotGamer(Board board, FlagManager flagManager) {
            this.board = board;
            originalOnOpenCellListener = board.onOpenCellListener;
            this.flagManager = flagManager;
            board.onOpenCellListener = this;

            ReadableCell[][] readableCells = board.getReadableCells();
            cells = new SolverCell[readableCells.length][readableCells[0].length];
            for (int i = 0; i < readableCells.length; i++) {
                for (int j = 0; j < readableCells[i].length; j++) {
                    cells[i][j] = new SolverCell(readableCells[i][j]);
                }
            }
        }

        /**
         * without flag!
         */
        private List<SolverCell> getExcludedFromGroupsClosedCells() {
            List<SolverCell> excluded = new ArrayList<>();

            for (int i = 0; i < board.size; i++) {
                for (int j = 0; j < board.size; j++) {
                    if (!cells[i][j].isOpen() && getGroups(cells[i][j].x(), cells[i][j].y()).isEmpty() && !cells[i][j].hasFlag()) {
                        excluded.add(cells[i][j]);
                    }
                }
            }
            return excluded;
        }

        private List<SolverCell> getNumeratedCells() {
            List<SolverCell> numeratedCells = new ArrayList<>();

            for (int i = 0; i < board.size; i++) {
                for (int j = 0; j < board.size; j++) {
                    if (cells[i][j].isOpen() && cells[i][j].getNeighbourBombs() > 0) {
                        numeratedCells.add(cells[i][j]);
                    }
                }
            }

            return numeratedCells;
        }

        private List<SolverCell> getAllNeighbours(ReadableCell cell) {
            List<SolverCell> neighbours = new ArrayList<>();

            for (int x = Integer.max(cell.x() - 1, 0); x < Integer.min(cell.x() + 2, board.size); x++) {
                for (int y = Integer.max(cell.y() - 1, 0); y < Integer.min(cell.y() + 2, board.size); y++) {
                    neighbours.add(cells[x][y]);
                }
            }

            return neighbours;
        }

        // first (bigger) contains second (smaller)
        Pair<GroupCell, GroupCell> getFirstContainsGroupOrNull() {
            GroupCell[] groupAsArray = solverGroups.toArray(new GroupCell[0]);
            for (int i = 0; i < groupAsArray.length; i++) {
                for (int j = i + 1; j < groupAsArray.length; j++) {
                    if (groupAsArray[i].contains(groupAsArray[j])) {
                        // если две группы имеют одни и те же клетки, но разное число мин
                        // и возвращаем группу с большим числом мин как бОльшую группу
                        if (groupAsArray[i].cells.size() == groupAsArray[j].cells.size()) {
                            return groupAsArray[i].minesCount > groupAsArray[j].minesCount ? new Pair<>(groupAsArray[i], groupAsArray[j]) : new Pair<>(groupAsArray[j], groupAsArray[i]);
                        }
                        return new Pair<>(groupAsArray[i], groupAsArray[j]);
                    }
                    if (groupAsArray[j].contains(groupAsArray[i]))
                        return new Pair<>(groupAsArray[j], groupAsArray[i]);
                }
            }
            return null;
        }

        /**
         * Возвращает все группы, в которые входит cell
         */
//        Set<GroupCell> getGroups(ReadableCell cell) {
//            //return solverGroups.stream().filter(group -> group.cells.contains(cell)).collect(Collectors.toSet());
//            return solverGroups.stream().filter(group -> group.cells.stream().anyMatch(it -> it.delegate.equals(cell))).collect(Collectors.toSet());
//        }

        Set<GroupCell> getGroups(int x, int y) {
            return solverGroups.stream().filter(group -> group.cells.contains(cells[x][y])).collect(Collectors.toSet());
        }

        void restructureGroups() {
            Pair<GroupCell, GroupCell> containsGroups = getFirstContainsGroupOrNull();

            while (containsGroups != null) {

                GroupCell bigger = containsGroups.getKey();
                GroupCell smaller = containsGroups.getValue();

                // remove from set to avoid mutations in hashset (will be restored at bottom)
                solverGroups.remove(bigger);
                solverGroups.remove(smaller);

                bigger.cells.removeAll(smaller.cells);
                bigger.minesCount -= smaller.minesCount;

                solverGroups.add(bigger);
                if (!smaller.cells.isEmpty())
                    solverGroups.add(smaller);

                containsGroups = getFirstContainsGroupOrNull();
            }

        }

        private void makeGroups() {
            for (SolverCell numeratedCell : getNumeratedCells()) {
                addGroup(numeratedCell);
            }
            restructureGroups();
        }

        private void printProbabilitiesTable() {
            for (int i = 0; i < board.size; i++) {
                for (int j = 0; j < board.size; j++) {
                    System.out.printf("%+.2f  ", cells[j][i].isOpen() ? -1 : (cells[j][i].sureBomb ? 9 : cells[j][i].getMineProbability()));
                }
                System.out.println();
            }
        }

        public void play() {
            makeGroups();

            do {
                restructureGroups();
                evaluateClearGroupsProbabilities();

                for (SolverCell[] cellsRow : cells) {
                    for (SolverCell cell : cellsRow) {
                        if (!cell.sureBomb && !cell.sureNotBomb && !getGroups(cell.x(), cell.y()).isEmpty()) {
                            evaluateCellsProbabilities(cell);
                        }
                    }
                }

                for (int i = 0; i < 100; i++) {
                    solverGroups.forEach(GroupCell::correctProbabilityOnMinesCount);
                }

                List<SolverCell> closedExcluded = getExcludedFromGroupsClosedCells();
                closedExcluded.forEach(it -> it.setMineProbability(((double) board.numOfBombs - detectedBombs) / closedExcluded.size()));

                SolverCell safest = getSafestCell();

                printProbabilitiesTable();
                board.guess(safest.x(), safest.y());

            } while (!board.gameFinished());
        }

        private SolverCell getSafestCell() {
            SolverCell minProbCell = null;

            loop:
            for (SolverCell[] cellsRow : cells) {
                for (SolverCell cell : cellsRow) {
                    if (!cell.isOpen()) {
                        minProbCell = cell;
                        break loop;
                    }
                }
            }
            assert minProbCell != null;

            for (SolverCell[] cellsRow : cells) {
                for (SolverCell cell : cellsRow) {
                    if (!cell.isOpen()) {
                        if (cell.getMineProbability() < minProbCell.getMineProbability()) minProbCell = cell;
                    }
                }
            }
            return minProbCell;
        }

        @Override
        public void onOpenCell(ReadableCell rc, boolean asBoomCell) {
            SolverCell cell = cells[rc.x()][rc.y()];

            removeCellFromGroups(cell);

            if (cell.getNeighbourBombs() > 0) {
                addGroup(cell);
            }

            originalOnOpenCellListener.onOpenCell(cell, asBoomCell);
        }

        void evaluateClearGroupsProbabilities() {
            solverGroups.forEach(group -> {
                if (group.minesCount == 0) {
                    group.cells.forEach(cell -> cell.sureNotBomb = true);
                } else if (group.minesCount == group.cells.size() || group.cells.size() == group.minesCount - group.cells.stream().filter(it -> it.sureBomb).count()) {
                    group.cells.forEach(cell -> {
                        if (!cell.sureBomb) {
                            cell.sureBomb = true;
                            this.setFlag(cell.x(), cell.y());
                        }
                    });
                }
            });
        }

        private void evaluateCellsProbabilities(SolverCell cell) {
            Set<GroupCell> cellGroups = getGroups(cell.x(), cell.y());

            double mulProb = cellGroups.stream().mapToDouble(group -> 1 - (double) group.minesCount / group.cells.size()).reduce(1, (a, b) -> a * b);

            cell.setMineProbability(1 - mulProb);
        }

        void addGroup(ReadableCell cell) {
            List<SolverCell> closedNeighbours = getAllNeighbours(cell).stream().filter(it -> !it.isOpen()).collect(Collectors.toList());
            if (closedNeighbours.isEmpty()) return;

            GroupCell group = new GroupCell(closedNeighbours, cell.getNeighbourBombs());
            solverGroups.add(group);
        }

        private void removeCellFromGroups(SolverCell cell) {

            Set<GroupCell> cellGroups = new HashSet<>(getGroups(cell.x(), cell.y()));

            if (cellGroups.isEmpty()) return;

            // update solverGroups set to avoid mutations in hashset
            solverGroups.removeAll(getGroups(cell.x(), cell.y()));

            cellGroups.forEach(it -> it.removeCell(cell));

            solverGroups.addAll(cellGroups.stream().filter(it -> !it.cells.isEmpty()).collect(Collectors.toList()));
        }

        @Override
        public void setFlag(int x, int y) {
            detectedBombs++;
            flagManager.setFlag(x, y);
        }

        /**
         * Not used
         */
        @Override
        public void removeFlag(int x, int y) {
            flagManager.removeFlag(x, y);
        }
    }

    static class GroupCell {
        int minesCount;

        protected Set<SolverCell> cells;

        GroupCell(Collection<SolverCell> cells, int bombs) {
            this.minesCount = bombs;
            this.cells = new HashSet<>(cells);
        }

        public Set<SolverCell> getCells() {
            return cells;
        }

        public void removeCell(SolverCell cell) {
            this.cells.remove(cell);
        }

        public boolean contains(GroupCell other) {
            return this.cells.containsAll(other.cells);
        }

        /*public Set<SolverCell> intersectWith(GroupCell other) {
            Set<SolverCell> intersections = new HashSet<>(this.cells);
            intersections.retainAll(other.cells);

            return intersections;
        }

        public boolean intersects(GroupCell other) {
            return !intersectWith(other).isEmpty();
        }*/

        public void correctProbabilityOnMinesCount() {
            if (this.minesCount == 0) return;

            double correction = (double) this.minesCount / (cells.stream().mapToDouble(SolverCell::getMineProbability).sum());

            cells.forEach(it -> {
                if (!it.sureBomb && !it.sureNotBomb) it.correctProbabilityOnValue(correction);
            });
        }

        @Override
        public String toString() {
            return "Group(cells=" + cells + ", bombs=" + this.minesCount + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            GroupCell that = (GroupCell) o;

            if (minesCount != that.minesCount) return false;
            return cells.equals(that.cells);
        }

        @Override
        public int hashCode() {
            int result = minesCount;
            result = 31 * result + cells.hashCode();
            return result;
        }
    }

    @FunctionalInterface
    public interface OnOpenCellListener {
        void onOpenCell(ReadableCell cell, boolean asBoomCell); // в controller openTile()
    }

    public interface FlagManager {
        void setFlag(int x, int y);
        void removeFlag(int x, int y);
    }
}


