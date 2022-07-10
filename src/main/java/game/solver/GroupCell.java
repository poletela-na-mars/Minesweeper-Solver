package game.solver;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class GroupCell {
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

    public void correctProbabilityOnMinesCount() {
        if (this.minesCount == 0) return;

        double correction = (double) this.minesCount / (cells.stream().mapToDouble(SolverCell::getMineProbability).sum());

        cells.forEach(it -> {
            if (!it.sureBomb() && !it.sureNotBomb()) it.correctProbabilityOnValue(correction);
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
