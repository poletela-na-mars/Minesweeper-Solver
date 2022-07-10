package game.solver;

import game.Model;

class SolverCell extends Model.Cell {

    private final Model.Cell delegate;

    SolverCell(Model.Cell cell) {
        super(cell.x, cell.y);
        delegate = cell;
    }

    private boolean sureBomb = false;
    private boolean sureNotBomb = false;

    boolean sureBomb() {
        return sureBomb;
    }

    boolean sureNotBomb() {
        return sureNotBomb;
    }

    void markAs(boolean isBomb) {
        if (isBomb) sureBomb = true;
        else sureNotBomb = true;
    }

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

        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }

    @Override
    public String toString() {
        return "Cell(x=" + x + ", y=" + y + ", open=" + isOpen() + " " + (isOpen() ? getMineProbability() : "") + ")";
    }
}