package game;

import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

//board + cell + bot gamer + group cells
public class Model {

    private final Board board;

    private Model.BotGamer bot;

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

    public void solveWithBot(Function<Void, Void> onFinish) {
        this.bot = new BotGamer(board);

        Thread solutionThread = new Thread(() -> {
            bot.play();
            onFinish.apply(null);
        });

        solutionThread.start();

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
            numOfGuesses++;
        }

        public void guess(int x, int y) {
            numOfGuesses++;
            System.out.printf("Open %d,%d%n", x, y);

            //Проиграл?
            if (cells[x][y].hasBomb()) {
                cells[x][y].open();
                onOpenCellListener.onOpenCell(cells[x][y], true);
                lostTheGame = true;
                return;
            }
            //Открыть все не соседние с бомбами клетки
            openAllNulls(cells[x][y]);

            analyseProcess();
            printProcess();
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

        public void printProcess() {
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

        public void analyseProcess() {
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

        public Cell[][] getCells() {
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

        public int getNumOfGuesses() {
            return numOfGuesses;
        }
    }

    public interface ReadableCell {
        boolean hasBomb();
        int getNeighbourBombs();
        boolean isOpen();
        boolean hasFlag();

        int x();
        int y();
    }

    static class Cell implements ReadableCell {

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

        @Override
        public boolean hasBomb() {
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
    }

    /*class Gamer {

        private final int size;

        private final int numOfBombs;

        public Gamer(int size, int numOfBombs) {
            this.size = size;
            this.numOfBombs = numOfBombs;
        }

        public int getNumOfBombs() {
            return numOfBombs;
        }

        public int getSize() {
            return size;
        }
    }*/

    static class SolverCell implements ReadableCell {

        private final ReadableCell delegate;

        SolverCell(ReadableCell cell) {
            delegate = cell;
        }

        private boolean isPossibleBomb;

        public void markAsPossibleBomb(boolean isBomb) {
            isPossibleBomb = isBomb;
        }

        public boolean isPossibleBomb() {
            return isPossibleBomb;
        }

        @Override
        public boolean hasBomb() {
            return delegate.hasBomb();
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
    }

    static class BotGamer {

        // TODO it's unused
        private Pair<Integer, Integer> lastGuess;

        private Board board;

        private int detectedBombs;

        List<Pair<Integer, Integer>> coordinates;

        Set<GroupCells> groups;

        private final SolverCell[][] cells;


        public BotGamer(Board board) {
            this.board = board;
            coordinates = new ArrayList<>();

            ReadableCell[][] readableCells = board.getReadableCells();
            cells = new SolverCell[readableCells.length][readableCells[0].length];
            for (int i = 0; i < readableCells.length; i++) {
                for (int j = 0; j < readableCells[i].length; j++) {
                    cells[i][j] = new SolverCell(readableCells[i][j]);
                }
            }

            for (int i = 0; i < board.size; i++) {
                for (int j = 0; j < board.size; j++) {
                    coordinates.add(new Pair<>(i, j));
                }
            }
        }

        void makeGroups() {
            groups = new HashSet<>();
            Set<Pair<Integer, Integer>> guaranteedBombs = new HashSet<>();
            for (int i = 0; i < board.size; i++) {
                for (int j = 0; j < board.size; j++) {
                    SolverCell cell = cells[i][j];
                    if (cell.isOpen() && cell.getNeighbourBombs() > 0) {   //если клетка с цифрой, не пустая
                        Set<Pair<Integer, Integer>> coordinates = new HashSet<>();
                        for (int x = Integer.max(i - 1, 0); x < Integer.min(i + 2, board.size); x++) {
                            for (int y = Integer.max(j - 1, 0); y < Integer.min(j + 2, board.size); y++) {
                                if (!cells[x][y].isOpen()) {
                                    coordinates.add(new Pair<>(x, y));
                                    continue;
                                }
                                if (cells[x][y].isPossibleBomb) {
                                    Pair<Integer, Integer> pair = new Pair<>(x, y);
                                    coordinates.add(pair);
                                    guaranteedBombs.add(pair);
                                }
                            }
                        }
                        groups.add(new GroupCells(coordinates, cell.getNeighbourBombs()));
                    }
                }
            }
            groups.add(new GroupCells(guaranteedBombs, guaranteedBombs.size()));
            Set<GroupCells> groupsToAdd = new HashSet<>();
            Set<GroupCells> lastGroups = new HashSet<>();
            GroupCells emptyGroup = new GroupCells(new HashSet<>(), 0);
            boolean noChanges = false;
            while (!noChanges) {
                groups.remove(emptyGroup);
                lastGroups.clear();
                lastGroups.addAll(groups);
                groups.clear();
                for (GroupCells group : lastGroups) {
                    groups.addAll(group.split()); //разбивка по группам по количеств бомб в группе
                }
                for (GroupCells group1 : groups) {
                    for (GroupCells group2 : groups) {
                        groupsToAdd.add(group1.mergeOther(group2)); //делаем группы не пересекающимися
                    }
                }
                groups.addAll(groupsToAdd);
                groupsToAdd.clear();
                noChanges = true;
                for (GroupCells group : groups) {
                    noChanges = noChanges && lastGroups.contains(group);
                }
            }
        }

        //true - когда можно открывать группу ("чистая")
        boolean setGuaranteed() {
            boolean res = false;
            for (GroupCells group : groups) {
                if (group.getBombsCount() == 0 && group.getGroup().size() != 0) {
                    for (Pair<Integer, Integer> pair : group.getGroup()) {
                        board.guess(pair.getKey(), pair.getValue());
                        if (board.gameFinished()) {
                            return true;
                        }
                        // analogue is upper
//                        if (guess(pair.getKey(), pair.getValue())) {
//                            return true;
//                        }
                    }
                    res = true;
                }
                if (group.getBombsCount() == group.getGroup().size()) {
                    for (Pair<Integer, Integer> pair : group.getGroup()) {
                        int x = pair.getKey();
                        int y = pair.getValue();
                        if (!cells[x][y].isOpen() && !cells[x][y].isPossibleBomb) {
                            detectedBombs++;
                            cells[x][y].markAsPossibleBomb(true);
                        }
                    }
                }
            }
            return res;
        }

        //Когда совпадет число в ячейке с количеством бомб вокруг
        private boolean checkingIfPossibleComb(int[] bombsPlacing, int[] bombsCountInAllCases) {
            for (int i : bombsPlacing) {
                int x = coordinates.get(i).getKey();
                int y = coordinates.get(i).getValue();
                cells[x][y].markAsPossibleBomb(true);
            }
            boolean rightComb = true;
            loop:
            for (int i = 0; i < board.size; i++) {
                for (int j = 0; j < board.size; j++) {
                    if (cells[i][j].isOpen() && !cells[i][j].isPossibleBomb) { //если открыта и не бомба
                        int bombsCount = 0;
                        for (int x = Integer.max(i - 1, 0); x < Integer.min(i + 2, board.size); x++) {
                            for (int y = Integer.max(j - 1, 0); y < Integer.min(j + 2, board.size); y++) {
                                if (cells[x][y].isOpen() && cells[x][y].isPossibleBomb) {
                                    bombsCount++;
                                }
                            }
                        }
                        if (bombsCount != cells[i][j].getNeighbourBombs()) {
                            rightComb = false;
                            break loop;
                        }
                    }
                }
            }
            for (int i : bombsPlacing) {
                int x = coordinates.get(i).getKey();
                int y = coordinates.get(i).getValue();
                cells[x][y].markAsPossibleBomb(false);
                if (rightComb) {
                    bombsCountInAllCases[i]++;
                }
            }
            return rightComb;
        }

        boolean enumerationOfCombs() {
            int closedBombs = board.numOfBombs - detectedBombs;
            if (closedBombs == 0) {
                for (Pair<Integer, Integer> coord : coordinates) {
                    board.guess(coord.getKey(), coord.getValue());
                }
                return true;
            }
            int closedCells = coordinates.size();
            // Для быстродействия, поскольку при нескольких известных ячейках, солвер очень долго думает
            // Если примерно половина открыта, то воспользуемся enumerationOfCombs, если нет, то рандомные клики по null в play
            if (closedCells > (board.size / 2)) {
                return false;
            }
            int combsCount = 0;
            int[] bombsPlacing = new int[closedBombs];
            int[] bombInCellCases = new int[closedCells];
            for (int i = 0; i < closedBombs; i++) {
                bombsPlacing[i] = i;
            }
            while (true) {
                if (checkingIfPossibleComb(bombsPlacing, bombInCellCases)) {
                    combsCount++;
                }
                if (bombsPlacing[0] == closedCells - closedBombs) {
                    break;
                }
                for (int i = 0; i < closedBombs; i++) {
                    if (i != closedBombs - 1) {
                        if ((bombsPlacing[i] == closedCells - closedBombs + i)) throw new AssertionError();
                        if (bombsPlacing[i + 1] == closedCells - closedBombs + i + 1) {
                            bombsPlacing[i]++;
                            for (int j = i + 1; j < closedBombs; j++) {
                                bombsPlacing[j] = bombsPlacing[j - 1] + 1;
                            }
                            break;
                        }
                    } else {
                        if ((bombsPlacing[i] >= closedCells - 1)) throw new AssertionError();
                        bombsPlacing[i]++;
                    }
                }
            }
            boolean res = false;
            for (int i = 0; i < closedCells; i++) {
                int x = coordinates.get(i).getKey();
                int y = coordinates.get(i).getValue();
                if (bombInCellCases[i] == combsCount) {
                    detectedBombs++;
                    cells[x][y].markAsPossibleBomb(true);
                }
                if (bombInCellCases[i] == 0) {
                    res = true;

                    board.guess(x, y);

                    if (board.gameFinished()) {
                        return true;
                    }
                }
            }
                return res;
        }

        public void play() {
            while (!board.gameFinished()) {
                coordinates.removeIf(coord -> cells[coord.getKey()][coord.getValue()].isOpen());
                makeGroups();
                if (!setGuaranteed()) {
                    coordinates.removeIf(coord -> cells[coord.getKey()][coord.getValue()].isOpen());
                    if (enumerationOfCombs()) {
                        continue;
                    }
                    coordinates.removeIf(coord -> cells[coord.getKey()][coord.getValue()].isOpen());
                    if (coordinates.size() == 0) {
                        continue;
                    }

                    int index = (int) (Math.random() * coordinates.size());
                    Pair<Integer, Integer> pair = coordinates.get(index);
                    board.guess(pair.getKey(), pair.getValue());

                }
            }
        }


    }

    static class GroupCells {
        private final Set<Pair<Integer, Integer>> group;

        private int bombsCount;

        public GroupCells(Set<Pair<Integer, Integer>> group, int bombsCount) {
            this.group = group;
            this.bombsCount = bombsCount;
        }

        public GroupCells mergeOther(@NotNull GroupCells other) {
            if (this.equals(other))
                return this;
            Set<Pair<Integer, Integer>> newGroup = new HashSet<>(group);
            newGroup.retainAll(other.group); //то, что не содержится в other.group, удалить в newGroup (ТО, ЧТО ЕСТЬ И В GROUP, И В OTHER.GROUP)
            if (newGroup.isEmpty())
                return this;
            //Делаем группы не пересекающимися
            if (group.equals(newGroup)) {
                other.bombsCount = other.bombsCount - bombsCount;
                other.group.removeAll(newGroup); //удалить в other.group то, что есть в newGroup
                return other;
            }
            if (other.group.equals(newGroup)) {
                bombsCount = bombsCount - other.bombsCount;
                group.removeAll(newGroup);
                return this;
            }
            return this;
        }

        public Set<GroupCells> split() {
            Set<GroupCells> res = new HashSet<>();
            int bomb = bombsCount == group.size() ? 1 : bombsCount == 0 ? 0 : -1;
            if (bomb >= 0) {
                for (Pair<Integer, Integer> coords : group) {
                    Set<Pair<Integer, Integer>> set = new HashSet<>();
                    set.add(coords);
                    res.add(new GroupCells(set, bomb));
                }
            } else {
                res.add(this);
            }
            return res;
        }

        public Set<Pair<Integer, Integer>> getGroup() {
            return group;
        }

        public int getBombsCount() {
            return bombsCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GroupCells that = (GroupCells) o;
            return bombsCount == that.bombsCount &&
                    Objects.equals(group, that.group);
        }

        @Override
        public int hashCode() {
            return Objects.hash(group, bombsCount);
        }
    }


    @FunctionalInterface
    interface OnOpenCellListener {
        void onOpenCell(ReadableCell cell, boolean asBoomCell);
    }
}


