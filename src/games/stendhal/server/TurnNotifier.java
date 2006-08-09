package games.stendhal.server;

import games.stendhal.server.events.TurnListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Other classes can register here to be notified sometime in the future.
 *
 * @author hendrik
 */
public class TurnNotifier {
	private static Logger logger = Logger.getLogger(TurnNotifier.class);
	
	/** The Singleton instance **/
	private static TurnNotifier instance = null;
	
	private int currentTurn = -1;
	
	/**
	 * This Map maps each turn to the set of all events that will take place
	 * at this turn.
	 * Turns at which no event should take place needn't be registered here.
	 */
	private Map<Integer, Set<TurnListener>> register = new HashMap<Integer, Set<TurnListener>>();
	
	/** Used for multi-threading synchronization. **/
	private final Object sync = new Object();

	private TurnNotifier() {
		// singleton
	}

	/**
	 * Return the TurnNotifier instance.
	 *
	 * @return TurnNotifier
	 */
	public static TurnNotifier get() {
		if (instance == null) {
			instance = new TurnNotifier();
		}
		return instance;
	}

	/**
	 * This method is invoked by StendhalRPRuleProcessor.endTurn().
	 *
	 * @param currentTurn currentTurn
	 */
	public void logic(int currentTurn) {
		// Note: It is OK to only synchronize the remove part
		//       because notifyAtTurn will not allow registrations
		//       for the current turn. So it is important to
		//       adjust currentTurn before the loop.

		this.currentTurn = currentTurn;

		// get and remove the set for this turn
		Set<TurnListener> set = null;
		synchronized (sync) {
			set = register.remove(new Integer(currentTurn));
		}

		if (set != null) {
			for (TurnListener turnEvent : set) {
				turnEvent.onTurnReached(currentTurn);
			}
		}
	}

	/**
	 * Return the number of the next turn
	 *
	 * @return number of the next turn
	 */
	public int getNumberOfNextTurn() {
		return this.currentTurn + 1;
	}
	
	/**
	 * Notifies the class <i>turnEvent</i> in <i>diff</i> turns.
	 * 
	 * @param diff the number of turns to wait
	 * @param turnEvent the class to notify
	 */
	public void notifyInTurns(int diff, TurnListener turnEvent) {
		notifyAtTurn(currentTurn + diff + 1, turnEvent);
	}

	/**
	 * Notifies the class <i>turnEvent</i> at turn number <i>turn</i>.
	 * 
	 * @param turn the number of the turn
	 * @param turnEvent the class to notify
	 */
	public void notifyAtTurn(int turn, TurnListener turnEvent) {
		if (turn <= currentTurn) {
			logger.error("requested turn " + turn + " is in the past. Current turn is " + currentTurn, new Throwable());
			return;
		}
		synchronized (sync) {
			// do we have other events for this turn?
			Integer turnInt = new Integer(turn);
			Set<TurnListener> set = register.get(turnInt);
			if (set == null) {
				set = new HashSet<TurnListener>();
				register.put(turnInt, set);
			}
			// add it to the list
			set.add(turnEvent);
		}
	}
}
