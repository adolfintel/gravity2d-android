package com.dosse.gravity2d.gdxdemo;

import java.util.LinkedList;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.dosse.gravity2d.Point;

public class GdxDemo extends ApplicationAdapter {
	// simulation
	private Demo simulation;

	// game rendering stuff
	private OrthographicCamera camera;
	private SpriteBatch batch;
	private Texture circle;

	// controls stuff
	private OrthographicCamera ctrlCam;
	private int CTRLCAM_WIDTH = 1600;
	private SpriteBatch ctrlBatch;
	private boolean controlsShown = true;
	private Texture stick, sun, planet, asteroids, sunselected, planetselected,
			asteroidsselected, zoomin, zoomout, showcontrols, hidecontrols,
			reset, quit;
	private int STICK_HEIGHT = 380, STICK_WIDTH = 380, BUTTON_WIDTH = 128,
			BUTTON_HEIGHT = 128, CTRL_TOGGLE_WIDTH = 256,
			CTRL_TOGGLE_HEIGHT = 64;
	private int BRUSH_ASTEROIDS = 0, BRUSH_PLANET = 1, BRUSH_SUN = 2;
	private int selectedBrush = BRUSH_ASTEROIDS;
	private static final int MAX_TOUCHES = 16;
	private boolean[] lastTouchStatus = new boolean[MAX_TOUCHES];
	private int stickDown = -1; // the point that's currently holding down the
	// stick, or -1 if not down
	private Vector2[] dragStartPoints = new Vector2[MAX_TOUCHES],
			dragEndPoints = new Vector2[MAX_TOUCHES];

	// android stuff
	public LinkedList<String> toastQueue = new LinkedList<String>(); // queue of
																		// toast
																		// notifications
																		// (sim
																		// terminated,
																		// etc.)
	private boolean disableToasts = false, terminated = false;

	private void toast(String s) {
		if (disableToasts)
			return;
		synchronized (toastQueue) {
			toastQueue.addLast(s);
		}
	}

	public boolean isTerminated() {
		return terminated;
	}

	private void loadTextures() {
		circle = new Texture(Gdx.files.internal("point.png"), true);
		circle.setFilter(TextureFilter.MipMapLinearLinear,
				TextureFilter.MipMapLinearLinear);
		stick = new Texture(Gdx.files.internal("stick.png"), true);
		sun = new Texture(Gdx.files.internal("sun.png"), true);
		planet = new Texture(Gdx.files.internal("planet.png"), true);
		asteroids = new Texture(Gdx.files.internal("asteroids.png"), true);
		sunselected = new Texture(Gdx.files.internal("sunselected.png"), true);
		planetselected = new Texture(Gdx.files.internal("planetselected.png"),
				true);
		asteroidsselected = new Texture(
				Gdx.files.internal("asteroidsselected.png"), true);
		zoomin = new Texture(Gdx.files.internal("zoomin.png"), true);
		zoomout = new Texture(Gdx.files.internal("zoomout.png"), true);
		showcontrols = new Texture(Gdx.files.internal("showcontrols.png"), true);
		hidecontrols = new Texture(Gdx.files.internal("hidecontrols.png"), true);
		reset = new Texture(Gdx.files.internal("reset.png"), true);
		quit = new Texture(Gdx.files.internal("quit.png"), true);
	}

	@Override
	public void create() {
		camera = new OrthographicCamera();
		batch = new SpriteBatch();
		ctrlCam = new OrthographicCamera();
		ctrlBatch = new SpriteBatch();
		camera.setToOrtho(false, Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight());
		ctrlCam.setToOrtho(
				false,
				CTRLCAM_WIDTH,
				CTRLCAM_WIDTH
						* ((float) Gdx.graphics.getHeight() / (float) Gdx.graphics
								.getWidth()));
		loadTextures();
		if (simulation == null)
			resume();
		// we need all the shit below to have the back key terminate the
		// application on android. absolutely disgusting.
		Gdx.input.setInputProcessor(new InputProcessor() {

			@Override
			public boolean touchUp(int screenX, int screenY, int pointer,
					int button) {
				return false;
			}

			@Override
			public boolean touchDragged(int screenX, int screenY, int pointer) {
				return false;
			}

			@Override
			public boolean touchDown(int screenX, int screenY, int pointer,
					int button) {
				return false;
			}

			@Override
			public boolean scrolled(int amount) {
				return false;
			}

			@Override
			public boolean mouseMoved(int screenX, int screenY) {
				return false;
			}

			@Override
			public boolean keyUp(int keycode) {
				return false;
			}

			@Override
			public boolean keyTyped(char character) {
				return false;
			}

			@Override
			public boolean keyDown(int keycode) {
				if (keycode == Keys.BACK) { // if back is pressed, terminate
											// without calling pause
					dispose();
				}
				return false;
			}
		});
		Gdx.input.setCatchBackKey(true);
	}

	@Override
	public void render() {
		if (Thread.currentThread().getPriority() != Thread.MAX_PRIORITY)
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		parseInput();
		Gdx.gl.glClearColor(0.02f, 0.02f, 0.15f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		camera.update();
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		if (simulation != null) {
			for (float[] p : simulation.getPlotData()) {
				final float s = (float) (p[3] - 1) / 10;
				batch.setColor(1, 1 - (s > 1 ? 1 : s), 1 - (s > 1 ? 1 : s), 1);
				batch.draw(circle, p[0] - p[2], p[1] - p[2], 2 * p[2], 2 * p[2]);
			}
		}
		batch.end();
		ctrlCam.update();
		ctrlBatch.setProjectionMatrix(ctrlCam.combined);
		ctrlBatch.begin();
		ctrlBatch.setColor(1, 1, 1, 1);
		if (controlsShown) {
			ctrlBatch.draw(stick, 0, 0, STICK_WIDTH, STICK_WIDTH);
			ctrlBatch.draw(zoomin, STICK_WIDTH, STICK_HEIGHT - BUTTON_HEIGHT,
					BUTTON_WIDTH, BUTTON_HEIGHT);
			ctrlBatch
					.draw(zoomout, STICK_WIDTH, 0, BUTTON_WIDTH, BUTTON_HEIGHT);
			ctrlBatch.draw(selectedBrush == BRUSH_ASTEROIDS ? asteroidsselected
					: asteroids, CTRLCAM_WIDTH - BUTTON_WIDTH * 3, 0,
					BUTTON_WIDTH, BUTTON_HEIGHT);
			ctrlBatch.draw(selectedBrush == BRUSH_PLANET ? planetselected
					: planet, CTRLCAM_WIDTH - BUTTON_WIDTH * 2, 0,
					BUTTON_WIDTH, BUTTON_HEIGHT);
			ctrlBatch.draw(selectedBrush == BRUSH_SUN ? sunselected : sun,
					CTRLCAM_WIDTH - BUTTON_WIDTH, 0, BUTTON_WIDTH,
					BUTTON_HEIGHT);
			ctrlBatch.draw(reset, CTRLCAM_WIDTH / 2 - BUTTON_WIDTH,
					CTRL_TOGGLE_HEIGHT + BUTTON_HEIGHT / 2, BUTTON_WIDTH,
					BUTTON_HEIGHT);
			ctrlBatch.draw(quit, CTRLCAM_WIDTH / 2, CTRL_TOGGLE_HEIGHT
					+ BUTTON_HEIGHT / 2, BUTTON_WIDTH, BUTTON_HEIGHT);
		}
		ctrlBatch.draw(controlsShown ? hidecontrols : showcontrols,
				CTRLCAM_WIDTH / 2 - CTRL_TOGGLE_WIDTH / 2, 0,
				CTRL_TOGGLE_WIDTH, CTRL_TOGGLE_HEIGHT);
		ctrlBatch.end();
	}

	private void parseInput() {
		final Vector3 p = new Vector3(), pp = new Vector3();
		for (int n = 0; n < MAX_TOUCHES; n++) {
			try {
				if (Gdx.input.isTouched(n)) {
					final Vector3 c = ctrlCam.unproject(p.set(
							Gdx.input.getX(n), Gdx.input.getY(n), 0)); // c is
																		// coordinates
																		// in
																		// ctrlCam
					final Vector3 g = camera.unproject(pp.set(
							Gdx.input.getX(n), Gdx.input.getY(n), 0)); // g is
																		// coordinates
					// in camera (game
					// world)
					if (!lastTouchStatus[n]) { // just pressed something
						if (controlsShown && c.y >= 0 && c.y <= STICK_HEIGHT
								&& c.x >= 0 && c.x <= STICK_WIDTH) { // stick
																		// pressed
							stickDown = n;
						}
						if (stickDown != n) {
							if (controlsShown
									&& c.y >= STICK_HEIGHT - BUTTON_HEIGHT
									&& c.y <= STICK_HEIGHT
									&& c.x >= STICK_WIDTH
									&& c.x <= STICK_WIDTH + BUTTON_WIDTH) { // zoom
																			// in
																			// tapped
								camera.zoom -= camera.zoom >= 2 ? 1 : 0.1f;
								if (camera.zoom < 0.5f)
									camera.zoom = 0.5f;
								continue;
							}
							if (controlsShown && c.y >= 0
									&& c.y <= BUTTON_HEIGHT
									&& c.x >= STICK_WIDTH
									&& c.x <= STICK_WIDTH + BUTTON_WIDTH) { // zoom
																			// out
																			// tapped
								camera.zoom += camera.zoom < 1 ? 0.1f : 1;
								if (camera.zoom > 20)
									camera.zoom = 20;
								continue;
							}
							if (controlsShown && c.y >= 0
									&& c.y <= BUTTON_HEIGHT
									&& c.x >= CTRLCAM_WIDTH - BUTTON_WIDTH * 3
									&& c.x < CTRLCAM_WIDTH - BUTTON_WIDTH * 2) { // asteroids
																					// tapped
								selectedBrush = BRUSH_ASTEROIDS;
								continue;
							}
							if (controlsShown && c.y >= 0
									&& c.y <= BUTTON_HEIGHT
									&& c.x >= CTRLCAM_WIDTH - BUTTON_WIDTH * 2
									&& c.x < CTRLCAM_WIDTH - BUTTON_WIDTH) { // planet
																				// tapped
								selectedBrush = BRUSH_PLANET;
								continue;
							}
							if (controlsShown && c.y >= 0
									&& c.y <= BUTTON_HEIGHT
									&& c.x >= CTRLCAM_WIDTH - BUTTON_WIDTH
									&& c.x < CTRLCAM_WIDTH) { // sun tapped
								selectedBrush = BRUSH_SUN;
								continue;
							}
							if (controlsShown
									&& c.y >= CTRL_TOGGLE_HEIGHT
											+ BUTTON_HEIGHT / 2
									&& c.y <= CTRL_TOGGLE_HEIGHT
											+ BUTTON_HEIGHT / 2 + BUTTON_HEIGHT
									&& c.x >= CTRLCAM_WIDTH / 2 - BUTTON_WIDTH
									&& c.x < CTRLCAM_WIDTH / 2) { // reset
																	// tapped
								simulation.reset();
								camera.translate(-camera.position.x,
										-camera.position.y);
								camera.zoom = 1;
								toast("Simulation reset");
								continue;
							}
							if (controlsShown
									&& c.y >= CTRL_TOGGLE_HEIGHT
											+ BUTTON_HEIGHT / 2
									&& c.y <= CTRL_TOGGLE_HEIGHT
											+ BUTTON_HEIGHT / 2 + BUTTON_HEIGHT
									&& c.x >= CTRLCAM_WIDTH / 2
									&& c.x <= CTRLCAM_WIDTH / 2 + BUTTON_WIDTH) { // quit
																					// tapped
								dispose();
								continue;
							}
							if (c.y >= 0
									&& c.y <= CTRL_TOGGLE_HEIGHT
									&& c.x >= CTRLCAM_WIDTH / 2
											- CTRL_TOGGLE_WIDTH / 2
									&& c.x <= CTRLCAM_WIDTH / 2
											+ CTRL_TOGGLE_WIDTH / 2) { // toggle
																		// controls
								controlsShown = !controlsShown;
								continue;
							}
							// not touching the controls
							
							dragStartPoints[n] = new Vector2(g.x, g.y);
						}
					}
					if (stickDown != n) { // stick is not down
						if (selectedBrush == BRUSH_ASTEROIDS) { // dragging,
																// create
																// asteroids
							simulation.createAt((float) (g.x + Math.random()
									* 128 - 64), (float) (g.y + Math.random()
									* 128 - 64),
									0.2f * (g.x - dragStartPoints[n].x),
									0.2f * (g.y - dragStartPoints[n].y),
									(float) (0.7 + Math.random() * 2), 1);
							dragStartPoints[n] = new Vector2(g.x, g.y);
						}
						dragEndPoints[n] = new Vector2(g.x, g.y);
					}
					if (controlsShown && stickDown == n) { // stick is down,
															// move camera
						camera.translate(0.05f * (c.x - STICK_WIDTH / 2),
								0.05f * (c.y - STICK_HEIGHT / 2), 0);
					}
				} else {
					if (lastTouchStatus[n] && n == stickDown) // stopped
																// dragging
																// stick
						stickDown = -1;
					if (lastTouchStatus[n] && dragStartPoints[n] != null) { // released
																			// while
																			// dragging
																			// to
																			// create
																			// a
																			// planet or a sun
						if (selectedBrush == BRUSH_PLANET) {
							simulation
									.createAt(
											dragStartPoints[n].x,
											dragStartPoints[n].y,
											0.03f * (dragEndPoints[n].x - dragStartPoints[n].x),
											0.03f * (dragEndPoints[n].y - dragStartPoints[n].y),
											(float) (10 + Math.random() * 30),
											1);
						}
						if (selectedBrush == BRUSH_SUN) { // create sun here
							simulation
									.createAt(
											dragStartPoints[n].x,
											dragStartPoints[n].y,
											0.03f * (dragEndPoints[n].x - dragStartPoints[n].x),
											0.03f * (dragEndPoints[n].y - dragStartPoints[n].y),
											(float) (10000 + Math.random() * 15000),
											10);
						}
						dragStartPoints[n] = null;
						dragEndPoints[n] = null;
					}
				}

			} catch (Throwable t) {
				// device does not support touch point number n
			}
		}
		for (int n = 0; n < MAX_TOUCHES; n++) {
			try {
				lastTouchStatus[n] = Gdx.input.isTouched(n);
			} catch (Throwable t) {
				// device does not support so many touch points
			}
		}

	}

	private Point[] savedState = null;

	@Override
	public void pause() {
		if (simulation == null)
			return;
		toast("Simulation saved");
		savedState = simulation.saveState();
		simulation.stopASAP = true;
		simulation = null;
	}

	@Override
	public void resume() {
		if (simulation != null)
			return;
		simulation = new Demo();
		if (savedState != null) {
			toast("Simulation resumed");
			simulation.loadState(savedState);
		}
		simulation.start();
	}

	public void dispose() {
		toast("Simulation terminated");
		disableToasts = true;
		terminated = true;
		Gdx.app.exit();
	}
}
