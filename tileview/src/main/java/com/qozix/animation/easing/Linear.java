package com.qozix.animation.easing;

public abstract class Linear extends EasingEquation {
	public static final Linear EaseNone = new Linear(){
		
	};
	public static final Linear EaseIn = EaseNone;
	public static final Linear EaseOut = EaseNone;
}
