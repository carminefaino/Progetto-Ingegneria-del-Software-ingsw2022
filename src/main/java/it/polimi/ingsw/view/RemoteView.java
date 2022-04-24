package it.polimi.ingsw.view;

import it.polimi.ingsw.client.cli.Cli;
import it.polimi.ingsw.controller.ActionParser;
import it.polimi.ingsw.model.*;
import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.server.SocketClientConnection;
import it.polimi.ingsw.utilities.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

//Observer: osserva qualcosa e quando c'è una notify
// dell'oggetto osservato lancia l'update

//Observable: un metodo della classe lancerà una notify del
//tipo detto nell'observable
//public class RemoteView extends Observable<MessageForParser> implements Observer<Game>, PropertyChangeListener {
public class RemoteView implements PropertyChangeListener{
    private final SocketClientConnection clientConnection;
    private final Player player;
    private final Game currentGame;
    private final ActionParser actionParser;

    public RemoteView(Player player, SocketClientConnection c, Game currentGame, ActionParser actionParser) {
        this.player = player;
        this.clientConnection = c;
        this.currentGame = currentGame;
        this.actionParser = actionParser;
        //c.addObserver(new MessageReceiver());
        //c.asyncSend("Your opponent is: " + opponent);

    }

    protected void showMessage(Object message) {
        clientConnection.asyncSend(message);
    }

    /**
     public void eventPerformed(EventObject evt){
     System.out.println("Fired: " + ((Integer)evt.getSource()).toString());
     showMessage(evt);
     }
     */

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if(evt.getPropertyName().equals("currentPlayerChanged")){
            List<Board> boards = new ArrayList<>();
            for(Player p : currentGame.getListOfPlayer()) {
                boards.add(p.getMyBoard());
            }
            ListOfBoards boards1 = new ListOfBoards(boards);
            showMessage(boards1);

            ListOfArchipelagos archipelagos = new ListOfArchipelagos(currentGame.getListOfArchipelagos());
            showMessage(archipelagos);

            ListOfClouds clouds = new ListOfClouds(currentGame.getListOfClouds());
            showMessage(clouds);

            ListOfPlayers players = new ListOfPlayers(currentGame.getOrderOfPlayers());
            showMessage(players);

            if(currentGame.getCurrentPlayer() == player){
                showMessage(player.getMyDeck());
            }
            if(player.getNickname().equals(evt.getNewValue())){
                System.out.println("I'm notified and is my turn");
                showMessage(evt.getNewValue() + " is your turn!");
                if(currentGame.getPhase().equals(Phase.CARD_SELECTION)){
                    showMessage(gameMessage.cardSelectionMessage);
                }
            }else{
                System.out.println("I'm notified");
                showMessage("is the turn of " + evt.getNewValue());
            }
        }
        if(evt.getPropertyName().equals("MNmove") || evt.getPropertyName().equals("ArchUnified")){
            for(Archipelago a : currentGame.getListOfArchipelagos()){
                showMessage(a);
            }
        } else if(evt.getPropertyName().equals("PhaseChanged")){
            if(currentGame.getCurrentPlayer() == player){
                switch (currentGame.getPhase()){
                    case CARD_SELECTION -> {
                        System.out.println("card selection");
                        showMessage(gameMessage.cardSelectionMessage);
                    }case MOVE_STUDENTS ->
                            showMessage(gameMessage.studentMovementMessage);
                    case MOVE_MN ->
                            showMessage(gameMessage.moveMotherNatureMessage);
                    case CLOUD_SELECTION ->
                            showMessage(gameMessage.chooseCloudMessage);
                }
            }
        }else if(evt.getPropertyName().equals("UsedCard")){
            if(currentGame.getCurrentPlayer() == player){
                showMessage(player.getMyDeck());
            }
            for(Player p : currentGame.getOrderOfPlayers()){
                showMessage(p.getLastUsedCard());
            }
        }else if (evt.getPropertyName().equals("RemovedStudentFromEntrance")){
            for(Archipelago a : currentGame.getListOfArchipelagos()){
                showMessage(a);
            }
            for(Player p : currentGame.getListOfPlayer()) {
                showMessage(p.getMyBoard());
            }
        }else if(evt.getPropertyName().equals("ChangedProfessor")){
            for(Player p : currentGame.getListOfPlayer()) {
                showMessage(p.getMyBoard());
            }
        }else if(evt.getPropertyName().equals("MessageForParser")){
            System.out.println("Action to send to parser ");
            actionParser.actionSerializer(player.getNickname(),(String)evt.getNewValue());

        }else if(evt.getPropertyName().equals("ChangedCloudStatus")){
            for(Cloud c : currentGame.getListOfClouds()){
                showMessage(c);
            }
        }
    }

    /**
     private class MessageReceiver extends Observable<MessageForParser> implements Observer<String>{
    @Override
    public void update(String message) {
    // riceve il messaggio da socketClientConnection (input del client) e lo manda al parser
    System.out.println("Received: " + message);
    try{
    MessageForParser m = new MessageForParser(player, message);
    notify(m);
    }catch(IllegalArgumentException | ArrayIndexOutOfBoundsException e){
    clientConnection.asyncSend("Error!");
    }
    }
    }
     */
}