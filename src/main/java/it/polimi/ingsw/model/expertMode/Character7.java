package it.polimi.ingsw.model.expertMode;

/**
 * image 12 in image folder ;
 * card 2 in list of cards ( in game tutorial)
 * When this card is used, assignProfessor assign the professor to the player who use the card
 * also if the number of students in the dining rooms of the two players is the same
 */
public class Character7 extends Characters{

    public Character7() {
        super(2);
    }
    @Override
    public void usePower() {

    }

    @Override
    public int getPrice() {
        return 0;
    }
}