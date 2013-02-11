package com.surajc.starfish;

import org.andengine.entity.particle.SpriteParticleSystem;
import org.andengine.entity.particle.emitter.CircleParticleEmitter;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.opengl.texture.region.ITiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

public class StarfishSprite extends AnimatedSprite {
	
	private SpriteParticleSystem mBubblesParticleSystem;
	
	public StarfishSprite(float pX, float pY,
			ITiledTextureRegion pTiledTextureRegion,
			VertexBufferObjectManager vertexBufferObjectManager) {
		super(pX, pY, pTiledTextureRegion, vertexBufferObjectManager);
	}

	@Override
	protected void onManagedUpdate(float pSecondsElapsed) {
		super.onManagedUpdate(pSecondsElapsed);
		
		if (mBubblesParticleSystem != null) {
			((CircleParticleEmitter) mBubblesParticleSystem.getParticleEmitter()).setCenter(this.getX(), this.getY());
		}
		
	}

	public void attachParticleSystem(SpriteParticleSystem pParticleSystem) {
		this.mBubblesParticleSystem = pParticleSystem;
	}
}
