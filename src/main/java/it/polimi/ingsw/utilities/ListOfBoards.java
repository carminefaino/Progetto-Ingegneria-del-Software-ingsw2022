package it.polimi.ingsw.utilities;

import it.polimi.ingsw.model.board.Board;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ListOfBoards implements Serializable {
    private List<Board> boards = new ArrayList<>();

    public ListOfBoards(List<Board> boards) {
        this.boards = boards;
    }

    public List<Board> getBoards() {
        return boards;
    }
}
