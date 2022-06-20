package it.polimi.ingsw.utilities.Lists;

import it.polimi.ingsw.model.Archipelago;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializable list of archipelagos.
 */
public class ListOfArchipelagos implements Serializable {
    private List<Archipelago> archipelagos = new ArrayList<>();

    public ListOfArchipelagos(List<Archipelago> archipelagos) {
        this.archipelagos = archipelagos;
    }

    public List<Archipelago> getArchipelagos() {
        return archipelagos;
    }
}
