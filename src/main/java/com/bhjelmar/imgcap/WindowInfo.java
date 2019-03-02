package com.bhjelmar.imgcap;

public class WindowInfo {
	int hwnd;
	RECT rect;
	String title;

	public WindowInfo(int hwnd, RECT rect, String title) {
		this.hwnd = hwnd;
		this.rect = rect;
		this.title = title;
	}

	public String toString() {
		return String.format("(%d,%d)-(%d,%d) : \"%s\"", rect.left, rect.top, rect.right, rect.bottom, title);
	}
}
