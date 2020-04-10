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

	//**********************UI SCreen variables************************//
	FrameLayout uiFrameLayout;
	WindowManager.LayoutParams uiLayoutParams;
	Button homeButton, backButton,cancelButton;
	boolean longTapReceived=false;
	boolean longTapReceivedByHandler=false;
	//********************drawing imageView Layout variables********************//
	ImageView drawingImageView;
	WindowManager.LayoutParams drawingLayoutParams;
	FrameLayout drawingFrameLayout;
	//*********************side layout related variables*************************//
	FrameLayout bottomScreenFrameLayout;
	WindowManager.LayoutParams bottomScreenLayoutParams;

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


		//**************************SETTING UP THE LAYOUTS**************************************//
		this.setUpTheLayoutsOtherThanSideLayouts();




		//***************************************THE HANDLER MODULE*******************************************//
		handler=new Handler(Looper.getMainLooper()){
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				Bundle bundle=msg.getData();

				//************************CURSOR MOVEMENT CODE STARTS**********************//
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



				cursorParams.x+=plusCursorX;

				if(cursorParams.x>deviceWidth-50)cursorParams.x=deviceWidth-50;
				else if(cursorParams.x<0)cursorParams.x=0;

				cursorParams.y+=plusCursorY;
				if(cursorParams.y>deviceHeight-20)cursorParams.y=deviceHeight-20;
				else if(cursorParams.y<0)cursorParams.y=0;

				myWindowManager.updateViewLayout(cursorFrameLayout,cursorParams);



				//************************CURSOR MOVEMENT CODE ENDS**********************//

				//*******************Color change code starts**************************//
				if(fate==NO_ACTION){cursorImageView.setBackgroundColor(Color.WHITE);
					Log.d("TAGDrag","plusCursorx="+plusCursorX+",plusCursorY="+plusCursorY);
					if(plusCursorX==0 && plusCursorY==0){

						cursorImageView.setBackgroundColor(Color.GREEN);
					}
				}
				else if(fate==TAP)cursorImageView.setBackgroundColor(Color.BLUE);
				else if(fate==LONG_TAP)	cursorImageView.setBackgroundColor(Color.RED);

				//*******************Color change code ends**************************//

				//**************************TAPN RELATED CODES*********************//
				if(fate==NO_ACTION){

					firstTapReceivedByHandler=false;
				}


				if(fate==TAP){
					if(firstTapReceivedByHandler ==false){
						firstTapReceivedByHandler =true;

						//cursor is on extreme right
						if(cursorParams.x==deviceWidth-50){
							staticDraggingGestures(SWAP_RIGHT);
							score=0;
							thereIsTapPotential=false;
							timer=0;
						}//cursor is onextreme left
						else if(cursorParams.x==0){
							staticDraggingGestures(SWAP_LEFT);
							score=0;
							thereIsTapPotential=false;
							timer=0;

						}else if(cursorParams.y==deviceHeight-20){
							staticDraggingGestures(DRAG_DOWN);
							score=0;
							thereIsTapPotential=false;
							timer=0;
						}else if(cursorParams.y==0){
							staticDraggingGestures(DRAG_UP);
							score=0;
							thereIsTapPotential=false;
							timer=0;
						}
					}



				}



				//*********************LONG TAP RELATED CODES*************************

				if(fate==LONG_TAP){
					longTapReceivedByHandler=true;
				}
				if(longTapReceivedByHandler==true){

					if(optionScreenSetUp==false){
						optionScreenSetUp=true;
						myWindowManager.removeView(cursorFrameLayout);
						//myWindowManager.addView(drawingFrameLayout,drawingLayoutParams);
						myWindowManager.addView(uiFrameLayout,uiLayoutParams);

						myWindowManager.addView(cursorFrameLayout,cursorParams);

					}else if(optionScreenSetUp==true){
						Log.d("TAG3","option screen flag is set true");
					}

				}



			}
		};



	}
	//handler related more vars
	boolean optionScreenSetUp=false;
	boolean aSideLayoutIsPresent=false;
	boolean firstTapReceivedByHandler =false;

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
	int NO_ACTION=0;
	int timer=0;
	boolean teethDetected=false;
	boolean timerRunning=false;
	int prevframeTime;



	int oldCursorParamsX=0,oldCursorParamsY=0;
	//*****************gesture decision variables************************//
	boolean thereIsTapPotential=false;
	boolean firstSignalForLongTapReceived=false;
	Point oldLongTapPoint;
	Path dragPath;

	//***************************************gesture related variables****************************//
	//this description will be dispatched
	GestureDescription gestureDescription;
	//we will build the gesture Description with these two
	GestureDescription.Builder gestureDescriptionBuilder;
	GestureDescription.StrokeDescription strokeDescription;

	//****************************************related to two tap clicks****************************//
	int dragClickCount=0;
	boolean longTapModeOn=false;

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
		timer=0;



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
		//*************************************THE TEETH MODULE**********************************************//
		MatOfRect teeth=new MatOfRect();
		haarCascadeClassifierForTeeth.detectMultiScale(mGray, teeth, 1.1, 3,
				2, new Size(0.01,0.01), new Size(20,20));
		Rect[]teethArray = teeth.toArray();
		if(teeth.toArray().length>0){
			Imgproc.rectangle(mRgba,teethArray[0].tl(),teethArray[0].br(),new Scalar(0,0,255),3);
			teethDetected=true;

		}else{
			teethDetected=false;

		}

		if(teethDetected){
			score=50;
		}else{
			score--;
			if(score<0)score=0;
		}

		fate=NO_ACTION;

		//******************************THE SCORE WALL HAS BEEN RAISED NOW DECIDING THE FLAGS********************//
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
				if(timer>1500){
					fate=TAP;
				}
				if(timer>5000){
					fate=LONG_TAP;
				}
			}
		}else{
			timerRunning=false;
			timer=0;


		}
		Log.d("TAG1","score="+score);
		Log.d("TAG1","timer="+timer);
		//******************************TAP POTENTIAL FLAG CONTROLLER**************************//
		if(fate==NO_ACTION){
			Log.d("TAG1","I am gonna do nothing");
			firstSignalForLongTapReceived=false;
			//cursorImageView.setBackgroundColor(Color.WHITE);
		} else if(fate==TAP){
			//cursorImageView.setBackgroundColor(Color.BLUE);
			thereIsTapPotential=true;
			Log.d("TAG1","I am gonna TAP");
		}else if(fate==LONG_TAP){
			//cursorImageView.setBackgroundColor(Color.RED);
			thereIsTapPotential=false;
			Log.d("TAG1","I am gonna LONG TAP");



		}
		//**********************IF A TAP IS EXECUTED AS PER LOGIC***********************//
		if(thereIsTapPotential==true && score==0){
			//Log.d("TAG3","A Tap is executed");

			//****************TAP CODE**********************//
			//make the tap
			Path swipePath = new Path();
			swipePath.moveTo(cursorParams.x+35, cursorParams.y+90);
			GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 150));
			}
			dispatchGesture(gestureBuilder.build(), null, null);
			thereIsTapPotential=false;
		}
		//***************************CODE FOR LONG TAP***********************************//
		if(fate==LONG_TAP){
			//Log.d("TAG3","Long tap reached");

		}


		//*************************HANDLER MESSAGING CODE**********************************//
		int[] messageArray={xDiff,yDiff,xDir,yDir,fate};

		Bundle bundle=new Bundle();
		bundle.putIntArray("vals",messageArray);

		Message message=new Message();
		message.setData(bundle);
		handler.sendMessage(message);



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

	Button recentsButton;
	Button notificationsButton;
	Button screenShotButton;
	Button volUpButton,volDownButton,longTapButton,customDragButton;
	void setUpTheButtons(){
		longTapButton=uiFrameLayout.findViewById(R.id.longPressButton);






		//SWIPE LEFT BUTTON
		homeButton =uiFrameLayout.findViewById(R.id.homeButton);
		homeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d("TAG2","home button pressed");
				//********************swipe left code here***************
				performGlobalAction(GLOBAL_ACTION_HOME);
				//************************removal code here********************
				longTapReceived=false;
				Log.d("TAG2","Long Tap Received ="+longTapReceived);
				myWindowManager.removeView(uiFrameLayout);
				myWindowManager.removeView(drawingFrameLayout);
				longTapReceivedByHandler=false;
				optionScreenSetUp=false;
			}
		});
		//SWIPE RIGHT BUTTON
		backButton =uiFrameLayout.findViewById(R.id.backButton);
		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d("TAG2","back Button Pressed");
				//********************swipe right code here***************
				performGlobalAction(GLOBAL_ACTION_BACK);
				//************************removal code here********************
				longTapReceived=false;
				Log.d("TAG2","Long Tap Received ="+longTapReceived);
				myWindowManager.removeView(uiFrameLayout);
				myWindowManager.removeView(drawingFrameLayout);
				longTapReceivedByHandler=false;
				optionScreenSetUp=false;

			}
		});
		recentsButton =uiFrameLayout.findViewById(R.id.recentAppsButton);
		recentsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d("TAG2","back Button Pressed");
				//********************swipe right code here***************
				performGlobalAction(GLOBAL_ACTION_RECENTS);
				//************************removal code here********************
				longTapReceived=false;
				Log.d("TAG2","Long Tap Received ="+longTapReceived);
				myWindowManager.removeView(uiFrameLayout);
				myWindowManager.removeView(drawingFrameLayout);
				longTapReceivedByHandler=false;
				optionScreenSetUp=false;

			}
		});
		screenShotButton =uiFrameLayout.findViewById(R.id.screenShotButton);
		screenShotButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d("TAG2","back Button Pressed");
				//********************swipe right code here***************
				performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
				//************************removal code here********************
				longTapReceived=false;
				Log.d("TAG2","Long Tap Received ="+longTapReceived);
				myWindowManager.removeView(uiFrameLayout);
				myWindowManager.removeView(drawingFrameLayout);
				longTapReceivedByHandler=false;
				optionScreenSetUp=false;

			}
		});
		notificationsButton =uiFrameLayout.findViewById(R.id.notificationButton);
		notificationsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d("TAG2","back Button Pressed");
				//********************swipe right code here***************
				performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
				//************************removal code here********************
				longTapReceived=false;
				Log.d("TAG2","Long Tap Received ="+longTapReceived);
				myWindowManager.removeView(uiFrameLayout);
				myWindowManager.removeView(drawingFrameLayout);
				longTapReceivedByHandler=false;
				optionScreenSetUp=false;

			}
		});
		

		//cancel button
		cancelButton=uiFrameLayout.findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				myWindowManager.removeView(uiFrameLayout);
				myWindowManager.removeView(drawingFrameLayout);
				longTapReceived=false;
				longTapReceivedByHandler=false;
				optionScreenSetUp=false;

			}
		});






	}

	void setUpTheLayoutsOtherThanSideLayouts(){

		//*******************************SETTING UP THE GUI ON THE SCREEN*************************//
		uiFrameLayout=new FrameLayout(this);
		uiLayoutParams=new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat .TRANSLUCENT
		);
		uiLayoutParams.alpha= (float) 1;

		uiLayoutParams.gravity=Gravity.CENTER;
		LayoutInflater inflater=LayoutInflater.from(this);

		inflater.inflate(R.layout.ui_layout,uiFrameLayout);
		//myWindowManager.addView(uiFrameLayout,uiLayoutParams);
		//**************************************SETTING UP THE DRAWING IMAGEVIEW LAYOUT******************************//
		drawingFrameLayout=new FrameLayout(this);
		drawingLayoutParams=new WindowManager.LayoutParams(
				WindowManager.LayoutParams.MATCH_PARENT,
				WindowManager.LayoutParams.MATCH_PARENT,
				WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
				PixelFormat .TRANSLUCENT
		);

		drawingLayoutParams.alpha= (float) 0.3;
		drawingLayoutParams.gravity=Gravity.TOP|Gravity.LEFT;
		inflater=LayoutInflater.from(this);

		inflater.inflate(R.layout.drawing_image_view_layout,drawingFrameLayout);


		//***************************SETTING UP THE BUTTONS**********************//
		this.setUpTheButtons();
		//************************SETTING UP THE FACEVIEWLAYOUT***************************************//

		faceView= LayoutInflater.from(this).inflate(R.layout.face_view_layout,null);

		faceParams= new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
				WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				,
				PixelFormat.TRANSLUCENT
		);
		faceParams.gravity= Gravity.CENTER_HORIZONTAL;
		faceParams.x=0;
		faceParams.y=0;
		faceParams.alpha= (float)0 ;
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
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
						| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				PixelFormat .TRANSLUCENT
		);
		cursorParams.gravity=Gravity.TOP|Gravity.LEFT;
		inflater=LayoutInflater.from(this);

		inflater.inflate(R.layout.cursor_layout,cursorFrameLayout);
		//use cursorFramlayout instead of cursor view
		cursorImageView=cursorFrameLayout.findViewById(R.id.cursorImageView);
		myWindowManager.addView(cursorFrameLayout,cursorParams);

	}

	//*******************************GESTURE CODES***********************************//
	int SWAP_LEFT=100;
	int SWAP_RIGHT=200;

	int DRAG_UP=300;
	int DRAG_DOWN=400;
	void staticDraggingGestures(int gestureCode){
		int fromX=0;
		int fromY=0;
		int toX=0;
		int toY=0;
		if(gestureCode==SWAP_LEFT){
			fromX=deviceWidth-50;
			fromY=deviceHeight/2;
			toX=10;
			toY=deviceHeight/2;
			Log.d("TAGDrag","SWAP LEFT CALLED");

		}else if(gestureCode==SWAP_RIGHT){
			fromX=10;
			fromY=deviceHeight/2;
			toX=deviceWidth-50;
			toY=deviceHeight/2;
			Log.d("TAGDrag","SWAP RIGHT CALLED");

		}else if(gestureCode==DRAG_UP){
			fromX=deviceWidth/2;
			fromY=deviceHeight/3;
			toX=deviceWidth/2;
			toY=deviceHeight/2;
		}else if(gestureCode==DRAG_DOWN){
			fromX=deviceWidth/2;
			fromY=deviceHeight-30;
			toX=deviceWidth/2;
			toY=deviceHeight/2;
		}
		Path swipePath = new Path();
		swipePath.moveTo(fromX, fromY);
		swipePath.lineTo(toX, toY);
		GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
		gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 1000));
		dispatchGesture(gestureBuilder.build(), null, null);


	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {

	}

	@Override
	public void onInterrupt() {

	}

}
//COMMIT NO:01
//COMMIT NO:02