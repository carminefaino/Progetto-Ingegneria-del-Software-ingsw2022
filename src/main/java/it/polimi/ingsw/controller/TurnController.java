package it.polimi.ingsw.controller;
import it.polimi.ingsw.model.*;

//TODO: ADDTESTS!!!!!!
// and add cloud choice
public class TurnController {
    private Controller controller;
    private ActionController actionController;
    private GameHandler gameHandler;
    private Game game;

    public TurnController(Controller controller, GameHandler gameHandler, Game game){
        this.controller = controller;
        this.actionController = new ActionController(game, this);
        this.gameHandler = gameHandler;
        this.game = game;

    }

    public Controller getController() {
        return controller;
    }

    public ActionController getActionController() {
        return actionController;
    }

    public GameHandler getGameHandler() {
        return gameHandler;
    }

//TODO: place a listener for the number of students to end the game?
    /**
     * Start the turn :
     * Add the students to the clouds
     * Call endGame if the number of the students in the bag is finished
     */
    public void startTurn(){
        if(game.getBag().getNumberOfLeftStudents() == 0) {
            gameHandler.endGame();
        }else{
            //Add the students to the clouds
            for(Cloud cloud : game.getListOfClouds()){
                cloud.addStudents( game.getBag().pickStudent(gameHandler.getNumberOfStudentsOnCloud()) );
            }
        }
        System.out.println("Turn started");
        game.findPlayerOrder();
        //Message to be sent to the current player
      //  System.out.println(game.getCurrentPlayer().getNickname() + " is your turn!");
    }

    //TODO: listener of orderOfPlayer instead of calling it directly?
    // add check that the followed order is the clockwise order since the player that played the less value card in the match before
    /**
     * Receive a card and add it to lastUsedCard of the player
     * if the player is the last player find the new player order
     * @param player player that play the card
     * @param power power of the card that the player wants to use
     * @return true if the player can use the card, false otherwise
     */
    public boolean checkActionCard(Player player, int power){
        if(game.getPhase()== Phase.CARD_SELECTION && player == game.getCurrentPlayer()){
            //If the card is in the deck, remove it and place as last used card
            if(checkCardPresence(player, power) ){
                if(checkCardUsage(player, power)){
                    Card cardToUse = null;
                    for(Card c : player.getMyDeck().getLeftCards()){
                        if(c.getPower() == power){
                            cardToUse = c;
                            player.setLastUsedCard(c);

                            if(player == game.getListOfPlayer().get(game.getListOfPlayer().size()-1)){
                                //Send a message to all saying that the card selection phase is finished
                                game.nextPhase();
                                game.findPlayerOrder();

                            }else{
                                game.calculateNextPlayerPianification();
                            }
                        }
                    }
                    //Outside the for to avoid ConcurrentModificationException
                    player.getMyDeck().useCard(cardToUse);
                    return true;
                }else{
                    System.out.println("Card already played in this turn");
                    return false;
                }
            }else{
                System.out.println("Card not present in the deck!");
                return false;
            }

        }else if(game.getPhase()== Phase.CARD_SELECTION || player != game.getCurrentPlayer()){
            System.out.println("non è il tuo turno!!");
            return false;
        }else {
            System.out.println("hai inviato un'azione non valida, riprova");
            return false;
        }
    }

    //TODO: addTest
    /**
     * Check that no one else chose the same card before if the size of the player that send it is >1
     * @param p player that is playing the card
     * @param power of the card to be checked
     * @return True if the card has not been used before in the same turn, false otherwise
     */
    private boolean checkCardUsage(Player p, int power){
        if(p.getMyDeck().getLeftCards().size() >1 ){
            for(int i = 0; i< game.getOrderOfPlayers().indexOf(p); i++){
                if(game.getOrderOfPlayers().get(i).getLastUsedCard().getPower() == power){
                    return false;
                }
            }
            return true;
        }else{
            return true;
        }
    }

    //TODO: addTest
    /**
     * Check if a card with a power is present in the player's deck
     * @param p player that played the card
     * @param power power of the card to be checked
     * @return True if the card is in the deck, false otherwise
     */
    private boolean checkCardPresence(Player p, int power){
        for(Card c : p.getMyDeck().getLeftCards()){
            if(c.getPower() == power) return true;
        }
        return false;
    }

}


