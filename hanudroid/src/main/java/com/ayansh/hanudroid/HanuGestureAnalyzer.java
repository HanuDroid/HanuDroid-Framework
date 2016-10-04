package com.ayansh.hanudroid;

import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

public class HanuGestureAnalyzer extends SimpleOnGestureListener {

	private HanuGestureListener caller;
	
	public HanuGestureAnalyzer(HanuGestureListener caller){
		this.caller = caller;
	}
	
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		
		if(e2.getX() - e1.getX() > 150 && Math.abs(velocityX) > 150) {
			caller.swipeRight();
			return true;
		}
		
		if(e1.getX() - e2.getX() > 150 && Math.abs(velocityX) > 150) {
			caller.swipeLeft();
			return true;
		}
		
		if(e2.getY() - e1.getY() > 150 && Math.abs(velocityY) > 150) {
			caller.swipeUp();
			return true;
		}
		
		if(e1.getY() - e2.getY() > 150 && Math.abs(velocityY) > 150) {
			caller.swipeDown();
			return true;
		}
		
		return true;
	}
	
}
