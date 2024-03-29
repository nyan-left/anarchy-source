package org.rs2server.rs2.model.skills;

import org.rs2server.cache.format.CacheItemDefinition;
import org.rs2server.rs2.model.Item;
import org.rs2server.rs2.model.Mob;
import org.rs2server.rs2.model.Skills;
import org.rs2server.rs2.model.player.Player;
import org.rs2server.rs2.model.skills.smithing.Forging;
import org.rs2server.rs2.model.skills.smithing.SmithingUtils;
import org.rs2server.rs2.model.skills.smithing.SmithingUtils.ForgingBar;
import org.rs2server.rs2.net.ActionSender;
import org.rs2server.rs2.net.ActionSender.DialogueType;
import org.rs2server.rs2.util.Misc;


public class Smithing {// find code gotta turn down msuic rofl

	/**
	 * The smithing interface.
	 */
	public static final int INTERFACE = 312;

	public static void openSmithingInterface(Mob mob, ForgingBar bar) {
		if (!mob.getInventory().hasItem(SmithingUtils.HAMMER)) {
			mob.getActionSender().sendDialogue("", ActionSender.DialogueType.MESSAGE, -1, null, "You need a hammer to work the metal with.");
			mob.getActionSender().sendMessage("You need a hammer to work the metal with.");
			return;
		}
		if (mob.getSkills().getLevel(Skills.SMITHING) < bar.getBaseLevel()) {
			mob.getActionSender().sendMessage("You need a Smithing level of at least " + bar.getBaseLevel() + " to work " + bar.toString().toLowerCase() + " bars.");
			return;
		}
		mob.getActionSender().sendConfig(210, bar.ordinal() + 1);//? k ill try that lol
		mob.getActionSender().sendInterface(INTERFACE, true);
		mob.setInterfaceAttribute("smithingBar", bar);
	}

	public static void handleForgingOptions(Player player, int button, int menuIndex) {
		if (player.getInterfaceAttribute("smithingBar") == null) {
			return;
		}
		int clickedChild = -1;
		int productionAmount = -1;
		for (int i = 0; i <= 4; i++) {
			for (int index = 0; index < SmithingUtils.CHILD_IDS.length; index++) {
				if (SmithingUtils.CHILD_IDS[index] == button) {
					clickedChild = index;
					productionAmount = SmithingUtils.CLICK_OPTIONS[menuIndex];
					break;
				}
			}
		}
		if (clickedChild == -1 || productionAmount == -1) {
			return;
		}
		ForgingBar bar = player.getInterfaceAttribute("smithingBar");
		if (bar != null) {
			if (productionAmount == 32767) {
				player.getActionSender().sendEnterAmountInterface();
				player.setInterfaceAttribute("smithingIndex", clickedChild);
				player.getActionSender().closeAll();
				return;
			}
			int levelRequired = bar.getBaseLevel() + SmithingUtils.getLevelIncrement(bar, bar.getItems()[clickedChild]);
			int barsRequired = SmithingUtils.getBarAmount(levelRequired, bar, bar.getItems()[clickedChild]);
			String itemName = Misc.withPrefix(CacheItemDefinition.get(bar.getItems()[clickedChild]).getName().toLowerCase());
			if (!player.getInventory().hasItem(new Item(bar.getBarId(), barsRequired))) {
				String barName = bar.toString().toLowerCase();
				player.getActionSender().sendDialogue("", DialogueType.MESSAGE, -1, null, "You do not have enough " + barName + " bars to smith " + itemName + ".");
				player.getActionSender().closeAll();
				//player.getActionSender().removeInterface2();
				return;
			}
			if (player.getSkills().getLevel(Skills.SMITHING) < levelRequired) {
				player.getActionSender().sendDialogue("", DialogueType.MESSAGE, -1, null, "You need a Smithing level of at least " + levelRequired + " to make " + itemName + ".");
				player.getActionSender().closeAll();
				//player.getActionSender().removeInterface2();
				return;
			}
			player.getActionSender().removeAllInterfaces();
			player.removeInterfaceAttribute("smithingBar");
			player.getActionQueue().addAction(new Forging(player, bar, productionAmount, clickedChild));
			player.getActionSender().closeAll();
		}
	}

}