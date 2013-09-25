package lib.catgirl.nihilist;

import java.util.ArrayList;
import java.util.List;

import lib.catgirl.common.Dynamics;
import lib.catgirl.common.SimpleDynamics;
import lib.catgirl.common.TaggedRecycler;
import lib.catgirl.common.TaggedView;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AbsListView;
import android.view.animation.Transformation;

/**
 * 
 * Whee, a list that can animate addition and deletion of elements.
 * Nifty for creating animated multi-level expandable tree views.
 *
 */
public class NihilistView extends ViewGroup {

	public NihilistView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	public NihilistView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public NihilistView(Context context) {
		super(context);
		init();
	}
	
	NihilistAdapter adapter;
	
	int firstPosition = 0;
	int lastPosition = 0;
	
	float yOffset = 0;
	
	int layoutWidth = 0;
	int layoutHeight = 0;
	
	List<TaggedView> currentItems = new ArrayList<TaggedView>();
	
	TaggedRecycler recycler = new TaggedRecycler();
	
	public boolean shouldRebuild = false;
	private boolean shouldMeasure = false;
	
	boolean isAnimating = false;
	
	// BASED SONY //
	
    /** Represents an invalid child index */
    private static final int INVALID_INDEX = -1;
	
    /** User is not touching the list */
    private static final int TOUCH_STATE_RESTING = 0;

    /** User is touching the list and right now it's still a "click" */
    private static final int TOUCH_STATE_CLICK = 1;

    /** User is scrolling the list */
    private static final int TOUCH_STATE_SCROLL = 2;
    
    /** Current touch state */
    private int mTouchState = TOUCH_STATE_RESTING;
    
    /** Distance to drag before we intercept touch events */
    private static final int TOUCH_SCROLL_THRESHOLD = 10;
    
    /** X-coordinate of the down event */
    private int mTouchStartX;

    /** Y-coordinate of the down event */
    private int mTouchStartY;
    
    /** Used to check for long press actions */
    private Runnable mLongPressRunnable;

    /** Reusable rect */
    private Rect mRect;
    
    private Runnable mDynamicsRunnable;
    
    private Dynamics mDynamics;
    
    private VelocityTracker mVelocityTracker;
    
    public NihilistView self = this;
    
    public boolean forcedPositionChange = true;
    
    protected boolean noclick = false;
	
	public void init() {
		if(isInEditMode())
			this.setBackgroundColor(Color.GREEN);
	}
	
	public void setAdapter(NihilistAdapter adapter)
	{
		this.adapter = adapter;
		adapter.setNihilist(this);
		
		shouldRebuild = true;
		
		mDynamicsRunnable = new Runnable() {
		    public void run() {
		        mDynamics.update(SystemClock.uptimeMillis());
	
		        yOffset += mDynamics.getDifference();
		        requestLayout();
		 
		        if (!mDynamics.isAtRest(5) && !fixBounds()) {
		        	ViewCompat.postOnAnimation(self, this);
		        }
		        else
		        {
		        	yOffset = (int) yOffset;
		        }
		    }
		};
		
		mDynamics = new SimpleDynamics(0.935f);
		
		requestLayout();
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		
		if(adapter == null)
			return;
		
		if(firstPosition >= adapter.getCount())
			firstPosition = adapter.getCount() - 1;
		if(firstPosition < 0)
			firstPosition = 0;
		
		if(lastPosition >= adapter.getCount())
			lastPosition = adapter.getCount() - 1;
		if(lastPosition < firstPosition)
			lastPosition = firstPosition;
		
		
		if(isAnimating)
			return;
		
		if(changed || layoutWidth < 0 || layoutHeight < 0)
		{
			layoutWidth = right - left;
			layoutHeight = bottom - top;
		}
		
		if(!shouldRebuild)
		{
			if(shouldMeasure)
			{
				shouldMeasure = false;
				measureItems();
			}
			arrangeItems();
			fixBounds();
			recycleItems();
		}
		else
		{
			shouldRebuild = false;
			removeAllViewsInLayout();
			int size = currentItems.size();
			for(int i = 0; i < size; i++)
			{
				recycler.recycleView(currentItems.remove(0));
			}
			
		}
		
		fillItems();
		fixBounds();
		
		if(forcedPositionChange)
		{
			forcedPositionChange = false;
			fillItems();
			fixBounds();
		}
		
		lastPosition = firstPosition + currentItems.size() - 1;
		
		invalidate();
		if(getParent() != null && getParent() instanceof View)
		{
			((View) getParent()).invalidate();
		}
	}
	
	public void setPosition(int position, boolean moveToTop)
	{
		if(position >= adapter.getCount())
			position = adapter.getCount() - 1;
		if(position < 0)
			position = 0;
		
		firstPosition = position;
		yOffset = 0;
		forcedPositionChange = true;
		requestLayout();
	}
	
	public TaggedView getLiveView(int position)
	{
		if(adapter == null)
			return null;
		
		if(position >= firstPosition && position < firstPosition + currentItems.size())
		{
			return currentItems.get(position - firstPosition);
		}
		return null;
	}
	
//	@SuppressLint("NewApi")
//	public void logItemPositions()
//	{
//		 Log.v("Nihilist", "Items in array: " + currentItems.size() + "; items in layout: " + getChildCount());
//		
//		for(TaggedView item : currentItems)
//		{
//			 Log.v("Nihilist", "Item at: " + item.view.getX() + " " + item.view.getY() + " Size: " + item.view.getWidth() + " " + item.view.getHeight());
//		}
//	}
	
	public void requestLayoutWithMeasure()
	{
		shouldMeasure = true;
		requestLayout();
	}
	
	private boolean fixBounds()
	{
		if(firstPosition == 0 && yOffset >= 0)
		{
			yOffset = 0;
			arrangeItems();
			
			return true;
		}
		else if(lastPosition == adapter.getCount() - 1)
		{
			int cHeight = 0;
			for(TaggedView item : currentItems)
			{
				cHeight += item.view.getHeight();
			}
			if(yOffset + cHeight < layoutHeight)
			{
				yOffset = layoutHeight - cHeight;
				arrangeItems();
		        
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Fill the items
	 */
	private void fillItems()
	{	
		
		// Fill down
		int cPosition = firstPosition;
		int rOffset = (int) yOffset;
		
		cPosition += currentItems.size();
		for(TaggedView item : currentItems)
			rOffset += item.view.getHeight();
		
		while(cPosition < adapter.getCount() && rOffset < layoutHeight)
		{
			rOffset += addItemAtBottom(adapter.getView(cPosition, recycler), rOffset);
			cPosition++;
		}
		
		lastPosition = cPosition - 1;
		
		fixBounds();
		
		// Fill up
		
		rOffset = (int) yOffset;
		cPosition = firstPosition - 1;
		
		while(cPosition >= 0 && rOffset > 0)
		{
			rOffset -= addItemAtTop(adapter.getView(cPosition, recycler), rOffset);
			yOffset = rOffset;  
			cPosition--;
		}
		
		firstPosition = cPosition + 1;
		
		fixBounds();
	}
	
	/**
	 * Recycle the items that are hidden off-screen
	 */
	private void recycleItems()
	{
		int removeDownTo = -1;
		int removeUpTo = -1;
		
		if(!currentItems.isEmpty())
		{
			int rOffset = (int) yOffset;
			
			for(int i = 0; i < currentItems.size(); i++)
			{
				View v = currentItems.get(i).view;
				
				if(rOffset + v.getHeight() < 0)
					removeDownTo = i;
				else
					break;
				
				rOffset += v.getHeight();
			}
			
			for(int i = 0; i <= removeDownTo; i++)
			{
				firstPosition++;
				TaggedView item = currentItems.remove(0);
				yOffset += item.view.getHeight();
				removeView(item.view); 
				recycler.recycleView(item);
			}
		}
		
		if(!currentItems.isEmpty())
		{
			int lastOffset = (int) yOffset;
			for(TaggedView item : currentItems)
			{
				lastOffset += item.view.getHeight();
			}
			
			for(int i = currentItems.size() - 1; i >= 0; i--)
			{
				View v = currentItems.get(i).view;
				
				if(lastOffset - v.getHeight() > layoutHeight)
					removeUpTo = i;
				else
					break;
				
				lastOffset -= v.getHeight();
			}
			
			if(removeUpTo > 0)
			{
				int size = currentItems.size();
				for(int i = removeUpTo; i < size; i++)
				{
					lastPosition--;
					TaggedView item = currentItems.remove(removeUpTo);
					removeView(item.view);
					recycler.recycleView(item);
				}
			}
		}
	}
	
	/**
	 * This one updates the views based on their current height, just in case (I don't plan on using views that change size)
	 */
	private void arrangeItems()
	{
		int rOffset = (int) yOffset;
		
		for(TaggedView item : currentItems)
		{	
			int width = item.view.getMeasuredWidth();
	        int height = item.view.getMeasuredHeight();
	        int left = (getWidth() - width) / 2;
			
	        item.view.clearAnimation();
			item.view.layout(left, rOffset, left + width, rOffset + height);
			
			rOffset += height;
		}
	}
	
	private void measureItems()
	{
		int itemWidth = getWidth();
		
		for(TaggedView item : currentItems)
		{
			item.view.measure(MeasureSpec.EXACTLY | itemWidth, MeasureSpec.UNSPECIFIED);
		}
	}
	
	/**
	 * Measure and add a view to the bottom
	 */
	private int addItemAtBottom(TaggedView item, int bottom)
	{
		item.view.measure(MeasureSpec.EXACTLY | getWidth(), MeasureSpec.UNSPECIFIED);
		
		int width = item.view.getMeasuredWidth();
        int height = item.view.getMeasuredHeight();
        int left = (getWidth() - width) / 2;
		
        item.view.clearAnimation();
		item.view.layout(left, bottom, left + width, bottom + height);
		
		addView(item.view);
		currentItems.add(item);
		
		return item.view.getHeight();
	}
	
	private int addItemAtTop(TaggedView item, int top)
	{
		item.view.measure(MeasureSpec.EXACTLY | getWidth(), MeasureSpec.UNSPECIFIED);
		
		int width = item.view.getMeasuredWidth();
        int height = item.view.getMeasuredHeight();
        int left = (getWidth() - width) / 2;
		
        item.view.clearAnimation();
		item.view.layout(left, top - height, left + width, top);
		
		addView(item.view);
		currentItems.add(0, item);
		
		return item.view.getHeight();
	}
	
	class AnimatedView {
		View v;
		int start;
		int finish;
		int left;
		int width;
		int height;
	}
	
	ArrayList<AnimatedView> animatedViews = new ArrayList<AnimatedView>();
	ArrayList<AnimatedView> appearingViews = new ArrayList<AnimatedView>();
	int topItemStart;
	int topItemEnd;
	
	/**
	 * Only for NihilistAdapter
	 * @param position
	 * @param count
	 */
	public void animateAddElements(int position, int count)
	{
		if(position < firstPosition || position > lastPosition + 1 || isAnimating)
			return;
		
		isAnimating = true;
		
		boolean haveToHide = !(position == lastPosition + 1);
			
		
		int rOffset = (int) yOffset;
		
		for(int i = 0; i < position - firstPosition; i++)
		{
			View v = currentItems.get(i).view;
			rOffset += v.getHeight();
		}
		
		int nOffset = rOffset;
		
		int addedViews = 0;
		
		for(int i = position; i < position + count; i++)
		{
			TaggedView item = adapter.getView(i, recycler);
			item.view.measure(MeasureSpec.EXACTLY | getWidth(), MeasureSpec.UNSPECIFIED);
			
			int width = item.view.getMeasuredWidth();
	        int height = item.view.getMeasuredHeight();
	        int left = (getWidth() - width) / 2;
			
			item.view.layout(left, nOffset, left + width, nOffset + height);
			
			addView(item.view);
			currentItems.add(i - firstPosition, item);
			
			if(haveToHide)
			{
				AnimatedView v = new AnimatedView();
				v.v = item.view;
				v.start = nOffset;
				v.height = v.v.getHeight();
				appearingViews.add(v);
			}
			
			nOffset += item.view.getHeight();
			addedViews++;
			lastPosition++;
			
			adapter.sendShouldAppear(i, item);
			
			if(nOffset > layoutHeight)
				break;
		}
		
		int diff = nOffset - rOffset;
		
		topItemStart = rOffset;
		topItemEnd = nOffset;
		
		for(int i = position - firstPosition + addedViews; i < currentItems.size(); i++)
		{
//			int j = i + addedViews - firstPosition;
			
			TaggedView item = currentItems.get(i);
			item.view.clearAnimation();
			item.view.bringToFront();
			
			int width = item.view.getMeasuredWidth();
//	        int height = item.view.getMeasuredHeight();
	        int left = (getWidth() - width) / 2;
			
			AnimatedView v = new AnimatedView();
			v.v = item.view;
			v.start = rOffset;
			v.finish = v.start + diff;
			v.left = left;
			v.width = width;
			v.height = v.v.getHeight();
			
			animatedViews.add(v);
			
			rOffset += item.view.getHeight();
		}
		
		invalidate();
		
		Animation a = getAnimator();
		a.setAnimationListener(new AnimationListener(){
			@Override
			public void onAnimationEnd(Animation a) {
				animatedViews.clear();
				appearingViews.clear();
				isAnimating = false;
				self.shouldRebuild = true;
				requestLayout();
			}
			@Override
			public void onAnimationRepeat(Animation a) {}
			@Override
			public void onAnimationStart(Animation a) {}
		});
		this.startAnimation(a);
	}

	/**
	 * Only for NihilistAdapter
	 * @param position
	 * @param count
	 */
	public void animateRemoveElements(final int position, int count)
	{
		if(adapter == null || isAnimating)
			return;
		
		if(position + count < firstPosition || position > lastPosition)
			return;
		
		int yOffset = (int) this.yOffset;
		
//		if(position + count > adapter.getCount())
//			count = adapter.getCount() - position;
		
		int size = currentItems.size();
		int removeFrom = Math.max(0, position - firstPosition);
		int removeTo = Math.min(size - 1, position + count - firstPosition - 1);
		final int removeViewCount = removeTo - removeFrom + 1;
		
		boolean haveToHide = !((size - 1) == removeTo);
		
		// The total height of items on screen - need to add new items
		int totalSize = 0;
		int topSize = 0;
		
		for(int i = 0; i < removeFrom; i++)
		{
			topSize += currentItems.get(i).view.getHeight();
		}
		
		// The total height of the items being removed
		int removeSize = 0;
		
		for(int i = removeFrom; i <= removeTo; i++)
		{
			removeSize += currentItems.get(i).view.getHeight();
		}
		
		// How much can we fill by bringing in items from the bottom
		int fillBottomSize = 0;
		
		boolean noneAddedAtBottom = true;
		
		// The items we already have in the view array
		for(int i = removeTo + 1; i < size; i++)
		{
			fillBottomSize += currentItems.get(i).view.getHeight();
			noneAddedAtBottom = false;
		}
		
		// The total height we need to fill, including items on the bottom
		// that will need to be moved
		int needToFill = removeSize + fillBottomSize;
		
		totalSize = topSize + needToFill;
		
		// And let's add new items until we fill the required space or, well...
		// ...run out of items
//		if(lastPosition < position + count)
//			lastPosition = position + count;
		
		lastPosition -= removeViewCount;
		
		
		
		while(fillBottomSize < needToFill && lastPosition < adapter.getCount() - 1)
		{
			TaggedView item = adapter.getView(lastPosition + 1, recycler);
			int height = addItemAtBottom(item, totalSize);
			fillBottomSize += height;
			totalSize += height;
			lastPosition++;
			noneAddedAtBottom = false;
		}
		
		// Now let's think for a bit. At this stage we have two cases:
		// a. enough items
		// b. not enough items, duh
		
		int diffBottom = 0;
		int diffTop = 0;
		
		boolean shouldLineUpAtTop = false;
		
		// a: move up, done with it
		if(fillBottomSize >= needToFill)
		{
			diffBottom = - removeSize;
			haveToHide = false;
		}
		// b:
		// look how much we can (if we need to) add to the top to 
		// fill (height - fillBottomSize)
		else
		{
			int needToFillTop = layoutHeight - fillBottomSize;
			
			int fillTopSize = topSize;
			while(fillTopSize < needToFillTop && firstPosition > 0)
			{
				TaggedView item = adapter.getView(firstPosition - 1, recycler);
				int height = addItemAtTop(item, (int) yOffset);
				fillTopSize += height;
				yOffset -= height;
				firstPosition--;
			}
			
			// Yes, yes, don't even tell me, just pull-request or wait for me to fix it
			// I'm a bit too tired to fix this unnecessary calculation right now
			
			int upper = Math.max(0, position - firstPosition);
			int lower = Math.min(currentItems.size(), position + count - firstPosition);
			
			int nOffset = (int) yOffset;
			for(int i = 0; i < upper; i++)
				nOffset += currentItems.get(i).view.getHeight();
			
			int kOffset = nOffset;
			for(int i = upper; i < lower; i++)
				kOffset += currentItems.get(i).view.getHeight();
			
			// if we have enough: move bottom items as high as possible
			// and top items to meet with the bottom items)
			if(fillTopSize >= needToFillTop)
			{
				int rOffset = kOffset;
				for(int i = lower; i < currentItems.size(); i++)
					rOffset += currentItems.get(i).view.getHeight();	
				
				diffBottom = layoutHeight - rOffset;
				
				diffTop = (layoutHeight - fillBottomSize) - nOffset;
				
				if(noneAddedAtBottom)
					diffTop = layoutHeight - nOffset;
				
				haveToHide = false;
			}
			else
			{
				// if we don't have enough AGAIN
				// move all items to the 'arranged at top' position			
				diffTop = - (int) yOffset;
				
				diffBottom = diffTop - removeSize;
				
				shouldLineUpAtTop = true;
				
				topItemStart = kOffset;
				topItemEnd = kOffset + diffBottom;
			}
			
		}
		
		// I want to streamline this but right now I just can't think
		// Sorry if you see this abomination
		
		if(diffBottom != 0 || diffTop != 0)
		{
			isAnimating = true;
			
			int removeBeginOffset = (int) yOffset;
			int rOffset = (int) yOffset;
			if(diffTop != 0)
			{
				if(shouldLineUpAtTop)
					yOffset = 0;
				else
					yOffset += diffTop;
				for(int i = 0; i < position - firstPosition; i++)
				{	
					TaggedView item = currentItems.get(i);
					item.view.clearAnimation();
					item.view.bringToFront();
					
					int width = item.view.getMeasuredWidth();
			        int left = (getWidth() - width) / 2;
					
					AnimatedView v = new AnimatedView();
					v.v = item.view;
					v.start = rOffset;
					v.finish = v.start + diffTop;
					v.left = left;
					v.width = width;
					v.height = v.v.getHeight();

					animatedViews.add(v);
					
					rOffset += item.view.getHeight();
					removeBeginOffset = rOffset;
				}
				for(int i = position - firstPosition; i < Math.min(position + removeViewCount - firstPosition, currentItems.size()); i++)
					rOffset += currentItems.get(i).view.getHeight();
			}
			else
			{
				for(int i = 0; i < Math.min(position + removeViewCount - firstPosition, currentItems.size()); i++)
				{
					if(i == position - firstPosition)
						removeBeginOffset = rOffset;
					rOffset += currentItems.get(i).view.getHeight();
				}
			}
			
			if(diffBottom != 0) 
			{	
				for(int i = position + removeViewCount - firstPosition; i < currentItems.size(); i++)
				{	
					TaggedView item = currentItems.get(i);
					item.view.clearAnimation();
					item.view.bringToFront();
					
					int width = item.view.getMeasuredWidth();
			        int left = (getWidth() - width) / 2;
					
					AnimatedView v = new AnimatedView();
					v.v = item.view;
					v.start = rOffset;
					v.finish = v.start + diffBottom;
					v.left = left;
					v.width = width;
					v.height = v.v.getHeight();

					animatedViews.add(v);
					
					rOffset += item.view.getHeight();
				}
			}
			
			for(int i = position - firstPosition; i < Math.min(position + removeViewCount - firstPosition, currentItems.size()); i++)
			{
				TaggedView item = currentItems.get(i);
				adapter.sendShouldDisappear(i, item);
				
				if(haveToHide)
				{
					AnimatedView v = new AnimatedView();
					v.v = item.view;
					v.start = removeBeginOffset;
					v.height = v.v.getHeight();
					appearingViews.add(v);
					removeBeginOffset += v.height;
				}
			}
			
			Animation a = getAnimator();
			a.setAnimationListener(new AnimationListener(){
				@Override
				public void onAnimationEnd(Animation a) {
					animatedViews.clear();
					appearingViews.clear();
					isAnimating = false;
					killItems(position - firstPosition, Math.min(position + removeViewCount - firstPosition - 1, currentItems.size()));
				}
				@Override
				public void onAnimationRepeat(Animation a) {}
				@Override
				public void onAnimationStart(Animation a) {}
			});
			this.startAnimation(a);
		}
		
		this.yOffset = yOffset;
	}
	
	private void killItems(int from, int to)
	{
		// A really horrible hack because for some reason... items don't get removed from the layout
		// in Gingerbread. Don't ask me why, but if you know why - please contact me at artemik@gmail.com
		// I'm genuinely curious.
		if(android.os.Build.VERSION.SDK_INT <= 11)
		{
			this.removeAllViews();
			this.removeAllViewsInLayout();
			for(TaggedView item : currentItems)
				recycler.recycleView(item);
			currentItems.clear();
			lastPosition = firstPosition;
		}
		else for(int i = from; i <= to; i++)
		{
			if(currentItems.size() > from)
			{
				TaggedView item = currentItems.remove(from);
				removeView(item.view);
				recycler.recycleView(item);
				item.view.clearAnimation();
				// Maybe it happens because I call this, I haven't really checked
				// But otherwise you'd ask "why can't I see my recycled view"
				// Still doesn't explain the glitch. On Honeycomb+ everything works perfectly.
				item.view.setVisibility(View.VISIBLE);
			}
		}
		
		shouldRebuild = true;
		requestLayout();
	}
	
	private Animation getAnimator()
	{
		Animation a = new Animation(){
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t)
			{
				for(AnimatedView v : animatedViews)
				{
					v.v.clearAnimation();
					v.v.layout(v.left, (int) (v.start + (v.finish - v.start) * interpolatedTime), v.left + v.width, (int) (v.start + (v.finish - v.start) * interpolatedTime + v.height));
				}
				if(!appearingViews.isEmpty())
				{
					float topPosition = topItemStart + (topItemEnd - topItemStart) * interpolatedTime;
					for(AnimatedView v : appearingViews)
					{
						if(v.start <= topPosition)
						{
							if(v.v.getVisibility() != View.VISIBLE)
							{
//								v.v.clearAnimation();
								v.v.setVisibility(View.VISIBLE);
							}
						}
						else
						{
							if(v.v.getVisibility() != View.INVISIBLE)
							{
								v.v.clearAnimation();
								v.v.setVisibility(View.INVISIBLE);
							}
						}
					}
				}
				postInvalidate();
			}
		};
		
		a.setInterpolator(new AccelerateDecelerateInterpolator());
		
		if(adapter != null)
			a.setDuration(adapter.getAnimationDuration());
		else
			a.setDuration(400);
		
		return a;

	}
	
	float lastY = 0;
	
    @Override
    public boolean onTouchEvent(final MotionEvent event) {
    	float velocity = 0;
        if (getChildCount() == 0) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            	if(isAnimating)
            		break;
            	if(event.getPointerCount() == 1)
            		startTouch(event);
                break;

            case MotionEvent.ACTION_MOVE:
            	if(isAnimating)
            		break;
                if (mTouchState == TOUCH_STATE_CLICK) {
                    startScrollIfNeeded(event);
                }
                if (mTouchState == TOUCH_STATE_SCROLL) {
                	float newY = event.getY();
                	yOffset += (newY - lastY);
                	lastY = newY;
                	mVelocityTracker.addMovement(event);
                	requestLayout();
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mTouchState == TOUCH_STATE_CLICK && adapter != null) {
                    final int index = getContainingChildIndex((int)event.getX(), (int)event.getY());
                    if (index != INVALID_INDEX && !noclick) {
                        adapter.sendItemClick(firstPosition + index, currentItems.get(index));
                    }
                }
                else if(mTouchState == TOUCH_STATE_SCROLL && !isAnimating)
                {
                    mVelocityTracker.addMovement(event);
                    mVelocityTracker.computeCurrentVelocity(1000);
                    velocity = mVelocityTracker.getYVelocity();
                }
                endTouch(velocity);
                break;

            default:
                endTouch(velocity);
                break;
        }
        return true;
    }
    
    /**
     * Checks if the user has moved far enough for this to be a scroll and if
     * so, sets the list in scroll mode
     * 
     * @param event The (move) event
     * @return true if scroll was started, false otherwise
     */
    private boolean startScrollIfNeeded(final MotionEvent event) {
        final int xPos = (int)event.getX();
        final int yPos = (int)event.getY();
        if (xPos < mTouchStartX - TOUCH_SCROLL_THRESHOLD
                || xPos > mTouchStartX + TOUCH_SCROLL_THRESHOLD
                || yPos < mTouchStartY - TOUCH_SCROLL_THRESHOLD
                || yPos > mTouchStartY + TOUCH_SCROLL_THRESHOLD) {
            // we've moved far enough for this to be a scroll
            removeCallbacks(mLongPressRunnable);
            mTouchState = TOUCH_STATE_SCROLL;
            return true;
        }
        return false;
    }
 
    
    /**
     * Sets and initializes all things that need to when we start a touch
     * gesture.
     * 
     * @param event The down event
     */
    private void startTouch(final MotionEvent event) {
        // save the start place
        mTouchStartX = (int)event.getX();
        mTouchStartY = (int)event.getY();
        
        lastY = mTouchStartY;

        removeCallbacks(mDynamicsRunnable);
        
        // start checking for a long press
        startLongPressCheck();
        
        mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(event);

        // we don't know if it's a click or a scroll yet, but until we know
        // assume it's a click
        mTouchState = TOUCH_STATE_CLICK;
    }

    /**
     * Resets and recycles all things that need to when we end a touch gesture
     */
    private void endTouch(float velocity) {
        // remove any existing check for longpress
        removeCallbacks(mLongPressRunnable);

        noclick = false;
        
        if(mVelocityTracker != null)
        {
        	mVelocityTracker.recycle();
        	mVelocityTracker = null;
        }
        
        // reset touch state
        mTouchState = TOUCH_STATE_RESTING;
        
        if (mDynamics != null && Math.abs(velocity) > 0) {
        	mDynamics.setState(0, velocity, SystemClock.uptimeMillis());
            post(mDynamicsRunnable);
        }
        else
        {
        	yOffset = (int) yOffset;
        }
    }
    
    /**
     * Posts (and creates if necessary) a runnable that will when executed call
     * the long click listener
     */
    private void startLongPressCheck() {
        // create the runnable if we haven't already
        if (mLongPressRunnable == null) {
            mLongPressRunnable = new Runnable() {
                public void run() {
                    if (mTouchState == TOUCH_STATE_CLICK) {
                        final int index = getContainingChildIndex(mTouchStartX, mTouchStartY);
                        if (index != INVALID_INDEX && adapter != null) {
                        	noclick = true;
                            adapter.sendItemLongClick(firstPosition + index, currentItems.get(index));
                        }
                    }
                }
            };
        }

        // then post it with a delay
        postDelayed(mLongPressRunnable, ViewConfiguration.getLongPressTimeout());
    }
    
    /**
     * Returns the index of the child that contains the coordinates given.
     * 
     * @param x X-coordinate
     * @param y Y-coordinate
     * @return The index of the child that contains the coordinates. If no child
     *         is found then it returns INVALID_INDEX
     */
    private int getContainingChildIndex(final int x, final int y) {
        if (mRect == null) {
            mRect = new Rect();
        }
        for (int i = 0; i < currentItems.size(); i++) {
            currentItems.get(i).view.getHitRect(mRect);
            if (mRect.contains(x, y)) {
                return i;
            }
        }
        return INVALID_INDEX;
    }
    
    // Save and restore the list on configuration change. Magic!
    
    @Override
    public Parcelable onSaveInstanceState() {
    	
    	Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        
        ss.position = firstPosition;
        ss.yOffset = (int) yOffset;
        
        return ss;
    }
    
    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());
        
        shouldRebuild = true;
        
        firstPosition = ss.position;
        yOffset = ss.yOffset;
        
        requestLayout();
    }

    static class SavedState extends BaseSavedState {
        int yOffset;
        int position;

        /**
         * Constructor called from {@link AbsListView#onSaveInstanceState()}
         */
        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            position = in.readInt();
            yOffset = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(position);
            out.writeInt(yOffset);
        }

        @Override
        public String toString() {
            return "NihilistView.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " position=" + position
                    + " yOffset=" + yOffset + "}";
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
    

}
