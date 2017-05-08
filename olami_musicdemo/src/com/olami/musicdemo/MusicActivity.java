package com.olami.musicdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MusicActivity extends Activity {
	private Handler mHandler;
	private Handler mInComingHandler;
	private ActivityComAssist mActivityComAssist;
	private Button mBtnStart;
	private Button mBtnStop;
	private Button mBtnCancel;
	private Button mBtnSend;
	private EditText mEditText;
	private TextView mTextView;
	private TextView mInputTextView;
	private TextView mTextViewVolume;
	private BookUtil mBookUtil = null;
	private MusicView mMusicView = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_music);
		initHandler();
		initInComingHandler();
		initCommunicationAssist();
		initView();
		Intent intent = new Intent();
		intent.setClass(MusicActivity.this, VoiceSdkService.class);
		startService(intent);
		
	}

	private void initView()
	{
		mBtnStart = (Button) findViewById(R.id.btn_start);
		mBtnStop = (Button) findViewById(R.id.btn_stop);
		mBtnCancel = (Button) findViewById(R.id.btn_cancel);
		mBtnSend = (Button) findViewById(R.id.btn_send);
		mInputTextView = (TextView) findViewById(R.id.tv_inputText);
		mEditText = (EditText) findViewById(R.id.et_content);
		mTextView = (TextView) findViewById(R.id.tv_result);
		mTextViewVolume = (TextView) findViewById(R.id.tv_volume);
				
		mBtnStart.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				sendMessageToService(MessageConst.CLIENT_ACTION_START_RECORED,0,0,null,null);
			}			
		});
		
		mBtnStop.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				sendMessageToService(MessageConst.CLIENT_ACTION_STOP_RECORED,0,0,null,null);
				mBtnStart.setText("开始");
				Log.i("led","MusicActivity mBtnStop onclick 开始");
			}			
		});
		
		mBtnCancel.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				sendMessageToService(MessageConst.CLIENT_ACTION_CANCEL_RECORED,0,0,null,null);
			}			
		});
		
		mBtnSend.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {				
				sendMessageToService(MessageConst.CLIENT_ACTION_SENT_TEXT,0,0,null,mEditText.getText());
				mInputTextView.setText("文字: "+mEditText.getText());
			}			
		});

		mMusicView = (MusicView) findViewById(R.id.music_view);
		//if(mMusicView != null)
			//mMusicView.initMusicView(MusicActivity.this,mHandler);
		
	}
	
	private void initHandler()
	{
		mHandler = new Handler(){
			@Override
			public void handleMessage(Message msg)
			{
				switch (msg.what){
				case MessageConst.CLIENT_ACTION_START_RECORED:
					//sendMessageToService(MessageConst.CLIENT_ACTION_START_RECORED,0,0,null,"Activity message coming");
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
					mBtnStart.setText("录音中");
					Log.i("led","MusicActivity 录音中");
				    break;
				case MessageConst.CLIENT_ACTION_STOP_RECORED:
					mBtnStart.setText("识别中");
					Log.i("led","MusicActivity 识别中");
					break;
				case MessageConst.CLIENT_ACTION_CANCEL_RECORED:
					mBtnStart.setText("开始");
					mTextView.setText("已取消");
					break;
				case MessageConst.CLIENT_ACTION_ON_ERROR:
					mTextView.setText("错误代码："+msg.arg1);
					mBtnStart.setText("开始");
					break;
				case MessageConst.CLIENT_ACTION_UPDATA_VOLUME:
					mTextViewVolume.setText("音量: "+msg.arg1);
					break;
				case MessageConst.SERVER_ACTION_RETURN_RESULT:
					//mTextView.setText(msg.obj.toString());
					mBtnStart.setText("开始");
					break;
				case MessageConst.CLIENT_ACTION_PLAY_BOOK_AFTER_SEARCH:
					mBtnStart.setText("开始");
					mBookUtil = BookUtil.getInstance();
					mBookUtil.play(msg.arg1);
					break;
				case MessageConst.CLIENT_ACTION_UPDATA_PLAYING_BOOK_NAME:
					mMusicView.setMusicName(msg.obj.toString());
					break;
				case MessageConst.CLIENT_ACTION_UPDATE_BOOK_PROGRESS:
					int current = msg.arg1;
					int duration = msg.arg2;
					mMusicView.setProgress(current*100/duration);
					float time = duration/1000/60;
					mMusicView.setTotalTime("总时间："+time);
					//mHandler.sendEmptyMessageDelayed(MessageConst.CLIENT_ACTION_UPDATE_BOOK_PROGRESS,1000);
					break;
				case MessageConst.CLIENT_ACTION_UPDATA_INPUT_TEXT:
					if(msg.obj != null)
					   mInputTextView.setText("文字: "+msg.obj.toString());
					break;
				case MessageConst.CLIENT_ACTION_UPDATA_SERVER_MESSAGE:
					if(msg.obj != null)
						mTextView.setText("服务器返回sentence: "+msg.obj.toString());
					break;
				}
			}
		};
	}

	private void initCommunicationAssist()
	{
		mActivityComAssist = new ActivityComAssist();
		OlamiApplication.getInstance().setServiceToActivityListener(mActivityComAssist);
	}
	
	private void sendMessageToService(int what, int arg1, int arg2, Bundle data, Object obj)
	{
		if(OlamiApplication.getInstance().getActivityToServiceListener() != null)
			OlamiApplication.getInstance().getActivityToServiceListener().callBack(what, arg1, arg2, data, obj);
	}
	
	private class ActivityComAssist implements CommunicationAssist{

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
	public void onDestroy() {
		super.onDestroy();
		Intent intent = new Intent();
		intent.setClass(MusicActivity.this, VoiceSdkService.class);
		stopService(intent);
	}
}
