package com.surajc.starfish;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.handler.physics.PhysicsHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.particle.Particle;
import org.andengine.entity.particle.SpriteParticleSystem;
import org.andengine.entity.particle.emitter.CircleParticleEmitter;
import org.andengine.entity.particle.initializer.AccelerationParticleInitializer;
import org.andengine.entity.particle.initializer.IParticleInitializer;
import org.andengine.entity.particle.initializer.RotationParticleInitializer;
import org.andengine.entity.particle.initializer.VelocityParticleInitializer;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.constants.PhysicsConstants;
import org.andengine.input.touch.TouchEvent;
import org.andengine.input.touch.controller.MultiTouch;
import org.andengine.input.touch.controller.MultiTouchController;
import org.andengine.opengl.texture.ITexture;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.atlas.bitmap.BuildableBitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.buildable.builder.BlackPawnTextureAtlasBuilder;
import org.andengine.opengl.texture.atlas.buildable.builder.ITextureAtlasBuilder.TextureAtlasBuilderException;
import org.andengine.opengl.texture.bitmap.BitmapTexture;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.ITiledTextureRegion;
import org.andengine.opengl.texture.region.TextureRegionFactory;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.adt.io.in.IInputStreamOpener;

import android.os.Bundle;
import android.view.MotionEvent;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;

public class StarfishActivity extends SimpleBaseGameActivity implements IOnSceneTouchListener {
	
	private static final int CAMERA_WIDTH = 800;
	private static final int CAMERA_HEIGHT = 480;
	
	private BuildableBitmapTextureAtlas mStarfishBitmapTextureAtlas;
	private BuildableBitmapTextureAtlas mFishBitmapTextureAtlas;
	
	private ITextureRegion mBackgroundTextureRegion;
	private ITiledTextureRegion mStarfishTextureRegion;
	private ITiledTextureRegion mFishTextureRegion;
	private ITextureRegion mRodTextureRegion;
	private ITextureRegion mBubbleWhiteTextureRegion;
	
	private Sprite mBackgroundSprite;
	private StarfishSprite mStarfishSprite;
	private AnimatedSprite mFishSprite;
	private Sprite mRodSprite;
	private SpriteParticleSystem mBubblesParticleSystem;
	
	private Scene mScene;
	private PhysicsWorld mPhysicsWorld;
	private PhysicsConnector mPhysicsConnector;
	private Body mStarfishBody;
	
	private PhysicsHandler fishPhysicsHandler;
	private PhysicsHandler rodPhysicsHandler;
	
	private static final float STARFISH_INIT_POS_X = 70.0f;
	private static final float STARFISH_INIT_POS_Y = 330.0f;
	private static final float STARFISH_VELOCITY = 500;
	
	private static final int BUBBLES_MAX_NUMBER = 20;
	private static final float BUBBLES_MIN_RATE = 1f;
	private static final float BUBBLES_MAX_RATE = 10f;
	private static final float BUBBLES_ACCELERATION_X = 0f;
	private static final float BUBBLES_ACCELERATION_Y = -20f;
	private static final float BUBBLES_VELOCITY_X = 0f;
	private static final float BUBBLES_VELOCITY_Y = -120f;
	private static final float BUBBLES_ROTATION_MIN = 0f;
	private static final float BUBBLES_ROTATION_MAX = 360f;
	private ArrayList<Particle<Sprite>> mBubbles;
	private int mNumberOfBubblesUsed = 0;
	
	private int touchCounter = 0; // number of places currently touched on the screen
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public EngineOptions onCreateEngineOptions() {
		final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera);
	}

	@Override
	protected void onCreateResources() {
		
		try {
			ITexture backgroundTexture = new BitmapTexture(getTextureManager(), new IInputStreamOpener() {
				@Override
				public InputStream open() throws IOException {
					return getAssets().open("gfx/background.png");
				}
			});
			
			ITexture bubbleWhiteTexture = new BitmapTexture(getTextureManager(), new IInputStreamOpener() {
				@Override
				public InputStream open() throws IOException {
					return getAssets().open("gfx/bubbles/Bubble - White.png");
				}
			});
			
			ITexture rodTexture = new BitmapTexture(getTextureManager(), new IInputStreamOpener() {
				@Override
				public InputStream open() throws IOException {
					return getAssets().open("gfx/tower.png");
				}
			});
			
			this.mStarfishBitmapTextureAtlas = new BuildableBitmapTextureAtlas(getTextureManager(), 128, 4096, TextureOptions.BILINEAR);
			this.mStarfishTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAssetDirectory(mStarfishBitmapTextureAtlas, getAssets(), "gfx/starfish");
			
			this.mFishBitmapTextureAtlas = new BuildableBitmapTextureAtlas(getTextureManager(), 4096, 4096, TextureOptions.BILINEAR);
			this.mFishTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAssetDirectory(mFishBitmapTextureAtlas, getAssets(), "gfx/fish");
			
			try {
				mStarfishBitmapTextureAtlas.build(new BlackPawnTextureAtlasBuilder<IBitmapTextureAtlasSource, BitmapTextureAtlas>(0, 0, 0));
			} catch (TextureAtlasBuilderException e) {
				e.printStackTrace();
			}
			
			try {
				mFishBitmapTextureAtlas.build(new BlackPawnTextureAtlasBuilder<IBitmapTextureAtlasSource, BitmapTextureAtlas>(0, 0, 0));
			} catch (TextureAtlasBuilderException e) {
				e.printStackTrace();
			}
			
			bubbleWhiteTexture.load();
			backgroundTexture.load();
			rodTexture.load();
			this.mStarfishBitmapTextureAtlas.load();
			this.mFishBitmapTextureAtlas.load();
			
			this.mBackgroundTextureRegion = TextureRegionFactory.extractFromTexture(backgroundTexture);
			this.mBubbleWhiteTextureRegion = TextureRegionFactory.extractFromTexture(bubbleWhiteTexture);
			this.mRodTextureRegion = TextureRegionFactory.extractFromTexture(rodTexture);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected Scene onCreateScene() {
		this.mScene = new Scene();
		this.mPhysicsWorld = new PhysicsWorld(new Vector2(), false);
		mBubbles = new ArrayList<Particle<Sprite>>();
		initEngine();
		initBackground();
		initWalls();
		initStarfish();
        initBubbles();
        initFish();
        initRod();
        
        this.mScene.attachChild(mBackgroundSprite);
        this.mScene.attachChild(mStarfishSprite);
        this.mScene.attachChild(mBubblesParticleSystem);
        this.mScene.attachChild(mRodSprite);
        this.mScene.attachChild(mFishSprite);
        this.mScene.registerUpdateHandler(this.mPhysicsWorld);
        
        this.mScene.registerUpdateHandler(new IUpdateHandler() {
			
			@Override
			public void reset() {
			}
			
			@Override
			public void onUpdate(float pSecondsElapsed) {
				// if fish collides with rod
				if (mFishSprite.collidesWith(mRodSprite)) {
					setFishToAnimateDead(mFishSprite);
					fishCaptured(fishPhysicsHandler, rodPhysicsHandler);
				}
			}
		});
        
        this.mScene.registerUpdateHandler(new IUpdateHandler() {
			
			@Override
			public void reset() {
			}
			
			@Override
			public void onUpdate(float pSecondsElapsed) {
				if (!mBubbles.isEmpty()) {
					for (int i = 0; i < mBubbles.size(); i++) {
						Sprite bubble = mBubbles.get(i).getEntity();
						if (mRodSprite.collidesWith(bubble) && bubble.isVisible()) {
							bubble.setVisible(false);
							mNumberOfBubblesUsed++;
							float rodX = mRodSprite.getX();
							float rodY = mRodSprite.getY();
							rodY -= 10;
							mRodSprite.setPosition(rodX, rodY);
						}
					}
				}
			}
		});
        
        this.mScene.setOnSceneTouchListener(this);
        
		return this.mScene;
	}

	private void initEngine() {
		this.mEngine.registerUpdateHandler(new FPSLogger());
		if (MultiTouch.isSupported(this)) {
			this.mEngine.setTouchController(new MultiTouchController());
		}
	}
	
	private void initBackground() {
		// Set up background sprite
		this.mBackgroundSprite = new Sprite(0, 0, this.mBackgroundTextureRegion, getVertexBufferObjectManager());
	}
	
	private void initWalls() {
		// Set up transparent walls
		final Rectangle ground = new Rectangle(0, CAMERA_HEIGHT - 1, CAMERA_WIDTH, 1, getVertexBufferObjectManager());
        final Rectangle roof = new Rectangle(0, 0, CAMERA_WIDTH, 1, getVertexBufferObjectManager());
        final Rectangle left = new Rectangle(0, 0, 1, CAMERA_HEIGHT, getVertexBufferObjectManager());
        final Rectangle right = new Rectangle(CAMERA_WIDTH - 1, 0, 1, CAMERA_HEIGHT, getVertexBufferObjectManager());
        ground.setAlpha(0.0f);
        roof.setAlpha(0.0f);
        left.setAlpha(0.0f);
        right.setAlpha(0.0f);
        		
	    final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0.0f, 0.0f);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, ground, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, roof, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, left, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, right, BodyType.StaticBody, wallFixtureDef);

        this.mScene.attachChild(ground);
        this.mScene.attachChild(roof);
        this.mScene.attachChild(left);
        this.mScene.attachChild(right);
	}

	private void initStarfish() {
		// Set up animated Starfish sprite
        this.mStarfishSprite = new StarfishSprite(STARFISH_INIT_POS_X, STARFISH_INIT_POS_Y, mStarfishTextureRegion, getVertexBufferObjectManager());
        this.setStarfishToAnimateNormal();
        
        final FixtureDef starfishFixtureDef = PhysicsFactory.createFixtureDef(1, 0.0f, 0.0f);
        this.mStarfishBody = PhysicsFactory.createBoxBody(mPhysicsWorld, mStarfishSprite, BodyType.DynamicBody, starfishFixtureDef);

        this.mPhysicsConnector = new PhysicsConnector(mStarfishSprite, mStarfishBody, true, false);
        this.mPhysicsWorld.registerPhysicsConnector(mPhysicsConnector);
	}
	
	private void initBubbles() {
		// Set up bubbles
        final CircleParticleEmitter particleEmitter = new CircleParticleEmitter(0, 0, 15);
        mBubblesParticleSystem = new SpriteParticleSystem(particleEmitter, BUBBLES_MIN_RATE, BUBBLES_MAX_RATE, BUBBLES_MAX_NUMBER, mBubbleWhiteTextureRegion, getVertexBufferObjectManager());
        mBubblesParticleSystem.addParticleInitializer(new VelocityParticleInitializer<Sprite>(BUBBLES_VELOCITY_X, BUBBLES_VELOCITY_Y));
        mBubblesParticleSystem.addParticleInitializer(new AccelerationParticleInitializer<Sprite>(BUBBLES_ACCELERATION_X, BUBBLES_ACCELERATION_Y));
        mBubblesParticleSystem.addParticleInitializer(new RotationParticleInitializer<Sprite>(BUBBLES_ROTATION_MIN, BUBBLES_ROTATION_MAX));
        mBubblesParticleSystem.addParticleInitializer(new IParticleInitializer<Sprite>() {
			@Override
			public void onInitializeParticle(Particle<Sprite> pParticle) {
				mBubbles.add(pParticle);
			}
		});
        mBubblesParticleSystem.setParticlesSpawnEnabled(false);
        this.mStarfishSprite.attachParticleSystem(mBubblesParticleSystem);		
	}
	
	private void initFish() {
		// Set up fish
		this.mFishSprite = new AnimatedSprite(-20, 100, mFishTextureRegion, getVertexBufferObjectManager());
		setFishToAnimateNormal(this.mFishSprite);
		fishPhysicsHandler = new PhysicsHandler(mFishSprite);
		this.mFishSprite.registerUpdateHandler(fishPhysicsHandler);
		fishPhysicsHandler.setVelocity(40, 0);
	}
	 
	private void initRod() {
		this.mRodSprite = new Sprite(500, -30, mRodTextureRegion, getVertexBufferObjectManager());
		rodPhysicsHandler = new PhysicsHandler(mRodSprite);
		this.mRodSprite.registerUpdateHandler(rodPhysicsHandler);
	}

	@Override
	public boolean onSceneTouchEvent(Scene pScene, TouchEvent pSceneTouchEvent) {
		float touchX = pSceneTouchEvent.getX();
		float touchY = pSceneTouchEvent.getY();
		
		if (pSceneTouchEvent.getAction() == MotionEvent.ACTION_DOWN) {
			touchCounter++;
			if (isTouchingStarfish(touchX, touchY)) {
				setStarfishToAnimateTouched();
				startMakingBubbles();
				stopMovingStarfish();
			} else {
				moveStarfishTowardsTouch(touchX, touchY);
			}
		}
		if (pSceneTouchEvent.getAction() == MotionEvent.ACTION_UP) {
			touchCounter--;
			if (touchCounter == 0) {
				stopMovingStarfish();
				stopMakingBubbles();
				setStarfishToAnimateNormal();
			}
		}
		if (pSceneTouchEvent.getAction() == MotionEvent.ACTION_MOVE) {
			if (isTouchingStarfish(touchX, touchY)) {
				//do nothing
			} else {
					stopMakingBubbles();
					setStarfishToAnimateNormal();
			}
		}
		
		return false;
	}
	
	private void teleportStarfish(float pX, float pY) {
		this.mStarfishBody.setTransform(pX / PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT, pY / PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT, 0);
	}

	private void moveStarfishTowardsTouch(float touchX, float touchY) {
		float diffX = touchX - (mStarfishSprite.getX() + (mStarfishSprite.getWidth() / 2));
		float diffY = touchY - (mStarfishSprite.getY() + (mStarfishSprite.getHeight() / 2));
		
		float ratioX = (diffX / Math.abs(diffX)) * Math.min(1, Math.abs(diffX/diffY));
		float ratioY = (diffY / Math.abs(diffY)) * Math.min(1, Math.abs(diffY/diffX));
		
		mStarfishBody.setLinearVelocity(new Vector2(ratioX * STARFISH_VELOCITY / PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT, 0));		
	}

	private void stopMakingBubbles() {
		mBubblesParticleSystem.setParticlesSpawnEnabled(false);
	}
	
	private void startMakingBubbles() {
		mBubblesParticleSystem.setParticlesSpawnEnabled(true);
		
	}

	private void stopMovingStarfish() {
		mStarfishBody.setLinearVelocity(0, 0);
	}

	private boolean isTouchingStarfish(float touchX, float touchY) {
		if (touchX > mStarfishSprite.getX() 
				&& touchX < (mStarfishSprite.getX() + mStarfishSprite.getWidth())
				&& touchY > mStarfishSprite.getY()  
				&& touchY < (mStarfishSprite.getY() + mStarfishSprite.getHeight())) {
			return true;
		}
		return false;
	}
	
	private void setStarfishToAnimateNormal() {
		this.mStarfishSprite.animate(new long[] {50, 50, 50, 50, 50, 50, 
                50, 50, 50, 50, 50, 50, 
                50, 50, 50, 50, 50, 50, 
                50, 50, 50, 50}, new int[] {0, 1, 2, 3, 4, 5,
              	  6, 7, 8, 9, 10, 11, 
              	  12, 13, 14, 15, 16, 17,
              	  18, 19, 20, 21});
	}
	
	private void setStarfishToAnimateTouched() {
		this.mStarfishSprite.animate(new long[] {50, 50, 50, 50, 50, 50, 50, 50, 50}, 
				new int[] {22, 23, 24, 25, 26, 27, 28, 29, 30});
	}
	
	private void setFishToAnimateNormal(AnimatedSprite pFish) {
		pFish.animate(new long[] {50, 50, 50, 50, 50, 50, 
                50, 50, 50, 50, 50, 50, 
                50, 50, 50, 50, 50, 50}, 
                	new int[] {0, 1, 2, 3, 4, 5,
              	  6, 7, 8, 9, 10, 11, 
              	  12, 13, 14, 15, 16, 17});
	}
	
	private void setFishToAnimateDead(AnimatedSprite pFish) {
		pFish.animate(new long[] {50, 50, 50, 50, 50, 50}, 
				new int[] {18, 19, 20, 21, 22, 23});
	}
	
	public void fishCaptured (PhysicsHandler pFishHandler, PhysicsHandler pRodHandler) {
		pFishHandler.setVelocity(0, -100);
		pRodHandler.setVelocity(0, -100);
	}
}
