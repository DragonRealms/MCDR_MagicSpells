package com.mcdr.spells;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.MagicConfig;

public class NightmareSpell extends TargetedEntitySpell {

        @SuppressWarnings("unused")
        private static final String SPELL_NAME = "potion";
        
        private List<Integer> typeNegative;
        private List<Integer> typeCancelPositive;
        private int duration;
        private int amplifier;
        private boolean ambient;
        private boolean targeted;
        private boolean targetPlayers;
        private boolean targetNonPlayers;
        private boolean obeyLos;
        
        public NightmareSpell(MagicConfig config, String spellName) {
                super(config, spellName);
                duration = getConfigInt("duration", 0);
                amplifier = getConfigInt("strength", 0);
                ambient = getConfigBoolean("ambient", false);
                targeted = getConfigBoolean("targeted", false);
                targetPlayers = getConfigBoolean("target-players", false);
                targetNonPlayers = getConfigBoolean("target-non-players", true);
                obeyLos = getConfigBoolean("obey-los", true);
                typeNegative = Arrays.asList(2, 4, 9, 15, 17);
                typeCancelPositive = Arrays.asList(1, 3, 5, 8, 10, 11, 12, 13, 14, 16);
        }

        @Override
        public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
                if (state == SpellCastState.NORMAL) {
                        LivingEntity target;
                        if (targeted) {
                                target = getTargetedEntity(player, range, targetPlayers, targetNonPlayers, obeyLos, true);
                        } else {
                                target = player;
                        }
                        if (target == null) {
                                // fail no target
                                return noTarget(player);
                        }
                        for(int i: typeNegative){
                        	if(i == 17){
                        		target.addPotionEffect(new PotionEffect(PotionEffectType.getById(i), Math.round(duration/2 * power), amplifier), ambient);
                        	}
                        	else{ 
                        	 target.addPotionEffect(new PotionEffect(PotionEffectType.getById(i), Math.round(duration*power), amplifier), ambient);
                        	}
                        }
                        for(int i: typeCancelPositive){
                        	target.addPotionEffect(new PotionEffect(PotionEffectType.getById(i), 1, amplifier), ambient);
                        }
                        if (targeted) {
                                playSpellEffects(player, target);
                        } else {
                                playSpellEffects(EffectPosition.CASTER, player);
                        }
                        sendMessages(player, target);
                        return PostCastAction.NO_MESSAGES;
                }               
                return PostCastAction.HANDLE_NORMALLY;
        }

        @Override
        public boolean castAtEntity(Player caster, LivingEntity target, float power) {
                if (target instanceof Player && !targetPlayers) {
                        return false;
                } else if (!(target instanceof Player) && !targetNonPlayers) {
                        return false;
                } else {
                	for(int i:typeNegative){
                        PotionEffect effect = new PotionEffect(PotionEffectType.getById(i), Math.round(duration*power), amplifier);
                        if (targeted) {
                                target.addPotionEffect( effect, ambient);
                                playSpellEffects(caster, target);
                        } else {
                                caster.addPotionEffect(effect, ambient);
                                playSpellEffects(EffectPosition.CASTER, caster);
                        }
                	}
                	
                	for(int i:typeCancelPositive){
                        PotionEffect effect = new PotionEffect(PotionEffectType.getById(i), 1, amplifier);
                        if (targeted) {
                                target.addPotionEffect( effect, ambient);
                                playSpellEffects(caster, target);
                        } else {
                                caster.addPotionEffect(effect, ambient);
                                playSpellEffects(EffectPosition.CASTER, caster);
                        }
                	}
                        return true;
                }
        }

}