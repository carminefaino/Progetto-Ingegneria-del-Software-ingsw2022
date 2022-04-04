package it.polimi.ingsw.model;

import it.polimi.ingsw.constants.Constants;
import it.polimi.ingsw.model.expertMode.Character8;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;

public class Game {
    private List<Player> listOfPlayers ;
    private List<Archipelago> listOfArchipelagos;
    private List<Cloud> listOfClouds;
    private List<Player> orderOfPlayers;
    private Bag bag;
    private int numberOfPlayers;
    private Player currentPlayer;
    private Phase phase;
    private boolean endTurn;
    private int bank;
    private int[] playableCharacters;

    private PropertyChangeSupport support;

    //Aggiungi expertMode come parametro
    public Game(int numberOfPlayers, Player player){
        this.support = new PropertyChangeSupport(this);
        this.listOfPlayers = new ArrayList<>();
        this.listOfArchipelagos = new ArrayList<>();
        this.listOfClouds = new ArrayList<>();
        this.orderOfPlayers = new ArrayList<>();
        this.numberOfPlayers = numberOfPlayers;
        listOfPlayers.add(player);
        orderOfPlayers.add(player);
        this.bag = new Bag(numberOfPlayers);
        this.currentPlayer = player;
        phase = Phase.CARD_SELECTION;

        for(int i = 0; i< Constants.NUMBEROFISLANDS; i++){
            Archipelago arc = new Archipelago(i);
            listOfArchipelagos.add(arc);
        }
        for(int i = 0; i < numberOfPlayers; i++){
            Cloud cloud = new Cloud(i);
            listOfClouds.add(cloud);
        }

        int[] studentsForIslands = new int[Constants.NUMBEROFKINGDOMS];
        //Place randomly two students of each color on the islands (one student per island)
        //except on the island where MN is present and on the opposite one
        for(int i = 0; i < Constants.NUMBEROFKINGDOMS; i++){
            studentsForIslands[i] = 2;
        }
        Random random = new Random();
        for(Archipelago a : this.listOfArchipelagos){
            for(Island i : a.getBelongingIslands()) {
                if(a.getIdArchipelago() != 0 && a.getIdArchipelago() !=5) {
                    int value = random.nextInt(Constants.NUMBEROFKINGDOMS);
                    while (studentsForIslands[value] <= 0) {
                        value = random.nextInt(Constants.NUMBEROFKINGDOMS);
                    }
                    i.addStudent(StudsAndProfsColor.values()[value]);
                    studentsForIslands[value]--;
                }
            }
        }

        //EXPERT MODE:

        //Extract 3 characters that will be available for the game
        this.playableCharacters = new int[Constants.NUMBEROFPLAYABLECHARACTERS];
        int i = 0;
        while (i < Constants.NUMBEROFPLAYABLECHARACTERS) {
            int value = random.nextInt(Constants.NUMBEROFCHARACTERS);
            boolean found = false;

            for (int playableCharacter : playableCharacters) {
                if (playableCharacter == value) {
                    found = true;
                    break;
                }
            }
            if(!found){
                playableCharacters[i]=value;
                i++;
            }
        }


    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        support.addPropertyChangeListener(pcl);
    }
    /**
     * Add a new player to the list of players
     * @param player to be added to the game
     */
    public void addPlayer(Player player){
        int oldSize = listOfPlayers.size();
        listOfPlayers.add(player);
        orderOfPlayers.add(player);
        support.firePropertyChange("listOfPlayers", oldSize, listOfPlayers.size());
    }

    public List<Player> getListOfPlayer(){
        return this.listOfPlayers;
    }

    public List<Cloud> getListOfClouds(){
        return this.listOfClouds;
    }

    public Player getCurrentPlayer(){
        return this.currentPlayer;
    }

    /**
     * For each player, according to the power of the card used, calculate his order in the next turn
     */
    public void findPlayerOrder(){

        Collections.sort(orderOfPlayers, new Comparator<Player>() {
            @Override
            public int compare(Player o1, Player o2) {
                return o1.getLastUsedCard().compareTo(o2.getLastUsedCard());
            }
        });
        this.currentPlayer = orderOfPlayers.get(0);
    }

    public List<Player> getOrderOfPlayers(){
        return orderOfPlayers;
    }

    //TODO: add an observer that when MN change the position calculate the influence
    /**
     * Move Mother Nature from the current archipelago to another one
     * @param steps number of steps that MN needs to do
     */
    public void moveMotherNature(int steps){
        int index = 0;
        for(Archipelago arci : listOfArchipelagos){
            if(arci.getIsMNPresent()){
                index = arci.getIdArchipelago();
                arci.changeMNPresence();
            }
        }
        index = (index + steps)%listOfArchipelagos.size();
        for(Archipelago arci : listOfArchipelagos){
            if(arci.getIdArchipelago() == index){
                arci.changeMNPresence();
            }
        }
    }

    public List<Archipelago> getListOfArchipelagos(){
        return listOfArchipelagos;
    }

    /**
     * Calculate who is the current player according to the ordered list of players for the action phase
     */
    public void calculateNextPlayerAction(){
        int index = 0;
        for( Player p : orderOfPlayers){
            if(p.getNickname().equals(currentPlayer.getNickname())){
                break;
            }
            index ++;
        }
        index = index + 1;
        currentPlayer = orderOfPlayers.get(index);
    }

    /**
     * Calculate who is the current player according to the  list of players for the pianification phase
     */
    public void calculateNextPlayerPianification(){
        int index = 0;
        for( Player p : listOfPlayers){
            if(p.getNickname().equals(currentPlayer.getNickname())){
                break;
            }
            index ++;
        }
        index = index + 1;
        currentPlayer = listOfPlayers.get(index);
    }

    /**
     * Check if a professor needs to be assigned to a player and do it
     * @param color of the professor to check
     */
    public void assignProfessor(StudsAndProfsColor color){
        if(currentPlayer.getUsedCharacter() == 8){
            Character8 character8 = new Character8(this);
            character8.assignProfessor(color);
        }
        else{
            Player player = currentPlayer;
            int max = 0;
            for(Player p : listOfPlayers){
                if(p != currentPlayer){
                    if(p.getMyBoard().getDiningRoom().getStudentsByColor(color) > max){
                        player = p;
                        max = p.getMyBoard().getDiningRoom().getStudentsByColor(color);
                    }
                }
            }
            if(currentPlayer.getMyBoard().getDiningRoom().getStudentsByColor(color) == player.getMyBoard().getDiningRoom().getStudentsByColor(color)){
                player.getMyBoard().getProfessorsTable().removeProfessor(color);
            }
            if(currentPlayer.getMyBoard().getDiningRoom().getStudentsByColor(color) > player.getMyBoard().getDiningRoom().getStudentsByColor(color)){
                player.getMyBoard().getProfessorsTable().removeProfessor(color);
                currentPlayer.getMyBoard().getProfessorsTable().addProfessor(color);
            }
        }
    }

    /**
     * Unify two archipelagos adding the islands of the archToBeUnified to the one of the actualArchipelago
     * @param actualArchipelago Archipelago where MN is present
     * @param archToBeUnified Archipelago to be unified to the actual one
     */
    public void unifyArchipelagos(Archipelago actualArchipelago, Archipelago archToBeUnified){
        for(Island island: archToBeUnified.getBelongingIslands()){
            actualArchipelago.addIsland(island);
        }
        listOfArchipelagos.remove(archToBeUnified);

    }

    public Phase getPhase(){
        return phase;
    }

    /**
     * Decide the nextPhase of the match
     */
    //every time that the phase of a player change send a message to everyone
    //saying what should he do
    public void nextPhase(){
        switch (phase){
            case CARD_SELECTION:
                phase = Phase.MOVE_STUDENTS;
                break;
            case MOVE_STUDENTS:
                phase = Phase.MOVE_MN;
                break;
            case MOVE_MN:
                phase = Phase.CLOUD_SELECTION;
                break;
            case CLOUD_SELECTION:
                if(getCurrentPlayer()==orderOfPlayers.get(orderOfPlayers.size()-1)){
                    phase = Phase.CARD_SELECTION;
                    support.firePropertyChange("lastTurnPlayer", currentPlayer, orderOfPlayers.get(0));
                    currentPlayer = orderOfPlayers.get(0);
                }else{
                    calculateNextPlayerAction();
                    phase = Phase.MOVE_STUDENTS;
                }
                break;
        }
    }

    public Bag getBag(){
        return bag;
    }

    //TODO: decide if move it to turn controller
    /**
     * Move a student from the entrance of a player to the dining room or to an archipelago
     * @param colorToMove color of the student to be picked from the entrance
     * @param destination of the student (0 for dining room, [1...12] for an archipelago
     */
    public void moveStudents(StudsAndProfsColor colorToMove, int destination){
        if(destination == Constants.DININGROOMDESTINATION){
                currentPlayer.getMyBoard().getEntrance().removeStudent(colorToMove);
                currentPlayer.getMyBoard().getDiningRoom().addStudent(colorToMove);

        }
        else{
                currentPlayer.getMyBoard().getEntrance().removeStudent(colorToMove);
                for(Archipelago arch : listOfArchipelagos){
                    for(Island island : arch.getBelongingIslands()){
                        if(island.getIdIsland() == destination){
                            island.addStudent(colorToMove);
                        }
                    }
                }
        }
    }

    public int getNumberOfPlayers() {
        return numberOfPlayers;
    }

    public void getCoinFromBank(int coins) {
        bank -= coins;
    }

    public int getBank() {
        return bank;
    }

    /**
     * Add an amount of coins to the bank of the game
     * @param coins to be added
     */
    public void addCoinInBank(int coins) {
        bank += coins;
    }

    public void setBank(int bank) {
        bank = bank;
    }

    public int[] getPlayableCharacters() {
        return playableCharacters;
    }
}
