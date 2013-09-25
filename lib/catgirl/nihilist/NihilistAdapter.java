package lib.catgirl.nihilist;

import lib.catgirl.common.CatOnItemClickListener;
import lib.catgirl.common.CatOnItemLongClickListener;
import lib.catgirl.common.CatOnItemShouldAnimateListener;
import lib.catgirl.common.TaggedRecycler;
import lib.catgirl.common.TaggedView;

public abstract class NihilistAdapter {
	
	public abstract int getCount();
	
	public abstract TaggedView getView(int position, TaggedRecycler recycler);
	
	private NihilistView list;
	private CatOnItemClickListener onClickListener;
	private CatOnItemLongClickListener onLongClickListener;
	private CatOnItemShouldAnimateListener onAnimateListener;
	
	private int animationDuration = 400;
	
	public void setOnItemClickListener(CatOnItemClickListener onClickListener)
	{
		this.onClickListener = onClickListener;
	}
	
	public void setOnItemLongClickListener(CatOnItemLongClickListener onLongClickListener)
	{
		this.onLongClickListener = onLongClickListener;
	}
	
	public void setOnItemShouldAnimateListener(CatOnItemShouldAnimateListener onAnimateListener)
	{
		this.onAnimateListener = onAnimateListener;
	}
	
	public void sendItemClick(int position, TaggedView view)
	{
		if(onClickListener != null)
			onClickListener.onItemClicked(list, position, view);
	}
	
	public void sendItemLongClick(int position, TaggedView view)
	{
		if(onLongClickListener != null)
			onLongClickListener.onItemLongClicked(list, position, view);
	}
	
	public void sendShouldAppear(int position, TaggedView view)
	{
		if(onAnimateListener != null)
			onAnimateListener.onItemShouldAppear(position, view);
	}
	
	public void sendShouldDisappear(int position, TaggedView view)
	{
		if(onAnimateListener != null)
			onAnimateListener.onItemShouldDisappear(position, view);
	}
	
	/***
	 * Don't use, it's for NihilistView only
	 * @param list
	 */
	public void setNihilist(NihilistView list)
	{
		this.list = list;
	}
	
	public void notifyDataChanged()
	{
		if(list != null)
		{
			list.shouldRebuild = true;
			list.requestLayout();
		}
	}
	
	/**
	 * Only for NihilistAdapter
	 * @param position
	 * @param count
	 */
	public void animateAddElements(int position, int count)
	{
		if(list != null)
			list.animateAddElements(position, count);
	}

	public void animateRemoveElements(int position, int count)
	{
		if(list != null)
			list.animateRemoveElements(position, count);
	}

	public int getAnimationDuration() {
		return animationDuration;
	}

	public void setAnimationDuration(int animationDuration) {
		this.animationDuration = animationDuration;
	}
	
	// Move to top is not working yet
	public void setPosition(int position, boolean moveToTop)
	{
		if(list != null)
			list.setPosition(position, moveToTop);
	}
	
	public TaggedView getLiveView(int position)
	{
		if(list == null)
			return null;
		
		return list.getLiveView(position);
	}
	
	public int getFirstPosition()
	{
		if(list == null)
			return -1;
		else
			return list.firstPosition;
	}
	
	public int getLastPosition()
	{
		if(list == null)
			return -1;
		else
			return list.lastPosition;
	}
}
