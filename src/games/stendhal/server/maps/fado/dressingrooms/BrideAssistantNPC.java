package games.stendhal.server.maps.fado.dressingrooms;

import games.stendhal.common.Direction;
import games.stendhal.server.StendhalRPZone;
import games.stendhal.server.entity.npc.NPCList;
import games.stendhal.server.entity.npc.OutfitChangerBehaviour;
import games.stendhal.server.entity.npc.SpeakerNPC;
import games.stendhal.server.maps.ZoneConfigurator;
import games.stendhal.server.pathfinder.Path;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Dressing rooms at fado hotel
 * 
 * @author kymara
 */
public class BrideAssistantNPC implements ZoneConfigurator {

	/**
	 * Configure a zone.
	 *
	 * @param	zone		The zone to be configured.
	 * @param	attributes	Configuration attributes.
	 */
	public void configureZone(StendhalRPZone zone, Map<String, String> attributes) {
		buildDressingRoom(zone, attributes);
	}

	private void buildDressingRoom(StendhalRPZone zone, Map<String, String> attributes) {
		SpeakerNPC npc = new SpeakerNPC("Tamara") {

			@Override
			protected void createPath() {
				List<Path.Node> nodes = new LinkedList<Path.Node>();
				// doesn't move
				setPath(nodes, false);
			}

			@Override
			protected void createDialog() {
				addGreeting("Welcome! If you're a bride-to-be I can #help you get ready for your wedding");
				addJob("I assist brides with getting dressed for their wedding.");
				addHelp("Just tell me if you want to #wear a #gown for your wedding.");
				addQuest("You don't want to be thinking about that kind of thing ahead of your big day!");
				addGoodbye("Have a lovely time!");

				Map<String, Integer> priceList = new HashMap<String, Integer>();
				priceList.put("gown", 100);
				OutfitChangerBehaviour behaviour = new OutfitChangerBehaviour(priceList);
				addOutfitChanger(behaviour, "wear");
			}
		};
		NPCList.get().add(npc);
		zone.assignRPObjectID(npc);
		npc.put("class", "woman_003_npc");
		npc.setDirection(Direction.RIGHT);
		npc.set(3, 9);
		npc.initHP(100);
		zone.add(npc);
	}
}
