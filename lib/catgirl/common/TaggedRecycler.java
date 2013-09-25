package lib.catgirl.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TaggedRecycler {
	
	public int TAG_LIMIT = 5;
	
	private HashMap<String, List<TaggedView>> bin = new HashMap<String, List<TaggedView>>();
	
	public void recycleView(TaggedView view)
	{
		if(bin.containsKey(view.tag))
		{
			if(bin.get(view.tag).size() < TAG_LIMIT)
				bin.get(view.tag).add(view);
		}
		else
		{
			List<TaggedView> list = new ArrayList<TaggedView>();
			list.add(view);
			bin.put(view.tag, list);
		}
	}
	
	public TaggedView getRecycledView(String tag)
	{
		if(bin.containsKey(tag))
			if(!bin.get(tag).isEmpty())
				return bin.get(tag).remove(0);
		
		return null;
	}
}
