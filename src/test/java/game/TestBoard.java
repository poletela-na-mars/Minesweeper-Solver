package game;

public class TestBoard extends Model.Board {
    public TestBoard(int size, int numOfBombs) {
        super(size, numOfBombs);
    }

    public void openCell(int x, int y) {
        cells[x][y].open();
    }

    public int getNeighbourBombs(int x, int y) {
        return cells[x][y].neighbourBombs;
    }

    public void setNeighbourBombs(int x, int y, int bombs) {
        cells[x][y].neighbourBombs = bombs;
    }

    public Model.CellImpl getCell(int x, int y) {
        return cells[x][y];
    }
}
