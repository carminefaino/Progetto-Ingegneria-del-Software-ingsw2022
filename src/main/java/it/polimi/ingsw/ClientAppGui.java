package it.polimi.ingsw;

import it.polimi.ingsw.FX.Gui;

public class ClientAppGui {
    public static void main(String[] args) {
        System.out.println("Eryantis Client | Welcome!");
        String ip = "127.0.0.1";
        int port = 5000;
        int input = 2;

        switch (input) {
          /*  case 1 -> {
                Cli client = new Cli(ip, port);
                try {
                    client.run();
                } catch (IOException e) {
                    //throw new RuntimeException(e);
                    System.err.println(e.getMessage());
                }
            }

           */
            case 2 -> {
                System.out.println("You selected the GUI interface, have fun!\nStarting...");
                Gui gui = new Gui(ip, port);
                    gui.main(null);
            }
            default -> System.err.println("Invalid argument, please run the executable again with one of these options:\n 2.client");
        }


    }
}