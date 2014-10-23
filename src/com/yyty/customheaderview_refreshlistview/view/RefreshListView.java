package com.yyty.customheaderview_refreshlistview.view;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yyty.customheaderview_refreshlistview.R;
import com.yyty.customheaderview_refreshlistview.inter.OnRefreshListener;


public class RefreshListView extends ListView implements OnScrollListener {

	private LinearLayout mHeadViewRoot;
	private ImageView ivArrow;
	private ProgressBar mProgressBar;
	private TextView tvState;
	private TextView tvDate;
	private LinearLayout llAddView;
	private LinearLayout mFootViewRoot;
	private int mFootHeigth;
	private RotateAnimation upAnimation;
	private RotateAnimation downAnimation;
	private final int PULLDOWNREFRESH=0;//下拉刷新状态
	private final int RELEASEREFRESH=1;//释放刷新状态
	private final int LOADINGREFRESH=2;//正在刷新状态
	private int currentState=PULLDOWNREFRESH;//当前的状态
	private int mFirstVisibleItem;
	private int mHeadHeigth;
	private View mCustomHeaderView;//用户自定义的头布局
	private boolean isLoadingMore=false;
	private OnRefreshListener onRefreshListener;
	private boolean isEnablePullDownRefresh;//下拉刷新是否可用
	private boolean isEnableLoadingMore;//加载更多是否可用

	public void setEnablePullDownRefresh(boolean isEnablePullDownRefresh) {
		this.isEnablePullDownRefresh = isEnablePullDownRefresh;
	}

	public void setEnableLoadingMore(boolean isEnableLoadingMore) {
		this.isEnableLoadingMore = isEnableLoadingMore;
	}

	public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
		this.onRefreshListener = onRefreshListener;
	}

	public RefreshListView(Context context) {
		super(context);
		initHead();
		initFoot();
		setOnScrollListener(this);
	}

	public RefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initHead();
		initFoot();
		setOnScrollListener(this);
	}

	public RefreshListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initHead();
		initFoot();
		setOnScrollListener(this);
	}

	/**
	 * 初始化头布局
	 */
	private void initHead() {
		View headView = View.inflate(getContext(), R.layout.refresh_head_listview, null);
		mHeadViewRoot = (LinearLayout) headView.findViewById(R.id.ll_refresh);
		ivArrow = (ImageView) headView.findViewById(R.id.iv_down_refresh);
		mProgressBar = (ProgressBar) headView.findViewById(R.id.pb_refresh);
		tvState = (TextView) headView.findViewById(R.id.tv_down_refresh);
		tvDate = (TextView) headView.findViewById(R.id.tv_last_refresh_date);
		llAddView = (LinearLayout) headView.findViewById(R.id.ll_add_anthor);
		mProgressBar.setVisibility(View.INVISIBLE);
		this.addHeaderView(headView);
		//初始化动画
		initAnimation();
		//先测量头布局,再获取头布局的高度,对头布局进行隐藏
		mHeadViewRoot.measure(0, 0);
		mHeadHeigth = mHeadViewRoot.getMeasuredHeight();
		mHeadViewRoot.setPadding(0, -mHeadHeigth, 0, 0);
		
	}
	
	/**
	 * 初始化脚布局
	 */
	private void initFoot() {
		View footView = View.inflate(getContext(), R.layout.refresh_foot_listview, null);
		mFootViewRoot = (LinearLayout) footView.findViewById(R.id.ll_refresh_foot);
		this.addFooterView(footView);
		//先测量脚布局,再获取脚布局的高度,对脚布局进行隐藏
		mFootViewRoot.measure(0, 0);
		mFootHeigth = mFootViewRoot.getMeasuredHeight();
		mFootViewRoot.setPadding(0, -mFootHeigth, 0, 0);
		
	}

	/**
	 * 释放刷新动画和下拉刷新动画
	 */
	private void initAnimation() {
		//释放刷新动画
		upAnimation = new RotateAnimation(
				0, -180, 
				Animation.RELATIVE_TO_SELF, 0.5f, 
				Animation.RELATIVE_TO_SELF, 0.5f);
		upAnimation.setDuration(800);
		upAnimation.setFillAfter(true);
		
		//下拉刷新动画
		downAnimation = new RotateAnimation(
				-180, -360, 
				Animation.RELATIVE_TO_SELF, 0.5f, 
				Animation.RELATIVE_TO_SELF, 0.5f);
		downAnimation.setDuration(800);
		downAnimation.setFillAfter(true);
	}
	private int startY;
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			startY=(int) ev.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			int moveY=(int) ev.getY();
			int dif = moveY-startY;
			int paddingtop =-mHeadHeigth+dif;
			//下拉刷新不可用,不执行刷新操作
			if(!isEnablePullDownRefresh){
				break;
			}
			//当前状态为正在加载数据,不响应下拉操作
			if(currentState==LOADINGREFRESH){
				break;
			}
			
			if(mCustomHeaderView!=null){
				int[] location=new int[2];
				mCustomHeaderView.getLocationInWindow(location);
				//mCustomHeaderView在屏幕对应Y的值
				int mCustomHeaderViewOnScreenY=location[1];
				this.getLocationInWindow(location);
				//ListView在屏幕对应Y的值
				int ListViewOnScreenY=location[1];
				//mCustomHeaderView在屏幕对应Y的值小于ListView在屏幕对应Y的值的时候不执行刷新
				if(mCustomHeaderViewOnScreenY<ListViewOnScreenY){
					break;
				}
			}
			// 当前把头布局完全显示, 并且当前的状态是下拉状态
			if(paddingtop>-mHeadHeigth && mFirstVisibleItem==0){
				if(paddingtop>0 && currentState==PULLDOWNREFRESH){
					currentState=RELEASEREFRESH;// 把当前的状态修改为释放刷新的状态
					swichRefreshMode();
				}else if(paddingtop<0 && currentState==RELEASEREFRESH){
					currentState=PULLDOWNREFRESH;// 把当前的状态修改为下拉刷新的状态
					swichRefreshMode();
				}
				mHeadViewRoot.setPadding(0, paddingtop, 0, 0);
				return true;
			}
			
			break;
		case MotionEvent.ACTION_UP:
			// 当前状态是松开刷新, 进入到正在刷新中的操作
			if(currentState==RELEASEREFRESH){
				currentState=LOADINGREFRESH;
				mHeadViewRoot.setPadding(0, 0, 0, 0);
				swichRefreshMode();
				if(onRefreshListener!=null){
					onRefreshListener.onPullDownRefresh();
				}
			// 当前状态是下拉刷新, 什么都不做, 把下拉头给隐藏.
			}else if(currentState==PULLDOWNREFRESH){
				mHeadViewRoot.setPadding(0, -mHeadHeigth, 0, 0);
			}
			break;

		default:
			break;
		}
		return super.onTouchEvent(ev);
	}

	/**
	 * 根据当前状态,该变FootView的布局
	 */
	private void swichRefreshMode() {
		switch (currentState) {
		case PULLDOWNREFRESH:
			ivArrow.startAnimation(downAnimation);
			tvState.setText("下拉刷新...");
			break;
		case RELEASEREFRESH:
			ivArrow.startAnimation(upAnimation);
			tvState.setText("释放刷新...");
			break;
		case LOADINGREFRESH:
			mProgressBar.setVisibility(View.VISIBLE);
			ivArrow.clearAnimation();
			ivArrow.setVisibility(View.INVISIBLE);
			tvState.setText("正在加载最新数据...");
	break;

		default:
			break;
		}
		
	}
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if(!isEnableLoadingMore){
			return;
		}
		//若果滚动停止,或快速滑动到底部,执行加载更多数据操作,显示脚布局
		if(scrollState==SCROLL_STATE_IDLE || scrollState==SCROLL_STATE_FLING){
			if(this.getLastVisiblePosition()==(getCount()-1) && !isLoadingMore){
				mFootViewRoot.setPadding(0, 0, 0, 0);
				this.setSelection(getCount());
				isLoadingMore=true;
				if(onRefreshListener!=null){
					onRefreshListener.onLoadMore();
				}
			}
		}
		
		
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		this.mFirstVisibleItem=firstVisibleItem;
		
	}
	
	
	/**
	 * 刷新数据完成调用此方法,移除headView或footView
	 */
	public void onFinishRefrsh(){
		if(currentState==LOADINGREFRESH){
			currentState=PULLDOWNREFRESH;
			mProgressBar.setVisibility(View.INVISIBLE);
			ivArrow.setVisibility(View.VISIBLE);
			tvState.setText("下拉刷新...");
			tvDate.setText("上一次刷新时间:"+getLastRefreshDate());
			mHeadViewRoot.setPadding(0, -mHeadHeigth, 0, 0);	
		}else if(isLoadingMore){
			isLoadingMore=false;
			mFootViewRoot.setPadding(0, -mFootHeigth, 0, 0);
		}
		
	}
	
	/**
	 * @param v
	 * 对外提供一个用户自定义的头布局
	 */
	public void addCustomHeaderView(View customHeaderView){
		this.mCustomHeaderView=customHeaderView;
		llAddView.addView(customHeaderView);
	}
	
	/**
	 * @return
	 * 获取最后刷新时间
	 */
	public String getLastRefreshDate(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		return sdf.format(new Date());
		
	}
}
