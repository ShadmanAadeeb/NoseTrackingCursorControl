package edu.trial.nosetrackingcursorcontrol;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
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

@RequiresApi(api = Build.VERSION_CODES.DONUT)
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
	boolean uiAtFtontSignalReceivedByHandler =false;
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

		//************************TAKING CONTROL OF THE LOCK****************************//
		/*PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"MyApp::MyWakelockTag");
		wakeLock.acquire();*/


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


				int plusCursorX=0;
				int plusCursorY=0;

				if(xDiff>80) plusCursorX=5;
				if(xDiff>120) plusCursorX=20;
				if(xDiff>150) plusCursorX=40;

				if(yDiff>60) plusCursorY=5;
				if(yDiff>80) plusCursorY=20;
				if(yDiff>120) plusCursorY=40;

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

				//*******************COLOR CHANGE CODE STARTS**************************//
				if(fate==NO_ACTION){cursorImageView.setBackgroundColor(Color.WHITE);
					Log.d("TAGDrag","plusCursorx="+plusCursorX+",plusCursorY="+plusCursorY);
					if(normalTappingSwitch==true){

						if(cursorParams.x==deviceWidth-50 && cursorParams.y==deviceHeight-20){
							cursorImageView.setBackgroundColor(Color.LTGRAY);
							cursorImageView.setImageResource(R.drawable.back);

							//asdasdasd
						}
						else if(cursorParams.x==0 && cursorParams.y==deviceHeight-20){
							cursorImageView.setBackgroundColor(Color.LTGRAY);
							cursorImageView.setImageResource(R.drawable.recents);

							//asdasdasd
						}
						else if((cursorParams.x>=deviceWidth*1/4 && cursorParams.x<=deviceWidth*2/4) &&
								(cursorParams.y==deviceHeight-20)){//home position
							cursorImageView.setBackgroundColor(Color.LTGRAY);
							cursorImageView.setImageResource(R.drawable.home);

							//asdasdasd
						}
						else if((cursorParams.x>=deviceWidth*1/4 && cursorParams.x<=deviceWidth*2/4) &&
								(cursorParams.y==0)){//home position
							cursorImageView.setBackgroundColor(Color.LTGRAY);
							cursorImageView.setImageResource(R.drawable.ic_notifications_black_24dp);

							//asdasdasd
						}
						else if((cursorParams.y>0 && cursorParams.y<=deviceWidth*1/6) && cursorParams.x==deviceWidth-50){
							cursorImageView.setBackgroundColor(Color.LTGRAY);
							cursorImageView.setImageResource(R.drawable.ic_volume_up_black_24dp);

						}
						else if((cursorParams.y>deviceWidth*1/6 && cursorParams.y<=deviceWidth*2/6) && cursorParams.x==deviceWidth-50){
							cursorImageView.setBackgroundColor(Color.LTGRAY);
							cursorImageView.setImageResource(R.drawable.ic_volume_off_black_24dp);


						}
						else if((cursorParams.y>deviceWidth*2/6 && cursorParams.y<=deviceWidth*3/6) && cursorParams.x==deviceWidth-50){
							cursorImageView.setBackgroundColor(Color.LTGRAY);
							cursorImageView.setImageResource(R.drawable.custom_drag);
						}

						else if((cursorParams.y>0 && cursorParams.y<=deviceWidth*1/6) && cursorParams.x==0){//cursor at double tap
							cursorImageView.setBackgroundColor(Color.LTGRAY);
							cursorImageView.setImageResource(R.drawable.ic_exposure_plus_2_black_24dp);

						}
						else if((cursorParams.y>deviceWidth*1/6 && cursorParams.y<=deviceWidth*2/6) && cursorParams.x==0){//cursor at long tap
							cursorImageView.setBackgroundColor(Color.LTGRAY);
							cursorImageView.setImageResource(R.drawable.long_tap);

						}

						else if((cursorParams.y>deviceWidth*2/6 && cursorParams.y<=deviceWidth*3/6) && cursorParams.x==0){//cursor at drag
							cursorImageView.setBackgroundColor(Color.LTGRAY);
							cursorImageView.setImageResource(R.drawable.ic_center_focus_strong_black_24dp);

						}

						else if(cursorParams.x==deviceWidth-50){
							cursorImageView.setBackgroundColor(Color.MAGENTA);
							cursorImageView.setImageResource(R.drawable.swipe_right);

						}//cursor is onextreme left
						else if(cursorParams.x==0){
							cursorImageView.setBackgroundColor(Color.MAGENTA);
							cursorImageView.setImageResource(R.drawable.swipe_left);
						}else if(cursorParams.y==deviceHeight-20){
							cursorImageView.setBackgroundColor(Color.MAGENTA);
							cursorImageView.setImageResource(R.drawable.drag_down);
						}else if(cursorParams.y==0){
							cursorImageView.setBackgroundColor(Color.MAGENTA);
							cursorImageView.setImageResource(R.drawable.drag_up);

						}else{//for all other positions

							//cursorImageView.setBackgroundColor(Color.GREEN);
							cursorImageView.setImageResource(R.drawable.ic_gps_fixed_black_24dp);
						}
						if(plusCursorX==0 && plusCursorY==0){
							cursorImageView.setBackgroundColor(Color.WHITE);
							//cursorImageView.setImageResource(R.drawable.ic_gps_fixed_black_24dp);
						}else {
							cursorImageView.setBackgroundColor(Color.YELLOW);
						}

					}else if(longPressingSwitch==true){
						cursorImageView.setBackgroundColor(Color.BLACK);
						cursorImageView.setImageResource(R.drawable.ic_gps_fixed_black_24dp);

					}else if(doubleTappingSwitch==true){
						cursorImageView.setBackgroundColor(Color.BLACK);
						cursorImageView.setImageResource(R.drawable.red_cursor);
					}else if(customDraggingSwitch==true){
						cursorImageView.setBackgroundColor(Color.BLACK);
						cursorImageView.setImageResource(R.drawable.red_cursor);
					}


				}
				else if(fate==TAP){
					cursorImageView.setBackgroundColor(Color.RED);

				}
				else if(fate== UI_AT_FRONT){
					cursorImageView.setBackgroundColor(Color.BLUE);
					cursorImageView.setImageResource(R.drawable.ic_gps_fixed_black_24dp);
				}

				//*******************COLOR CHANGE CODE ENDS**************************//

				//**************************TAP RELATED CODES*********************//
				if(fate==NO_ACTION){

					firstTapReceivedByHandler=false;
				}


				if(fate==TAP){


				}



				//*********************LONG TAP RELATED CODES*************************

				if(fate== UI_AT_FRONT){
					uiAtFtontSignalReceivedByHandler =true;
				}
				if(uiAtFtontSignalReceivedByHandler ==true){

					if(optionScreenSetUp==false){
						optionScreenSetUp=true;
						myWindowManager.removeView(cursorFrameLayout);
						//myWindowManager.addView(drawingFrameLayout,drawingLayoutParams);
						myWindowManager.addView(uiFrameLayout,uiLayoutParams);
						myWindowManager.addView(drawingFrameLayout,drawingLayoutParams);

						myWindowManager.addView(cursorFrameLayout,cursorParams);
						longTapSwitch=true;

						closeButtonSignal=false;//LAST NIGHT,I ADDED IT


					}else if(optionScreenSetUp==true){
						Log.d("TAG3","option screen flag is set true");
						if(closeButtonSignal==false){
							/*Log.d("TAGX","Close button still hasnt given me signal");*/
						}else if(closeButtonSignal==true){

							optionScreenSetUp=false;
							closeButtonSignal=false;
							uiAtFtontSignalReceivedByHandler =false;
							/*Log.d("TAGX","optionScreenSetUp="+optionScreenSetUp);
							Log.d("TAGX","closeButtonSignal="+closeButtonSignal);
							Log.d("TAGX","uiAtFtontSignalReceivedByHandler="+uiAtFtontSignalReceivedByHandler);*/
							myWindowManager.removeView(uiFrameLayout);
							myWindowManager.removeView(drawingFrameLayout);

						}
					}

				}




				//***************************UI SCREEN DEALER MODULE***************************//




			}
		};



	}
	//handler related more vars
	boolean optionScreenSetUp=false;
	boolean closeButtonSignal=false;
	boolean firstTapReceivedByHandler =false;
	boolean longTapSwitch=false;

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
	int UI_AT_FRONT =2;
	int NO_ACTION=0;
	int timer=0;
	boolean teethDetected=false;
	boolean timerRunning=false;
	int prevframeTime;



	int oldCursorParamsX=0,oldCursorParamsY=0;
	//*****************gesture decision variables************************//
	boolean thereIsTapPotential=false;

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


	//**********************************THE SWITCHES*************************************//
	boolean normalTappingSwitch=true;
	boolean doubleTappingSwitch=false;
	boolean longPressingSwitch=false;
	boolean customDraggingSwitch=false;
	int customDragCounter=0;
	int dragFromX,dragFromY,dragToX,dragToY;

	int whichCustomDrag=0;




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
		Rect firstRectangle=new Rect(new Point(matrixCentrePoint.x-150,matrixCentrePoint.y-120),
				new Point(matrixCentrePoint.x+150,matrixCentrePoint.y+120));
		Imgproc.rectangle(mRgba,firstRectangle.tl(),firstRectangle.br(),new Scalar(0,0,0),3);
		Rect secondRectangle=new Rect(new Point(matrixCentrePoint.x-80,matrixCentrePoint.y-60),
				new Point(matrixCentrePoint.x+80,matrixCentrePoint.y+60));
		Imgproc.rectangle(mRgba,secondRectangle.tl(),secondRectangle.br(),new Scalar(0,0,0),3);
		Rect thirdRectangle=new Rect(new Point(matrixCentrePoint.x-120,matrixCentrePoint.y-80),
				new Point(matrixCentrePoint.x+120,matrixCentrePoint.y+80));
		Imgproc.rectangle(mRgba,thirdRectangle.tl(),thirdRectangle.br(),new Scalar(0,0,0),3);

		//*************************************THIS IS THE DETECTOR AND TRACKER MODULE******************************//

		//After this module is completed,presentNosePoint will have the nose Point for calculation

		if(frameCount%10==0){
			//**********************RUNNING THE VIOLA JONES ALGORITHM************************//
			MatOfRect faces=new MatOfRect();
			//classifier detectsa face and stores the faces detected in faces object
			haarCascadeClassifierForFace.detectMultiScale(mGray, faces, 1.1, 2,
					2, new Size(200,200), new Size());
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
		//********************************THE DETECTOR AND TRACKER MODULE HAS BEEN COMPLETED***************************//





		//**************************************THE DIRECTION SENDER MODULE*******************************//

		//displacement and directions are collected in this module and are stored for the handler

		DirectionModule directionModule=new DirectionModule(matrixCentrePoint,presentNosePoint);
		int xDiff=directionModule.getXDiff();
		int yDiff=directionModule.getYDiff();
		int xDir=directionModule.getXMovementDirection();
		int yDir=directionModule.getYMovementDirection();
		//**************************************THE DIRECTION MODULE HAS BEEN COMPLETED***********************//



		//*************************************THE TEETH MODULE STARTS**********************************************//

		//This module sets of the boolean value teethDetected

		MatOfRect teeth=new MatOfRect();
		haarCascadeClassifierForTeeth.detectMultiScale(mGray, teeth, 1.1, 3,
				2, new Size(1,1), new Size(20,20));
		Rect[]teethArray = teeth.toArray();
		if(teeth.toArray().length>0){
			Imgproc.rectangle(mRgba,teethArray[0].tl(),teethArray[0].br(),new Scalar(0,0,255),3);
			teethDetected=true;

		}else{
			teethDetected=false;

		}
		//*************************************THE TEETH MODULE ENDS**********************************************//


		//******************************THE SCORING AND TIMING MODULE STARTS********************//


		//This part sets up the score
		if(teethDetected){
			score=50;
		}else{
			score--;
			if(score<0)score=0;
		}

		//This part deals with the timing and action
		fate=NO_ACTION;
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
					/*timerRunning=false;
					timer=0;
					score=0;*/

				}
				//code to be removed
				/*if(timer>5000){
					fate=UI_AT_FRONT;
				}*/
			}
		}else{
			timerRunning=false;
			timer=0;
		}
		//******************************THE SCORING AND TIMING MODULE ENDS********************//

		//******************************TAP POTENTIAL FLAG CONTROLLER**************************//
		if(fate==NO_ACTION){
			//Log.d("TAG1","I am gonna do nothing");
			//cursorImageView.setBackgroundColor(Color.WHITE);
		} else if(fate==TAP){
			//cursorImageView.setBackgroundColor(Color.BLUE);
			thereIsTapPotential=true;
			//score=0;
			Log.d("TAG1","I am gonna TAP");
		}
		//code to be removed
		/*else if(fate==UI_AT_FRONT){
			//cursorImageView.setBackgroundColor(Color.RED);
			thereIsTapPotential=false;
			timer=0;
			score=1;
			Log.d("TAG1","I am gonna LONG TAP");

		}*/


		//**********************IF A TAP IS EXECUTED AS PER LOGIC***********************//




		//********************************CODE FOR ALL SORTS OF TAPPING STARTS*********************************************//
		if(thereIsTapPotential==true && score==0){
			//Log.d("TAG3","A Tap is executed");

			//****************************CODE FOR NORMAL MODE TAPPING STARTS****************************//
			if(normalTappingSwitch==true){
				if(cursorParams.x==deviceWidth-50 && cursorParams.y==deviceHeight-20){//cursor is at extreme right and bottom
					performGlobalAction(GLOBAL_ACTION_BACK);
					score=0;
					thereIsTapPotential=false;
					timer=0;


				}
				else if(cursorParams.x==0 && cursorParams.y==deviceHeight-20){//cursor is at extreme left and bottom
					performGlobalAction(GLOBAL_ACTION_RECENTS);
					score=0;
					thereIsTapPotential=false;
					timer=0;


				}
				else if(cursorParams.x>=deviceWidth*1/4 && cursorParams.x<=deviceWidth*2/4 && (cursorParams.y==deviceHeight-20)){//cursor is at home position
					performGlobalAction(GLOBAL_ACTION_HOME);
					score=0;
					thereIsTapPotential=false;
					timer=0;


				}
				else if((cursorParams.x>=deviceWidth*1/4 && cursorParams.x<=deviceWidth*2/4) &&//cursor at notific position
						(cursorParams.y==0)){//home position
					performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
					score=0;
					thereIsTapPotential=false;
					timer=0;

					//asdasdasd
				}
				else if((cursorParams.y>0 && cursorParams.y<=deviceWidth*1/6) && cursorParams.x==deviceWidth-50){//cursor at vol+
					AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
					for(int i=1;i<=10;i++){
						audioManager.adjustVolume(AudioManager.ADJUST_RAISE,AudioManager.FLAG_PLAY_SOUND|AudioManager.FLAG_SHOW_UI);
					}
					score=0;
					thereIsTapPotential=false;
					timer=0;

				}
				else if((cursorParams.y>deviceWidth*1/6 && cursorParams.y<=deviceWidth*2/6) && cursorParams.x==deviceWidth-50){//cursor at vol-
					AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
					for(int i=1;i<=10;i++){
						audioManager.adjustVolume(AudioManager.ADJUST_LOWER,AudioManager.FLAG_PLAY_SOUND|AudioManager.FLAG_SHOW_UI);
					}
					score=0;
					thereIsTapPotential=false;
					timer=0;

				}

				else if((cursorParams.y>0 && cursorParams.y<=deviceWidth*1/6) && cursorParams.x==0){//cursor at double tap
					/*fate=UI_AT_FRONT;*/
					doubleTappingSwitch=true;

					normalTappingSwitch=false;
					longPressingSwitch=false;
					customDraggingSwitch=false;


					score=0;
					thereIsTapPotential=false;
					timer=0;

				}
				else if((cursorParams.y>deviceWidth*1/6 && cursorParams.y<=deviceWidth*2/6) && cursorParams.x==0){//cursor at long tap
					longPressingSwitch=true;

					normalTappingSwitch=false;
					doubleTappingSwitch=false;
					customDraggingSwitch=false;

					timer=0;
					score=0;
					thereIsTapPotential=false;
					timer=0;

				}

				else if((cursorParams.y>deviceWidth*2/6 && cursorParams.y<=deviceWidth*3/6) && cursorParams.x==0){//cursor at drag
					longPressingSwitch=false;

					normalTappingSwitch=false;
					doubleTappingSwitch=false;
					customDraggingSwitch=true;
					whichCustomDrag=1;
					timer=0;
					score=0;
					thereIsTapPotential=false;
					timer=0;

				}

				else if((cursorParams.y>deviceWidth*2/6 && cursorParams.y<=deviceWidth*3/6) && cursorParams.x==deviceWidth-50){//cursor at sharp drag
					longPressingSwitch=false;

					normalTappingSwitch=false;
					doubleTappingSwitch=false;
					customDraggingSwitch=true;
					whichCustomDrag=2;
					timer=0;
					score=0;
					thereIsTapPotential=false;
					timer=0;

				}

				else if(cursorParams.x==deviceWidth-50){
					staticDraggingGestures(SWAP_LEFT);
					score=0;
					thereIsTapPotential=false;
					timer=0;
				}//cursor is onextreme left
				else if(cursorParams.x==0){
					staticDraggingGestures(SWAP_RIGHT);
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
				}else{
					score=0;
					timer=0;
					Path swipePath = new Path();
					swipePath.moveTo(cursorParams.x+35, cursorParams.y+90);
					GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
						gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 150));
					}
					dispatchGesture(gestureBuilder.build(), null, null);

					thereIsTapPotential=false;

				}

			}
			//****************************CODE FOR NORMAL MODE TAPPING ENDS****************************//

			else if(doubleTappingSwitch==true){
				score=0;
				timer=0;
				Path swipePath = new Path();
				swipePath.moveTo(cursorParams.x+35, cursorParams.y+90);
				GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 10));
					gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 100, 150));
					Log.d("TAGY","I executed a double tap");
				}
				dispatchGesture(gestureBuilder.build(), null, null);

				thereIsTapPotential=false;

				longPressingSwitch=false;
				doubleTappingSwitch=false;
				normalTappingSwitch=true;
				customDraggingSwitch=false;

			}
			else if(longPressingSwitch==true){
				score=0;
				timer=0;
				Path swipePath = new Path();
				swipePath.moveTo(cursorParams.x+35, cursorParams.y+90);
				GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 650));
				}
				dispatchGesture(gestureBuilder.build(), null, null);

				thereIsTapPotential=false;

				longPressingSwitch=false;
				doubleTappingSwitch=false;
				normalTappingSwitch=true;
				customDraggingSwitch=false;
			}
			else if(customDraggingSwitch==true){
				timer=0;
				score=0;
				thereIsTapPotential=false;

				if(customDragCounter==0){
					customDragCounter+=1;
					dragFromX=cursorParams.x+35;
					dragFromY=cursorParams.y+90;
					Log.d("TAGY","From("+dragFromX+","+dragFromY+")");
					Log.d("TAGY","I am coming here :counter="+customDragCounter);

				}
				else if(customDragCounter==1){
					dragToX=cursorParams.x+35;
					dragToY=cursorParams.y+90;
					if(whichCustomDrag==1){
						//**********************DRAG CODE*********************//
						Path swipePath = new Path();
						swipePath.moveTo(dragFromX, dragFromY);

						Path swipePath2 = new Path();
						swipePath2.moveTo(dragFromX, dragFromY);
						swipePath2.lineTo(dragToX, dragToY);
						GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();


						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
							GestureDescription.StrokeDescription strokeDescription=new GestureDescription.StrokeDescription(swipePath, 0, 1000,true);
							final GestureDescription.StrokeDescription strokeDescription2=strokeDescription.continueStroke(swipePath2,2000,1000,false);
							//strokeDescription.continueStroke(swipePath2,2000,1000,false);
							gestureBuilder.addStroke(strokeDescription);
							dispatchGesture(gestureBuilder.build(),

									new GestureResultCallback() {
										@Override
										public void onCompleted(GestureDescription gestureDescription) {
											super.onCompleted(gestureDescription);
											dispatchGesture(new GestureDescription.Builder().addStroke(strokeDescription2).build(), null, null);

										}
									},

									null);

							//gestureBuilder.addStroke(strokeDescription.continueStroke(swipePath2,1001,1000,true));
							//gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath2,1001,2000,false));


						}

					/*gestureBuilder.addStroke(strokeDescription);
					gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath2, 0, 1000));*/


						dispatchGesture(gestureBuilder.build(), null, null);
						//********************DRAG CODE**************************//
					}else if(whichCustomDrag==2){
						Path swipePath2 = new Path();
						swipePath2.moveTo(dragFromX, dragFromY);
						swipePath2.lineTo(dragToX, dragToY);
						GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
							gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath2,0,1000,false));
						}
						dispatchGesture(gestureBuilder.build(),null,null);

					}

					Log.d("TAGY","From("+dragToX+","+dragToY+")");
					Log.d("TAGY","I just executed a custome drag");
					customDragCounter=0;
					longPressingSwitch=false;
					doubleTappingSwitch=false;
					normalTappingSwitch=true;
					customDraggingSwitch=false;

				}

			}
			//make the tap



		}
		//********************************CODE FOR ALL SORTS OF TAPPING ENDS*********************************************//


		//***************************CODE FOR LONG TAP***********************************//
		if(fate== UI_AT_FRONT){
			//Log.d("TAG3","Long tap reached");

				timer=0;

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

	Button volUpButton,volDownButton,longTapButton,customDragButton,doubleTapButton;



	void setUpTheButtons(){
		volUpButton=uiFrameLayout.findViewById(R.id.volumeUpButton);
		volUpButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
				audioManager.adjustVolume(AudioManager.ADJUST_RAISE,AudioManager.FLAG_PLAY_SOUND|AudioManager.FLAG_SHOW_UI);
				audioManager.adjustVolume(AudioManager.ADJUST_RAISE,AudioManager.FLAG_PLAY_SOUND|AudioManager.FLAG_SHOW_UI);
				audioManager.adjustVolume(AudioManager.ADJUST_RAISE,AudioManager.FLAG_PLAY_SOUND|AudioManager.FLAG_SHOW_UI);
				audioManager.adjustVolume(AudioManager.ADJUST_RAISE,AudioManager.FLAG_PLAY_SOUND|AudioManager.FLAG_SHOW_UI);
				audioManager.adjustVolume(AudioManager.ADJUST_RAISE,AudioManager.FLAG_PLAY_SOUND|AudioManager.FLAG_SHOW_UI);
			}
		});
		volDownButton=uiFrameLayout.findViewById(R.id.volumeDownButton);
		volDownButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
				audioManager.adjustVolume(AudioManager.ADJUST_LOWER,AudioManager.FLAG_PLAY_SOUND|AudioManager.FLAG_SHOW_UI);
				audioManager.adjustVolume(AudioManager.ADJUST_LOWER,AudioManager.FLAG_PLAY_SOUND|AudioManager.FLAG_SHOW_UI);
				audioManager.adjustVolume(AudioManager.ADJUST_LOWER,AudioManager.FLAG_PLAY_SOUND|AudioManager.FLAG_SHOW_UI);
				audioManager.adjustVolume(AudioManager.ADJUST_LOWER,AudioManager.FLAG_PLAY_SOUND|AudioManager.FLAG_SHOW_UI);
				audioManager.adjustVolume(AudioManager.ADJUST_LOWER,AudioManager.FLAG_PLAY_SOUND|AudioManager.FLAG_SHOW_UI);
			}
		});


	/*	customDragButton=uiFrameLayout.findViewById(R.id.customDragButton);
		customDragButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				customDraggingSwitch=true;

				doubleTappingSwitch=false;
				normalTappingSwitch=false;
				longPressingSwitch=false;


				closeButtonSignal=true;
				Log.d("TAGY","*********************I CHANGED TO CUSTOM TAP MODE"+customDraggingSwitch+"*********************");
			}
		});*/
		doubleTapButton=uiFrameLayout.findViewById(R.id.doubleTapButton);
		doubleTapButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				doubleTappingSwitch=true;

				normalTappingSwitch=false;
				longPressingSwitch=false;
				customDraggingSwitch=false;

				closeButtonSignal=true;


			}
		});


		longTapButton=uiFrameLayout.findViewById(R.id.longPressButton);
		longTapButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				longPressingSwitch=true;

				normalTappingSwitch=false;
				doubleTappingSwitch=false;
				customDraggingSwitch=false;

				closeButtonSignal=true;

			}
		});

		//SWIPE LEFT BUTTON
		homeButton =uiFrameLayout.findViewById(R.id.homeButton);
		homeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d("TAG2","home button pressed");
				//********************swipe left code here***************
				performGlobalAction(GLOBAL_ACTION_HOME);
				//************************removal code here********************
				//longTapReceived=false;
				//Log.d("TAG2","Long Tap Received ="+longTapReceived);
				/*myWindowManager.removeView(uiFrameLayout);
				myWindowManager.removeView(drawingFrameLayout);*/
				//uiAtFtontSignalReceivedByHandler=false;
				//optionScreenSetUp=false;
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
				//ongTapReceived=false;
				//Log.d("TAG2","Long Tap Received ="+longTapReceived);
				/*myWindowManager.removeView(uiFrameLayout);
				myWindowManager.removeView(drawingFrameLayout);*/
				//uiAtFtontSignalReceivedByHandler=false;
				//optionScreenSetUp=false;

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
				//longTapReceived=false;
				//Log.d("TAG2","Long Tap Received ="+longTapReceived);
				/*myWindowManager.removeView(uiFrameLayout);
				myWindowManager.removeView(drawingFrameLayout);*/
				//uiAtFtontSignalReceivedByHandler=false;
				//optionScreenSetUp=false;

			}
		});
		screenShotButton =uiFrameLayout.findViewById(R.id.screenShotButton);
		screenShotButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d("TAG2","back Button Pressed");
				//********************swipe right code here***************
				closeButtonSignal=true;

				performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
				//************************removal code here********************
				//longTapReceived=false;
				//Log.d("TAG2","Long Tap Received ="+longTapReceived);
				/*myWindowManager.removeView(uiFrameLayout);
				myWindowManager.removeView(drawingFrameLayout);*/
				//uiAtFtontSignalReceivedByHandler=false;
				//optionScreenSetUp=false;

			}
		});
		notificationsButton =uiFrameLayout.findViewById(R.id.notificationButton);
		notificationsButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d("TAG2","back Button Pressed");
				//********************swipe right code here***************

				performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);

				//notificationsButton.setEnabled(false);
				//************************removal code here********************
				//longTapReceived=false;

			//	Log.d("TAG2","Long Tap Received ="+longTapReceived);
				/*myWindowManager.removeView(uiFrameLayout);
				myWindowManager.removeView(drawingFrameLayout);*/
				//uiAtFtontSignalReceivedByHandler=false;
				//optionScreenSetUp=false;


			}
		});
		

		//cancel button
		cancelButton=uiFrameLayout.findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				closeButtonSignal=true;


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
		faceParams.alpha= (float) 0.2 ;
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
			fromY=cursorParams.y+90;
			toX=10;
			toY=cursorParams.y+90;
			Log.d("TAGDrag","SWAP LEFT CALLED");

		}else if(gestureCode==SWAP_RIGHT){
			fromX=10;
			fromY=cursorParams.y+90;
			toX=deviceWidth-50;
			toY=cursorParams.y+90;
			Log.d("TAGDrag","SWAP RIGHT CALLED");

		}else if(gestureCode==DRAG_UP){
			fromX=cursorParams.x+35;
			fromY=deviceHeight/3;
			toX=cursorParams.x+35;
			toY=deviceHeight/2;
		}else if(gestureCode==DRAG_DOWN){
			fromX=cursorParams.x+35;
			fromY=deviceHeight-30;
			toX=cursorParams.x+35;
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
//making some serious changes