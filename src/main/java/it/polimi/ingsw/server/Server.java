package it.polimi.ingsw.server;

import it.polimi.ingsw.utilities.ErrorMessage;
import it.polimi.ingsw.utilities.ServerMessage;
import it.polimi.ingsw.utilities.constants.Constants;
import it.polimi.ingsw.controller.GameHandler;
import it.polimi.ingsw.model.ColorOfTower;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.view.RemoteView;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*
 Manage the connections of the clients and starts configuration of the game
 when the expected number of players is connected
 */
public class Server {
    private int numberOfPlayers;
    private ServerSocket serverSocket;
    private ExecutorService executor = Executors.newFixedThreadPool(128);
    private Map<Player, SocketClientConnection> waitingConnection = new HashMap<>();

    private Map<GameHandler, Map<Player, SocketClientConnection>> mapGameWaitingConnection = new HashMap<>();

    private Map<GameHandler, List<RemoteView>> mapGameRemoteViews = new HashMap<>();

    GameHandler gameHandler;
    private List<List<SocketClientConnection>> listOfConnections = new ArrayList<>();
    private boolean setupAborted;

    /*
    listOfGames contains all the gameHandler of the matches that are currently playing
    when the server is turned off, they are all saved on a file
    when the server is turned on again and someone connect:
    - if the username used by the player is NOT contained in any saved matches,
        starts a new match in the usual way
    - if the user is contained in one of the old matches, a new waitingConnection is created for that game
      and associated to it thanks to mapGameWaitingConnection
     */
    private List<GameHandler> listOfGames = new ArrayList<>();
    private int port;

    public Server(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.port = port;
    }

    //Deregister connection
    public synchronized void deregisterConnection(SocketClientConnection c) {
        String userStartedDisconnection = null;
        for (List<SocketClientConnection> l : listOfConnections) {
            for (SocketClientConnection s : l) {
                userStartedDisconnection = c.getNickname();
                System.out.println("I'm confronting " + s.getNickname());
                if (s == c) {
                    for (SocketClientConnection toRemove : l) {
                        System.out.println("I'm confronting" + toRemove.getNickname());
                        if (!toRemove.getNickname().equals(c.getNickname())) {
                            System.out.println("I'm sending to " + toRemove.getNickname());
                            toRemove.send(String.format(ServerMessage.userClosedConnection, c.getNickname()));
                            toRemove.setPlayerQuitted(true);
                            toRemove.closeConnection();
                        }

                    }

                    listOfConnections.remove(l);
                    break;
                }
            }
        }
        if (!waitingConnection.isEmpty()) {
            for (SocketClientConnection s : waitingConnection.values()) {
                System.out.println("I'm confroning" + s.getNickname());
                if (s != c) {
                    System.out.println("I'm sending to " + s.getNickname());
                    s.send(String.format(ServerMessage.userClosedConnection, c.getNickname()));
                    s.setPlayerQuitted(true);
                    s.closeConnection();
                }
            }
            setupAborted = true;
            waitingConnection.clear();
        }

        GameHandler gameToDelete = null;
        for (GameHandler g : listOfGames) {
            for (Player p : g.getGame().getListOfPlayer())
                if (p.getNickname().equals(userStartedDisconnection)) {
                    gameToDelete = g;
                }
        }
        if (gameToDelete != null) {
            listOfGames.remove(gameToDelete);
        }
    }

    /**
     * checks if c is the last still active connection, in case
     * removes it from list of active games
     *
     * @param c
     */
    public synchronized void checkEmptyGames(SocketClientConnection c) {
        for (List<SocketClientConnection> l : listOfConnections) {
            for (SocketClientConnection s : l) {
                if (s.getNickname().equals(c.getNickname())) {
                    if (l.size() == 1) {
                        listOfConnections.remove(l);
                    }
                }
            }
        }

    }

    //Wait for another player
    public synchronized void lobby(SocketClientConnection c) {
        setupAborted = false;

        if(!listOfGames.isEmpty()){
            if(!waitingConnection.isEmpty()) {
                 c.asyncSend(ServerMessage.ongoingMatches + "Otherwise \n");
            }else{
                c.asyncSend(ServerMessage.ongoingMatches);
            }
        }
        if (!waitingConnection.isEmpty()) {
            String nickOfOtherPlayers = ServerMessage.joiningMessage;
            for (Player p : waitingConnection.keySet()) {
                nickOfOtherPlayers += p.getNickname() + " ";
            }
            c.asyncSend(nickOfOtherPlayers);
        }

        //I moved nickname here so when other player connect the others receive his name
        String nickname = c.askNickname();
        if(nickname.equalsIgnoreCase(Constants.QUIT)) setupAborted = true;
        while (!setupAborted && !checkNickname(nickname)) {
            c.asyncSend(ErrorMessage.DuplicateNickname);
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            nickname = c.askNickname();
        }
        if(!setupAborted){
            c.setNickname(nickname);

            GameHandler savedGame = checkPlayerAlreadyExists(nickname, c);

            if (savedGame == null) {
                setupNewMatch(nickname, c);
            } else {
                setupOldMatch(nickname, savedGame);
            }
        }


    }

    private void setupNewMatch(String nickname, SocketClientConnection c) {
        List<Player> keys = new ArrayList<>(waitingConnection.keySet());


        if (waitingConnection.size() == 0) {
            registerFirstPlayer(nickname, c);
        } else {
            for (Player p : keys) {
                SocketClientConnection connection = waitingConnection.get(p);
                connection.asyncSend(ServerMessage.connectedUser + nickname);
            }
            registerOtherPlayers(nickname, c);
        }
        if(!setupAborted){
            if (waitingConnection.size() < numberOfPlayers) {
                c.asyncSend(ServerMessage.waitingOtherPlayers);
            } else if (waitingConnection.size() == numberOfPlayers) {

                System.out.println("Number of player reached! Starting the game... ");

                List<SocketClientConnection> temp = new ArrayList<>();
                for (Player p : waitingConnection.keySet()) {
                    temp.add(waitingConnection.get(p));
                    listOfConnections.add(temp);
                }

                listOfGames.add(gameHandler);
                waitingConnection.clear();
            }
        }

    }

    private void setupOldMatch(String nickname, GameHandler savedGame) {
        for (SocketClientConnection s : mapGameWaitingConnection.get(savedGame).values()) {
            s.asyncSend(ServerMessage.connectedUser + nickname);
        }
        String nickOfPlayerToWait = "";
        for (Player p : savedGame.getGame().getListOfPlayer()) {
            if (!mapGameWaitingConnection.get(savedGame).containsKey(p)) {
                nickOfPlayerToWait += p.getNickname() + " ";
            }
        }
        if (mapGameWaitingConnection.get(savedGame).size() < savedGame.getPlayersNumber()) {
            for (SocketClientConnection s : mapGameWaitingConnection.get(savedGame).values()) {
                s.asyncSend(ServerMessage.waitingOldPlayers + nickOfPlayerToWait);
            }
        } else {

            System.out.println("Number of player reached! Starting the game... ");

            List<SocketClientConnection> temp = new ArrayList<>();
            for (Player p : mapGameWaitingConnection.get(savedGame).keySet()) {
                temp.add(mapGameWaitingConnection.get(savedGame).get(p));
                listOfConnections.add(temp);
            }
            for (SocketClientConnection s : mapGameWaitingConnection.get(savedGame).values()) {
                s.send(ServerMessage.startingGame);
            }
            for (RemoteView rem : mapGameRemoteViews.get(savedGame)) {
                rem.resendSituation();
            }

            mapGameWaitingConnection.remove(savedGame);
        }
    }

    /**
     * Check if the nickname chosen has already been taken
     *
     * @param nameToCheck nickname to check
     * @return true if the nickname is available globally, false otherwise
     */
    public boolean checkNickname(String nameToCheck) {
        for (Player p : waitingConnection.keySet()) {
            if (p.getNickname().equalsIgnoreCase(nameToCheck)) {
                return false;
            }
        }
        /*Use the mapGameRemoteViews because in this way if there was an old match
         * with a player nickname "nameToCheck", if it has not re-logged yet
         * it's allowed to do it, otherwise return that the nick is already used
         */

        for (GameHandler g : mapGameRemoteViews.keySet()) {
            for (RemoteView r : mapGameRemoteViews.get(g)) {
                if (r.getPlayer().getNickname().equalsIgnoreCase(nameToCheck)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check if the chosed color is already been taken
     *
     * @param colorOfTower color of the tower to check
     * @return True if the color is still available, false if it is already been taken
     */
    public boolean checkColorTower(ColorOfTower colorOfTower) {
        if (gameHandler.getGame().getNumberOfPlayers() == 2 || gameHandler.getGame().getNumberOfPlayers() == 4) {
            if (colorOfTower == ColorOfTower.GREY) {
                return false;
            }
        }
        for (Player p : waitingConnection.keySet()) {
            if (p.getColorOfTowers() == colorOfTower) {
                return false;
            }
        }
        return true;
    }

    public int getNumberOfPlayers() {
        return numberOfPlayers;
    }

    private void saveGames() {
        System.out.println("shut down...");
        try {
            // Create a file to write game system
            FileOutputStream out = new FileOutputStream(Constants.NAMEFILEFORSAVEMATCHES);

            // Code to write instance of GamingWorld will go here
            // Create an object output stream, linked to out
            ObjectOutputStream objectOut = new ObjectOutputStream(out);

// Write game system to object store
            objectOut.writeObject(listOfGames);

// Close object output stream
            objectOut.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Unable to create game data");
        }
    }

    private void restoreGame() {
        // Create a file input stream
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(Constants.NAMEFILEFORSAVEMATCHES);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        // Create an object input stream
        ObjectInputStream objectIn = null;
        try {
            objectIn = new ObjectInputStream(fin);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Read an object in from object store, and cast it to a mapGameConnection
        try {
            //listOfGames save te current 
            listOfGames = (List<GameHandler>) objectIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        if (!listOfGames.isEmpty()) {
            System.out.println("Games already saved: " + listOfGames.size());
        }
        // Set the object stream to standard output

    }

    private void registerFirstPlayer(String nickname, SocketClientConnection c) {
        ColorOfTower color = null;
        numberOfPlayers = c.askHowManyPlayers();
        if(numberOfPlayers == -2){
            setupAborted = true;
            return;
        }
        while ((numberOfPlayers <= 0 || numberOfPlayers > Constants.MAXPLAYERS) && !setupAborted) {
            c.asyncSend(ErrorMessage.NumberOfPlayersNotValid);
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            numberOfPlayers = c.askHowManyPlayers();
        }

        int mode = -1;
        mode = c.askMode();
        if(mode == -2) setupAborted = true;
        while (!setupAborted && mode == -1) {
            c.asyncSend(ErrorMessage.ModeNotValid);
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            mode = c.askMode();
        }
        if(setupAborted) return;
        color = c.askColor();
        while (!setupAborted && color == null) {
            c.asyncSend(ErrorMessage.ActionNotValid);
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            color = c.askColor();
        }
        if(setupAborted) return;
        Player player1 = new Player(nickname, color);
        player1.setTeam(0);

        waitingConnection.put(player1, c);
        gameHandler = new GameHandler(player1, numberOfPlayers, mode == 1);
        RemoteView remV1 = new RemoteView(player1, c, gameHandler.getGame(), gameHandler.getController().getTurnController().getActionController().getActionParser());
        c.addPropertyChangeListener(remV1);
        gameHandler.addPropertyChangeListener(remV1);
        gameHandler.getGame().addPropertyChangeListener(remV1);
        gameHandler.getController().getTurnController().getActionController().addPropertyChangeListener(remV1);

        gameHandler.getController().getTurnController().getActionController().getActionParser().addPropertyChangeListener(remV1);

        gameHandler.getController().getTurnController().addPropertyChangeListener(remV1);
    }

    private void registerOtherPlayers(String nickname, SocketClientConnection c) {
        ColorOfTower color = null;
        if (numberOfPlayers != 4 || (waitingConnection.size() + 1) % 2 != 0) {
            color = c.askColor();
            while ( (color == null || !checkColorTower(color) ) && !setupAborted) {
                if (color == null) {
                    c.asyncSend(ErrorMessage.ActionNotValid);
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    color = c.askColor();
                } else {
                    c.asyncSend(ErrorMessage.ColorNotValid);
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    color = c.askColor();
                }
            }
            if(setupAborted) return;
        } else {
            //Only the first member of the team take the towers
            color = null;
        }

        Player player = new Player(nickname, color);
            /*
            if 4 players:
            if size 1--> 0; 2 --> 1 ; 3--> 1
            else for 2,3 players team is a progressive number
             */
        if (numberOfPlayers == 4) {
            if (waitingConnection.size() == 1) {
                player.setTeam(0);
            } else {
                player.setTeam(1);
            }
            // player.setTeam(Math.round(waitingConnection.size() / 4));
        } else {
            player.setTeam(waitingConnection.size());
        }
        //player.setTeam(Math.round(waitingConnection.size() / 4));
        waitingConnection.put(player, c);
        RemoteView remV = new RemoteView(player, c, gameHandler.getGame(), gameHandler.getController().getTurnController().getActionController().getActionParser());
        c.addPropertyChangeListener(remV);
        gameHandler.addPropertyChangeListener(remV);
        gameHandler.getGame().addPropertyChangeListener(remV);
        gameHandler.getController().getTurnController().getActionController().addPropertyChangeListener(remV);
        gameHandler.getController().getTurnController().addPropertyChangeListener(remV);
        gameHandler.getController().getTurnController().getActionController().getActionParser().addPropertyChangeListener(remV);
        gameHandler.addNewPlayer(player);
    }

    /**
     * If already exists a saved match with that username, return the gameHandler of that game
     * @param nickname
     * @param c
     * @return
     */
    private GameHandler checkPlayerAlreadyExists(String nickname, SocketClientConnection c) {

        for (GameHandler g : listOfGames) {
            for (Player p : g.getGame().getListOfPlayer()) {
                if (p.getNickname().equalsIgnoreCase(nickname)) {
                    g.setNewController();
                    RemoteView remV = new RemoteView(p, c, g.getGame(), g.getController().getTurnController().getActionController().getActionParser());
                    c.addPropertyChangeListener(remV);
                    g.addPropertyChangeListener(remV);
                    g.getGame().addPropertyChangeListener(remV);
                    g.getController().getTurnController().getActionController().addPropertyChangeListener(remV);
                    g.getController().getTurnController().addPropertyChangeListener(remV);
                    g.getController().getTurnController().getActionController().getActionParser().addPropertyChangeListener(remV);

                    for (GameHandler game : mapGameRemoteViews.keySet()) {
                        if (game.equals(g)) {
                            mapGameRemoteViews.get(game).add(remV);
                        }
                    }
                    List<RemoteView> newList = new ArrayList<>();
                    newList.add(remV);
                    mapGameRemoteViews.put(g, newList);

                    for (GameHandler game : mapGameWaitingConnection.keySet()) {
                        if (game.equals(g)) {
                            mapGameWaitingConnection.get(game).put(p, c);
                            return g;
                        }
                    }
                    HashMap<Player, SocketClientConnection> newMap = new HashMap<>();
                    newMap.put(p, c);
                    mapGameWaitingConnection.put(g, newMap);
                    return g;


                }
            }
        }
        return null;
    }

    public void run() {

        Runtime.getRuntime().addShutdownHook(new Thread(this::saveGames));

        int connections = 0;
        System.out.println("Server is running");

        //If a game has been saved, it will restore it
        File gamesSaved = new File(Constants.NAMEFILEFORSAVEMATCHES);
        if (gamesSaved.isFile()) {
            restoreGame();
        }

        while (true) {
            try {
                Socket newSocket = serverSocket.accept();
                if(newSocket.getPort() == 22) {
                    System.out.println("ping sulla 22");
                } else {
                    connections++;
                    System.out.println("Ready for the new connection - " + connections);
                    SocketClientConnection socketConnection = new SocketClientConnection(newSocket, this);
                    executor.submit(socketConnection);
                }


                //socketConnection.run();
            } catch (IOException e) {
                System.out.println("Connection Error!");
            }
        }
    }

}