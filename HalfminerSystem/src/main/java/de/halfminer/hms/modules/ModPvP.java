package de.halfminer.hms.modules;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hms.enums.AttackSpeed;
import de.halfminer.hms.enums.DataType;
import de.halfminer.hms.enums.ModuleType;
import de.halfminer.hms.exception.CachingException;
import de.halfminer.hms.interfaces.Sweepable;
import de.halfminer.hms.util.ActionProbabilityContainer;
import de.halfminer.hms.util.CustomAction;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * - Halves PvP cooldown
 *   - Reduces damage immunity
 * - Strength potions damage nerfed, configurable
 * - Bow spamming disabled
 * - Disable hitting self with bow
 * - Killstreak via actionbar
 * - Run custom actions with custom probabilities on kill
 *   - See customactions.txt for example actions
 * - Sounds on kill/death
 * - Remove effects on teleport
 * - Halves satiation health regeneration during combat
 * - Remove regeneration potion effect when eating golden apple
 *   - Ensures that absorption does not fully regenerate when eating a non Notch golden apple
 * - Broadcast resurrect via Totem of Undying
 */
@SuppressWarnings("unused")
public class ModPvP extends HalfminerModule implements Listener, Sweepable {

    private double strengthReducePerLevel;
    private int thresholdUntilShown;

    private final Cache<Player, Boolean> hasShotBow = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .weakKeys()
            .build();

    private final Map<UUID, Integer> killStreak = new HashMap<>();
    private ActionProbabilityContainer container;

    @EventHandler
    public void onInteractSetAttackSpeed(PlayerInteractEvent e) {
        if (e.getHand() != null && e.getHand().equals(EquipmentSlot.HAND))
            setAttackSpeed(e.getPlayer(), e.getItem());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryScrollSetAttackSpeed(PlayerItemHeldEvent e) {
        setAttackSpeed(e.getPlayer(), e.getPlayer().getInventory().getItem(e.getNewSlot()));
    }

    @EventHandler
    public void onLeaveResetAttackSpeed(PlayerQuitEvent e) {
        e.getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(AttackSpeed.getDefaultSpeed());
    }

    private void setAttackSpeed(Player p, ItemStack stack) {

        double setTo = AttackSpeed.getDefaultSpeed();

        if (stack != null) setTo += AttackSpeed.getSpeed(stack.getType());
        else setTo *= 2;

        p.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(setTo);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAttackReduceStrengthAndLowerImmunity(EntityDamageByEntityEvent e) {

        if (!(e.getEntity() instanceof Player)) return;

        final Player damagee = (Player) e.getEntity();
        Player damager = Utils.getDamagerFromEvent(e);

        // prevent self hit with bow
        if (damagee.equals(damager)) {
            e.setCancelled(true);
        }

        if ((damager != null && damager.hasPermission("hms.bypass.pvp"))
                || damagee.hasPermission("hms.bypass.pvp")) return;

        // half damage immunity on next tick, else overwritten
        scheduler.runTaskLater(hms, () -> {
            // damage immunity passes when 10/20 has been reached
            if (!e.isCancelled()) damagee.setNoDamageTicks(15);
        }, 0L);

        if (damager != null) {

            // nerf strength potions
            for (PotionEffect effect : damager.getActivePotionEffects()) {
                if (effect.getType().equals(PotionEffectType.INCREASE_DAMAGE)) {

                    double newDamage = e.getDamage(EntityDamageEvent.DamageModifier.BASE)
                            - strengthReducePerLevel * (effect.getAmplifier() + 1);
                    double damageRatio = newDamage / e.getDamage(EntityDamageEvent.DamageModifier.BASE);

                    e.setDamage(EntityDamageEvent.DamageModifier.BASE, newDamage);
                    e.setDamage(EntityDamageEvent.DamageModifier.ARMOR,
                            e.getDamage(EntityDamageEvent.DamageModifier.ARMOR) * damageRatio);
                    e.setDamage(EntityDamageEvent.DamageModifier.MAGIC,
                            e.getDamage(EntityDamageEvent.DamageModifier.MAGIC) * damageRatio);
                    e.setDamage(EntityDamageEvent.DamageModifier.RESISTANCE,
                            e.getDamage(EntityDamageEvent.DamageModifier.RESISTANCE) * damageRatio);
                    e.setDamage(EntityDamageEvent.DamageModifier.BLOCKING,
                            e.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) * damageRatio);
                    break;
                }
            }

        }
    }

    @EventHandler(ignoreCancelled = true)
    public void preventSelfHitWithBowCombust(EntityCombustByEntityEvent e) {

        if (e.getEntity() instanceof Player && e.getCombuster() instanceof Projectile)
            e.setCancelled(e.getEntity().equals(((Projectile) e.getCombuster()).getShooter()));
    }

    @EventHandler(ignoreCancelled = true)
    public void antiBowSpam(EntityShootBowEvent e) {

        if (!e.getEntity().hasPermission("hms.bypass.pvp") && e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            long currentTime = System.currentTimeMillis();
            if (hasShotBow.getIfPresent(p) != null) {
                e.setCancelled(true);
            } else hasShotBow.put(p, true);
        }
    }

    @EventHandler
    public void deathSoundsAndStreaksAndHealAndActions(PlayerDeathEvent e) {

        e.setDeathMessage("");

        final Player killer = e.getEntity().getKiller();
        final Player victim = e.getEntity();
        if (killer != null && killer != e.getEntity()) {

            // Heal and play sound
            if (!killer.isDead()) killer.setHealth(killer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            scheduler.runTaskLaterAsynchronously(hms, () -> {
                killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);
                try {
                    Thread.sleep(300L);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f);
            }, 5);

            // run action on kill
            if (container != null &&
                    ((ModAntiKillfarming) hms
                    .getModule(ModuleType.ANTI_KILLFARMING))
                    .isNotRepeatedKill(killer, victim)) {
                CustomAction action = container.getNextAction();
                action.addPlaceholderForNextRun("%KILLNO%", storage.getPlayer(killer).getString(DataType.KILLS));
                action.runAction(killer, victim);
            }

            UUID killerUid = killer.getUniqueId();
            killStreak.remove(victim.getUniqueId());

            final int streak;
            if (killStreak.containsKey(killerUid)) streak = this.killStreak.get(killerUid) + 1;
            else streak = 1;

            killStreak.put(killerUid, streak);

            if (streak > thresholdUntilShown || streak % 5 == 0) {
                scheduler.runTaskLater(hms, () -> titleHandler.sendActionBar(null,
                        MessageBuilder.create("modPvPKillStreak", hms)
                                .addPlaceholderReplace("%PLAYER%", killer.getName())
                                .addPlaceholderReplace("%STREAK%", String.valueOf(streak))
                                .returnMessage()), 0L);
            }
        } else {
            victim.playSound(e.getEntity().getLocation(), Sound.AMBIENT_CAVE, 1.0f, 1.4f);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void teleportRemoveEffects(PlayerTeleportEvent e) {

        Player p = e.getPlayer();
        Location from = e.getFrom();
        Location to = e.getTo();
        if (!p.hasPermission("hms.bypass.pvp") && (!from.getWorld().equals(to.getWorld()) || from.distance(to) > 100.0d)) {
            p.removePotionEffect(PotionEffectType.JUMP);
            p.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
            p.removePotionEffect(PotionEffectType.LEVITATION);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void lowerRegenerationDuringFight(EntityRegainHealthEvent e) {

        if (e.getEntity() instanceof Player
                && e.getRegainReason().equals(EntityRegainHealthEvent.RegainReason.SATIATED)
                && (((ModCombatLog) hms.getModule(ModuleType.COMBAT_LOG)).isTagged((Player) e.getEntity()))) {
            e.setAmount(e.getAmount() / 2);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGoldAppleEatRemoveRegeneration(PlayerItemConsumeEvent e) {

        Player p = e.getPlayer();
        if (e.getItem().getType().equals(Material.GOLDEN_APPLE)
                && !p.hasPermission("hms.bypass.pvp")) {

            // if a player eats a Notch Apple first, and then a non Notch one, he will regain the 8
            // hearts from the Notch apple absorption, so we remove the effect first and readd it
            if (p.hasPotionEffect(PotionEffectType.ABSORPTION)
                    && e.getItem().getDurability() == 0
                    && p.getPotionEffect(PotionEffectType.ABSORPTION).getAmplifier() > 1) {

                PotionEffect oldEffect = p.getPotionEffect(PotionEffectType.ABSORPTION);
                p.removePotionEffect(PotionEffectType.ABSORPTION);
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,
                        120 * 20, // measured in ticks, 120 seconds
                        oldEffect.getAmplifier() / 4,
                        oldEffect.isAmbient()));
            }
            scheduler.runTask(hms, () -> p.removePotionEffect(PotionEffectType.REGENERATION));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onResurrect(EntityResurrectEvent e) {

        if (!(e.getEntity() instanceof Player)) return;

        Player p = (Player) e.getEntity();
        String killer = "";
        if (p.getKiller() != null) {
            killer = p.getKiller().getName();
        } else if (p.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            killer = ((EntityDamageByEntityEvent) p.getLastDamageCause()).getDamager().getName();
        }

        MessageBuilder broadcast = MessageBuilder.create(killer.length() > 0 ? "modPvPResurrect" : "modPvPResurrectSuicide", hms,
                "PvP")
                .addPlaceholderReplace("%PLAYER%", p.getName());

        if (killer.length() > 0) broadcast.addPlaceholderReplace("%KILLER%", killer);
        broadcast.broadcastMessage(true);
    }

    @Override
    public void loadConfig() {

        strengthReducePerLevel = hms.getConfig().getDouble("pvp.strengthEffectReducePerLevel", 1.5d);
        strengthReducePerLevel = Math.min(3.0d, strengthReducePerLevel);
        thresholdUntilShown = hms.getConfig().getInt("pvp.streakThreshold", 30);
        container = null;

        try {
            List<String> actionList = hms.getConfig().getStringList("pvp.actionsOnKill");
            container = new ActionProbabilityContainer(actionList, hms, storage);
        } catch (CachingException ignored) {
        }
    }

    @Override
    public void sweep() {
        hasShotBow.cleanUp();
    }
}
