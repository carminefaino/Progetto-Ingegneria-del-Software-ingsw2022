package it.polimi.ingsw.model;

public class Island {
    private int idIsland;
    private int[] studentOnIsland = new int[5];
    private Player owner;

    public Island(int idIsland) {
        this.idIsland = idIsland;
        for(int i = 0; i < 5; i++) {
            studentOnIsland[i] = 0;
        }
    }

    public Player getOwner() {
        return owner;
    }

    public int[] getAllStudents() {
        return studentOnIsland;
    }

    public int getStudentsByColor(int color) {
        return studentOnIsland[color];
    }

    public void addStudent(int color) {
        studentOnIsland[color] += 1;
    }

    public void changeOwner(Player player) {
        this.owner = player;
    }
}