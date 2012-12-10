package com.mcdr.spells;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.WitherSkull;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class WitherSkullSpell extends TargetedSpell {
        
        private boolean requireEntityTarget;
        private boolean obeyLos;
        private boolean targetPlayers;
        private boolean checkPlugins;
        private float damageMultiplier;
        private float explosionSize;
        private boolean noExplosion;
        private boolean noExplosionEffect;
        private int noExplosionDamage;
        private int noExplosionDamageRange;
        private boolean noFire;
        
        private HashMap<WitherSkull,Float> WitherSkulls;
        
        public WitherSkullSpell(MagicConfig config, String spellName) {
                super(config, spellName);
                
                requireEntityTarget = getConfigBoolean("require-entity-target", false);
                obeyLos = getConfigBoolean("obey-los", true);
                targetPlayers = getConfigBoolean("target-players", false);
                checkPlugins = getConfigBoolean("check-plugins", true);
                damageMultiplier = getConfigFloat("damage-multiplier", 0);
                explosionSize = getConfigFloat("explosion-size", 0);
                noExplosion = config.getBoolean("spells." + spellName + ".no-explosion", false);
                noExplosionEffect = getConfigBoolean("no-explosion-effect", true);
                noExplosionDamage = getConfigInt("no-explosion-damage", 5);
                noExplosionDamageRange = getConfigInt("no-explosion-damage-range", 3);
                noFire = getConfigBoolean("no-fire", false);
                
                WitherSkulls = new HashMap<WitherSkull,Float>();
        }

        @Override
        public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
                if (state == SpellCastState.NORMAL) {
                        // get a target if required
                        boolean selfTarget = false;
                        if (requireEntityTarget) {
                                LivingEntity entity = getTargetedEntity(player, range, targetPlayers, obeyLos);
                                if (entity == null) {
                                        return noTarget(player);
                                } else if (entity instanceof Player && checkPlugins) {
                                        // run a pvp damage check
                                        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, entity, DamageCause.ENTITY_ATTACK, 1);
                                        Bukkit.getServer().getPluginManager().callEvent(event);
                                        if (event.isCancelled()) {
                                                return noTarget(player);
                                        }
                                }
                                if (entity.equals(player)) {
                                        selfTarget = true;
                                }
                        }
                        
                        // create WitherSkull
                        Location loc;
                        if (!selfTarget) {
                                loc = player.getEyeLocation().toVector().add(player.getLocation().getDirection().multiply(2)).toLocation(player.getWorld(), player.getLocation().getYaw(), player.getLocation().getPitch());
                        } else {
                                loc = player.getLocation().toVector().add(player.getLocation().getDirection().setY(0).multiply(2)).toLocation(player.getWorld(), player.getLocation().getYaw()+180, 0);
                        }
                        WitherSkull WitherSkull;
                        
                        WitherSkull = player.getWorld().spawn(loc, WitherSkull.class);
                        player.getWorld().playEffect(player.getLocation(), Effect.GHAST_SHOOT, 0);
                        WitherSkull.setShooter(player);
                        WitherSkulls.put(WitherSkull,power);
                        
                        playSpellEffects(EffectPosition.CASTER, player);
                }
                return PostCastAction.HANDLE_NORMALLY;
        }
        
        @EventHandler(priority=EventPriority.HIGH)
        public void onExplosionPrime(ExplosionPrimeEvent event) {
                if (event.isCancelled()) {
                        return;
                }
                
                if (event.getEntity() instanceof WitherSkull) {
                        final WitherSkull WitherSkull = (WitherSkull)event.getEntity();
                        if (WitherSkulls.containsKey(WitherSkull)) {
                                playSpellEffects(EffectPosition.TARGET, WitherSkull.getLocation());
                                if (noExplosion) {
                                        event.setCancelled(true);
                                        Location loc = WitherSkull.getLocation();
                                        if (noExplosionEffect) {
                                                loc.getWorld().createExplosion(loc, 0);
                                        }
                                        if (noExplosionDamage > 0) {
                                                float power = WitherSkulls.get(WitherSkull);
                                                List<Entity> inRange = WitherSkull.getNearbyEntities(noExplosionDamageRange, noExplosionDamageRange, noExplosionDamageRange);
                                                for (Entity entity : inRange) {
                                                        if (entity instanceof LivingEntity) {
                                                                if (targetPlayers || !(entity instanceof Player)) {
                                                                        ((LivingEntity)entity).damage(Math.round(noExplosionDamage * power), WitherSkull.getShooter());
                                                                }
                                                        }
                                                }
                                        }
                                        if (!noFire) {
                                                final HashSet<Block> fires = new HashSet<Block>();
                                                for (int x = loc.getBlockX()-1; x <= loc.getBlockX()+1; x++) {
                                                        for (int y = loc.getBlockY()-1; y <= loc.getBlockY()+1; y++) {
                                                                for (int z = loc.getBlockZ()-1; z <= loc.getBlockZ()+1; z++) {
                                                                        if (loc.getWorld().getBlockTypeIdAt(x,y,z) == 0) {
                                                                                Block b = loc.getWorld().getBlockAt(x,y,z);
                                                                                b.setTypeIdAndData(Material.FIRE.getId(), (byte)15, false);
                                                                                fires.add(b);
                                                                        }
                                                                }
                                                        }                                               
                                                }
                                                WitherSkull.remove();
                                                if (fires.size() > 0) {
                                                        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                        for (Block b : fires) {
                                                                                if (b.getType() == Material.FIRE) {
                                                                                        b.setType(Material.AIR);
                                                                                }
                                                                        }
                                                                }
                                                        }, 20);
                                                }
                                        }
                                } else {
                                        if (noFire) {
                                                event.setFire(false);
                                        } else {
                                                event.setFire(true);
                                        }
                                        if (explosionSize > 0) {
                                                event.setRadius(explosionSize);
                                        }
                                }
                                if (noExplosion || (damageMultiplier == 0 && targetPlayers)) {
                                        // remove immediately
                                        WitherSkulls.remove(WitherSkull);
                                } else {
                                        // schedule removal (gotta wait for damage events)
                                        Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
                                                public void run() {
                                                        WitherSkulls.remove(WitherSkull);
                                                }
                                        }, 1);
                                }
                        }
                }
        }

        @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
        public void onEntityDamage(EntityDamageEvent event) {
                if ((damageMultiplier > 0 || !targetPlayers) && event.getEntity() instanceof LivingEntity && event instanceof EntityDamageByEntityEvent && (event.getCause() == DamageCause.ENTITY_EXPLOSION || event.getCause() == DamageCause.PROJECTILE)) {
                        EntityDamageByEntityEvent evt = (EntityDamageByEntityEvent)event;
                        if (evt.getDamager() instanceof WitherSkull) {
                                WitherSkull WitherSkull = (WitherSkull)evt.getDamager();
                                if (WitherSkull.getShooter() instanceof Player && WitherSkulls.containsKey(WitherSkull)) {
                                        float power = WitherSkulls.get(WitherSkull);
                                        if (event.getEntity() instanceof Player && !targetPlayers) {
                                                event.setCancelled(true);
                                        } else if (damageMultiplier > 0) {
                                                event.setDamage(Math.round(event.getDamage() * damageMultiplier * power));
                                        }
                                }
                        }
                }
        }
        
}