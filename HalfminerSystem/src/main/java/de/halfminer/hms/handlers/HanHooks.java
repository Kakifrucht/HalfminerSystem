package de.halfminer.hms.handlers;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.api.UserDoesNotExistException;
import de.halfminer.hms.HalfminerClass;
import de.halfminer.hms.exception.HookException;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import net.ess3.api.Economy;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;
import java.util.logging.Level;

/**
 * - Hooks external plugins
 * - Checks if plugins are loaded
 * - Fast reference to external api
 */
@SuppressWarnings("unused")
public class HanHooks extends HalfminerClass {

    private final Essentials essentialsHook;
    private final Chat vaultChatHook;

    public HanHooks() {

        Plugin essentials = server.getPluginManager().getPlugin("Essentials");
        if (essentials != null) {
            essentialsHook = (Essentials) essentials;
        } else essentialsHook = null;


        if (server.getPluginManager().getPlugin("Vault") != null) {

            RegisteredServiceProvider<Chat> provider = server.getServicesManager()
                    .getRegistration(net.milkbowl.vault.chat.Chat.class);
            if (provider != null) vaultChatHook = provider.getProvider();
            else {
                MessageBuilder.create("hanHooksLoadChatFailed").logMessage(Level.WARNING);
                vaultChatHook = null;
            }

        } else {
            MessageBuilder.create("hanHooksLoadChatFailed").logMessage(Level.WARNING);
            vaultChatHook = null;
        }
    }

    public Essentials getEssentialsHook() throws HookException {
        if (essentialsHook == null) throw new HookException();
        return essentialsHook;
    }

    void setLastTpLocation(Player player) throws HookException {
        if (essentialsHook == null) throw new HookException();
        essentialsHook.getUser(player).setLastLocation();
    }

    public boolean isAfk(Player player) throws HookException {
        if (essentialsHook == null) throw new HookException();
        return essentialsHook.getUser(player.getUniqueId()).isAfk();
    }

    public double getMoney(Player player) throws HookException {
        if (essentialsHook == null) throw new HookException();
        double balance;
        try {
            balance = net.ess3.api.Economy.getMoneyExact(player.getName()).doubleValue();
            return Utils.roundDouble(balance);
        } catch (UserDoesNotExistException e) {
            throw new HookException(e);
        }
    }

    public void addMoney(Player player, double amount) throws HookException {
        if (essentialsHook == null) throw new HookException();
        try {
            Economy.add(player.getName(), BigDecimal.valueOf(amount));
        } catch (Exception e) {
            throw new HookException(e);
        }
    }

    public String getPrefix(Player player) throws HookException {
        if (vaultChatHook == null) throw new HookException();
        return vaultChatHook.getPlayerPrefix(player);
    }

    public String getSuffix(Player player) throws HookException {
        if (vaultChatHook == null) throw new HookException();
        return vaultChatHook.getPlayerSuffix(player);
    }
}
