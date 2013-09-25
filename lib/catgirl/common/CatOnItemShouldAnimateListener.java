package lib.catgirl.common;

public interface CatOnItemShouldAnimateListener {
	public void onItemShouldAppear(int position, TaggedView view);
	public void onItemShouldDisappear(int position, TaggedView view);
}
