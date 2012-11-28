package com.mcdr.spells;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.MagicConfig;

public class ShockSpell extends TargetedEntitySpell {
	
	private boolean targetPlayers;
	private boolean obeyLos;
	private int duration;

	public ShockSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		targetPlayers = getConfigBoolean("target-players", false);
		obeyLos = getConfigBoolean("obey-los", true);
		duration = getConfigInt("duration", );
	}

	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (target instanceof Player && !targetPlayers) {
            return false;
		} else {
            return shock(caster, target, power);
		}
	}

	public PostCastAction castSpell(Player player, SpellCastState state,
			float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
            LivingEntity target = getTargetedEntity(player, range>0?range:100, targetPlayers, obeyLos);
            if (target == null) {
                    return noTarget(player);
            } else {
                    boolean shocked = shock(player, target, power);
                    if (!shocked) {
                            return noTarget(player);
                    }
                    sendMessages(player, target);
                    return PostCastAction.NO_MESSAGES;
            }
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean shock(Player player, final LivingEntity target, float power){
		target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 9));
		return false;
	}
	
}
