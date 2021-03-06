package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hms.util.Message;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * - Generates a PIN that can be used to temporarily identify a player
 * - PIN's are only valid for up to an hour after command execution
 * - Stores the current rank name with the pin
 *   - Sets boolean to check if player is upgraded
 *   - Sets IP address, to deny sharing of PIN codes
 */
@SuppressWarnings("unused")
public class Cmdpin extends HalfminerCommand {

    @Override
    protected void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage("PIN");
            return;
        }

        String pinCode = "";

        // check if player has valid pin code already, remove old ones
        Object pins = storage.get("pins");
        boolean alreadyHadPin = false;
        if (pins instanceof ConfigurationSection) {
            ConfigurationSection pinSection = (ConfigurationSection) pins;
            for (String pin : pinSection.getKeys(false)) {
                long expiry = pinSection.getLong(pin + ".expiry", 0);
                if (System.currentTimeMillis() / 1000 > expiry) {
                    pinSection.set(pin, null);
                    continue;
                }

                UUID owner = UUID.fromString(pinSection.getString(pin + ".uuid"));
                if (owner.equals(player.getUniqueId())) {
                    pinCode = pin;
                    alreadyHadPin = true;
                }
            }
        }

        while (pinCode.isEmpty() || (!alreadyHadPin && storage.get("pins." + pinCode) != null)) {
            pinCode = String.valueOf(new Random().nextInt(10000));
            StringBuilder sb = new StringBuilder(pinCode);
            while (sb.length() < 4) {
                sb.insert(0, '0');
            }
            pinCode = sb.toString();
        }

        List<String> rankNames = hmc.getConfig().getStringList("command.pin");
        String rankName;

        int level = storage.getPlayer(player).getLevel();
        if (level >= rankNames.size()) {
            rankName = rankNames.get(0);
        } else {
            rankName = rankNames.get(rankNames.size() - 1 - level);
        }

        String path = "pins." + pinCode + '.';
        storage.set(path + "expiry", (System.currentTimeMillis() / 1000) + 3600);
        storage.set(path + "uuid", player.getUniqueId().toString());
        storage.set(path + "rank", rankName);
        storage.set(path + "ip", storage.getPlayer(player).getIPAddress());
        storage.set(path + "isUpgraded", level > 0);

        Message.create("cmdPinShow", hmc, "PIN")
                .addPlaceholder("%PIN%", pinCode)
                .send(player);
    }
}
