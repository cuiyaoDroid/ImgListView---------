package com.roger.listimgdemo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Scroller;
/**
 * listviewͷͼƬ�����Ŵ� ʹ��ʱ��ص��÷���setImageId��setImageBitmap����ͼƬ ���򱨴� Created by Roger
 * on 14-4-30.
 */
public class ImgListView extends ListView implements OnScrollListener{

	private static final int BACK_SCALE = 0;
	private boolean isHaveHead = false;// ͷ���Ƿ���ͼƬ
	private float scaleY = 0;
	private boolean isBacking = false;// �Ƿ��ڻص�״̬
	private int displayWidth;
	private Context mContext;
	private Bitmap bmp;
	private View headerView;
	private ImageView imageView;
	/** ���ڼ�¼����ͼƬ�ƶ�������λ�� */
	private Matrix matrix = new Matrix();
	/** ���ڼ�¼ͼƬҪ��������ʱ�������λ�� */
	private Matrix currentMatrix = new Matrix();
	private Matrix defaultMatrix = new Matrix();
	private float imgHeight, imgWidth;
	/** ��¼��������Ƭģʽ���ǷŴ���С��Ƭģʽ 0:����ģʽ��1���Ŵ� */
	private int mode = 0;// ��ʼ״̬
	/** ������Ƭģʽ */
	private final int MODE_DRAG = 1;
	/** ���ڼ�¼��ʼʱ�������λ�� */
	private PointF startPoint = new PointF();

	private int mImageId;

	private AttributeSet attrs;
	
	
	private boolean mEnablePullLoad;
	private boolean mPullLoading;
	private boolean mIsFooterReady = false;
	private int mTotalItemCount;
	private float mLastY = -1; // save event y
	private final static float OFFSET_RADIO = 1.8f;
	private int mScrollBack;
	private final static int SCROLLBACK_HEADER = 0;
	private final static int SCROLLBACK_FOOTER = 1;

	private final static int SCROLL_DURATION = 400; // scroll back duration
	private final static int PULL_LOAD_MORE_DELTA = 50; 
	private XListViewFooter mFooterView;
	private IXListViewListener mListViewListener;
	
	private Scroller mScroller;
	public ImgListView(Context context) {
		super(context);
		this.mContext = context;
		initView();
	}

	public ImgListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.mContext = context;
		this.attrs = attrs;
		initView();
	}

	public ImgListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.mContext = context;
		this.attrs = attrs;
		initView();
	}

	public void setAdapter(ListAdapter adapter) {
		if (mIsFooterReady == false) {
			mIsFooterReady = true;
			addFooterView(mFooterView);
		}
		super.setAdapter(adapter);
	}

	public void addHeaderView(View v) {
		super.addHeaderView(v);
	}

	public void setImageId(int id) {
		this.mImageId = id;
		bmp = BitmapFactory.decodeResource(getResources(), mImageId);
		if (isHaveHead)
			this.removeHeaderView(headerView);
		initHead();
	}

	public void setImageBitmap(Bitmap bit) {
		this.bmp = bit;
		if (isHaveHead)
			this.removeHeaderView(headerView);
		initHead();
	}
	public void setXListViewListener(IXListViewListener l) {
		mListViewListener = l;
	}
	/**
	 * ��ʼ��ͼƬ
	 */
	private void initView() {
		/* ȡ����Ļ�ֱ��ʴ�С */
		mScroller = new Scroller(mContext, new DecelerateInterpolator());
		
		// XListView need the scroll event, and it will dispatch the event to
		// user's listener (as a proxy).
		
		super.setOnScrollListener(this);
		DisplayMetrics dm = new DisplayMetrics();
		WindowManager mWm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		mWm.getDefaultDisplay().getMetrics(dm);
		displayWidth = dm.widthPixels;

		TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.ImgListView);
		mImageId = a.getResourceId(R.styleable.ImgListView_headimage, 0);
		a.recycle();
		if (null == bmp && mImageId != 0) {
			bmp = BitmapFactory.decodeResource(getResources(), mImageId);
			initHead();
		}
		mFooterView = new XListViewFooter(mContext);
	}
	private void initHead() {
		LayoutInflater inflater = LayoutInflater.from(mContext);
		headerView = inflater.inflate(R.layout.top_img, null);
		imageView = (ImageView) headerView.findViewById(R.id.imageView);
		float scale = (float) displayWidth / (float) bmp.getWidth();// 1080/1800
		matrix.postScale(scale, scale, 0, 0);
		imageView.setImageMatrix(matrix);
		defaultMatrix.set(matrix);
		imgHeight = scale * bmp.getHeight();
		imgWidth = scale * bmp.getWidth();
		ListView.LayoutParams relativeLayout = new ListView.LayoutParams((int) imgWidth, (int) imgHeight);
		imageView.setLayoutParams(relativeLayout);
		this.addHeaderView(headerView);
		isHaveHead = true;
	}

	/**
	 * ���»�����ͼƬ���
	 * 
	 * @param event
	 * @return
	 */
	public boolean onTouchEvent(MotionEvent event) {
		if (mLastY == -1) {
			mLastY = event.getRawY();
		}
		if (!isHaveHead) {// ��ͷ��ͼƬ
			return super.onTouchEvent(event);
		}
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		// ��ָѹ����Ļ
		case MotionEvent.ACTION_DOWN:
			mLastY = event.getRawY();
			if (isBacking) {
				return super.onTouchEvent(event);
			}
			int[] location = new int[2];
			imageView.getLocationInWindow(location);
			if (location[1] >= 0) {
				mode = MODE_DRAG;
				// ��¼ImageView��ǰ���ƶ�λ��
				currentMatrix.set(imageView.getImageMatrix());
				startPoint.set(event.getX(), event.getY());
			}
			break;
		// ��ָ����Ļ���ƶ������¼��ᱻ���ϴ���
		case MotionEvent.ACTION_MOVE:
			
			// ����ͼƬ
			final float deltaY = event.getRawY() - mLastY;
			mLastY = event.getRawY();
			if (mode == MODE_DRAG) {
				float dx = event.getX() - startPoint.x; // �õ�x����ƶ�����
				float dy = event.getY() - startPoint.y; // �õ�y����ƶ�����
				// ��û���ƶ�֮ǰ��λ���Ͻ����ƶ�
				if (dy / 2 + imgHeight <= 1.8 * imgHeight) {
					matrix.set(currentMatrix);
					float scale = (dy / 2 + imgHeight) / (imgHeight);// �õ����ű���
					if (dy > 0) {
						scaleY = dy;
						ListView.LayoutParams relativeLayout = new ListView.LayoutParams((int) (scale * imgWidth), (int) (scale * imgHeight));
						imageView.setLayoutParams(relativeLayout);
						matrix.postScale(scale, scale, imgWidth / 2, 0);
						imageView.setImageMatrix(matrix);
						return false;
					}
				}
			}else if (getLastVisiblePosition() == mTotalItemCount - 1 && (mFooterView.getBottomMargin() > 0 || deltaY < 0)) {
				// last item, already pulled up or want to pull up.
				updateFooterHeight(-deltaY / OFFSET_RADIO);
			}
			break;
		// ��ָ�뿪��Ļ
		case MotionEvent.ACTION_UP:
			// �������뿪��Ļ��ͼƬ��ԭ
			mHandler.sendEmptyMessage(BACK_SCALE);
		case MotionEvent.ACTION_POINTER_UP:
			mLastY = -1; // reset
			mode = 0;
		default:
			if (getLastVisiblePosition() == mTotalItemCount - 1) {
				// invoke load more.
				if (mEnablePullLoad && mFooterView.getBottomMargin() > PULL_LOAD_MORE_DELTA) {
					startLoadMore();
				}
				resetFooterHeight();
			}
			break;
		}

		return super.onTouchEvent(event);
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
			case BACK_SCALE:
				float scale = (scaleY / 2 + imgHeight) / (imgHeight);// �õ����ű���
				if (scaleY > 0) {
					isBacking = true;
					matrix.set(currentMatrix);
					ListView.LayoutParams relativeLayout = new ListView.LayoutParams((int) (scale * imgWidth), (int) (scale * imgHeight));
					imageView.setLayoutParams(relativeLayout);
					matrix.postScale(scale, scale, imgWidth / 2, 0);
					imageView.setImageMatrix(matrix);
					scaleY = (float) (scaleY / 2 - 1);
					mHandler.sendEmptyMessageDelayed(BACK_SCALE, 20);
				} else {
					scaleY = 0;
					ListView.LayoutParams relativeLayout = new ListView.LayoutParams((int) imgWidth, (int) imgHeight);
					imageView.setLayoutParams(relativeLayout);
					matrix.set(defaultMatrix);
					imageView.setImageMatrix(matrix);
					isBacking = false;
				}
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
	};

	/**
	 * enable or disable pull up load more feature.
	 * 
	 * @param enable
	 */
	public void setPullLoadEnable(boolean enable) {
		mEnablePullLoad = enable;
		if (!mEnablePullLoad) {
			mFooterView.hide();
			mFooterView.setOnClickListener(null);
		} else {
			mPullLoading = false;
			mFooterView.show();
			mFooterView.setState(XListViewFooter.STATE_NORMAL);
			// both "pull up" and "click" will invoke load more.
			mFooterView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					//startLoadMore();
				}
			});
		}
	}
	public void stopLoadMore() {
		if (mPullLoading == true) {
			mPullLoading = false;
			mFooterView.setState(XListViewFooter.STATE_NORMAL);
		}
	}
	private void updateFooterHeight(float delta) {
		int height = mFooterView.getBottomMargin() + (int) delta;
		if (mEnablePullLoad && !mPullLoading) {
			if (height > PULL_LOAD_MORE_DELTA) { // height enough to invoke load
													// more.
				mFooterView.setState(XListViewFooter.STATE_READY);
			} else {
				mFooterView.setState(XListViewFooter.STATE_NORMAL);
			}
		}
		mFooterView.setBottomMargin(height);

		// setSelection(mTotalItemCount - 1); // scroll to bottom
	}

	private void resetFooterHeight() {
		int bottomMargin = mFooterView.getBottomMargin();
		if (bottomMargin > 0) {
			mScrollBack = SCROLLBACK_FOOTER;
			mScroller.startScroll(0, bottomMargin, 0, -bottomMargin, SCROLL_DURATION);
			invalidate();
		}
	}
	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			if (mScrollBack == SCROLLBACK_FOOTER) {
				mFooterView.setBottomMargin(mScroller.getCurrY());
			}
			postInvalidate();
		}
		super.computeScroll();
	}
	private void startLoadMore() {
		mPullLoading = true;
		mFooterView.setState(XListViewFooter.STATE_LOADING);
		if (mListViewListener != null) {
			mListViewListener.onLoadMore();
		}
	}
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		// TODO Auto-generated method stub
		mTotalItemCount = totalItemCount;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub
		
	}
}
