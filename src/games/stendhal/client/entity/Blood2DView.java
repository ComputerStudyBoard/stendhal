/*
 * @(#) games/stendhal/client/entity/Blood2DView.java
 *
 * $Id$
 */

package games.stendhal.client.entity;

//
//

import games.stendhal.client.sprite.Sprite;
import games.stendhal.client.sprite.SpriteStore;

import java.util.Map;

/**
 * The 2D view of blood.
 */
public class Blood2DView extends StateEntity2DView {
	/**
	 * The bloo entity.
	 */
	protected Blood		blood;


	/**
	 * Create a 2D view of blood.
	 *
	 * @param	entity		The entity to render.
	 */
	public Blood2DView(final Blood blood) {
		super(blood);

		this.blood = blood;
	}


	//
	// StateEntity2DView
	//

	/**
	 * Populate named state sprites.
	 *
	 * @param	map		The map to populate.
	 */
	@Override
	protected void buildSprites(final Map<Object, Sprite> map) {
		String clazz = entity.getEntityClass();

		/*
		 * If no class (or a single character), fallback to red
		 */
		if((clazz == null) || (clazz.length() == 1)) {
			clazz = "red";
		}

		SpriteStore store = SpriteStore.get();
		Sprite tiles = store.getSprite("data/sprites/combat/blood_" + clazz + ".png");

		for(int i = 0; i < 4; i++) {
			map.put(new Integer(i), store.getSprite(tiles, 0, i, 1.0, 1.0));
		}
	}


	/**
	 * Get the current entity state.
	 *
	 * @return	The current state.
	 */
	@Override
	protected Object getState() {
		return blood.getAmount();
	}


	//
	// Entity2DView
	//

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
		return 2000;
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

		if(property == Blood.PROP_AMOUNT) {
			stateChanged = true;
		}
	}
}
