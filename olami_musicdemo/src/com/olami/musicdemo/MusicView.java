package com.olami.musicdemo;

import android.content.Context;
import android.widget.RelativeLayout;
import android.os.Handler;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.util.AttributeSet;

public class MusicView extends RelativeLayout{
    private Context mContext;
    private Handler mHandler;
    private TextView mTextViewName;
    private TextView mTextViewTotalTime;
    private ProgressBar mProgressBar;
    public MusicView(Context context,AttributeSet attrs) {
        super(context,attrs);
        LayoutInflater inflater =(LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);

        RelativeLayout view = (RelativeLayout) inflater.inflate(R.layout.layout_musicview, this,true);
        mTextViewName = (TextView) view.findViewById(R.id.tv_name);
        mTextViewTotalTime = (TextView) view.findViewById(R.id.tv_total_time);
        mProgressBar = (ProgressBar)view.findViewById(R.id.progressbar_music);

    }

    public void initMusicView(Context context,Handler handler)
    {
        mContext = context;
        mHandler = handler;

//        LayoutInflater inflater =(LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
//
//        RelativeLayout view = (RelativeLayout) inflater.inflate(R.layout.layout_musicview, null);
//        mTextViewName = (TextView) view.findViewById(R.id.tv_name);
//        mProgressBar = (ProgressBar)view.findViewById(R.id.progressbar_music);

        //return view;
    }

    public void setMusicName(String name)
    {
        mTextViewName.setText(name);
    }

    public void setProgress(int progress)
    {
        mProgressBar.setProgress(progress);
    }

    public void setTotalTime(String time)
    {
        mTextViewTotalTime.setText(time);
    }

}
