/***************************************************************************
 *                      (C) Copyright 2003 - Marauroa                      *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

package games.stendhal.server.entity.npc;

import games.stendhal.common.Rand;
import games.stendhal.server.StendhalRPRuleProcessor;
import games.stendhal.server.entity.player.Outfit;
import games.stendhal.server.entity.player.Player;
import games.stendhal.server.events.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Represents the behaviour of a NPC who is able to sell outfits
 * to a player.
 */
public class OutfitChangerBehaviour extends MerchantBehaviour implements TurnListener, LoginListener {

	public static final int NEVER_WEARS_OFF = -1;

	private int endurance;
	
	private String wearOffMessage;
	
	// all available outfit types are predefined here.
	private static Map<String, List<Outfit>> outfitTypes = new HashMap<String, List<Outfit>>();
	static {
		// In each line, there is one possible outfit of this
		// outfit type, in the format: hair, head, dress, base.
		// One of these outfit will be chosen randomly.
		outfitTypes.put("male_swimsuit", Arrays.asList(
				new Outfit(Outfit.NO_CHANGE, Outfit.NO_CHANGE, 95, Outfit.NO_CHANGE),
				new Outfit(Outfit.NO_CHANGE, Outfit.NO_CHANGE, 96, Outfit.NO_CHANGE),
				new Outfit(Outfit.NO_CHANGE, Outfit.NO_CHANGE, 97, Outfit.NO_CHANGE),
				new Outfit(Outfit.NO_CHANGE, Outfit.NO_CHANGE, 98, Outfit.NO_CHANGE)));
		
		outfitTypes.put("female_swimsuit", Arrays.asList(
				new Outfit(Outfit.NO_CHANGE, Outfit.NO_CHANGE, 91, Outfit.NO_CHANGE),
				new Outfit(Outfit.NO_CHANGE, Outfit.NO_CHANGE, 92, Outfit.NO_CHANGE),
				new Outfit(Outfit.NO_CHANGE, Outfit.NO_CHANGE, 93, Outfit.NO_CHANGE),
				new Outfit(Outfit.NO_CHANGE, Outfit.NO_CHANGE, 94, Outfit.NO_CHANGE)));
		
		outfitTypes.put("mask", Arrays.asList(
				new Outfit(0, 80, Outfit.NO_CHANGE, Outfit.NO_CHANGE),
				new Outfit(0, 81, Outfit.NO_CHANGE, Outfit.NO_CHANGE),
				new Outfit(0, 82, Outfit.NO_CHANGE, Outfit.NO_CHANGE),
				new Outfit(0, 83, Outfit.NO_CHANGE, Outfit.NO_CHANGE),
				new Outfit(0, 84, Outfit.NO_CHANGE, Outfit.NO_CHANGE)));

		outfitTypes.put("pizza_delivery_uniform", Arrays.asList(
				new Outfit(Outfit.NO_CHANGE, Outfit.NO_CHANGE, 90, Outfit.NO_CHANGE)));
	}
	
	/**
	 * Creates a new OutfitChangerBehaviour for outfits never wear off
	 * automatically.
	 *
	 * @param priceList list of outfit types and their prices
	 */
	public OutfitChangerBehaviour(Map<String, Integer> priceList) {
		this(priceList, NEVER_WEARS_OFF, null);
	}

	/**
	 * Creates a new OutfitChangerBehaviour for outfits that wear off
	 * automatically after some time.
	 *
	 * @param priceList list of outfit types and their prices
	 * @param endurance the time (in turns) the outfit will stay, or
	 * 					DONT_WEAR_OFF if the outfit should never disappear
	 * 				    automatically.
	 * @param wearOffMessage the message that the player should receive after
	 * 					the outfit has worn off, or null if no message should
	 * 					be sent.
	 */
	public OutfitChangerBehaviour(Map<String, Integer> priceList, int endurance, String wearOffMessage) {
		super(priceList);
		this.endurance = endurance;
		this.wearOffMessage = wearOffMessage;
	}

	/**
	 * Transacts the sale that has been agreed on earlier via
	 * setChosenItem() and setAmount().
	 *
	 * @param seller The NPC who sells
	 * @param player The player who buys
	 * @return true iff the transaction was successful, that is when the
	 *              player was able to equip the item(s).
	 */
	@Override
	protected boolean transactAgreedDeal(SpeakerNPC seller, Player player) {
		String outfitType = chosenItem;
		if (player.isEquipped("money", getCharge(player))) {
			player.drop("money", getCharge(player));
			putOnOutfit(player, outfitType);
			return true;
		} else {
			seller.say("Sorry, you don't have enough money!");
			return false;
		}
	}

	/**
	 * Tries to get back the bought/lent outfit and give the player
	 * his original outfit back.
	 * This will only be successful if the player is wearing an outfit
	 * he got here, and if the original outfit has been stored.
	 * @param player The player.
	 * @return true iff returning was successful.
	 */
	public void putOnOutfit(Player player, String outfitType) {
		List<Outfit> possibleNewOutfits = outfitTypes.get(outfitType);
		Outfit newOutfit = Rand.rand(possibleNewOutfits);
		player.setOutfit(player.getOutfit().combineWith(newOutfit), true);

		if (endurance != NEVER_WEARS_OFF) {
			// make the costume disappear after some time
			TurnNotifier.get().notifyInTurns(endurance, this, player.getName());
		}
	}
	
	/**
	 * Checks whether or not the given player is currently
	 * wearing an outfit that may have been bought/lent from an
	 * NPC with this behaviour.
	 * @param player The player.
	 * @return true iff the player wears an outfit from here.
	 */
	public boolean wearsOutfitFromHere(Player player) {
		Outfit currentOutfit = player.getOutfit();
		
		for (String outfitType: priceList.keySet()) { 
			List<Outfit> possibleOutfits = outfitTypes.get(outfitType);
			for (Outfit possibleOutfit: possibleOutfits) {
				if ((possibleOutfit.getHair() == Outfit.NO_CHANGE
								|| possibleOutfit.getHair() == currentOutfit.getHair())
						&& (possibleOutfit.getHead() == Outfit.NO_CHANGE
								|| possibleOutfit.getHead() == currentOutfit.getHead())
						&& (possibleOutfit.getDress() == Outfit.NO_CHANGE
								|| possibleOutfit.getDress() == currentOutfit.getDress())
						&& (possibleOutfit.getBase() == Outfit.NO_CHANGE
								|| possibleOutfit.getBase() == currentOutfit.getBase())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Tries to get back the bought/lent outfit and give the player
	 * his original outfit back.
	 * This will only be successful if the player is wearing an outfit
	 * he got here, and if the original outfit has been stored.
	 * @param player The player.
	 * @return true iff returning was successful.
	 */
	public boolean returnToOriginalOutfit(Player player) {
		if (wearsOutfitFromHere(player)) {
			return player.returnToOriginalOutfit();
		}
		return false;
	}
	
	protected void onWornOff(Player player) {
		player.sendPrivateText(wearOffMessage);
		returnToOriginalOutfit(player);
	}

	public void onTurnReached(int currentTurn, String message) {
		String playerName = message;
		Player player = StendhalRPRuleProcessor.get().getPlayer(playerName);
		if (player != null) {
			onWornOff(player);
		} else {
			// The player has logged out before the outfit wore off.
			// Remove it when the player logs in again.
			LoginNotifier.get().notifyOnLogin(playerName, this, null);
		}
	}

	public void onLoggedIn(String playerName, String message) {
		Player player = StendhalRPRuleProcessor.get().getPlayer(playerName);
		onWornOff(player);
	}
}
