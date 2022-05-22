package it.polimi.ingsw.FX;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URL;
import java.util.ResourceBundle;

public class loginController implements Initializable {
    @FXML
    Label usernameText;
    @FXML
    Label textHowManyPlayers;
    @FXML
    Label textMode;
    @FXML
    Label textColor;
    @FXML
    TextField usernameTextField;
    @FXML
    Button submitUsername;
    @FXML
    Button submitNumberOfPlayers;
    @FXML
    Button submitMode;
    @FXML
    Button submitColor;

    private PropertyChangeSupport support = new PropertyChangeSupport(this);

    @FXML
    RadioButton radio2,radio3,radio4,radiostd,radioexp,radioWhite,radioBlack,radioGrey;
    String username ;
    int nOfPlayers, choosedColor;
    boolean mode;

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        System.out.println("add listener called");
        support.addPropertyChangeListener(pcl);
    }

    public void submitUsername(ActionEvent event) {


        try {
            username = usernameTextField.getText();
            usernameText.setText("Welcome " + username);
            support.firePropertyChange("username", "", username );
            submitUsername.setVisible(false);
            usernameTextField.setVisible(false);

            textHowManyPlayers.setVisible(true);
            radio2.setVisible(true);
            radio3.setVisible(true);
            radio4.setVisible(true);
            submitNumberOfPlayers.setVisible(true);
            System.out.println("username");

        }
        catch (Exception e) {
            usernameText.setText("error");
        }
    }

    public void submitNumberOfPlayers(ActionEvent event){
        String numberOfPlayers = null;
        if(radio2.isSelected()) {
            System.out.println(radio2.getText());
            numberOfPlayers = radio2.getText();
        }
        else if(radio3.isSelected()) {
            System.out.println(radio3.getText());
            numberOfPlayers = radio3.getText();
        }
        else if(radio4.isSelected()) {
            System.out.println(radio4.getText());
            numberOfPlayers = radio4.getText();
        }

        support.firePropertyChange("numberOfPlayers", "",  numberOfPlayers );
        submitNumberOfPlayers.setVisible(false);
        radio2.setDisable(true);
        radio3.setDisable(true);
        radio4.setDisable(true);

        textMode.setVisible(true);
        radiostd.setVisible(true);
        radioexp.setVisible(true);
        submitMode.setVisible(true);
    }

    public void submitMode(ActionEvent event){
        String mode = null;
        if(radiostd.isSelected()) {
            System.out.println(radiostd.getText());
            mode = "0";
        } else if(radioexp.isSelected()) {
            System.out.println(radioexp.getText());
            mode = "1";
        }

        support.firePropertyChange("mode", "",  mode );
        radiostd.setDisable(true);
        radioexp.setDisable(true);
        submitMode.setVisible(false);

        textColor.setVisible(true);
        radioWhite.setVisible(true);
        radioBlack.setVisible(true);
        radioGrey.setVisible(true);
        submitColor.setVisible(true);
    }

    public void submitColor(ActionEvent event){
        String color = null;
        if(radioWhite.isSelected()) {
            System.out.println(radioWhite.getText());
            color = "0";
        } else if(radioBlack.isSelected()) {
            System.out.println(radioBlack.getText());
            color = "1";
        } else if(radioGrey.isSelected()) {
            System.out.println(radioGrey.getText());
            color = "2";
        }
        support.firePropertyChange("color", "",  color);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        textHowManyPlayers.setVisible(false);
        textMode.setVisible(false);
        textColor.setVisible(false);
        radio2.setVisible(false);
        radio3.setVisible(false);
        radio4.setVisible(false);
        radiostd.setVisible(false);
        radioexp.setVisible(false);
        radioWhite.setVisible(false);
        radioBlack.setVisible(false);
        radioGrey.setVisible(false);
        submitNumberOfPlayers.setVisible(false);
        submitMode.setVisible(false);
        submitColor.setVisible(false);
    }


}