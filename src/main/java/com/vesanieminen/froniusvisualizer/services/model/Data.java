package com.vesanieminen.froniusvisualizer.services.model;

import java.io.Serializable;

public class Data implements Serializable {
    private Produced energyReal_WAC_Sum_Produced;

    public Produced getEnergyReal_WAC_Sum_Produced() {
        return energyReal_WAC_Sum_Produced;
    }

    public void setEnergyReal_WAC_Sum_Produced(Produced energyReal_WAC_Sum_Produced) {
        this.energyReal_WAC_Sum_Produced = energyReal_WAC_Sum_Produced;
    }
}
