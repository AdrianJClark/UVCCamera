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

package com.serenegiant.usbcameratest7;

import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.CameraViewInterface;
import com.serenegiant.widget.UVCCameraTextureView;

import java.nio.ByteBuffer;

/**
 * Show side by side view from two camera.
 * You cane record video images from both camera, but secondarily started recording can not record
 * audio because of limitation of Android AudioRecord(only one instance of AudioRecord is available
 * on the device) now.
 */
public final class MainActivity extends BaseActivity implements CameraDialog.CameraDialogParent {
	private static final boolean DEBUG = false;	// FIXME set false when production
	private static final String TAG = "MainActivity";

	private static final float[] BANDWIDTH_FACTORS = { 0.5f, 0.5f };

    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor;

	private UVCCameraHandler mHandlerR;
	private CameraViewInterface mUVCCameraViewR;
	private Surface mRightPreviewSurface;

	private UVCCameraHandler mHandlerL;
	private CameraViewInterface mUVCCameraViewL;
	private Surface mLeftPreviewSurface;


	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		findViewById(R.id.RelativeLayout1).setOnClickListener(mOnClickListener);
		mUVCCameraViewL = (CameraViewInterface)findViewById(R.id.camera_view_L);
		mUVCCameraViewL.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float)UVCCamera.DEFAULT_PREVIEW_HEIGHT);
		((UVCCameraTextureView)mUVCCameraViewL).setOnClickListener(mOnClickListener);
		mHandlerL = UVCCameraHandler.createHandler(this, mUVCCameraViewL, UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, BANDWIDTH_FACTORS[0]);

		mUVCCameraViewR = (CameraViewInterface)findViewById(R.id.camera_view_R);
		mUVCCameraViewR.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float)UVCCamera.DEFAULT_PREVIEW_HEIGHT);
		((UVCCameraTextureView)mUVCCameraViewR).setOnClickListener(mOnClickListener);
		mHandlerR = UVCCameraHandler.createHandler(this, mUVCCameraViewR, UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, BANDWIDTH_FACTORS[1]);

		mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
	}

	@Override
	protected void onStart() {
		super.onStart();
		mUSBMonitor.register();
		if (mUVCCameraViewR != null)
			mUVCCameraViewR.onResume();
		if (mUVCCameraViewL != null)
			mUVCCameraViewL.onResume();
	}

	@Override
	protected void onStop() {
		mHandlerR.close();
		if (mUVCCameraViewR != null)
			mUVCCameraViewR.onPause();
		mHandlerL.close();
		if (mUVCCameraViewL != null)
			mUVCCameraViewL.onPause();
		mUSBMonitor.unregister();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		if (mHandlerR != null) {
			mHandlerR = null;
  		}
		if (mHandlerL != null) {
			mHandlerL = null;
  		}
		if (mUSBMonitor != null) {
			mUSBMonitor.destroy();
			mUSBMonitor = null;
		}
		mUVCCameraViewR = null;
		mUVCCameraViewL = null;
		super.onDestroy();
	}

	private final OnClickListener mOnClickListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {
			if (view.getId() == R.id.camera_view_L) {
				if (mHandlerL != null) {
					if (!mHandlerL.isOpened()) {
						CameraDialog.showDialog(MainActivity.this);
					} else {
						mHandlerL.close();
					}
				}
			} else if (view.getId() == R.id.camera_view_R) {
				if (mHandlerR != null) {
					if (!mHandlerR.isOpened()) {
						CameraDialog.showDialog(MainActivity.this);
					} else {
						mHandlerR.close();
					}
				}
			}
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
			if (!mHandlerL.isOpened()) {
				mHandlerL.open(ctrlBlock);
				mHandlerL.setFrameCallback(mIFrameCallbackL, UVCCamera.PIXEL_FORMAT_RGBX);
				final SurfaceTexture st = mUVCCameraViewL.getSurfaceTexture();
				mHandlerL.startPreview(new Surface(st));
			} else if (!mHandlerR.isOpened()) {
				mHandlerR.open(ctrlBlock);
				mHandlerR.setFrameCallback(mIFrameCallbackR, UVCCamera.PIXEL_FORMAT_RGBX);
				final SurfaceTexture st = mUVCCameraViewR.getSurfaceTexture();
				mHandlerR.startPreview(new Surface(st));
			}
		}

		@Override
		public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
			if (DEBUG) Log.v(TAG, "onDisconnect:" + device);
			if ((mHandlerL != null) && !mHandlerL.isEqual(device)) {
				queueEvent(new Runnable() {
					@Override
					public void run() {
						mHandlerL.close();
						if (mLeftPreviewSurface != null) {
							mLeftPreviewSurface.release();
							mLeftPreviewSurface = null;
						}
					}
				}, 0);
			} else if ((mHandlerR != null) && !mHandlerR.isEqual(device)) {
				queueEvent(new Runnable() {
					@Override
					public void run() {
						mHandlerR.close();
						if (mRightPreviewSurface != null) {
							mRightPreviewSurface.release();
							mRightPreviewSurface = null;
						}
					}
				}, 0);
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

}
