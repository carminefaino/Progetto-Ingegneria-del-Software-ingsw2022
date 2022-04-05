package it.polimi.ingsw.modelTest.expertMode;

import it.polimi.ingsw.controller.GameHandler;
import it.polimi.ingsw.model.ColorOfTower;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.StudsAndProfsColor;
import it.polimi.ingsw.model.expertMode.Character3;
import it.polimi.ingsw.model.expertMode.Character5;
import it.polimi.ingsw.model.expertMode.Character7;
import it.polimi.ingsw.model.expertMode.Character8;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Character8Test {
    Player player1 = new Player("player1", ColorOfTower.BLACK);
    Player player2 = new Player("player2", ColorOfTower.WHITE);
    Game game = new Game(2, player1);
    Character3 character3 = new Character3(game);
    Character7 character7 = new Character7(game);
    Character5 character5 = new Character5(game);
    Character8 character8 = new Character8(game);
    GameHandler gameHandler = new GameHandler(player1, 2);

    /**
     * check if the professor is correctly assigned even if the number of players' students are the same
     */
    @Test
    void assignProfessor() {
        game.addPlayer(player2);
        player1.getMyBoard().getDiningRoom().addStudent(StudsAndProfsColor.RED);
        player2.getMyBoard().getDiningRoom().addStudent(StudsAndProfsColor.RED);
        player1.addCoinsToWallet(5);
        character8.usePower();
        character8.assignProfessor(StudsAndProfsColor.RED);
        assertEquals(true, player1.getMyBoard().getProfessorsTable().getHasProf(StudsAndProfsColor.RED));
    }
}