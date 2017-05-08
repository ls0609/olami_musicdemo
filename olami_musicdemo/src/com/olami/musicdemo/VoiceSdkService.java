package com.olami.musicdemo;

import ai.olami.aiCloudService.sdk.engin.OlamiVoiceRecognizer;
import ai.olami.aiCloudService.sdk.interfaces.IOlamiVoiceRecognizerListener;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.TelephonyManager;

import org.json.JSONArray;
import org.json.JSONObject;
public class VoiceSdkService extends Service{

	private Handler mHandler;
	private Handler mInComingHandler;
	private VoiceSdkComAssist mVoiceSdkComAssist;
	private OlamiVoiceRecognizer mOlamiVoiceRecognizer;
	private OlamiVoiceRecognizerListener mOlamiVoiceRecognizerListener;
    private BookUtil mBookUtil = null;
    private boolean mIsRecordPause = false;
	@Override
	public void onCreate() {
		initHandler();
		initInComingHandler();
		initCommunicationAssist();
		initViaVoiceRecognizerListener();
		init();
		initXmly();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void init()
    {
    	initHandler();
    	mOlamiVoiceRecognizer = new OlamiVoiceRecognizer(VoiceSdkService.this);
    	TelephonyManager telephonyManager=(TelephonyManager) this.getSystemService(this.getBaseContext().TELEPHONY_SERVICE);
    	String imei=telephonyManager.getDeviceId();
    	mOlamiVoiceRecognizer.init(imei);//set null if you do not want to notify olami server.
    	
    	mOlamiVoiceRecognizer.setListener(mOlamiVoiceRecognizerListener);
    	mOlamiVoiceRecognizer.setLocalization(OlamiVoiceRecognizer.LANGUAGE_SIMPLIFIED_CHINESE);
    	mOlamiVoiceRecognizer.setAuthorization("51a4bb56ba954655a4fc834bfdc46af1","asr","68bff251789b426896e70e888f919a6d","nli");    	
    	mOlamiVoiceRecognizer.setVADTailTimeout(2000);
    	mOlamiVoiceRecognizer.setLatitudeAndLongitude(31.155364678184498,121.34882432933009); 
    }
	
	private void initHandler()
	{
		mHandler = new Handler(){
			@Override
			public void handleMessage(Message msg)
			{
				switch (msg.what){
//				case MessageConst.MSG_USER_DATA_REFRESH:
//					/*byte[] byData = (byte[]) msg.obj;
//					int dataType = msg.arg1;
//					byte[] newData = null;
//					if (byData != null) {
//						    newData = MsgRaw.prepareRawData(
//								MsgRaw.COMPRESS_GZ, MsgConst.TS_C_PROMPT,
//								byData);
//						}
//					    viaVoiceRecognizer.sendRawMessage(newData, false);					    
//						UserPhoneDataUtil.saveData(MainActivity.this,byData, dataType);*/
//					break;
				case MessageConst.CLIENT_ACTION_START_RECORED:
					sendMessageToActivity(MessageConst.CLIENT_ACTION_START_RECORED,0,0,null,null);
					break;
				case MessageConst.CLIENT_ACTION_STOP_RECORED:
					sendMessageToActivity(MessageConst.CLIENT_ACTION_STOP_RECORED,0,0,null,null);
					break;
				case MessageConst.CLIENT_ACTION_ON_ERROR:
					sendMessageToActivity(MessageConst.CLIENT_ACTION_ON_ERROR,msg.arg1,0,null,null);
					break;

				case MessageConst.CLIENT_ACTION_PLAY_BOOK_AFTER_SEARCH:
					sendMessageToActivity(MessageConst.CLIENT_ACTION_PLAY_BOOK_AFTER_SEARCH, msg.arg1, 0, null, msg.obj);
					break;
				case MessageConst.CLIENT_ACTION_UPDATA_PLAYING_BOOK_NAME:
					sendMessageToActivity(MessageConst.CLIENT_ACTION_UPDATA_PLAYING_BOOK_NAME, msg.arg1, 0, null, msg.obj);
					break;
				case MessageConst.CLIENT_ACTION_UPDATE_BOOK_PROGRESS:
					sendMessageToActivity(MessageConst.CLIENT_ACTION_UPDATE_BOOK_PROGRESS, msg.arg1, msg.arg2, null, null);
					break;
				case MessageConst.CLIENT_ACTION_CANCEL_RECORED:
					sendMessageToActivity(MessageConst.CLIENT_ACTION_CANCEL_RECORED, msg.arg1, msg.arg2, null, null);
					break;
				}
			}
		};
	}
	
	private void initInComingHandler()
	{
		mInComingHandler = new Handler(){
			@Override
			public void handleMessage(Message msg)
			{
				switch (msg.what){
				case MessageConst.CLIENT_ACTION_START_RECORED:
					if(mOlamiVoiceRecognizer != null)
						mOlamiVoiceRecognizer.start();	
					break;
				case MessageConst.CLIENT_ACTION_STOP_RECORED:
					if(mOlamiVoiceRecognizer != null)
						mOlamiVoiceRecognizer.stop();	
					break;
				case MessageConst.CLIENT_ACTION_CANCEL_RECORED:
					if(mOlamiVoiceRecognizer != null)
						mOlamiVoiceRecognizer.cancel();	
					break;
				case MessageConst.CLIENT_ACTION_SENT_TEXT:
					if(mOlamiVoiceRecognizer != null)
						mOlamiVoiceRecognizer.sendText(msg.obj.toString());					
					break;
				}
			}
		};
	}
	
//	public void sendContactData()
//    {
//		UserPhoneDataUtil.setStringTypeInData(false);
//
//		byte[] byData;
//		byData = UserPhoneDataUtil.initSavedData(this,
//				UserPhoneDataUtil.DATA_TYPE_CONTACT);	
//	    UserPhoneDataUtil.startCollectData(this, mHandler,
//	    		MessageConst.MSG_USER_DATA_REFRESH, UserPhoneDataUtil.DATA_TYPE_CONTACT);
// 
//    }
	
	private void initViaVoiceRecognizerListener()
	{
		mOlamiVoiceRecognizerListener = new OlamiVoiceRecognizerListener();
	}
	private class OlamiVoiceRecognizerListener implements IOlamiVoiceRecognizerListener{

		@Override
		public void onError(int errCode) {
			mHandler.sendMessage(mHandler.obtainMessage(MessageConst.CLIENT_ACTION_ON_ERROR,errCode,0));

		}

		@Override
		public void onEndOfSpeech() {
			mHandler.sendEmptyMessage(MessageConst.CLIENT_ACTION_STOP_RECORED);
			if(mIsRecordPause)
			{
				mIsRecordPause = false;
				mBookUtil.resumePlay();
			}
			
		}

		@Override
		public void onBeginningOfSpeech() {
			if(mBookUtil.isPlaying())
			{
				mBookUtil.pause();
				mIsRecordPause = true;
			}
			mHandler.sendEmptyMessage(MessageConst.CLIENT_ACTION_START_RECORED);
			
		}

		@Override
		public void onResult(String result, int type) {		
			sendMessageToActivity(MessageConst.SERVER_ACTION_RETURN_RESULT,type,0,null,result);
			processServiceMessage(result);
		}

		@Override
		public void onCancel() {
			mHandler.sendEmptyMessage(MessageConst.CLIENT_ACTION_CANCEL_RECORED);

		}

		@Override
		public void onUpdateVolume(int volume) {
			//mHandler.sendMessage(mHandler.obtainMessage(MessageConst.CLIENT_ACTION_UPDATA_VOLUME,volume,0));
			sendMessageToActivity(MessageConst.CLIENT_ACTION_UPDATA_VOLUME,volume,0,null,null);

		}
		
	}
	
	private void initCommunicationAssist()
	{
		mVoiceSdkComAssist = new VoiceSdkComAssist();
		OlamiApplication.getInstance().setActivityToServiceListener(mVoiceSdkComAssist);
	}
	
	private void initXmly()
	{
		if(mBookUtil == null)
		{
			mBookUtil = BookUtil.getInstance();
			mBookUtil.init(VoiceSdkService.this);
			mBookUtil.setHandler(mHandler);
		}
	}

	private void processServiceMessage(String message)
	{
		String input = null;
		String serverMessage = null;
		try{
			JSONObject jsonObject = new JSONObject(message);
			JSONArray jArrayNli = jsonObject.optJSONObject("data").optJSONArray("nli");
			JSONObject jObj = jArrayNli.optJSONObject(0);
			JSONArray jArraySemantic = null;
			if(message.contains("semantic"))
			  jArraySemantic = jObj.getJSONArray("semantic");
			else{
				input = jsonObject.optJSONObject("data").optJSONObject("asr").optString("result");
				sendMessageToActivity(MessageConst.CLIENT_ACTION_UPDATA_INPUT_TEXT, 0, 0, null, input);
				serverMessage = jObj.optJSONObject("desc_obj").opt("result").toString();
				sendMessageToActivity(MessageConst.CLIENT_ACTION_UPDATA_SERVER_MESSAGE, 0, 0, null, serverMessage);
				return;
			}
			JSONObject jObjSemantic;
			JSONArray jArraySlots;
			JSONArray jArrayModifier;
			String type = null;
			String songName = null;
			String singer = null;
			
			
			if(jObj != null) {
				type = jObj.optString("type");
				if("musiccontrol".equals(type))
				{
					jObjSemantic = jArraySemantic.optJSONObject(0);
					input = jObjSemantic.optString("input");
					jArraySlots = jObjSemantic.optJSONArray("slots");
					jArrayModifier = jObjSemantic.optJSONArray("modifier");
					String modifier = (String)jArrayModifier.opt(0);
					if((jArrayModifier != null) && ("play".equals(modifier)))
					{
						if(jArraySlots != null)
						   for(int i=0,k=jArraySlots.length(); i<k; i++)
						   {
							   JSONObject obj = jArraySlots.getJSONObject(i);
							   String name = obj.optString("name");
							   if("singer".equals(name))
								   singer = obj.optString("value");
							   else if("songname".equals(name))
								   songName = obj.optString("value");

						   }
					}else if((modifier != null) && ("stop".equals(modifier)))
					{
						if(mBookUtil != null)
							if(mBookUtil.isPlaying())
								mBookUtil.stop();
					}else if((modifier != null) && ("pause".equals(modifier)))
					{
						if(mBookUtil != null)
							if(mBookUtil.isPlaying())
								mBookUtil.pause();
					}else if((modifier != null) && ("resume_play".equals(modifier)))
					{
						if(mBookUtil != null)
							mBookUtil.resumePlay();
					}else if((modifier != null) && ("add_volume".equals(modifier)))
					{
						if(mBookUtil != null)
							mBookUtil.addVolume();
					}else if((modifier != null) && ("del_volume".equals(modifier)))
					{
						if(mBookUtil != null)
							mBookUtil.delVolume();
					}else if((modifier != null) && ("next".equals(modifier)))
					{
						if(mBookUtil != null)
							mBookUtil.next();
					}else if((modifier != null) && ("previous".equals(modifier)))
					{
						if(mBookUtil != null)
							mBookUtil.prev();
					}else if((modifier != null) && ("play_index".equals(modifier)))
					{
						int position = 0;
						if(jArraySlots != null)
							   for(int i=0,k=jArraySlots.length(); i<k; i++)
							   {
								   JSONObject obj = jArraySlots.getJSONObject(i);
								   JSONObject jNumDetial = obj.getJSONObject("num_detail");
								   String index = jNumDetial.optString("recommend_value");
								   position = Integer.parseInt(index) - 1;
							   }
						if(mBookUtil != null)
							mBookUtil.skipTo(position);
					}
				}
			}
			if(songName != null)
			{
				if(singer != null)
				{

				}else{
					mBookUtil.searchBookAndPlay(songName,0,0);
				}
			}else if(singer != null)
			{
				mBookUtil.searchBookAndPlay(songName,0,0);
			}
			serverMessage = jObj.optJSONObject("desc_obj").opt("result").toString();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		sendMessageToActivity(MessageConst.CLIENT_ACTION_UPDATA_INPUT_TEXT, 0, 0, null, input);
		sendMessageToActivity(MessageConst.CLIENT_ACTION_UPDATA_SERVER_MESSAGE, 0, 0, null, serverMessage);

	}

	private void sendMessageToActivity(int what, int arg1, int arg2, Bundle data, Object obj)
	{
		if(OlamiApplication.getInstance().getServiceToActivityListener() != null)
			OlamiApplication.getInstance().getServiceToActivityListener().callBack(what, arg1, arg2, data, obj);
	}
	
	private class VoiceSdkComAssist implements CommunicationAssist{

		@Override
		public void callBack(int what, int arg1, int arg2, Bundle data,Object obj) {
			Message msg = Message.obtain(null, what);
			msg.arg1 = arg1;
			msg.arg2 = arg2;
			if (data != null)
				msg.setData(data);
			if (obj != null)
				msg.obj = obj;
			mInComingHandler.sendMessage(msg);
		}		
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		if(mOlamiVoiceRecognizer != null)
			mOlamiVoiceRecognizer.destroy();
		if(mBookUtil != null)
		{
			mBookUtil.destroy();
		}
			
	}
	
	
}
