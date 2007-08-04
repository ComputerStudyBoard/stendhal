/*
 * @(#) games/stendhal/client/entity/Corpse2DView.java
 *
 * $Id$
 */
package games.stendhal.client.entity;

//
//

import games.stendhal.client.GameScreen;
import games.stendhal.client.sprite.Sprite;
import games.stendhal.client.sprite.SpriteStore;
import games.stendhal.client.gui.wt.EntityContainer;

import java.util.List;

/**
 * The 2D view of a corpse.
 */
public class Corpse2DView extends Entity2DView {
	/**
	 * The RP entity this view is for.
	 */
	private Corpse		corpse;

	/**
	 * The corpse height.
	 */
	private double		height;

	/**
	 * The corpse width.
	 */
	private double		width;

	/**
	 * The slot content inspector.
	 */
	private Inspector	inspector;

	/**
	 * The current content inspector.
	 */
	private EntityContainer wtEntityContainer;


	/**
	 * Create a 2D view of an entity.
	 *
	 * @param	corpse		The entity to render.
	 */
	public Corpse2DView(final Corpse corpse) {
		super(corpse);

		this.corpse = corpse;

		height = 1.0;
		width = 1.0;
	}


	//
	// Corpse2DView
	//

	/**
	 * Get the height.
	 *
	 * @return	The height in tile units.
	 */
	@Override
    public double getHeight() {
		return height;
	}


	/**
	 * Get the width.
	 *
	 * @return	The width in tile units.
	 */
	@Override
    public double getWidth() {
		return width;
	}


	//
	// Entity2DView
	//

	/**
	 * Build a list of entity specific actions.
	 * <strong>NOTE: The first entry should be the default.</strong>
	 *
	 * @param	list		The list to populate.
	 */
	@Override
	protected void buildActions(final List<String> list) {
		list.add(ActionType.INSPECT.getRepresentation());

		super.buildActions(list);
	}


	/**
	 * Build the visual representation of this entity.
	 */
	@Override
	protected void buildRepresentation() {
		String clazz = corpse.getEntityClass();
		String corpseType = corpse.getType();

		if (clazz != null) {
			if (clazz.equals("player")) {
				corpseType = corpseType + "_player";
			} else if (clazz.equals("giant_animal")) {
				corpseType = corpseType + "_giantrat";
			} else if (clazz.equals("giant_human")) {
				corpseType = corpseType + "_giantrat";
			} else if (clazz.equals("huge_animal")) {
				corpseType = corpseType + "_giantrat";
			} else if (clazz.equals("mythical_animal")) {
				corpseType = corpseType + "_huge_animal";
			} else if (clazz.equals("boss")) {
				corpseType = corpseType + "_giantrat";
			} else if (clazz.equals("enormous_creature")) {
				corpseType = corpseType + "_enormous_creature";
			}
		}

		Sprite sprite = SpriteStore.get().getSprite(translate(corpseType));

		width = (double) sprite.getWidth() / GameScreen.SIZE_UNIT_PIXELS;
		height = (double) sprite.getHeight() / GameScreen.SIZE_UNIT_PIXELS;

		setSprite(sprite);
	}


	/**
	 * Determines on top of which other entities this entity should be
	 * drawn. Entities with a high Z index will be drawn on top of ones
	 * with a lower Z index.
	 * 
	 * Also, players can only interact with the topmost entity.
	 * 
	 * @return	The drawing index.
	 */
	@Override
	public int getZIndex() {
		return 5500;
	}


	/**
	 * Set the content inspector for this entity.
	 *
	 * @param	inspector	The inspector.
	 */
	@Override
	public void setInspector(final Inspector inspector) {
		this.inspector = inspector;
	}


	//
	// EntityChangeListener
	//

	/**
	 * An entity was changed.
	 *
	 * @param	entity		The entity that was changed.
	 * @param	property	The property identifier.
	 */
	@Override
	public void entityChanged(final Entity entity, final Object property)
	{
		super.entityChanged(entity, property);

		if(property == Entity.PROP_CLASS) {
			representationChanged = true;
		}
	}


	//
	// EntityView
	//

	/**
	 * Determine if this entity can be moved (e.g. via dragging).
	 *
	 * @return	<code>true</code> if the entity is movable.
	 */
	@Override
	public boolean isMovable() {
		return true;
	}


	/**
	 * Perform the default action.
	 */
	@Override
	public void onAction() {
		onAction(ActionType.INSPECT);
	}


	/**
	 * Perform an action.
	 *
	 * @param	at		The action.
	 */
	@Override
	public void onAction(final ActionType at) {
		switch (at) {
			case INSPECT:
				wtEntityContainer = inspector.inspectMe(corpse, corpse.getContent(), wtEntityContainer);
				break;

			default:
				super.onAction(at);
				break;
		}
	}


	/**
	 * Release any view resources. This view should not be used after
	 * this is called.
	 */
	@Override
	public void release() {
		if (wtEntityContainer != null) {
			wtEntityContainer.destroy();
			wtEntityContainer = null;
		}

		super.release();
	}
}
