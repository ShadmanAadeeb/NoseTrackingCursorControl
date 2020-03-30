package edu.trial.nosetrackingcursorcontrol;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.video.Video;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MyAccessibilityService extends AccessibilityService implements CameraBridgeViewBase.CvCameraViewListener2  {


	//************variables for the face_view_layout*******************//
	View faceView;
	WindowManager myWindowManager;
	WindowManager.LayoutParams faceParams;
	CameraBridgeViewBase cameraView;
	//*************cariables related to the cursor********************//
	FrameLayout cursorFrameLayout;
	ImageView cursorImageView;
	WindowManager.LayoutParams cursorParams;
	//******************variables related to device metrics*****************//
	int deviceWidth,deviceHeight;
	//******************classifier variables******************************//
	CascadeClassifier haarCascadeClassifierForFace,haarCascadeClassifierForTeeth;
	//**************************handler***********************************//
	Handler handler;

	//************handler tap vars*************************//
	int tapDelayTimer =0;
	//**********************UI SCreen variables************************//
	FrameLayout uiFrameLayout;
	WindowManager.LayoutParams uiLayoutParams;
	Button swipeLeftButton,swipeRightButton,cancelButton;
	boolean longTapReceived=false;

	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
		//*******************LOAFING THE OPENCV MODULES************************//
		if(OpenCVLoader.initDebug()){
			Log.d("TAG1","Opencv started successfully");
		}
		//*************************GETTING THE DEVICE METRICS***********************************//
		DisplayMetrics displayMetrics = new DisplayMetrics();
		myWindowManager= (WindowManager) getSystemService(WINDOW_SERVICE);
		myWindowManager.getDefaultDisplay().getMetrics(displayMetrics);
		deviceHeight = displayMetrics.heightPixels;
		deviceWidth = displayMetrics.widthPixels;



		//*******************************SETTING UP THE GUI ON THE SCREEN*************************//
		uiFrameLayout=new FrameLayout(this);
		uiLayoutParams=new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat .TRANSLUCENT
		);
		uiLayoutParams.gravity=Gravity.CENTER;
		LayoutInflater inflater=LayoutInflater.from(this);

		inflater.inflate(R.layout.ui_layout,uiFrameLayout);
		myWindowManager.addView(uiFrameLayout,uiLayoutParams);



		//***************************SETTING UP THE BUTTONS**********************//
		//SWIPE LEFT BUTTON
		swipeLeftButton=uiFrameLayout.findViewById(R.id.swipeLeftButton);
		swipeLeftButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d("TAG2","SWIPE LEFT");
				//********************swipe left code here***************
				Path swipePath = new Path();
				swipePath.moveTo(700, 500);
				swipePath.lineTo(10, 500);
				GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
				gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 100));
				dispatchGesture(gestureBuilder.build(), null, null);
				//************************removal code here********************

				longTapReceived=false;
				Log.d("TAG2","Long Tap Received ="+longTapReceived);
				myWindowManager.removeView(uiFrameLayout);
			}
		});
		//SWIPE RIGHT BUTTON
		swipeRightButton=uiFrameLayout.findViewById(R.id.swipeRightButton);
		swipeRightButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d("TAG2","SWIPE RIGHT");
				//********************swipe right code here***************
				Path swipePath = new Path();
				swipePath.moveTo(10, 500);
				swipePath.lineTo(700, 500);
				GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
				gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 100));
				dispatchGesture(gestureBuilder.build(), null, null);
				//************************removal code here********************
				longTapReceived=false;
				Log.d("TAG2","Long Tap Received ="+longTapReceived);
				myWindowManager.removeView(uiFrameLayout);
			}
		});
		//cancel button
		cancelButton=uiFrameLayout.findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				myWindowManager.removeView(uiFrameLayout);
				longTapReceived=false;
			}
		});





		//************************SETTING UP THE FACEVIEWLAYOUT***************************************//

		faceView= LayoutInflater.from(this).inflate(R.layout.face_view_layout,null);

		faceParams= new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
				WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT
		);
		faceParams.gravity= Gravity.TOP;
		faceParams.x=0;
		faceParams.y=0;
		//faceParams.alpha= (float) ;
		myWindowManager.addView(faceView,faceParams);

		//**************************MAKING THE FACEVIEW SHOW FACE******************************//
		cameraView=faceView.findViewById(R.id.faceView);
		cameraView.setVisibility(SurfaceView.VISIBLE);
		cameraView.setCvCameraViewListener(this);
		cameraView.enableView();



		//***************************SHOWING THE CURSOR ON THE SCREEN***************************//
		cursorFrameLayout=new FrameLayout(this);
		cursorParams=new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
				PixelFormat .TRANSLUCENT
		);
		cursorParams.gravity=Gravity.TOP|Gravity.LEFT;
		inflater=LayoutInflater.from(this);

		inflater.inflate(R.layout.cursor_layout,cursorFrameLayout);
		//use cursorFramlayout instead of cursor view
		cursorImageView=cursorFrameLayout.findViewById(R.id.cursorImageView);
		myWindowManager.addView(cursorFrameLayout,cursorParams);




		//***************************************THE HANDLER MODULE*******************************************//
		handler=new Handler(Looper.getMainLooper()){
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				Bundle bundle=msg.getData();
				int []messageArray=bundle.getIntArray("vals");
				int xDiff=messageArray[0];
				int yDiff=messageArray[1];
				int xDir=messageArray[2];
				int yDir=messageArray[3];
				int fate=messageArray[4];
			/*	Log.d("TAG1","*********************************");
				Log.d("TAG1","xDiff="+xDiff);
				Log.d("TAG1","yDiff="+yDiff);
				Log.d("TAG1","xDir="+xDir);
				Log.d("TAG1","yDir="+yDir);
				Log.d("TAG1","fate="+fate);
				Log.d("TAG1","*********************************");*/

				int plusCursorX=0;
				int plusCursorY=0;

				if(xDiff>40) plusCursorX=5;
				if(xDiff>60) plusCursorX=20;
				if(xDiff>90) plusCursorX=40;

				if(yDiff>30) plusCursorY=5;
				if(yDiff>50) plusCursorY=20;
				if(yDiff>80) plusCursorY=40;

				plusCursorX=xDir*plusCursorX;
				plusCursorY=yDir*plusCursorY;

				//**********************FATE STILL NOT DECIDED**********************//
				cursorParams.x+=plusCursorX;

				if(cursorParams.x>deviceWidth)cursorParams.x=deviceWidth;
				else if(cursorParams.x<0)cursorParams.x=0;

				cursorParams.y+=plusCursorY;
				if(cursorParams.y>deviceHeight)cursorParams.y=deviceHeight;
				else if(cursorParams.y<0)cursorParams.y=0;

				myWindowManager.updateViewLayout(cursorFrameLayout,cursorParams);


				//***********************code for the ui screen**********************//
				Log.d("TAG2","fate="+fate);
				Log.d("TAG2","longTapReceived="+longTapReceived);
				if(fate==LONG_TAP && longTapReceived==false){
					Log.d("TAG2","I have sensed a long tap");
					longTapReceived=true;
					myWindowManager.removeView(cursorFrameLayout);
					myWindowManager.addView(uiFrameLayout,uiLayoutParams);
					myWindowManager.addView(cursorFrameLayout,cursorParams);

				}


			}
		};



	}

	//***************************VARIABLES RELATED TO CAMERA FRAMES******************************//


	Mat mGray,prevmGray;
	Mat mRgba;
	Point screenCentrePoint;
	Point matrixCentrePoint;
	int frameCount;
	Point presentNosePoint,prevNosePoint;
	MatOfPoint2f prevFeatures,presentFeatures;
	MatOfByte status;
	MatOfFloat err;
	//*************************************Teeth related variables******************************//
	int score;
	int fate;
	int TAP=1;
	int LONG_TAP=2;
	int NO_ACTION=3;
	int timer=0;
	boolean teethDetected=false;
	boolean timerRunning=false;
	int prevframeTime;

	int oldCursorParamsX=0,oldCursorParamsY=0;

	@Override
	public void onCameraViewStarted(int width, int height) {


		mGray=new Mat();
		mRgba=new Mat();
		prevmGray=new Mat();
		screenCentrePoint=new Point(this.deviceWidth/2,this.deviceHeight/2);
		try {
			this.bringInTheCascadeFileForFaceDetection();
			this.bringInTheCascadeFileForTeethDetection();
		} catch (IOException e) {
			e.printStackTrace();
		}
		frameCount=-1;
		presentNosePoint=new Point(0,0);
		prevNosePoint=new Point(0,0);

		prevFeatures=new MatOfPoint2f();
		prevFeatures.fromArray(prevNosePoint);

		presentFeatures=new MatOfPoint2f();
		presentFeatures.fromArray(presentNosePoint);

		status=new MatOfByte();
		err=new MatOfFloat();
		//******************************teeth related variables*****************************//
		score=0;


	}

	@Override
	public void onCameraViewStopped() {

	}

	@Override
	public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

		frameCount=(frameCount+1)%101;

		//***********************SETTING UP THE PREVIEW OREINTATION***********************//
		mGray=inputFrame.gray().t();
		Core.flip(mGray,mGray,-1);
		Imgproc.resize(mGray,mGray,inputFrame.gray().size());
		mRgba=inputFrame.rgba().t();
		Core.flip(mRgba,mRgba,-1);
		Imgproc.resize(mRgba,mRgba,inputFrame.rgba().size());


		//*************************FINDING THE CENTRE OF THE MATRIX**********************//
		matrixCentrePoint=new Point(mRgba.cols()/2,mRgba.rows()/2);

		//*******************DRAWING THE THREE SQUARES FOR HELP************************//

		Rect firstRectangle=new Rect(new Point(matrixCentrePoint.x-90,matrixCentrePoint.y-80),
				new Point(matrixCentrePoint.x+90,matrixCentrePoint.y+80));
		Imgproc.rectangle(mRgba,firstRectangle.tl(),firstRectangle.br(),new Scalar(0,0,0),3);
		Rect secondRectangle=new Rect(new Point(matrixCentrePoint.x-40,matrixCentrePoint.y-30),
				new Point(matrixCentrePoint.x+40,matrixCentrePoint.y+30));
		Imgproc.rectangle(mRgba,secondRectangle.tl(),secondRectangle.br(),new Scalar(0,0,0),3);
		Rect thirdRectangle=new Rect(new Point(matrixCentrePoint.x-60,matrixCentrePoint.y-50),
				new Point(matrixCentrePoint.x+60,matrixCentrePoint.y+50));
		Imgproc.rectangle(mRgba,thirdRectangle.tl(),thirdRectangle.br(),new Scalar(0,0,0),3);

		//**********************************************************************************************//
		//**********************************************************************************************//
		//*************************************THIS IS THE TRACKER MODULE******************************//
		if(frameCount%10==0){
			//**********************RUNNING THE VIOLA JONES ALGORITHM************************//
			MatOfRect faces=new MatOfRect();
			//classifier detectsa face and stores the faces detected in faces object
			haarCascadeClassifierForFace.detectMultiScale(mGray, faces, 1.1, 2,
					2, new Size(100,100), new Size());
			//this array stores all the faces
			Rect[]facesArray = faces.toArray();
			if(facesArray.length>0) {
				Imgproc.rectangle(mRgba,facesArray[0].tl(),facesArray[0].br(),new Scalar(0,0,0),3);
				//i obtain the nose co-ordinates
				presentNosePoint=new Point(facesArray[0].tl().x/2+facesArray[0].br().x/2,
						facesArray[0].tl().y/2+facesArray[0].br().y/2
				);
				prevNosePoint=presentNosePoint;
				prevFeatures.fromArray(prevNosePoint);

				if(prevFeatures.toArray().length>0){
					Imgproc.circle(mRgba,prevFeatures.toArray()[0],10,new Scalar(255,0,0),20);
				}

			}
		}else {
			Video.calcOpticalFlowPyrLK(prevmGray,mGray,prevFeatures, presentFeatures, status, err);
			if(presentFeatures.toArray().length>0){
				Imgproc.circle(mRgba,presentFeatures.toArray()[0],10,new Scalar(255,0,0),20);
			}
			prevFeatures=presentFeatures;
			if(prevFeatures.toArray().length>0){
				presentNosePoint=presentFeatures.toArray()[0];
				prevNosePoint=presentNosePoint;
			}
		}

		//********************************THE TRACKER MODULE HAS BEEN COMPLETED***************************//
		//presentNosePoint variable will be used now for the direction sender module


		//**************************************THE DIRECTION SENDER MODULE*******************************//
		DirectionModule directionModule=new DirectionModule(matrixCentrePoint,presentNosePoint);
		int xDiff=directionModule.getXDiff();
		int yDiff=directionModule.getYDiff();
		int xDir=directionModule.getXMovementDirection();
		int yDir=directionModule.getYMovementDirection();



		//**************************************THE DIRECTION MODULE HAS BEEN COMPLETED***********************//
		//displacement and directions are ready to be sent to the handler

		//*************************************THE TEETH MODULE**********************************************//
		MatOfRect teeth=new MatOfRect();
		haarCascadeClassifierForTeeth.detectMultiScale(mGray, teeth, 1.1, 3,
				2, new Size(0.01,0.01), new Size(20,20));
		Rect[]teethArray = teeth.toArray();
		if(teeth.toArray().length>0){
			Imgproc.rectangle(mRgba,teethArray[0].tl(),teethArray[0].br(),new Scalar(0,0,255),3);
			teethDetected=true;
			score=50;
		}else{
			teethDetected=false;
			score--;
			if(score<0)score=0;
			Log.d("TAG1","score="+score);
			//Log.d("TAG1","teeth detected="+teethDetected);

		}

		fate=NO_ACTION;//initially let's assume no action

		if(score>0){

			//Log.d("TAG1","score="+score);
			if(timerRunning==false){
				timerRunning=true;
				timer=0;
				prevframeTime= (int) System.currentTimeMillis();


			}
			else if(timerRunning==true){

				timer= (int) (    timer+   Math.abs(prevframeTime-System.currentTimeMillis() )      ) ;
				prevframeTime= (int) System.currentTimeMillis();
				//Log.d("TAG1","Time = "+timer);


				if(timer>3000){
					fate=TAP;

				}
				if(timer>8000){
					fate=LONG_TAP;

				}
				Log.d("TAG1","timer="+timer+" fate="+fate);
			}


		} else if(score==0){//no teeth detected
			//Log.d("TAG1","score="+score);
			timer=0;
			Log.d("TAG1","Timer="+timer);
			//cursorImageView.setBackgroundColor(Color.WHITE);
			//Log.d("TAG1","Time = "+timer);
		}


		//*************************************THE TEETH MODULE HAS BEEN COMPLETED**********************************************//


		int[] messageArray={xDiff,yDiff,xDir,yDir,fate};

		Bundle bundle=new Bundle();
		bundle.putIntArray("vals",messageArray);

		Message message=new Message();
		message.setData(bundle);
		handler.sendMessage(message);

		//********************NOW DECISION IS BEING TAKEN AS PER FATE****************//
		if(fate==NO_ACTION){



			cursorImageView.setBackgroundColor(Color.WHITE);
			tapDelayTimer =0;

		}else if(fate==TAP){

			if(tapDelayTimer ==0){//no tap delayTimer was there
				//set the timer
				tapDelayTimer =100;

				//make the tap
				Path swipePath = new Path();
				swipePath.moveTo(cursorParams.x+35, cursorParams.y+90);
				GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 150,true));
				}
				dispatchGesture(gestureBuilder.build(), null, null);
				cursorImageView.setBackgroundColor(Color.BLUE);
				Log.d("TAG1","Timer="+tapDelayTimer+" and i just made  tap on the screen");

			}else{
				tapDelayTimer--;
				if(tapDelayTimer <0){
					tapDelayTimer =0;
					Log.d("TAG1","The timer has stopped");
				}
				//Log.d("TAG1","Timer="+tapDelayTimer+" and i am decreasing it");

			}

		}else if(fate==LONG_TAP){
			//Log.d("TAG1","Long Tap");
			cursorImageView.setBackgroundColor(Color.GREEN);





		}
		//***************************FATE RELATED CODE IS HERE***************************//


		oldCursorParamsX=cursorParams.x+35;
		oldCursorParamsY=cursorParams.y+90;
		Log.d("TAG1","Setting oldCX="+oldCursorParamsX+",Setting oldCY="+oldCursorParamsY);

		prevmGray=mGray.clone();
		return  mRgba;
	}





	void bringInTheCascadeFileForFaceDetection() throws IOException {
		InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_default);//smile er jonno cascade file ta nilam
		File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);//directory banalam jeta private
		File mCascadeFile = new File(cascadeDir,"cascade.xml");//directoryr moddhe cascade.xml file ta rakhlam
		FileOutputStream os = new FileOutputStream(mCascadeFile);
		//it is stream to write to my new cascade.xml
		//file
		byte[] buffer = new byte[4096];//ekta byte array banalam buffer naame
		int bytesRead;//this will collect a  byte of data from input stream

		while((bytesRead = is.read(buffer)) != -1)//is.read reads  from file and puts in buffer and returns koy byte porlo
		{
			os.write(buffer, 0, bytesRead);//buffer theke data niye write korche
		}
		is.close();
		os.close();
		haarCascadeClassifierForFace = new CascadeClassifier(mCascadeFile.getAbsolutePath());//ekta cascade classifier banalam using the file
		if(!haarCascadeClassifierForFace.empty()){
			Log.d("TAG1","The haar Cascde object ain't empty");
		}
	}
	void bringInTheCascadeFileForTeethDetection() throws IOException {
		InputStream is = getResources().openRawResource(R.raw.cascadefina);//smile er jonno cascade file ta nilam
		File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);//directory banalam jeta private
		File mCascadeFile = new File(cascadeDir,"cascade.xml");//directoryr moddhe cascade.xml file ta rakhlam
		FileOutputStream os = new FileOutputStream(mCascadeFile);
		//it is stream to write to my new cascade.xml
		//file
		byte[] buffer = new byte[4096];//ekta byte array banalam buffer naame
		int bytesRead;//this will collect a  byte of data from input stream

		while((bytesRead = is.read(buffer)) != -1)//is.read reads  from file and puts in buffer and returns koy byte porlo
		{
			os.write(buffer, 0, bytesRead);//buffer theke data niye write korche
		}
		is.close();
		os.close();
		haarCascadeClassifierForTeeth = new CascadeClassifier(mCascadeFile.getAbsolutePath());//ekta cascade classifier banalam using the file
		if(!haarCascadeClassifierForTeeth.empty()){
			Log.d("TAG2","The haar Cascde object for eyes ain't empty");
		}
	}


	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {

	}

	@Override
	public void onInterrupt() {

	}

}
