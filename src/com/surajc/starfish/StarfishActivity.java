package com.surajc.starfish;

import java.io.IOException;
import java.io.InputStream;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.SpriteBackground;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.extension.physics.box2d.util.constants.PhysicsConstants;
import org.andengine.input.sensor.acceleration.AccelerationData;
import org.andengine.input.sensor.acceleration.IAccelerationListener;
import org.andengine.input.touch.TouchEvent;
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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;

import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.MotionEvent;

public class StarfishActivity extends SimpleBaseGameActivity implements IAccelerationListener, IOnSceneTouchListener {
	
	private static final int CAMERA_WIDTH = 800;
	private static final int CAMERA_HEIGHT = 480;
	
	private BuildableBitmapTextureAtlas mStarfishBitmapTextureAtlas;
	
	private ITextureRegion mBackgroundTextureRegion;
	private ITiledTextureRegion mStarfishTextureRegion;
	
	private AnimatedSprite mStarfishSprite;
	private Sprite mBackgroundSprite;
	
	private Scene mScene;
	private PhysicsWorld mPhysicsWorld;
	private PhysicsConnector mPhysicsConnector;
	private Body mStarfishBody;
	
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
			
			this.mStarfishBitmapTextureAtlas = new BuildableBitmapTextureAtlas(getTextureManager(), 4096, 4096, TextureOptions.BILINEAR);
			this.mStarfishTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAssetDirectory(mStarfishBitmapTextureAtlas, getAssets(), "gfx/SF_movement");
			
			try {
				mStarfishBitmapTextureAtlas.build(new BlackPawnTextureAtlasBuilder<IBitmapTextureAtlasSource, BitmapTextureAtlas>(0, 0, 0));
			} catch (TextureAtlasBuilderException e) {
				e.printStackTrace();
			}
			
			backgroundTexture.load();
			this.mEngine.getTextureManager().loadTexture(this.mStarfishBitmapTextureAtlas);
			this.mBackgroundTextureRegion = TextureRegionFactory.extractFromTexture(backgroundTexture);
		
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	protected Scene onCreateScene() {
		this.mEngine.registerUpdateHandler(new FPSLogger());
		this.mScene = new Scene();	
		
		this.mBackgroundSprite = new Sprite(0, 0, this.mBackgroundTextureRegion, getVertexBufferObjectManager());
		this.mScene.attachChild(mBackgroundSprite);
		
		this.mPhysicsWorld = new PhysicsWorld(new Vector2(0, SensorManager.GRAVITY_EARTH), false);
		
		final Rectangle ground = new Rectangle(0, CAMERA_HEIGHT - 1, CAMERA_WIDTH, 1, getVertexBufferObjectManager());
        final Rectangle roof = new Rectangle(0, 0, CAMERA_WIDTH, 1, getVertexBufferObjectManager());
        final Rectangle left = new Rectangle(0, 0, 1, CAMERA_HEIGHT, getVertexBufferObjectManager());
        final Rectangle right = new Rectangle(CAMERA_WIDTH - 1, 0, 1, CAMERA_HEIGHT, getVertexBufferObjectManager());
        
        ground.setAlpha(0.0f);
        roof.setAlpha(0.0f);
        left.setAlpha(0.0f);
        right.setAlpha(0.0f);
        
        final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0.25f, 0.0f);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, ground, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, roof, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, left, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, right, BodyType.StaticBody, wallFixtureDef);

        this.mScene.attachChild(ground);
        this.mScene.attachChild(roof);
        this.mScene.attachChild(left);
        this.mScene.attachChild(right);
        
        this.mScene.registerUpdateHandler(this.mPhysicsWorld);
        this.mScene.setOnSceneTouchListener(this);
		
        this.mStarfishSprite = new AnimatedSprite(140, 80, mStarfishTextureRegion, getVertexBufferObjectManager());
        this.mStarfishSprite.animate(50);
        
		
        final FixtureDef objectFixtureDef = PhysicsFactory.createFixtureDef(1, 0.0f, 0.0f);
        mStarfishBody = PhysicsFactory.createBoxBody(this.mPhysicsWorld, mStarfishSprite, BodyType.DynamicBody, objectFixtureDef);
        this.mScene.attachChild(mStarfishSprite);
        this.mPhysicsConnector = new PhysicsConnector(mStarfishSprite, mStarfishBody, true, false);
        this.mPhysicsWorld.registerPhysicsConnector(mPhysicsConnector);
        
        
		return this.mScene;
	}
	
	 @Override
     public void onResumeGame() {
             super.onResumeGame();
             this.enableAccelerationSensor(this);
     }
	 
	 @Override
     public void onPauseGame() {
             super.onPauseGame();
             this.disableAccelerationSensor();
     }

	@Override
	public void onAccelerationAccuracyChanged(AccelerationData pAccelerationData) {
	}

	@Override
	public void onAccelerationChanged(AccelerationData pAccelerationData) {
		final Vector2 gravity = Vector2Pool.obtain(pAccelerationData.getX(), pAccelerationData.getY());
        this.mPhysicsWorld.setGravity(gravity);
        Vector2Pool.recycle(gravity);	
	}

	@Override
	public boolean onSceneTouchEvent(Scene pScene, TouchEvent pSceneTouchEvent) {
		// When screen is touched, teleport the starfish
		if (pSceneTouchEvent.getAction() == MotionEvent.ACTION_DOWN) {
			mStarfishBody.setTransform(new Vector2(pSceneTouchEvent.getX() / PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT, pSceneTouchEvent.getY() / PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT), 0);
		}
		return false;
	}
}
