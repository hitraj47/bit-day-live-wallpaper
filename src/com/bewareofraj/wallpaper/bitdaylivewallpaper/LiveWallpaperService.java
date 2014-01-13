package com.bewareofraj.wallpaper.bitdaylivewallpaper;

import java.util.Calendar;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
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
		private boolean visible = true;

		// Wallpaper images
		public Bitmap afternoonImage, earlyMorningImage, eveningImage,
				lateAfternoonImage, lateEveningImage, lateMorningImage,
				lateNightImage, morningImage, nightImage;
		
		public Bitmap background;

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
		}

		public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			this.visible = visible;
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
			this.visible = false;
			handler.removeCallbacks(drawRunner);
		}

		public void onOffsetsChanged(float xOffset, float yOffset, float xStep,
				float yStep, int xPixels, int yPixels) {
			draw();
		}

		public void draw() {
			final SurfaceHolder holder = getSurfaceHolder();

			Canvas canvas = null;
			try {
				canvas = holder.lockCanvas();
				// clear the canvas
				canvas.drawColor(Color.BLACK);
				if (canvas != null) {
					// get a scaled image that fills the height of the device
					background = createScaledImageFillHeight(
							getImageBasedOnHour(), canvas.getWidth(), canvas.getHeight());
					// draw the background image
					canvas.drawBitmap(background, 0, 0, null);
					background.recycle();
					background = null;
				}
			} finally {
				if (canvas != null) {
					holder.unlockCanvasAndPost(canvas);
				}
			}

			handler.removeCallbacks(drawRunner);
			if (visible) {
				handler.postDelayed(drawRunner, 10); // delay 10 milliseconds
			}
		}

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

		private Bitmap createScaledImageFillWidth(Bitmap originalImage,
				int width, int height) {
			Bitmap scaledImage = Bitmap.createBitmap((int) width, (int) height,
					Config.ARGB_8888);
			float originalWidth = originalImage.getWidth(), originalHeight = originalImage
					.getHeight();
			Canvas canvas = new Canvas(scaledImage);
			float scale = width / originalWidth;
			float xTranslation = 0.0f, yTranslation = (height - originalHeight
					* scale) / 2.0f;
			Matrix transformation = new Matrix();
			transformation.postTranslate(xTranslation, yTranslation);
			transformation.preScale(scale, scale);
			Paint paint = new Paint();
			paint.setFilterBitmap(true);
			canvas.drawBitmap(originalImage, transformation, paint);
			return scaledImage;
		}

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
