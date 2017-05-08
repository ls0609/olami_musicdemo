package com.olami.musicdemo;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

public class OlamiApplication extends Application{
	 private static OlamiApplication mOlamiApplicationSingleton;
	   private static CommunicationAssist mActivityToServiceListener;
	   private static CommunicationAssist mServiceToActivityListener;
	   public static Activity assitActivity;
	   
	   public static OlamiApplication getInstance()
	   {
		   return mOlamiApplicationSingleton;
	   }
	   
	   
	   public Context getContext(){
		   return getApplicationContext();
	   } 
	   
	   public void setActivityToServiceListener(CommunicationAssist listener)
	   {
		   mActivityToServiceListener = listener;
	   }
	   
	   public void setServiceToActivityListener(CommunicationAssist listener)
	   {
		   mServiceToActivityListener = listener;
	   }
	   
	   public  CommunicationAssist getActivityToServiceListener()
	   {
		   return mActivityToServiceListener;
	   }
	   public  CommunicationAssist getServiceToActivityListener()
	   {
		   return mServiceToActivityListener;
	   }
	   
	   @Override
		public void onCreate() {
		   super.onCreate();
		   mOlamiApplicationSingleton = this;
	    }
}
