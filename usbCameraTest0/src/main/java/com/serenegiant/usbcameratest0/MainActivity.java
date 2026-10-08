/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.serenegiant.usbcameratest0;

import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

import java.nio.ByteBuffer;

import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
//import com.serenegiant.usbcameracommon.UVCCameraHandler;
//import com.serenegiant.widget.CameraViewInterface;
//import com.serenegiant.widget.UVCCameraTextureView;

import com.serenegiant.usb.IFrameCallback;
//import com.unity3d.player.UnityPlayerGameActivity;
//import com.serenegiant.usbcameratest0.MyCallbackListener;

/**
 * Show side by side view from two camera.
 * You cane record video images from both camera, but secondarily started recording can not record
 * audio because of limitation of Android AudioRecord(only one instance of AudioRecord is available
 * on the device) now.
 */
public final class MainActivity extends BaseActivity implements CameraDialog.CameraDialogParent {
	private static final boolean DEBUG = true;	// FIXME set false when production
	private static final String TAG = "MainActivity";

	private static final float[] BANDWIDTH_FACTORS = { 0.5f, 0.5f };

	// for accessing USB and USB camera
	private USBMonitor mUSBMonitor;

	//private UVCCameraHandler mHandlerR;
	//private CameraViewInterface mUVCCameraViewR;
	private UVCCamera mUVCCameraR;
	private SurfaceView mUVCCameraViewR;
	private ImageButton mCaptureButtonR;
	private Surface mRightPreviewSurface;
	private final Object mSyncRight = new Object();
	//MyCallbackListener frameCallbackR = null;

	//private UVCCameraHandler mHandlerL;
	//private CameraViewInterface mUVCCameraViewL;
	private UVCCamera mUVCCameraL;
	private SurfaceView mUVCCameraViewL;
	private ImageButton mCaptureButtonL;
	private Surface mLeftPreviewSurface;
	private final Object mSyncLeft = new Object();
	//MyCallbackListener frameCallbackL = null;

	private boolean settingCameraLeft = true;

	private ImageButton mCameraButtonL;
	private ImageButton mCameraButtonR;

	private Surface mPreviewSurfaceL;
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//findViewById(R.id.RelativeLayout1).setOnClickListener(mOnClickListener);
		//mUVCCameraViewL = (TextureView)findViewById(R.id.camera_surface_view); //mUnityPlayer.getView(); //= (CameraViewInterface)findViewById(R.id.camera_view_L);
		mUVCCameraViewL = (SurfaceView)findViewById(R.id.camera_surface_view);
		mUVCCameraViewL.getHolder().addCallback(mSurfaceViewCallbackL);
		//mUVCCameraViewL.getHolder().addCallback(mSurfaceViewCallbackL);
		//mUVCCameraViewL.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float)UVCCamera.DEFAULT_PREVIEW_HEIGHT);
		//((UVCCameraTextureView)mUVCCameraViewL).setOnClickListener(mOnClickListener);
		//mCaptureButtonL = (ImageButton)findViewById(R.id.capture_button_L);
		//mCaptureButtonL.setOnClickListener(mOnClickListener);
		//mCaptureButtonL.setVisibility(View.INVISIBLE);
		//mHandlerL = UVCCameraHandler.createHandler(this, mUVCCameraViewL, UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, BANDWIDTH_FACTORS[0]);

		mUVCCameraViewR = (SurfaceView)findViewById(R.id.camera_surface_view); //mUnityPlayer.getView(); //= (CameraViewInterface)findViewById(R.id.camera_view_R);
		//mUVCCameraViewR.getHolder().addCallback(mSurfaceViewCallbackR);
		//mUVCCameraViewR.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float)UVCCamera.DEFAULT_PREVIEW_HEIGHT);
		//((UVCCameraTextureView)mUVCCameraViewR).setOnClickListener(mOnClickListener);
		//mCaptureButtonR = (ImageButton)findViewById(R.id.capture_button_R);
		//mCaptureButtonR.setOnClickListener(mOnClickListener);
		//mCaptureButtonR.setVisibility(View.INVISIBLE);
		//mHandlerR = UVCCameraHandler.createHandler(this, mUVCCameraViewR, UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, BANDWIDTH_FACTORS[1]);

		mCameraButtonL = (ImageButton)findViewById(R.id.camera_buttonL);
		mCameraButtonL.setOnClickListener(mOnClickListenerL);

		mCameraButtonR = (ImageButton)findViewById(R.id.camera_buttonR);
		mCameraButtonR.setOnClickListener(mOnClickListenerR);

		mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
	}

	@Override
	protected void onStart() {
		super.onStart();
		mUSBMonitor.register();
		//if (mUVCCameraViewR != null)
		//	mUVCCameraViewR.onResume();
		//if (mUVCCameraViewL != null)
		//	mUVCCameraViewL.onResume();
	}

	@Override
	protected void onStop() {
		//mUVCCameraR.close();
		//if (mUVCCameraViewR != null)
		//	mUVCCameraViewR.onPause();
		//mUVCCameraL.close();
		//if (mUVCCameraViewL != null)
		//	mUVCCameraViewL.onPause();
		//mCaptureButtonR.setVisibility(View.INVISIBLE);
		//mCaptureButtonL.setVisibility(View.INVISIBLE);
		mUSBMonitor.unregister();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		if (mUVCCameraR != null) {
			mUVCCameraR.destroy();
			mUVCCameraR = null;
		}
		if (mUVCCameraL != null) {
			mUVCCameraL.destroy();
			mUVCCameraL = null;
		}
		if (mUSBMonitor != null) {
			mUSBMonitor.destroy();
			mUSBMonitor = null;
		}
		mUVCCameraViewR = null;
		//mCaptureButtonR = null;
		mUVCCameraViewL = null;
		//mCaptureButtonL = null;
		super.onDestroy();
	}

	/*public void registerFrameCallbackL(MyCallbackListener frameCallback) {
		frameCallbackL = frameCallback;
	}

	public void registerFrameCallbackR(MyCallbackListener frameCallback) {
		frameCallbackR = frameCallback;
	}*/


		private final OnClickListener mOnClickListenerL = new OnClickListener() {
			@Override
			public void onClick(final View view) {
            /*final var viewId = view.getId();
			if (viewId == R.id.camera_view_L) {*/
				if (mUVCCameraL == null) {
					//if (!mHandlerL.isOpened()) {
					CameraDialog.showDialog(MainActivity.this);
					settingCameraLeft = true;
				} else {
					mUVCCameraL.close();
					//setCameraButton();
					//}
				}
            /* } else if (viewId == R.id.capture_button_L) {
				if (mHandlerL != null) {
					if (mHandlerL.isOpened()) {
						if (checkPermissionWriteExternalStorage() && checkPermissionAudio()) {
							if (!mHandlerL.isRecording()) {
								mCaptureButtonL.setColorFilter(0xffff0000);	// turn red
								mHandlerL.startRecording();
							} else {
								mCaptureButtonL.setColorFilter(0);	// return to default color
								mHandlerL.stopRecording();
							}
						}
					}
				}
			*/
			}
		};

	private final OnClickListener mOnClickListenerR = new OnClickListener() {
		@Override
	public void onClick(final View view) {
		//} else if (viewId == R.id.camera_view_R) {
		if (mUVCCameraR == null) {
			//if (!mHandlerR.isOpened()) {
			CameraDialog.showDialog(MainActivity.this);
			settingCameraLeft = false;
		} else {
			mUVCCameraR.close();
			//	setCameraButton();
			//}
		}
             /* }  else if (viewId == R.id.capture_button_R) {
				if (mHandlerR != null) {
					if (mHandlerR.isOpened()) {
						if (checkPermissionWriteExternalStorage() && checkPermissionAudio()) {
							if (!mHandlerR.isRecording()) {
								mCaptureButtonR.setColorFilter(0xffff0000);	// turn red
								mHandlerR.startRecording();
							} else {
								mCaptureButtonR.setColorFilter(0);	// return to default color
								mHandlerR.stopRecording();
							}
						}
					}
				}
			}*/
	}
	};


	private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
		@Override
		public void onAttach(final UsbDevice device) {
			if (DEBUG) Log.v(TAG, "onAttach:" + device);
			Toast.makeText(MainActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
			if (DEBUG) Log.v(TAG, "onConnect:" + device);
			if (settingCameraLeft) {
				if (mUVCCameraL == null) {
					mUVCCameraL = new UVCCamera();
					mUVCCameraL.open(ctrlBlock);
					mLeftPreviewSurface = mUVCCameraViewL.getHolder().getSurface();
					mUVCCameraL.setFrameCallback(mIFrameCallbackL, UVCCamera.PIXEL_FORMAT_RGBX);
					mUVCCameraL.setPreviewDisplay(mLeftPreviewSurface);
					mUVCCameraL.startPreview();

					/*runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mCaptureButtonL.setVisibility(View.VISIBLE);
						}
					});*/
				}
			} else
			{
				if (mUVCCameraR == null) {
					mUVCCameraR = new UVCCamera();
					mUVCCameraR.open(ctrlBlock);
					//final SurfaceTexture st = mUVCCameraViewR.getSurfaceTexture();
					//mUVCCameraR.startPreview(new Surface(st));
					//mRightPreviewSurface = mUVCCameraViewR.getHolder().getSurface();
					//mUVCCameraR.setFrameCallback(mIFrameCallbackR, UVCCamera.PIXEL_FORMAT_RGBX);
					//mUVCCameraR.setPreviewDisplay(mRightPreviewSurface);
					mUVCCameraR.setPreviewSize(640,480,1);
					mUVCCameraR.startPreview();
					/*runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mCaptureButtonR.setVisibility(View.VISIBLE);
						}
					});*/
				}
			}
		}

		private final IFrameCallback mIFrameCallbackL = new IFrameCallback() {
			@Override
			public void onFrame(final ByteBuffer frame) {
				Log.d(TAG, "FrameCallbackL");
				/*if (frameCallbackL!=null) {
					frameCallbackL.onFrame(frame);
				}*/
			}
		};

		private final IFrameCallback mIFrameCallbackR = new IFrameCallback() {
			@Override
			public void onFrame(final ByteBuffer frame) {
				Log.d(TAG, "FrameCallbackR");
				/*if (frameCallbackR!=null) {
					frameCallbackR.onFrame(frame);
				}*/
			}
		};

		@Override
		public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
			if (DEBUG) Log.v(TAG, "onDisconnect:" + device);
			if ((mUVCCameraL != null) && mUVCCameraL.getDevice() == device) {
				/*queueEvent(new Runnable() {
					@Override
					public void run() {*/
				mUVCCameraL.close();
				if (mLeftPreviewSurface != null) {
					mLeftPreviewSurface.release();
					mLeftPreviewSurface = null;
				}
				//	setCameraButton();
					/*}
				}, 0);*/
			} else if ((mUVCCameraR != null) && mUVCCameraR.getDevice() == device) {
				/*queueEvent(new Runnable() {
					@Override
					public void run() {*/
				mUVCCameraR.close();
				if (mRightPreviewSurface != null) {
					mRightPreviewSurface.release();
					mRightPreviewSurface = null;
				}
				//	setCameraButton();
				/*	}
				}, 0);*/
			}
		}

		@Override
		public void onDettach(final UsbDevice device) {
			if (DEBUG) Log.v(TAG, "onDettach:" + device);
			Toast.makeText(MainActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onCancel(final UsbDevice device) {
			if (DEBUG) Log.v(TAG, "onCancel:");
		}
	};

	/**
	 * to access from CameraDialog
	 * @return
	 */
	@Override
	public USBMonitor getUSBMonitor() {
		return mUSBMonitor;
	}

	@Override
	public void onDialogResult(boolean canceled) {
		if (canceled) {
			/*runOnUiThread(new Runnable() {
				@Override
				public void run() {
					setCameraButton();
				}
			}, 0);*/
		}
	}

	/*private void setCameraButton() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if ((mHandlerL != null) && !mHandlerL.isOpened() && (mCaptureButtonL != null)) {
					mCaptureButtonL.setVisibility(View.INVISIBLE);
				}
				if ((mHandlerR != null) && !mHandlerR.isOpened() && (mCaptureButtonR != null)) {
					mCaptureButtonR.setVisibility(View.INVISIBLE);
				}
			}
		}, 0);
	}*/


	private final SurfaceHolder.Callback mSurfaceViewCallbackL = new SurfaceHolder.Callback() {
		@Override
		public void surfaceCreated(final SurfaceHolder holder) {
			if (DEBUG) Log.v(TAG, "surfaceCreated:");
		}

		@Override
		public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
			if ((width == 0) || (height == 0)) return;
			if (DEBUG) Log.v(TAG, "surfaceChanged:");
			mLeftPreviewSurface = holder.getSurface();
			//synchronized (mSync) {
			if (/*isActive && !isPreview &&*/ (mUVCCameraL != null)) {
				mUVCCameraL.setPreviewDisplay(mLeftPreviewSurface);
				mUVCCameraL.startPreview();
				//isPreview = true;
			}
			//}
		}

		@Override
		public void surfaceDestroyed(final SurfaceHolder holder) {
			if (DEBUG) Log.v(TAG, "surfaceDestroyed:");
			//synchronized (mSync) {
			if (mUVCCameraL != null) {
				mUVCCameraL.stopPreview();
			}
			//isPreview = false;
			//}
			mLeftPreviewSurface = null;
		}
	};

	private final SurfaceHolder.Callback mSurfaceViewCallbackR = new SurfaceHolder.Callback() {
		@Override
		public void surfaceCreated(final SurfaceHolder holder) {
			if (DEBUG) Log.v(TAG, "surfaceCreated:");
		}

		@Override
		public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
			if ((width == 0) || (height == 0)) return;
			if (DEBUG) Log.v(TAG, "surfaceChanged:");
			mRightPreviewSurface = holder.getSurface();
			//synchronized (mSync) {
			if (/*isActive && !isPreview &&*/ (mUVCCameraR != null)) {
				mUVCCameraR.setPreviewDisplay(mRightPreviewSurface);
				mUVCCameraR.startPreview();
				//isPreview = true;
			}
			//}
		}

		@Override
		public void surfaceDestroyed(final SurfaceHolder holder) {
			if (DEBUG) Log.v(TAG, "surfaceDestroyed:");
			//synchronized (mSync) {
			if (mUVCCameraR != null) {
				mUVCCameraR.stopPreview();
			}
			//isPreview = false;
			//}
			mRightPreviewSurface = null;
		}
	};
}
