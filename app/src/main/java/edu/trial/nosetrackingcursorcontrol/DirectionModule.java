package edu.trial.nosetrackingcursorcontrol;

import org.opencv.core.Point;

public class DirectionModule {

	Point centrePoint,presentNosePoint;
	public DirectionModule(Point centrePoint,Point presentNosePoint){
		this.centrePoint=centrePoint;
		this.presentNosePoint=presentNosePoint;
	}
	int getXDiff(){
		return (int) Math.abs(centrePoint.x-presentNosePoint.x);
	}
	int getYDiff(){
		return (int) Math.abs(centrePoint.y-presentNosePoint.y);
	}
	int getXMovementDirection(){
		if(presentNosePoint.x-centrePoint.x>40){
			//right side
			return +1;
		}else if(presentNosePoint.x-centrePoint.x<-40){
			//left side
			return -1;
		}else{
			//no movement
			return 0;
		}
	}
	int getYMovementDirection(){
		if(presentNosePoint.y-centrePoint.y<-40){
			//up
			return -1;
		}else if(presentNosePoint.y-centrePoint.y>40){
			//down
			return +1;
		}else{
			//no movement
			return 0;

		}

	}


}
