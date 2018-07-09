package sample.imgcap;

import com.sun.jna.win32.StdCallLibrary;

public interface IWndEnumProc extends StdCallLibrary.StdCallCallback {
	boolean callback(int hWnd, int lParam);
}
