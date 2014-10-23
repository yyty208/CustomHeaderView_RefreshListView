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
	private final int PULLDOWNREFRESH=0;//����ˢ��״̬
	private final int RELEASEREFRESH=1;//�ͷ�ˢ��״̬
	private final int LOADINGREFRESH=2;//����ˢ��״̬
	private int currentState=PULLDOWNREFRESH;//��ǰ��״̬
	private int mFirstVisibleItem;
	private int mHeadHeigth;
	private View mCustomHeaderView;//�û��Զ����ͷ����
	private boolean isLoadingMore=false;
	private OnRefreshListener onRefreshListener;
	private boolean isEnablePullDownRefresh;//����ˢ���Ƿ����
	private boolean isEnableLoadingMore;//���ظ����Ƿ����

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
	 * ��ʼ��ͷ����
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
		//��ʼ������
		initAnimation();
		//�Ȳ���ͷ����,�ٻ�ȡͷ���ֵĸ߶�,��ͷ���ֽ�������
		mHeadViewRoot.measure(0, 0);
		mHeadHeigth = mHeadViewRoot.getMeasuredHeight();
		mHeadViewRoot.setPadding(0, -mHeadHeigth, 0, 0);
		
	}
	
	/**
	 * ��ʼ���Ų���
	 */
	private void initFoot() {
		View footView = View.inflate(getContext(), R.layout.refresh_foot_listview, null);
		mFootViewRoot = (LinearLayout) footView.findViewById(R.id.ll_refresh_foot);
		this.addFooterView(footView);
		//�Ȳ����Ų���,�ٻ�ȡ�Ų��ֵĸ߶�,�ԽŲ��ֽ�������
		mFootViewRoot.measure(0, 0);
		mFootHeigth = mFootViewRoot.getMeasuredHeight();
		mFootViewRoot.setPadding(0, -mFootHeigth, 0, 0);
		
	}

	/**
	 * �ͷ�ˢ�¶���������ˢ�¶���
	 */
	private void initAnimation() {
		//�ͷ�ˢ�¶���
		upAnimation = new RotateAnimation(
				0, -180, 
				Animation.RELATIVE_TO_SELF, 0.5f, 
				Animation.RELATIVE_TO_SELF, 0.5f);
		upAnimation.setDuration(800);
		upAnimation.setFillAfter(true);
		
		//����ˢ�¶���
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
			//����ˢ�²�����,��ִ��ˢ�²���
			if(!isEnablePullDownRefresh){
				break;
			}
			//��ǰ״̬Ϊ���ڼ�������,����Ӧ��������
			if(currentState==LOADINGREFRESH){
				break;
			}
			
			if(mCustomHeaderView!=null){
				int[] location=new int[2];
				mCustomHeaderView.getLocationInWindow(location);
				//mCustomHeaderView����Ļ��ӦY��ֵ
				int mCustomHeaderViewOnScreenY=location[1];
				this.getLocationInWindow(location);
				//ListView����Ļ��ӦY��ֵ
				int ListViewOnScreenY=location[1];
				//mCustomHeaderView����Ļ��ӦY��ֵС��ListView����Ļ��ӦY��ֵ��ʱ��ִ��ˢ��
				if(mCustomHeaderViewOnScreenY<ListViewOnScreenY){
					break;
				}
			}
			// ��ǰ��ͷ������ȫ��ʾ, ���ҵ�ǰ��״̬������״̬
			if(paddingtop>-mHeadHeigth && mFirstVisibleItem==0){
				if(paddingtop>0 && currentState==PULLDOWNREFRESH){
					currentState=RELEASEREFRESH;// �ѵ�ǰ��״̬�޸�Ϊ�ͷ�ˢ�µ�״̬
					swichRefreshMode();
				}else if(paddingtop<0 && currentState==RELEASEREFRESH){
					currentState=PULLDOWNREFRESH;// �ѵ�ǰ��״̬�޸�Ϊ����ˢ�µ�״̬
					swichRefreshMode();
				}
				mHeadViewRoot.setPadding(0, paddingtop, 0, 0);
				return true;
			}
			
			break;
		case MotionEvent.ACTION_UP:
			// ��ǰ״̬���ɿ�ˢ��, ���뵽����ˢ���еĲ���
			if(currentState==RELEASEREFRESH){
				currentState=LOADINGREFRESH;
				mHeadViewRoot.setPadding(0, 0, 0, 0);
				swichRefreshMode();
				if(onRefreshListener!=null){
					onRefreshListener.onPullDownRefresh();
				}
			// ��ǰ״̬������ˢ��, ʲô������, ������ͷ������.
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
	 * ���ݵ�ǰ״̬,�ñ�FootView�Ĳ���
	 */
	private void swichRefreshMode() {
		switch (currentState) {
		case PULLDOWNREFRESH:
			ivArrow.startAnimation(downAnimation);
			tvState.setText("����ˢ��...");
			break;
		case RELEASEREFRESH:
			ivArrow.startAnimation(upAnimation);
			tvState.setText("�ͷ�ˢ��...");
			break;
		case LOADINGREFRESH:
			mProgressBar.setVisibility(View.VISIBLE);
			ivArrow.clearAnimation();
			ivArrow.setVisibility(View.INVISIBLE);
			tvState.setText("���ڼ�����������...");
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
		//��������ֹͣ,����ٻ������ײ�,ִ�м��ظ������ݲ���,��ʾ�Ų���
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
	 * ˢ��������ɵ��ô˷���,�Ƴ�headView��footView
	 */
	public void onFinishRefrsh(){
		if(currentState==LOADINGREFRESH){
			currentState=PULLDOWNREFRESH;
			mProgressBar.setVisibility(View.INVISIBLE);
			ivArrow.setVisibility(View.VISIBLE);
			tvState.setText("����ˢ��...");
			tvDate.setText("��һ��ˢ��ʱ��:"+getLastRefreshDate());
			mHeadViewRoot.setPadding(0, -mHeadHeigth, 0, 0);	
		}else if(isLoadingMore){
			isLoadingMore=false;
			mFootViewRoot.setPadding(0, -mFootHeigth, 0, 0);
		}
		
	}
	
	/**
	 * @param v
	 * �����ṩһ���û��Զ����ͷ����
	 */
	public void addCustomHeaderView(View customHeaderView){
		this.mCustomHeaderView=customHeaderView;
		llAddView.addView(customHeaderView);
	}
	
	/**
	 * @return
	 * ��ȡ���ˢ��ʱ��
	 */
	public String getLastRefreshDate(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		return sdf.format(new Date());
		
	}
}
