package com.jhlee.vbudget.collect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

/**
 * Camera preview
 * 
 * @author popopome
 * 
 */
public class RRCameraPreview extends SurfaceView implements
		SurfaceHolder.Callback {

	private static final int CAPTURE_IMAGE_WIDTH = 640;
	private static final int CAPTURE_IMAGE_HEIGHT = 480;

	private static final String TAG = "RRCameraPreview";

	/**
	 * Callback for picture is taken.
	 * 
	 * @author Administrator
	 * 
	 */
	public interface OnPictureTakenListener {
		public void pictureTaken(Bitmap bmp);
	}

	private SurfaceHolder mHolder;
	private Camera mCamera;

	/** CTOR */
	public RRCameraPreview(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initializeInternal();
	}

	public RRCameraPreview(Context context, AttributeSet attrs) {
		super(context, attrs);
		initializeInternal();
	}

	public RRCameraPreview(Context context) {
		super(context);
		initializeInternal();
	}

	/**
	 * Initialize internal variables
	 */
	private void initializeInternal() {
		/*
		 * Install a SurfaceHolder.Callback so we get notified when the
		 * underlying surface is created and destroyed
		 */
		mHolder = this.getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

	}

	/**
	 * Surface has been created, acquire the camera and tell it where to draw
	 */
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			mCamera = Camera.open();

			mCamera.setPreviewDisplay(holder);

			Camera.Parameters params = mCamera.getParameters();
			params.setPictureFormat(PixelFormat.RGB_565);
			params.setPictureSize(CAPTURE_IMAGE_WIDTH, CAPTURE_IMAGE_HEIGHT);
			mCamera.setParameters(params);
		} catch (Exception e) {
			Log.e(TAG, "Unable to prepare camera");
			if (null != mCamera) {
				mCamera.release();
				mCamera = null;
			}

			/* Show why camera cannot come up */
			Toast.makeText(this.getContext(),
					"Unable to start camera(" + e.toString() + ")",
					Toast.LENGTH_LONG).show();
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		if (mCamera == null) {
			Log.e(TAG, "Camera is not ready");
			return;
		}

		/*
		 * Now the size is known, set up the camera parameters and begin the
		 * preview
		 */
		Camera.Parameters params = mCamera.getParameters();
		params.setPreviewSize(w, h);
		mCamera.setParameters(params);
		mCamera.startPreview();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mCamera == null) {
			Log.e(TAG, "camer is not ready");
			return;
		}

		/*
		 * Surface will be destroyed when we return, so stop the preview.
		 * Because the CameraDevice object is not a shared resource, it's very
		 * important to release it when the activity is paused
		 */
		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
	}

	/**
	 * Take a picture from camera
	 */
	public void takePicture(OnPictureTakenListener listener) {
		final OnPictureTakenListener listenerFinal = listener;
		Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
			/* We expect data is jpeg byte stream. */
			public void onPictureTaken(byte[] data, Camera camera) {
				/*
				 * In order to get more free-spaced memory.
				 */
				System.gc();
				
				Bitmap bmp = BitmapFactory
						.decodeByteArray(data, 0, data.length);
				int w = bmp.getWidth();
				int h = bmp.getHeight();
				boolean isTooBig = (w > CAPTURE_IMAGE_WIDTH && h > CAPTURE_IMAGE_HEIGHT);
				if (isTooBig) {
					/*
					 * Resize image to fit in our expectation
					 */
					Log.v(TAG, "Bitmap is too big:w=" + Integer.toString(w)
							+ ",h=" + Integer.toString(h));
					Bitmap resized = resizeBitmapToExpectedSize(bmp);
					bmp = null;
					bmp = resized;
				}
				
				listenerFinal.pictureTaken(bmp);
				bmp = null;
				System.gc();
			}
		};

		mCamera.takePicture(null, null, pictureCallback);
	}
	
	
	/**
	 * Resize bitmap to expected size
	 */
	public Bitmap resizeBitmapToExpectedSize(Bitmap bmp) {
		Matrix m = new Matrix();
		int w = bmp.getWidth();
		int h = bmp.getHeight();
		RectF srcRect = new RectF(0, 0, w, h);
		RectF dstRect = new RectF(0, 0, CAPTURE_IMAGE_WIDTH, CAPTURE_IMAGE_HEIGHT);
		
		m.setRectToRect(srcRect, dstRect, Matrix.ScaleToFit.CENTER);
		m.mapRect(dstRect, srcRect);
		int dstW = (int) dstRect.width();
		int dstH = (int) dstRect.height();
		
		Log.v(TAG, "Resized bitmap:w<" + Integer.toString(dstW) + ">,h=<" + Integer.toString(dstH) + ">");
		
		/*
		 * Matrix only accept scale.
		 * Here to resize bitmap I do not need to involve translation factor.
		 */
		m.reset();
		m.postScale(dstW/(float)w, dstH/(float)h);
		return Bitmap.createBitmap(bmp, 0, 0, w, h, m, true);
	}

}
