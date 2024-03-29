package it.polimi.ingsw.controller;

import it.polimi.ingsw.utilities.ErrorMessage;
import it.polimi.ingsw.utilities.Constants;
import it.polimi.ingsw.model.*;
import it.polimi.ingsw.model.expertMode.*;
import it.polimi.ingsw.utilities.EventName;

import java.beans.PropertyChangeSupport;

import java.beans.PropertyChangeListener;

/**
 * Contains the methods to check that the action received from the Client via ActionParser
 * are allowed for that client; if yes, perform the action
 */
public class ActionController {
    private final Game game;
    private final ActionParser actionParser;
    private final TurnController turnController;
    private final PropertyChangeSupport support;

    public ActionController(Game game, TurnController turnController) {
        this.game = game;
        actionParser = new ActionParser(this);
        this.turnController = turnController;
        this.support = new PropertyChangeSupport(this);
    }

    /**
     * Add a listener to this class
     *
     * @param pcl
     */
    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        support.addPropertyChangeListener(pcl);
    }

    public Game getGame() {
        return game;
    }

    /**
     * Calculate the influence on the archipelago where MN is present
     * Manage the tower situation of an archipelago and of the players after that
     * Call checkUnification to check if the archipelago needs to be unified to another one
     */
    public void calculateInfluence() {
        if (game.isExpertModeOn()) {
            if (getCurrentPlayer().getUsedCharacter() != null) {
                switch (getCurrentPlayer().getUsedCharacter()) {
                    case Character4 character4 -> {
                        System.out.println("Char 4 played");
                        character4.calculateInfluence();
                        return;
                    }
                    case Character5 character5 -> {
                        if (character5.calculateInfluence()) {
                            turnController.getGameHandler().endGame();
                        } else {
                            for (Player p : game.getListOfPlayer()) {
                                checkWinner(p);
                            }
                        }
                        return;
                    }
                    case Character6 character6 -> {
                        character6.calculateInfluence();
                        return;
                    }
                    case null, default -> {
                    }
                }
            }
        }
        for (Archipelago a : game.getListOfArchipelagos()) {
            if (a.getIsMNPresent() && !a.getIsForbidden()) {
                Player oldOwner;
                int[] influences = new int[game.getNumberOfPlayers()];
                for (int i = 0; i < game.getNumberOfPlayers(); i++) {
                    influences[i] = 0;
                }

                if (a.getOwner() == null) {
                    oldOwner = null;
                } else {
                    oldOwner = a.getOwner();
                }

                /*
                Calculate the influence of each player on the archipelago a
                give this value increasing the influences vector at the position
                of the team:
                In this way, both if is a game with 2 or 4 players
                the influence is correctly calculated
                 */
                for (Player p : game.getOrderOfPlayers()) {
                    /*
                    if oldOwner is not null and his team's number corresponds with p,
                    add number of towers to team's influence
                     */
                    if (oldOwner != null && oldOwner.getTeam() == p.getTeam()) {
                        influences[p.getTeam()] = a.getBelongingIslands().size();
                    }
                    Character4.findInfluence(a, influences, p);
                }
                //Find the max in the vector influence and save the team number
                int maxInfluence = 0;
                int teamMaxInfluence = 0;
                for (int i = 0; i < influences.length; i++) {
                    if (influences[i] > maxInfluence) {
                        maxInfluence = influences[i];
                        teamMaxInfluence = i;
                    }
                }
                boolean tie = false;

                //Check if there are two different players with the same influence (tie)
                for (int i = 0; i < influences.length; i++) {
                    if (influences[i] == maxInfluence && i != teamMaxInfluence) {
                        tie = true;
                        break;
                    }
                }

                /*
                Change the tower only if these conditions are verified:
                - at least 1 player has the influence ont he archipelago (maxInfluence>0)
                - the archipelago had no owner or the owner changes
                - there is no tie
                 */
                if (maxInfluence > 0 && (oldOwner == null || oldOwner.getTeam() != teamMaxInfluence) && !tie) {
                    for (int i = 0; i < a.getBelongingIslands().size(); i++) {
                        //Only if the newOwner is different from the oldOwner (or this was null) change the towers

                        for (Player p : game.getOrderOfPlayers()) {
                            if (oldOwner != null && oldOwner.getTeam() == p.getTeam() && p.getColorOfTowers() != null) {
                                p.getMyBoard().getTowersOnBoard().addTower();
                            }
                            if (p.getTeam() == teamMaxInfluence && p.getColorOfTowers() != null) {
                                p.getMyBoard().getTowersOnBoard().removeTower();
                                a.changeOwner(p);
                                checkWinner(p);
                            }
                        }
                    }
                    checkUnification(a);
                    if (game.getListOfArchipelagos().size() < 4) {
                        turnController.getGameHandler().endGame();
                    }
                    break;
                }
            } else if (a.getIsMNPresent() && a.getIsForbidden()) {
                support.firePropertyChange("ErrorMessage", getCurrentPlayer().getNickname(), ErrorMessage.Forbidden);
                a.setIsForbidden(false);
                break;
            }
        }
    }

    /**
     * Check if an archipelago has to be unified with previous or next,
     * if yes, it calls game.unifyArchipelagos()
     *
     * @param a archipelago to be checked
     */
    public void checkUnification(Archipelago a) {
        int index = game.getListOfArchipelagos().indexOf(a);
        int previous = index - 1;
        if (index == 0) {
            previous = game.getListOfArchipelagos().size() - 1;
        }

        if (a.getOwner() == game.getListOfArchipelagos().get(previous).getOwner()) {
            game.unifyArchipelagos(a, game.getListOfArchipelagos().get(previous));
            index = game.getListOfArchipelagos().indexOf(a);
        }
        int next = index + 1;
        if (index == game.getListOfArchipelagos().size() - 1) {
            next = 0;
        }
        if (a.getOwner() == game.getListOfArchipelagos().get(next).getOwner()) {
            game.unifyArchipelagos(a, game.getListOfArchipelagos().get(next));
        }
    }

    /**
     * Check if a movement of a student from the entrance of the player's board is allowed
     * Call assignProfessor to check if a prof needs to be added
     *
     * @param player       that do the movement
     * @param colors       of the students that the player wants to move
     * @param destinations of the students (0 = dining room, [1..12] number of the archipelago
     */
    public boolean checkActionMoveStudent(Player player, StudsAndProfsColor[] colors, int[] destinations) {
        StudsAndProfsColor color;
        int destination;
        if (game.getPhase() == Phase.MOVE_STUDENTS && player == game.getCurrentPlayer()) {
            if (colors.length == turnController.getGameHandler().getNumberOfMovements()) {
                if (checkColors(player, colors)) {
                    if (checkDestinations(player, colors, destinations)) {
                        for (int i = 0; i < destinations.length; i++) {
                            color = colors[i];
                            destination = destinations[i];
                            player.getMyBoard().getEntrance().removeStudent(color);
                            if (destination == Constants.DININGROOMDESTINATION) {
                                player.getMyBoard().getDiningRoom().addStudent(color);
                                if (game.isExpertModeOn()) {
                                    if (player.getMyBoard().getDiningRoom().getStudentsByColor(color) % 3 == 0) {
                                        if (game.getCoinFromBank(1)) {
                                            player.addCoinsToWallet(1);
                                        } else {
                                            support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.EmptyBank);
                                        }
                                    }
                                }
                                game.assignProfessor(color);
                            } else {
                                for (Archipelago arc : game.getListOfArchipelagos()) {
                                    for (Island island : arc.getBelongingIslands()) {
                                        if (island.getIdIsland() == destination) {
                                            island.addStudent(color);
                                        }
                                    }
                                }
                            }
                        }
                        game.nextPhase();
                        support.firePropertyChange(EventName.moveStudent, 0, 1);
                        return true;
                    } else {
                        System.out.println("Destination not valid");
                        return false;
                    }
                } else {
                    support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.StudentNotPresent);
                    return false;
                }
            } else {
                support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.WrongNumberOfMovements + turnController.getGameHandler().getNumberOfMovements() + "students");
                return false;
            }
        } else if (player != game.getCurrentPlayer()) {
            support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.NotYourTurn);
            return false;
        } else {
            support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.wrongPhase + game.getPhase());
            return false;
        }
    }

    /**
     * Check if a player can move MN of the steps that he sent
     *
     * @param player that want to move MN
     * @param steps  that ask to MN to do
     * @return true if the player can do the steps, false otherwise
     */
    public boolean checkActionMoveMN(Player player, int steps) {
        if (game.getPhase() == Phase.MOVE_MN && player == game.getCurrentPlayer()) {
            if (steps > 0 && steps <= player.getLastUsedCard().getMaxSteps() + (player.getUsedCharacter() != null ? player.getUsedCharacter().getBonusSteps() : 0)) {
                game.moveMotherNature(steps);
                calculateInfluence();
                support.firePropertyChange(EventName.MNmove, 0, 1);

                //if there are not enough students the cloud selection must be skipped
                if(getTurnController().isNotEnoughStudents()){
                    if(game.getCurrentPlayer() == game.getOrderOfPlayers().get(game.getNumberOfPlayers() - 1)){
                        turnController.getGameHandler().endGame();
                        return true;
                    }else{
                        game.calculateNextPlayerAction();
                        game.setPhase(Phase.MOVE_STUDENTS);
                        support.firePropertyChange(EventName.PhaseChanged, 0, 1);
                    }
                }else if(game.getPhase() != Phase.END_GAME) {
                    game.nextPhase();
                }
                return true;

            } else {
                support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.TooManySteps + player.getLastUsedCard().getMaxSteps() + " steps");
                return false;
            }

        } else if (game.getPhase() == Phase.MOVE_MN || player != game.getCurrentPlayer()) {
            System.out.println("non è il tuo turno!!");
            return false;
        } else {
            support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.wrongPhase + game.getPhase());
            return false;
        }
    }

    /**
     * Check if a cloud can be picked and do it
     *
     * @param player  that wants to pick a cloud
     * @param cloudId id of the cloud to pick
     */
    public boolean checkActionCloud(Player player, int cloudId) {
        if (game.getPhase() == Phase.CLOUD_SELECTION && player == game.getCurrentPlayer()) {
            if (cloudId >= 0 && cloudId < game.getListOfClouds().size()) {
                for (Cloud cloud : game.getListOfClouds()) {
                    if (cloud.getIdCloud() == cloudId) {
                        if (!cloud.getIsTaken()) {
                            player.getMyBoard().getEntrance().addStudent(cloud.getStudents());
                            cloud.removeStudents();
                            game.getCurrentPlayer().setUsedCharacter(null);

                            if (player == game.getOrderOfPlayers().get(game.getNumberOfPlayers() - 1)) {
                                if (turnController.isFinished()) {
                                    turnController.getGameHandler().endGame();
                                    return true;
                                }
                                turnController.startTurn();
                            } else {
                                game.nextPhase();

                            };
                            return true;
                        } else {
                            support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.CloudTaken);
                            return false;
                        }
                    }
                }
            } else {
                support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.DestinationNotValid);
                return false;
            }

        } else if (game.getPhase() == Phase.CLOUD_SELECTION || player != game.getCurrentPlayer()) {
            support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.NotYourTurn);
            return false;
        }
        support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.ActionNotValid);
        return false;
    }

    public Player getCurrentPlayer() {
        return game.getCurrentPlayer();
    }

    public TurnController getTurnController() {
        return turnController;
    }

    /**
     * Check that a player can use a character and use it
     *
     * @param player        of the player that is playing the character
     * @param idOfCharacter that the player want to use
     * @param action        action associated with the character (e.g. number of the archipelago)
     * @return false if there is some error
     */
    public boolean checkActionCharacter(Player player, int idOfCharacter, String action) {
        if (game.isExpertModeOn()) {
            if (player == game.getCurrentPlayer()) {
                if(game.getPhase() != Phase.CARD_SELECTION) {
                    if (player.getUsedCharacter() == null) {
                        for (Characters c : game.getCharactersPlayable()) {
                            if (c.getId() == idOfCharacter) {
                                switch (idOfCharacter) {
                                    case 1 -> {
                                        return useCharacter1(action, player, c);
                                    }
                                    case 2 -> {
                                        return useCharacter2(player, c);
                                    }
                                    case 3 -> {
                                        return useCharacter3(action, player, c);
                                    }
                                    case 4 -> {
                                        return useCharacter4(player, c);
                                    }
                                    case 5 -> {
                                        return useCharacter5(player, c);
                                    }
                                    case 6 -> {
                                        return useCharacter6(action, player, c);
                                    }
                                    case 7 -> {
                                        return useCharacter7(action, player, c);
                                    }
                                    case 8 -> {
                                        return useCharacter8(player, c);
                                    }
                                    default -> {
                                        support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.characterNotValid);
                                        return false;
                                    }
                                }
                            }
                        }
                        support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.CharacterNotPresent);
                    } else {
                        support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.AlreadyUsedCharacter);
                    }
                } else {
                    support.firePropertyChange("ErrorMessage", player.getNickname(),ErrorMessage.wrongPhase + " " + game.getPhase());
                }
            } else {
                support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.NotYourTurn);
            }
        } else {
            support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.NotExpertMode);
        }
        return false;
    }

    /**
     * If the players has the requirements, plays the character 1
     *
     * @param action
     * @param player
     * @param c
     * @return
     */
    private boolean useCharacter1(String action, Player player, Characters c) {
        String playedCharacter = "";
        for (Archipelago arc : game.getListOfArchipelagos()) {
            Integer actionToUse = tryParseInteger(action);
            if (actionToUse == null) {
                System.out.println(ErrorMessage.ActionNotValid);
                support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.ActionNotValid);
                support.firePropertyChange(EventName.PhaseChanged, 0, 1);
                return false;
            }
            if (arc.getIdArchipelago() == actionToUse) {
                Character1 character1 = (Character1) c;
                if (character1.usePower(actionToUse)) {
                    playedCharacter = CharactersEnum.CHARACTER1.toString();
                    support.firePropertyChange("playedCharacter", player.getNickname(), playedCharacter);
                    if (game.getListOfArchipelagos().size() < 4) {
                        turnController.getGameHandler().endGame();
                    } else {
                        for (Player p : game.getListOfPlayer()) {
                            checkWinner(p);
                        }
                    }
                    player.setUsedCharacter(character1);
                    support.firePropertyChange(EventName.PhaseChanged, 0, 1);
                    return true;
                } else {
                    System.out.println("Error in playing character" + playedCharacter);
                    support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.notEnoughCoinsOrWrongAction);
                    support.firePropertyChange(EventName.PhaseChanged, 0, 1);
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * If the players has the requirements, plays the character 2
     *
     * @param player
     * @param c
     * @return
     */
    private boolean useCharacter2(Player player, Characters c) {
        String playedCharacter = "";
        Character2 character2 = (Character2) c;
        if (character2.usePower()) {
            playedCharacter = CharactersEnum.CHARACTER2.toString();
            support.firePropertyChange("playedCharacter", "", playedCharacter);
            player.setUsedCharacter(character2);
            if (game.getPhase().equals(Phase.MOVE_MN)) {
                support.firePropertyChange(EventName.PhaseChanged, "", Phase.MOVE_MN);
            }
            support.firePropertyChange(EventName.PhaseChanged, 0, 1);
            return true;
        } else {
            System.out.println("Error in playing character" + playedCharacter);
            support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.notEnoughCoinsOrWrongAction);
            support.firePropertyChange(EventName.PhaseChanged, 0, 1);
            return false;
        }
    }

    /**
     * If the players has the requirements, plays the character 3
     *
     * @param action
     * @param player
     * @param c
     * @return
     */
    private boolean useCharacter3(String action, Player player, Characters c) {
        String playedCharacter;
        for (Archipelago arc : game.getListOfArchipelagos()) {
            Integer actionToUse = tryParseInteger(action);
            if (actionToUse == null) {
                System.out.println(ErrorMessage.ActionNotValid);
                support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.ActionNotValid);
                support.firePropertyChange(EventName.PhaseChanged, 0, 1);
                return false;
            }
            if (arc.getIdArchipelago() == actionToUse) {
                Character3 character3 = (Character3) c;
                playedCharacter = CharactersEnum.CHARACTER3.toString();
                if (character3.usePower(actionToUse)) {
                    support.firePropertyChange("playedCharacter", player.getNickname(), playedCharacter);
                    player.setUsedCharacter(character3);
                    support.firePropertyChange(EventName.PhaseChanged, 0, 1);
                    return true;
                } else {
                    System.out.println("Error in playing character" + playedCharacter);
                    support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.notEnoughCoinsOrWrongAction);
                    support.firePropertyChange(EventName.PhaseChanged, 0, 1);
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * If the players has the requirements, plays the character 4
     *
     * @param player
     * @param c
     * @return
     */
    private boolean useCharacter4(Player player, Characters c) {
        String playedCharacter = CharactersEnum.CHARACTER4.toString();
        Character4 character4 = (Character4) c;
        return useSimpleCharacter(player, playedCharacter, character4.usePower());
    }

    /**
     * If the players has the requirements, plays the character 5
     *
     * @param player
     * @param c
     * @return
     */
    private boolean useCharacter5(Player player, Characters c) {
        String playedCharacter = CharactersEnum.CHARACTER5.toString();
        Character5 character5 = (Character5) c;
        return useSimpleCharacter(player, playedCharacter, character5.usePower());
    }

    /**
     * If the players has the requirements, plays the character 6
     *
     * @param action
     * @param player
     * @param c
     * @return
     */
    private boolean useCharacter6(String action, Player player, Characters c) {
        String playedCharacter = CharactersEnum.CHARACTER6.toString();
        Character6 character6 = (Character6) c;
        try {
            if (character6.usePower(StudsAndProfsColor.valueOf(action))) {
                support.firePropertyChange("playedCharacter", player.getNickname(), playedCharacter);
                support.firePropertyChange(EventName.PhaseChanged, 0, 1);
                return true;
            } else {
                support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.notEnoughCoinsOrWrongAction);
                support.firePropertyChange(EventName.PhaseChanged, 0, 1);
                return false;
            }
        } catch (IllegalArgumentException e) {
            System.out.println(ErrorMessage.ActionNotValid);
            support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.ActionNotValid);
            support.firePropertyChange(EventName.PhaseChanged, 0, 1);
            return false;
        }
    }

    /**
     * If the players has the requirements, plays the character 7
     *
     * @param action
     * @param player
     * @param c
     * @return
     */
    private boolean useCharacter7(String action, Player player, Characters c) {
        String playedCharacter = CharactersEnum.CHARACTER7.toString();
        Character7 character7 = (Character7) c;
        if (!action.contains(",")) {
            System.out.println(ErrorMessage.ActionNotValid);
            support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.ActionNotValid);
            support.firePropertyChange(EventName.PhaseChanged, 0, 1);
            return false;
        }
        String[] colorDestination = action.split(",");
        support.firePropertyChange("playedCharacter", player.getNickname(), playedCharacter);
        try {
            boolean result = character7.usePower(StudsAndProfsColor.valueOf(colorDestination[0]), StudsAndProfsColor.valueOf(colorDestination[1]), StudsAndProfsColor.valueOf(colorDestination[2]), StudsAndProfsColor.valueOf(colorDestination[3]));
            if (!result) {
                support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.notEnoughCoinsOrWrongAction);
            } else {
                //Emulate a changing in the professors in order to resend the boards updated
                support.firePropertyChange(EventName.ChangedProfessor, player.getNickname(), "");
            }
            support.firePropertyChange(EventName.PhaseChanged, 0, 1);
            return result;
        } catch (IllegalArgumentException e) {
            System.out.println(ErrorMessage.ActionNotValid);
            support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.ActionNotValid);
            support.firePropertyChange(EventName.PhaseChanged, 0, 1);
            return false;
        }
    }

    /**
     * If the players has the requirements, plays the character 8
     *
     * @param player
     * @param c
     * @return
     */
    private boolean useCharacter8(Player player, Characters c) {
        String playedCharacter = CharactersEnum.CHARACTER8.toString();
        Character8 character8 = (Character8) c;
        return useSimpleCharacter(player, playedCharacter, character8.usePower());
    }

    /**
     * Plays a character who doesn't need other specifications
     *
     * @param player
     * @param playedCharacter
     * @param b
     * @return
     */
    private boolean useSimpleCharacter(Player player, String playedCharacter, boolean b) {
        if (b) {
            support.firePropertyChange("playedCharacter", "", playedCharacter);
            support.firePropertyChange(EventName.PhaseChanged, 0, 1);
            return true;
        } else {
            support.firePropertyChange("ErrorMessage", player.getNickname(), ErrorMessage.notEnoughCoinsOrWrongAction);
            support.firePropertyChange(EventName.PhaseChanged, 0, 1);
            return false;
        }
    }

    public ActionParser getActionParser() {
        return actionParser;
    }

    /**
     * Check if the player p has enough player of the colors to be moved from the entrance
     *
     * @param p the player
     * @param colors the colors to check
     * @return
     */
    public boolean checkColors(Player p, StudsAndProfsColor[] colors) {
        int studsOfAColorToBeMoved;
        for (StudsAndProfsColor colorToCheck : StudsAndProfsColor.values()) {
            studsOfAColorToBeMoved = 0;
            for (StudsAndProfsColor colorToMove : colors) {
                if (colorToCheck.equals(colorToMove)) {
                    studsOfAColorToBeMoved++;
                }
            }
            if (studsOfAColorToBeMoved != 0 && p.getMyBoard().getEntrance().getStudentsByColor(colorToCheck) < studsOfAColorToBeMoved) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the destination typed by the user is an existing archipelago
     * or if it has place to put the student in the dining room
     * @param p
     * @param colors of student moved
     * @param destinations where to put students
     * @return true if the destination is correct
     */
    private boolean checkDestinations(Player p, StudsAndProfsColor[] colors, int[] destinations) {
        for (int i = 0; i < destinations.length; i++) {
            StudsAndProfsColor color = colors[i];
            int destination = destinations[i];
            if (destination == Constants.DININGROOMDESTINATION) {
                //Check if the dining room of a color is already full
                if (p.getMyBoard().getDiningRoom().getStudentsByColor(color) == Constants.MAXSTUDENTSINDINING) {
                    support.firePropertyChange("ErrorMessage", p.getNickname(), ErrorMessage.FullDiningRoom);
                    return false;
                }
                //check if the arc of that index exists
            } else if (!checkArchipelagoExistence(destination)) {
                support.firePropertyChange("ErrorMessage", p.getNickname(), ErrorMessage.DestinationNotValid);
                return false;
            }
        }
        return true;
    }

    /**
     * check if an archipelago exists
     * @param archipelago to check
     * @return true if it exists
     */
    private boolean checkArchipelagoExistence(int archipelago) {
        for (Archipelago a : game.getListOfArchipelagos()) {
            if (a.getIdArchipelago() == archipelago) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parse a string considering the null value
     * @param text string to parse
     * @return integer value or null
     */
    private static Integer tryParseInteger(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * check if a player won the game
     * @param p is the player to check
     */
    public void checkWinner(Player p) {
        if (p.getMyBoard().getTowersOnBoard().getNumberOfTowers() == 0 && p.getColorOfTowers() != null) {
            game.setPhase(Phase.END_GAME);
            turnController.getGameHandler().endGameImmediately(p);
        }
    }
}