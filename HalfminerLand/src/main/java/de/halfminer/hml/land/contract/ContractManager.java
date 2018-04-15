package de.halfminer.hml.land.contract;

import de.halfminer.hml.land.Land;
import org.bukkit.entity.Player;

public interface ContractManager {

    boolean hasContract(Player player);

    void setContract(AbstractContract contract);

    AbstractContract getContract(Player player, Land land);

    void fulfillContract(AbstractContract contract);
}
