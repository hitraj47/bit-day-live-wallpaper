package com.bewareofraj.wallpaper.bitdaylivewallpaper;

import java.util.Calendar;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;

public class LiveWallpaperService extends WallpaperService {

	@Override
	public Engine onCreateEngine() {
		return new MyWallpaperEngine();
	}

	public class MyWallpaperEngine extends Engine {

		private final Handler handler = new Handler();
		private final Runnable drawRunner = new Runnable() {
			@Override
			public void run() {
				draw();
			}
		};

		/**
		 * Flag to determine if the wallpaper is visible or not.
		 */
		private boolean visible = true;

		/**
		 * A memory cache to store created/resized bitmaps.
		 */
		private LruCache<String, Bitmap> mMemoryCache;

		/**
		 * The original wallpaper images.
		 */
		private final Bitmap afternoonImage, earlyMorningImage, eveningImage,
				lateAfternoonImage, lateEveningImage, lateMorningImage,
				lateNightImage, morningImage, nightImage;

		/**
		 * The Bitmap that will store the resized image for the device's
		 * wallpaper.
		 */
		private Bitmap background;

		/**
		 * The wallpaper Canvas that will be drawn to.
		 */
		private Canvas canvas;

		/**
		 * Shared preferences file to store information that determines if a new
		 * image needs to be created and cached.
		 */
		public static final String SHARED_PREFERENCES_FILE = "bitday_preferences";

		/**
		 * The key for the last used width preference.
		 */
		public static final String PREFERENCES_WIDTH = "bitday_width";

		/**
		 * The key for the last used height preference.
		 */
		public static final String PREFERENCES_HEIGHT = "bitday_height";

		/**
		 * The key for the last used hour preference.
		 */
		public static final String PREFERENCES_HOUR = "bitday_hour";

		/**
		 * The constructor for MyWallpaperEngine. Initialized and sets the
		 * Bitmaps for the original wallpaper images. Also sets up the memory
		 * cache.
		 */
		public MyWallpaperEngine() {

			// assign the resources for each image
			afternoonImage = BitmapFactory.decodeResource(getResources(),
					R.drawable.afternoon);
			earlyMorningImage = BitmapFactory.decodeResource(getResources(),
					R.drawable.early_morning);
			eveningImage = BitmapFactory.decodeResource(getResources(),
					R.drawable.evening);
			lateAfternoonImage = BitmapFactory.decodeResource(getResources(),
					R.drawable.late_afternoon);
			lateEveningImage = BitmapFactory.decodeResource(getResources(),
					R.drawable.late_evening);
			lateMorningImage = BitmapFactory.decodeResource(getResources(),
					R.drawable.late_morning);
			lateNightImage = BitmapFactory.decodeResource(getResources(),
					R.drawable.late_night);
			morningImage = BitmapFactory.decodeResource(getResources(),
					R.drawable.morning);
			nightImage = BitmapFactory.decodeResource(getResources(),
					R.drawable.night);

			// get max memory
			final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

			// Use 1/8 of the available memory for this memory cache.
			final int cacheSize = maxMemory / 8;

			// create memory cache
			mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
				@SuppressLint("NewApi")
				@Override
				protected int sizeOf(String key, Bitmap bitmap) {
					// The cache size will be measured in kilobytes rather than
					// number of items. Also, getByteCount() only works on API
					// >= 12, if not, use bitmap.getRowBytes()
					if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) >= 12) {
						return bitmap.getByteCount() / 1024;
					} else {
						return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024;
					}

				}

			};

		}

		/**
		 * Adds a Bitmap to the memory cache.
		 * 
		 * @param key
		 *            Value that will be used to store and retrieve the Bitmap.
		 * @param bitmap
		 *            The Bitmap that will be cached.
		 */
		public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
			if (getBitmapFromMemCache(key) == null) {
				mMemoryCache.put(key, bitmap);
			}
		}

		/**
		 * Retrieve a Bitmap based on the key.
		 * 
		 * @param key
		 *            The key value that was used to initially store the Bitmap.
		 * @return The Bitmap associated with the key, or null if the Bitmap
		 *         does not exist for the specified key.
		 */
		public Bitmap getBitmapFromMemCache(String key) {
			return mMemoryCache.get(key);
		}

		public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);
		}

		/**
		 * This method is called when the visibility of the wallpaper background
		 * changes.
		 */
		@Override
		public void onVisibilityChanged(boolean _visible) {
			visible = _visible;
			// if screen wallpaper is visible then draw the image otherwise do
			// not draw
			if (visible) {
				handler.post(drawRunner);
			} else {
				handler.removeCallbacks(drawRunner);
			}
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			super.onSurfaceDestroyed(holder);
			visible = false;
			handler.removeCallbacks(drawRunner);
		}

		/**
		 * This method is called when the screen offsets are changed. e.g. when
		 * the user flips through their home screens.
		 */
		public void onOffsetsChanged(float xOffset, float yOffset, float xStep,
				float yStep, int xPixels, int yPixels) {
			draw();
		}

		/**
		 * This is the main method for the app. It will try to get the Canvas on
		 * which the wallpaper is drawn. Will then determine if a new image
		 * needs to be created or not, and then draws the appropriate image onto
		 * the canvas.
		 */
		public void draw() {

			// determine if dimensions have changed
			DisplayMetrics metrics = getResources().getDisplayMetrics();
			int currentWidth = metrics.widthPixels;
			int currentHeight = metrics.heightPixels;
			boolean dimensionsChanged = dimensionsChanged(currentWidth,
					currentHeight);

			// determine if the hour has changed
			int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
			boolean hourChanged = hourChanged(currentHeight);

			// determine if there is a cached version available
			StringBuilder bitmapKey = new StringBuilder();
			bitmapKey.append(Integer.toString(currentHour));
			bitmapKey.append(Integer.toString(currentWidth));
			bitmapKey.append(Integer.toString(currentHeight));
			boolean bitmapNotCached = bitmapNotCached(bitmapKey.toString());

			if (dimensionsChanged || hourChanged || bitmapNotCached) {

				final SurfaceHolder holder = getSurfaceHolder();

				canvas = null;

				try {
					// get the canvas object
					canvas = holder.lockCanvas();

					// clear the canvas
					canvas.drawColor(Color.BLACK);

					if (canvas != null) {
						// get a scaled image that fills the height of the
						// device
						background = createScaledImageFillHeight(
								getImageBasedOnHour(), canvas.getWidth(),
								canvas.getHeight());
						// draw the background image
						canvas.drawBitmap(background, 0, 0, null);

						addBitmapToMemoryCache(bitmapKey.toString(), background);

						background = null;
					}
				} finally {
					if (canvas != null) {
						holder.unlockCanvasAndPost(canvas);
					}
				}

			}

			handler.removeCallbacks(drawRunner);
		}

		/**
		 * Checks if a cached version of the Bitmap we want to draw exists.
		 * 
		 * @param key
		 *            The value used to store the Bitmap.
		 * @return Returns true if the Bitmap does NOT exist in the cache.
		 */
		private boolean bitmapNotCached(String key) {
			boolean bitmapNotCached = false;
			if (mMemoryCache.get(key) == null) {
				bitmapNotCached = true;
			}
			return bitmapNotCached;
		}

		/**
		 * Determines if the screen width or height has changed. If either has
		 * changed, the SharedPreferences is updated with the new values.
		 * 
		 * @param currentWidth
		 *            The current width of the screen.
		 * @param currentHeight
		 *            The current height of the screen.
		 * @return Returns true if either the width or height has changed.
		 */
		private boolean dimensionsChanged(int currentWidth, int currentHeight) {
			boolean dimensionsChanged = false;
			SharedPreferences settings = getSharedPreferences(
					SHARED_PREFERENCES_FILE, 0);
			SharedPreferences.Editor editor = settings.edit();

			int width = settings.getInt(PREFERENCES_WIDTH, 0);
			if (currentWidth != width) {
				dimensionsChanged = true;
				editor.putInt(PREFERENCES_WIDTH, currentWidth);
				editor.commit();
			}

			int height = settings.getInt(PREFERENCES_HEIGHT, 0);
			if (currentHeight != height) {
				dimensionsChanged = true;
				editor.putInt(PREFERENCES_HEIGHT, currentHeight);
				editor.commit();
			}

			return dimensionsChanged;
		}

		/**
		 * Determines if the hour has changed. If the hour is changed, the
		 * SharedPreferences is updated.
		 * 
		 * @param currentHour
		 *            The current hour.
		 * @return Returns true if the current hour is different from the one
		 *         last saved.
		 */
		private boolean hourChanged(int currentHour) {
			boolean hourChanged = false;
			SharedPreferences settings = getSharedPreferences(
					SHARED_PREFERENCES_FILE, 0);
			SharedPreferences.Editor editor = settings.edit();

			int hour = settings.getInt(PREFERENCES_HOUR, 25);
			if (currentHour != hour) {
				hourChanged = true;
				editor.putInt(PREFERENCES_HOUR, currentHour);
				editor.commit();
			}

			return hourChanged;
		}

		/**
		 * Determines which image to show based on the current hour.
		 * 
		 * @return A Bitmap of the original image.
		 */
		private Bitmap getImageBasedOnHour() {
			int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

			if (hour >= 4 && hour < 7) {
				return earlyMorningImage;
			} else if (hour >= 7 && hour < 10) {
				return morningImage;
			} else if (hour >= 10 && hour < 12) {
				return lateMorningImage;
			} else if (hour >= 12 && hour < 16) {
				return afternoonImage;
			} else if (hour >= 16 && hour < 18) {
				return lateAfternoonImage;
			} else if (hour >= 18 && hour < 20) {
				return eveningImage;
			} else if (hour >= 20 && hour < 21) {
				return lateEveningImage;
			} else if (hour >= 21 && hour < 23) {
				return nightImage;
			} else {
				return lateNightImage;
			}

		}

		/**
		 * Creates a scaled/resized image that fills the height of the
		 * screen/device.
		 * 
		 * @param originalImage
		 *            The Bitmap image to resize.
		 * @param width
		 *            The target width that the new image needs to be.
		 * @param height
		 *            The target height the new image needs to be.
		 * @return The rescaled Bitmap.
		 */
		private Bitmap createScaledImageFillHeight(Bitmap originalImage,
				int width, int height) {
			Bitmap scaledImage = Bitmap.createBitmap(width, height,
					Config.ARGB_8888);
			float originalWidth = originalImage.getWidth(), originalHeight = originalImage
					.getHeight();
			Canvas canvas = new Canvas(scaledImage);
			float scale = height / originalHeight;
			float yTranslation = 0.0f, xTranslation = (width - originalWidth
					* scale) / 2.0f;
			Matrix transformation = new Matrix();
			transformation.postTranslate(xTranslation, yTranslation);
			transformation.preScale(scale, scale);
			Paint paint = new Paint();
			paint.setFilterBitmap(true);
			canvas.drawBitmap(originalImage, transformation, paint);
			return scaledImage;
		}

	}

}
