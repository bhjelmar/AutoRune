package sample.imgcap;

import com.sun.jna.Structure;

import java.util.ArrayList;
import java.util.List;

public class RECT extends Structure {
	public int left, top, right, bottom;

	@Override
	protected List<String> getFieldOrder() {
		List<String> order = new ArrayList<>();
		order.add("left");
		order.add("top");
		order.add("right");
		order.add("bottom");
		return order;
	}
}
