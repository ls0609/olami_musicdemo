package com.olami.musicdemo;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ximalaya.ting.android.opensdk.constants.DTransferConstants;
import com.ximalaya.ting.android.opensdk.datatrasfer.CommonRequest;
import com.ximalaya.ting.android.opensdk.datatrasfer.IDataCallBack;
import com.ximalaya.ting.android.opensdk.model.PlayableModel;
import com.ximalaya.ting.android.opensdk.model.advertis.Advertis;
import com.ximalaya.ting.android.opensdk.model.advertis.AdvertisList;
import com.ximalaya.ting.android.opensdk.model.album.AlbumList;
import com.ximalaya.ting.android.opensdk.model.album.SearchAlbumList;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.model.track.TrackList;
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager;
import com.ximalaya.ting.android.opensdk.player.advertis.IXmAdsStatusListener;
import com.ximalaya.ting.android.opensdk.player.service.IXmPlayerStatusListener;
import com.ximalaya.ting.android.opensdk.player.service.XmPlayerException;

import android.app.Service;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;

public class BookUtil {
	private String mAppSecret = "4d8e605fa7ed546c4bcb33dee1381179";
	private int mNotificationId = (int) System.currentTimeMillis();
	public final int PAGE_SIZE = 5;
	private SearchAlbumList mSearchAlbumList = null;
	private XmPlayerManager mPlayerManager;
	private CommonRequest mXimalaya;
	private TrackList mTrackList = null;
	private Context mContext;
	private Handler mHandler;
	private int mPage = 1;
	private static BookUtil mBookUtilInstance;
	private int mPosition = 0;
	private int mSeekTime = 0;
	private int mTotalCount = 0;
	private String mBookName = null;
	public static BookUtil getInstance()
	{
		if(mBookUtilInstance == null)
			mBookUtilInstance = new BookUtil();
		return mBookUtilInstance;
	}
	
	public void init(Context context)
	{
		mContext = context;
		mXimalaya = CommonRequest.getInstanse();
        mXimalaya.init(mContext, mAppSecret);		
		mXimalaya.setDefaultPagesize(PAGE_SIZE);
	}
		
	public void setHandler(Handler handler)
	{
		mHandler = handler;
	}
	
	public TrackList getTrackList()
	{
		return mTrackList;
	}
	public boolean isNeedSearchBook(String bookName)
	{
		boolean ret = true;
		if((bookName != null)&&!"".equals(bookName))
		{
		  if(mTrackList != null)
			  if(mTrackList.getTotalCount() > 0)
				  if(mTrackList.getAlbumTitle().equals(bookName))
					  ret = false;
		}
		else
		{
			ret = true;
		}
		return ret;
	}
	
	public boolean isPlaying()
	{
		boolean ret = false;
		if(mPlayerManager != null)
		  if(mPlayerManager.isPlaying())
			ret = true;
		return ret;
	}
	
	public int getPage()
	{
		return mPage;
	}
	
	public int getSeekTime()
	{
		return mSeekTime;
	}
	
	public void SetSeekTime(int value)
	{
		mSeekTime = value;
	}
	
	public void setVolume(int volume)
	{
		float value = volume;
		float v = value/100;
		mPlayerManager.setVolume(v, v);
		Log.i("ppp","volume = "+volume);
	}
	
	public void searchBookAndPlay(String bookName,int position,int seekTime)
	{
		if((bookName != null)&&!"".equals(bookName))
		{
			searchBookInfo(bookName,position,true);
			mSeekTime = seekTime;
		}
		else
		{
			searchHotBook();
		}
	}
	
	public void searchBookInfo(String bookName,final int index,final boolean isNeedPlay)
	{
		mBookName = bookName;
		Map<String, String> param = new HashMap<String, String>();
		param.put(DTransferConstants.SEARCH_KEY, bookName);
		param.put(DTransferConstants.CATEGORY_ID, "" + 3);//0
		//param.put(DTransferConstants.PAGE, "" + mPageId);
		param.put(DTransferConstants.SORT, "asc");
		param.put(DTransferConstants.PAGE_SIZE, "" + PAGE_SIZE);
		mPage = (index/PAGE_SIZE)+1;
		
		mPlayerManager = XmPlayerManager.getInstance(mContext);
		mPlayerManager.init(mNotificationId, null);
		mPlayerManager.addPlayerStatusListener(mPlayerStatusListener);
		mPlayerManager.addAdsStatusListener(mAdsListener);
		
		CommonRequest.getSearchedAlbums(param, new IDataCallBack<SearchAlbumList>()
		{

			@Override
			public void onSuccess(SearchAlbumList object)   
			{					
				if (object != null && object.getAlbums() != null
						&& object.getAlbums().size() != 0)
				{
					if (mSearchAlbumList == null)
					{
						mSearchAlbumList = object;
					}
					else
					{
						mSearchAlbumList.getAlbums().addAll(object.getAlbums());
					}
					//mTrackAdapter.notifyDataSetChanged();
					
					Map<String, String> map = new HashMap<String, String>();
					
					map.put(DTransferConstants.ALBUM_ID, ""+object.getAlbums().get(0).getId());
					map.put(DTransferConstants.SORT, "asc");
					map.put(DTransferConstants.PAGE, "" + mPage);
				    map.put(DTransferConstants.PAGE_SIZE,  "" + PAGE_SIZE);

					CommonRequest.getTracks(map, new IDataCallBack<TrackList>()
					{

							@Override
							public void onSuccess(TrackList object)
							{
								mTrackList = object;
								mTotalCount = mTrackList.getTotalCount();
								if(mTrackList.getTracks().size() <= 0)
									return;
								String str = "专辑："+mTrackList.getAlbumTitle()+ "\n当前播放：" +mTrackList.getTracks().get(0).getTrackTitle().toString();
								if(isNeedPlay)
								{
									mPosition = index % PAGE_SIZE;
									mHandler.sendMessage(mHandler.obtainMessage(MessageConst.CLIENT_ACTION_PLAY_BOOK_AFTER_SEARCH, index % PAGE_SIZE,0));
									Log.i("ppp","str "+str+" mTotalCount = "+mTotalCount+"  index =  "+index+"  mPosition = "+mPosition);
									Log.i("ppp","mPosition "+mPosition);
								}
								else
								    sendBookInfoToServer();									
							}
							@Override
							public void onError(int code, String message)
							{
								Log.i("ppp","error: "+message);
								sendBookInfoToServer();
							}
					});
					
				}
			}

			@Override
			public void onError(int code, String message)
			{
               Log.i("ppp","error: "+message);
               sendBookInfoToServer();
			}
		});
	}
	
	public void searchHotBook()
	{
		Map<String, String> map = new HashMap<String, String>();
		map.put(DTransferConstants.CATEGORY_ID, "" + 3);//0
		map.put(DTransferConstants.PAGE, "" + 60);
		CommonRequest.getAlbums(map, new IDataCallBack<AlbumList>()
		{
					@Override
					public void onSuccess(AlbumList object)
					{
						int size = object.getAlbums().size();
						int index = (int) (Math.random()*size);
						if(index == size)
							index = size -1;
						String bookName = object.getAlbums().get(index).getAlbumTitle();
						if(bookName == null)
							bookName = object.getAlbums().get(0).getAlbumTitle();
						searchBookInfo(bookName,0,true);
						
					}
					@Override
					public void onError(int code, String message)
					{
						Log.i("ppp","error: "+message);
						sendBookInfoToServer();					
					}
		});                
	}
	
	public void play(int position)
	{
		if(position < 0)
			position = 0;
		mPlayerManager.playList(mTrackList, position);
		mPosition = position;
		Log.i("ppp","play() current index: "+mPlayerManager.getCurrentIndex());
	}
	
	public void pause()
	{
		if(mPlayerManager != null)
			mPlayerManager.pause();
	}
	
	public void resumePlay()
	{
		if(mPlayerManager != null)
			mPlayerManager.play();
	}
	
	public void seek(int seekTime)
	{
		if(mPlayerManager != null)
			mPlayerManager.seekTo(seekTime);
	}
	
	public void stop()
	{
		if(mPlayerManager != null)
			mPlayerManager.stop();
	}
	
	public void addVolume()
	{
		int volume = getPercentCurrentVolume(mContext);
		AudioManager audioManager = (AudioManager) mContext.getSystemService(Service.AUDIO_SERVICE);
		int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		volume = (int) (volume +100*2/maxVolume);
		if(volume > 100)
			volume = 100;
		setVolume(volume);
		setCurrentPercentVolume(mContext,volume);
	}
	
	public void delVolume()
	{
		int volume = getPercentCurrentVolume(mContext);
		AudioManager audioManager = (AudioManager) mContext.getSystemService(Service.AUDIO_SERVICE);
		int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		volume = (int) (volume - 100*2/maxVolume);
		if(volume < 30)
			volume = 30;
		setVolume(volume);
		setCurrentPercentVolume(mContext,volume);
	}
	
	private  int getPercentCurrentVolume(Context context)
	{
		int percent_volume = 0;
		AudioManager audioManager = (AudioManager) context.getSystemService(Service.AUDIO_SERVICE);
		int current =audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		int maxVolume =audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		percent_volume = current*100/maxVolume;
		return percent_volume;
	}
	
	private void setCurrentPercentVolume(Context context,int volume)
	{
		AudioManager audioManager = (AudioManager) context.getSystemService(Service.AUDIO_SERVICE);		
		int maxVolume =audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume*maxVolume/100, 0);
	}
	
	public void prev()
	{
		if(mPosition == 0)
		{
			if(mPage > 1)
			{
				mPosition = PAGE_SIZE - 1;
				searchBookAndPlay(mBookName,(mPage-1)*PAGE_SIZE-1,0);
			}
			else{
				mPage = mTotalCount/PAGE_SIZE;
				mPosition = mTotalCount%PAGE_SIZE - 1;
				searchBookAndPlay(mBookName,mTotalCount-1,0);
			}
		}		
		else
		{
		   mPosition -- ;
		   if(mPlayerManager != null)
			   mPlayerManager.play(mPosition);
		   mHandler.sendMessage(mHandler.obtainMessage(MessageConst.CLIENT_ACTION_BOOK_RESET_POSITION, mPosition,0));
		}		
	}
	
	public void next()
	{
		mPosition++;
		
		if(((mPage-1)*PAGE_SIZE+mPosition) == mTotalCount)
		{
			mPosition = 0;
			mPage = 1;
			searchBookAndPlay(mBookName,mPosition,0);
		}
		else if(mPosition < mTrackList.getTracks().size())
		{
		   if(mPlayerManager != null)
				mPlayerManager.play(mPosition);
		    mHandler.sendMessage(mHandler.obtainMessage(MessageConst.CLIENT_ACTION_BOOK_RESET_POSITION, mPosition,0));
		}
		else
		{
			mPosition = 0;
			searchBookAndPlay(mBookName,mPage*PAGE_SIZE,0);
		}		
	}
	
	public void skipTo(int playPosition)
	{
		Log.i("ppp","skipTo() playPosition : "+playPosition);
		mPosition = playPosition;
		if(playPosition > PAGE_SIZE-1)
		{//next 
			if(((mPage-1)*PAGE_SIZE+mPosition) == mTotalCount)//the last
			{
				mPosition = 0;
				mPage = 1;
				searchBookAndPlay(mBookName,0,0);//go to the first
			}
			else
			{
			  searchBookAndPlay(mBookName,mPage*PAGE_SIZE,0);// go to next page
			}
		}
		else if(playPosition < 0)
		{//previous
			if(mPage == 1)
			{
				mPage = mTotalCount/PAGE_SIZE;
				mPosition = mTotalCount%PAGE_SIZE - 1;
				searchBookAndPlay(mBookName,mTotalCount-1,0);//go to the last
			}else
			{
			   searchBookAndPlay(mBookName,(mPage-1)*PAGE_SIZE-1,0);//go to previous page
			}
		}
		else
		{
		   if(((mPage-1)*PAGE_SIZE+mPosition) == mTotalCount)// go to the first
		   {
				mPosition = 0;
				mPage = 1;
				searchBookAndPlay(mBookName,mPosition,0);
		   }
		   else
		   {
			   mPlayerManager.play(mPosition);
			   mHandler.sendMessage(mHandler.obtainMessage(MessageConst.CLIENT_ACTION_BOOK_RESET_POSITION, mPosition,0));
		   }
		   Log.i("ppp","skipTo() current index: "+mPlayerManager.getCurrentIndex());
		}		
		Log.i("ppp","skipTo() mPosition: "+mPosition);
	}
	
	public int getPlayIndex()
	{
		int index = (mPage-1)*PAGE_SIZE+mPosition+1;
		return index;
	}
	public int getIndexInPlayList()
	{
		return mPosition;
	}
	
	public int getCurrentPlayingTime()
	{
		return mPlayerManager.getPlayCurrPositon();
	}
	
	public int getDuration()
	{
		return  mPlayerManager.getDuration();		
	}
	
	public String getBookName()
	{
		return mTrackList.getAlbumTitle();
	}
	
	public String getCurrentPlayInfo()
	{
		Log.i("ppp","getCurrentPlayInfo() mPosition: "+mPosition);
		if(mTrackList.getTracks().size() > mPosition)
		   return mTrackList.getTracks().get(mPosition).getTrackTitle().toString();
		else
		   return "need search again.";
			
	}
	private IXmPlayerStatusListener mPlayerStatusListener = new IXmPlayerStatusListener()
	{

		@Override
		public void onSoundPrepared()
		{
			
		}

		@Override
		public void onSoundSwitch(PlayableModel laModel, PlayableModel curModel)
		{
			
		}

		

		@Override
		public void onPlayStop()
		{
			
		}

		@Override
		public void onPlayStart()
		{
			mHandler.sendMessage(mHandler.obtainMessage(MessageConst.CLIENT_ACTION_UPDATA_PLAYING_BOOK_NAME, 0,0,getCurrentPlayInfo()));
		}

		@Override
		public void onPlayProgress(int currPos, int duration)
		{
			mHandler.sendMessage(mHandler.obtainMessage(MessageConst.CLIENT_ACTION_UPDATE_BOOK_PROGRESS, currPos,duration));
		}

		@Override
		public void onPlayPause()
		{
		}

		@Override
		public void onSoundPlayComplete()
		{
			next();
		}

		@Override
		public boolean onError(XmPlayerException exception)
		{
			return false;		
		}

		@Override
		public void onBufferProgress(int position)
		{
		
		}

		public void onBufferingStart()
		{
			
		}

		public void onBufferingStop()
		{
			
		}

	};
	
	
	private IXmAdsStatusListener mAdsListener = new IXmAdsStatusListener()
	{
		
		@Override
		public void onStartPlayAds(Advertis ad, int position)
		{
			
		}
		
		@Override
		public void onStartGetAdsInfo()
		{
			
		}
		
		@Override
		public void onGetAdsInfo(AdvertisList ads)
		{
			Log.e("ppp", "onGetAdsInfo " + (ads != null));
		}
		
		@Override
		public void onError(int what, int extra)
		{
			Log.e("ppp", "onError what:" + what + ", extra:" + extra);
		}
		
		@Override
		public void onCompletePlayAds()
		{
			Log.e("ppp", "onCompletePlayAds");
			
		}
		
		@Override
		public void onAdsStopBuffering()
		{
			Log.e("ppp", "onAdsStopBuffering");
		}
		
		@Override
		public void onAdsStartBuffering()
		{
			Log.e("ppp", "onAdsStartBuffering");
		}
	}; 
	
	private void sendBookInfoToServer()
	{
		JSONObject jObj = new JSONObject();
		JSONObject objApp = new JSONObject();
		JSONArray arrayId = new JSONArray();
		JSONArray arrayTitles = new JSONArray();
		JSONArray arrayUrls = new JSONArray();
		JSONArray arrayArtists = new JSONArray();
		JSONArray arrayPhoto = new JSONArray();
		JSONArray arrayTime = new JSONArray();
		JSONArray arrayAlbum = new JSONArray(); 
		if(mTrackList != null && mTrackList.getTotalCount() > 0)
		{
			try {
				Track track = mTrackList.getTracks().get(0);
				arrayId.put(0,0);
				arrayTitles.put(0,mTrackList.getAlbumTitle());
				arrayUrls.put(0,track.getDownloadUrl());
				arrayPhoto.put(0,track.getCoverUrlSmall());
				arrayTime.put(0,track.getDuration());
				arrayAlbum.put(0,track.getAlbum());
				
				jObj.put("id", arrayId);
				jObj.put("titles", arrayId);
				jObj.put("urls", arrayUrls);
				jObj.put("artists", arrayArtists);
				jObj.put("photo", arrayPhoto);
				jObj.put("time", arrayTime);
				jObj.put("album", arrayAlbum);
				objApp.put("App", jObj);				
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}else{
			try {
				jObj.put("id", arrayId);
				jObj.put("titles", arrayId);
				jObj.put("urls", arrayUrls);
				jObj.put("artists", arrayArtists);
				jObj.put("photo", arrayPhoto);
				jObj.put("time", arrayTime);
				jObj.put("album", arrayAlbum);
				objApp.put("App", jObj);		
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
		}
		if(mHandler != null)
			mHandler.sendMessage(mHandler.obtainMessage(MessageConst.CLIENT_ACTION_UPLOAD_SEARCH_BOOK_INFO, objApp));
	}
	
	public void destroy()
	{
		if(mXimalaya != null)
		  mXimalaya.destroy();
		if(mPlayerManager != null)
		  mPlayerManager.release();
	}
}
