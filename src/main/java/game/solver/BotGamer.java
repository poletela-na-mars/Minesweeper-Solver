package game.solver;

import game.Model;
import javafx.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class BotGamer implements Model.OnOpenCellListener, Model.FlagManager {
    private final Model.Board board;

    private int detectedBombs;

    private final Map<SolverCell, Set<GroupCell>> cellsToGroups = new HashMap<>();

    protected final SolverCell[][] cells;
    
    private final Model.FlagManager flagManager;

    public BotGamer(Model.Board board, Model.FlagManager flagManager) {
        this.board = board;
        this.flagManager = flagManager;
        board.addOnOpenCellListener(this);

        Model.Cell[][] readableCells = board.getCells();
        cells = new SolverCell[readableCells.length][readableCells[0].length];
        for (int i = 0; i < readableCells.length; i++) {
            for (int j = 0; j < readableCells[i].length; j++) {
                cells[i][j] = new SolverCell(readableCells[i][j]);
                cellsToGroups.put(cells[i][j], new HashSet<>());
            }
        }
    }

    private Set<GroupCell> getAllGroups() {
        return cellsToGroups.entrySet().stream().flatMap(it -> it.getValue().stream()).collect(Collectors.toSet());
    }

    /**
     * without flag!
     */
    private List<SolverCell> getExcludedFromGroupsClosedCells() {
        List<SolverCell> excluded = new ArrayList<>();

        for (int i = 0; i < board.size; i++) {
            for (int j = 0; j < board.size; j++) {
                if (!cells[i][j].isOpen() && getGroups(cells[i][j].x, cells[i][j].y).isEmpty() && !cells[i][j].hasFlag()) {
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

    private List<SolverCell> getAllNeighbours(Model.Cell cell) {
        List<SolverCell> neighbours = new ArrayList<>();

        for (int x = Integer.max(cell.x - 1, 0); x < Integer.min(cell.x + 2, board.size); x++) {
            for (int y = Integer.max(cell.y - 1, 0); y < Integer.min(cell.y + 2, board.size); y++) {
                neighbours.add(cells[x][y]);
            }
        }

        return neighbours;
    }

//    Pair<GroupCell, GroupCell> getFirstContainsGroupOrNull() {
//        GroupCell[] groupAsArray = getAllGroups().toArray(new GroupCell[0]);
//        for (int i = 0; i < groupAsArray.length; i++) {
//            for (int j = i + 1; j < groupAsArray.length; j++) {
//                if (groupAsArray[i].contains(groupAsArray[j])) {
//                    // если две группы имеют одни и те же клетки, но разное число мин
//                    // и возвращаем группу с большим числом мин как бОльшую группу
//                    if (groupAsArray[i].cells.size() == groupAsArray[j].cells.size()) {
//                        return groupAsArray[i].minesCount > groupAsArray[j].minesCount ? new Pair<>(groupAsArray[i], groupAsArray[j]) : new Pair<>(groupAsArray[j], groupAsArray[i]);
//                    }
//                    return new Pair<>(groupAsArray[i], groupAsArray[j]);
//                }
//                if (groupAsArray[j].contains(groupAsArray[i]))
//                    return new Pair<>(groupAsArray[j], groupAsArray[i]);
//            }
//        }
//        return null;
//    }

    // first (bigger) contains second (smaller)
    Pair<GroupCell, GroupCell> getFirstContainsGroupOrNull() {
        Map<SolverCell, Set<GroupCell>> cells = cellsToGroups.entrySet().stream().filter(it -> !it.getValue().isEmpty()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        for (Map.Entry<SolverCell, Set<GroupCell>> cell: cells.entrySet()) {
            GroupCell[] groups = cell.getValue().toArray(GroupCell[]::new);

            for (int i = 0; i < groups.length; i++) {
                for (int j = i + 1; j < groups.length; j++) {
                    if (groups[i].contains(groups[j])) {
                        // если две группы имеют одни и те же клетки, но разное число мин
                        // и возвращаем группу с большим числом мин как бОльшую группу
                        if (groups[i].cells.size() == groups[j].cells.size()) {
                            return groups[i].minesCount > groups[j].minesCount ? new Pair<>(groups[i], groups[j]) : new Pair<>(groups[j], groups[i]);
                        }
                        return new Pair<>(groups[i], groups[j]);
                    }
                    if (groups[j].contains(groups[i]))
                        return new Pair<>(groups[j], groups[i]);
                }
            }
        }
        return null;
    }

    /**
     * Возвращает все группы, в которые входит cell
     */
    Set<GroupCell> getGroups(int x, int y) {
        return new HashSet<>(cellsToGroups.get(cells[x][y]));
    }

    void restructureGroups() {
        Pair<GroupCell, GroupCell> containsGroups = getFirstContainsGroupOrNull();

        while (containsGroups != null) {

            GroupCell bigger = containsGroups.getKey();
            GroupCell smaller = containsGroups.getValue();

            // remove from set to avoid mutations in hashset (will be restored at bottom)
            cellsToGroups.forEach((cell, groups) -> {
                groups.remove(bigger);
                groups.remove(smaller);
            });

            bigger.cells.removeAll(smaller.cells);
            bigger.minesCount -= smaller.minesCount;

            bigger.cells.forEach(it -> cellsToGroups.get(it).add(bigger));
            if (!smaller.cells.isEmpty()) {
                smaller.cells.forEach(it -> cellsToGroups.get(it).add(smaller));
            }

            containsGroups = getFirstContainsGroupOrNull();
        }
    }

    private void makeGroups() {
        for (SolverCell numeratedCell : getNumeratedCells()) {
            addGroup(numeratedCell);
        }
        restructureGroups();
    }

    public void play() {
        makeGroups();

        do {
            SolverCell sureNotBomb = Arrays.stream(cells).flatMap(Arrays::stream)
                    .filter(it -> it.sureNotBomb() && !it.isOpen()).findFirst().orElse(null);
            if (sureNotBomb != null) {
                board.guess(sureNotBomb.x, sureNotBomb.y);
                continue;
            }

            restructureGroups();
            evaluateClearGroupsProbabilities();

            for (SolverCell[] cellsRow : cells) {
                for (SolverCell cell : cellsRow) {
                    if (!cell.sureBomb() && !cell.sureNotBomb() && !getGroups(cell.x, cell.y).isEmpty()) {
                        evaluateCellsProbabilities(cell);
                    }
                }
            }

            Set<GroupCell> allGroups = getAllGroups();
            for (int i = 0; i < 100; i++) {
                allGroups.forEach(GroupCell::correctProbabilityOnMinesCount);
            }

            List<SolverCell> closedExcluded = getExcludedFromGroupsClosedCells();
            closedExcluded.forEach(it -> it.setMineProbability(((double) board.numOfBombs - detectedBombs) / closedExcluded.size()));

            SolverCell safest = getSafestCell();

            board.guess(safest.x, safest.y);

        } while (!board.gameFinished());
    }

    private SolverCell getSafestCell() {
        return Arrays.stream(cells).flatMap(Arrays::stream).filter(it -> !it.isOpen())
                .min(Comparator.comparingDouble(SolverCell::getMineProbability)).get();
    }

    @Override
    public void onOpenCell(Model.Cell rc, boolean asBoomCell) {
        SolverCell cell = cells[rc.x][rc.y];

        removeCellFromGroups(cell);

        if (cell.getNeighbourBombs() > 0) {
            addGroup(cell);
        }
    }

    void evaluateClearGroupsProbabilities() {
        getAllGroups().forEach(group -> {
            if (group.minesCount == 0) {
                group.cells.forEach(cell -> cell.markAs(false));
            } else if (group.minesCount == group.cells.size()) {
                group.cells.forEach(cell -> {
                    if (!cell.sureBomb()) {
                        cell.markAs(true);
                        this.setFlag(cell.x, cell.y);
                    }
                });
            }
        });
    }

    private void evaluateCellsProbabilities(SolverCell cell) {
        Set<GroupCell> cellGroups = getGroups(cell.x, cell.y);

        double mulProb = cellGroups.stream().mapToDouble(group -> 1 - (double) group.minesCount / group.cells.size()).reduce(1, (a, b) -> a * b);

        cell.setMineProbability(1 - mulProb);
    }

    void addGroup(Model.Cell cell) {
        List<SolverCell> closedNeighbours = getAllNeighbours(cell).stream().filter(it -> !it.isOpen()).collect(Collectors.toList());
        if (closedNeighbours.isEmpty()) return;

        GroupCell group = new GroupCell(closedNeighbours, cell.getNeighbourBombs());
        group.cells.forEach(it -> cellsToGroups.get(it).add(group));
    }

    private void removeCellFromGroups(SolverCell cell) {

        Set<GroupCell> cellGroups = new HashSet<>(getGroups(cell.x, cell.y));

        if (cellGroups.isEmpty()) return;

        // update groups set to avoid mutations in hashset
        cellsToGroups.forEach((solverCell, groups) -> groups.removeAll(cellGroups));
        cellsToGroups.get(cell).clear();

        cellGroups.forEach(it -> it.removeCell(cell));
        cellGroups.forEach(group -> group.cells.forEach(it -> cellsToGroups.get(it).add(group)));
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
