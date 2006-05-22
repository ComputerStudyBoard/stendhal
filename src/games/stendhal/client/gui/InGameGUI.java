/* $Id$ */
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
package games.stendhal.client.gui;

import games.stendhal.client.*;
import games.stendhal.client.entity.*;
import games.stendhal.client.gui.wt.*;
import games.stendhal.client.gui.wt.core.*;
import games.stendhal.common.CollisionDetection;
import games.stendhal.common.Direction;

import java.awt.Point;
import java.awt.event.*;

import java.util.HashMap;
import java.util.Map;

import marauroa.common.Log4J;
import marauroa.common.game.RPAction;
import marauroa.common.game.RPObject;
import marauroa.common.game.RPSlot;

import org.apache.log4j.Logger;

public class InGameGUI implements KeyListener {
	/** the logger instance. */
	private static final Logger logger = Log4J.getLogger(InGameGUI.class);

	private StendhalClient client;

	private GameObjects gameObjects;

	private GameScreen screen;

	/** a nicer way of handling the keyboard */
	private Map<Integer, Object> pressed;

	/** the main frame */
	private WtFrame frame;

	/** this is the ground */
	private WtPanel ground;

	/** settings panel */
	private SettingsPanel settings;

	/** the dialog "really quit?" */
	private WtPanel quitDialog;

	private Sprite offlineIcon;

	private boolean offline;

	private int blinkOffline;

	private void fixkeyboardHandlinginX() {
		logger.debug("OS: " + System.getProperty("os.name"));

		if (System.getProperty("os.name").toLowerCase().contains("linux")) {
			try {
				// NOTE: X does handle input in a different way of the rest of
				// the world.
				// This fixs the problem.
				Runtime.getRuntime().exec("xset r off");
				Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run() {
						try {
							Runtime.getRuntime().exec("xset r on");
						} catch (Exception e) {
							logger.fatal(e);
						}
					}
				});
			} catch (Exception e) {
				logger.error("Error setting keyboard handling", e);
			}
		}
	}

	public InGameGUI(StendhalClient client) {
		fixkeyboardHandlinginX();

		client.setGameGUI(this);
		this.client = client;

		gameObjects = client.getGameObjects();
		screen = GameScreen.get();

		pressed = new HashMap<Integer, Object>();

		offlineIcon = SpriteStore.get().getSprite("data/gui/offline.png");

		buildGUI();
	}

	public void offline() {
		offline = true;
	}

	public void online() {
		offline = false;
	}

	private void buildGUI() {
		// create the frame
		frame = new WtFrame(screen);
		// register native event handler
		screen.getComponent().addMouseListener(frame);
		screen.getComponent().addMouseMotionListener(frame);
		// create ground
		ground = new GroundContainer(screen, gameObjects, this);
		frame.addChild(ground);
		// the settings panel creates all other
		settings = new SettingsPanel(ground, gameObjects);
		ground.addChild(settings);

		// set some default window positions
		WtWindowManager windowManager = WtWindowManager.getInstance();
		windowManager.setDefaultProperties("corpse", false, 0, 190);
		windowManager.setDefaultProperties("chest", false, 100, 190);
	}

	// MouseListener event functions where unused and therefore have been
	// removed intensifly@gmx.com

	private boolean ctrlDown;

	private boolean shiftDown;

	private boolean altDown;

	public void onKeyPressed(KeyEvent e) {
		RPAction action;

		if (e.isShiftDown()) {
			/*
			 * We are going to use shift to move to previous/next line of text
			 * with arrows so we just ignore the keys if shift is pressed.
			 */
			return;
		}

		if (e.getKeyCode() == KeyEvent.VK_L && e.isControlDown()) {
			/* If Ctrl+L we set the Game log dialog visible */
			client.getGameLogDialog().setVisible(true);
		} else if (e.getKeyCode() == KeyEvent.VK_LEFT
				|| e.getKeyCode() == KeyEvent.VK_RIGHT
				|| e.getKeyCode() == KeyEvent.VK_UP
				|| e.getKeyCode() == KeyEvent.VK_DOWN) {
			action = new RPAction();
			if (e.isControlDown()) {
				// We use Ctrl+arrow to face
				action.put("type", "face");
			} else {
				// While arrow only moves the player
				action.put("type", "move");
			}

			switch (e.getKeyCode()) {
			case KeyEvent.VK_LEFT:
				action.put("dir", Direction.LEFT.get());
				break;
			case KeyEvent.VK_RIGHT:
				action.put("dir", Direction.RIGHT.get());
				break;
			case KeyEvent.VK_UP:
				action.put("dir", Direction.UP.get());
				break;
			case KeyEvent.VK_DOWN:
				action.put("dir", Direction.DOWN.get());
				break;
			}

			client.send(action);
		}
	}

	public void onKeyReleased(KeyEvent e) {
		RPAction action = new RPAction();
		action.put("type", "stop");

		switch (e.getKeyCode()) {
		case KeyEvent.VK_LEFT:
		case KeyEvent.VK_RIGHT:
		case KeyEvent.VK_UP:
		case KeyEvent.VK_DOWN:
			// Notify server that player is stopped.
			int keys = (pressed.containsKey(KeyEvent.VK_LEFT) ? 1 : 0)
					+ (pressed.containsKey(KeyEvent.VK_RIGHT) ? 1 : 0)
					+ (pressed.containsKey(KeyEvent.VK_UP) ? 1 : 0)
					+ (pressed.containsKey(KeyEvent.VK_DOWN) ? 1 : 0);
			if (keys == 1) {
				client.send(action);
			}
			break;
		}
	}

	public void keyPressed(KeyEvent e) {
		altDown = e.isAltDown();
		ctrlDown = e.isControlDown();
		shiftDown = e.isShiftDown();

		if (!pressed.containsKey(Integer.valueOf(e.getKeyCode()))) {
			onKeyPressed(e);
			pressed.put(Integer.valueOf(e.getKeyCode()), null);
		}
	}

	public void keyReleased(KeyEvent e) {
		altDown = e.isAltDown();
		ctrlDown = e.isControlDown();
		shiftDown = e.isShiftDown();

		onKeyReleased(e);
		pressed.remove(Integer.valueOf(e.getKeyCode()));
	}

	/**
	 * Stops all player actions and shows a dialog in which the player can
	 * confirm that he really wants to quit the program. If so, requests a
	 * logout via the StendhalClient class.
	 */
	public void showQuitDialog() {
		// Stop the player
		RPAction rpaction = new RPAction();
		rpaction.put("type", "stop");
		rpaction.put("attack", "");
		client.send(rpaction);

		// quit messagebox already showing?
		if (quitDialog == null) {
			// no, so show it
			quitDialog = new WtMessageBox("quit", 220, 220, 200,
					"Quit Stendhal?", WtMessageBox.ButtonCombination.YES_NO);
			quitDialog.registerClickListener(new WtClickListener() {
				public void onClick(String name, Point point) {
					quitDialog = null; // remove field as the messagebox is
										// closed now
					if (name.equals(WtMessageBox.ButtonEnum.YES.getName())) {
						// Yes-Button clicked...logut and quit.
						client.requestLogout();
					}
				};
			});
			frame.addChild(quitDialog);
		}
	}
	
	public void keyTyped(KeyEvent e) {
		if (e.getKeyChar() == 27) {
			// escape typed
			showQuitDialog();
		}
	}

	/**
	 * This methods inspects an entity by enabling all the droppable areas. To
	 * stop inspecting this method is called with entity=null
	 */
	public EntityContainer inspect(Entity entity, RPSlot slot) {
		return inspect(entity, slot, 2, 2);
	}

	public EntityContainer inspect(Entity entity, RPSlot slot, int width,
			int height) {
		if (entity == null || slot == null || ground == null) {
			return null;
		}

		EntityContainer container = new EntityContainer(gameObjects, entity
				.getType(), width, height);
		container.setSlot(entity, slot.getName());
		ground.addChild(container);

		return container;
	}

	public void draw(GameScreen screen) {
		// create the map if there is none yet
		StaticGameLayers gl = client.getStaticGameLayers();
		if (gl.changedArea()) {
			CollisionDetection cd = gl.getCollisionDetection();
			if (cd != null) {
				gl.resetChangedArea();
				settings.updateMinimap(cd, screen.expose()
						.getDeviceConfiguration(), gl.getArea());
			}
		}

		RPObject player = client.getPlayer();
		settings.setPlayer(player);

		frame.draw(screen.expose());

		if (offline && blinkOffline > 0) {
			offlineIcon.draw(screen.expose(), 560, 420);
		}

		if (blinkOffline < -10) {
			blinkOffline = 20;
		} else {
			blinkOffline--;
		}

	}

	/**
	 * @return Returns the altDown.
	 */
	public boolean isAltDown() {
		return altDown;
	}

	/**
	 * @return Returns the ctrlDown.
	 */
	public boolean isCtrlDown() {
		return ctrlDown;
	}

	/**
	 * @return Returns the shiftDown.
	 */
	public boolean isShiftDown() {
		return shiftDown;
	}

	/**
	 * @return Returns the window toolkit baseframe.
	 */
	public WtFrame getFrame() {
		return frame;
	}
}
