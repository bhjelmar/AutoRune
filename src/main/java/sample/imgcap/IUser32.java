package sample.imgcap;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;

public interface IUser32 extends StdCallLibrary {
	public static final String SHELL_TRAY_WND = "Shell_TrayWnd";
	public static final int WM_COMMAND = 0x111;
	public static final int MIN_ALL = 0x1a3;
	public static final int MIN_ALL_UNDO = 0x1a0;

	final IUser32 instance = (IUser32) Native.loadLibrary("user32", IUser32.class);

	boolean EnumWindows(IWndEnumProc wndenumproc, int lParam);

	boolean IsWindowVisible(int hWnd);

	int GetWindowRect(int hWnd, RECT r);

	void GetWindowTextA(int hWnd, byte[] buffer, int buflen);

	int GetTopWindow(int hWnd);

	int GetWindow(int hWnd, int flag);

	boolean ShowWindow(int hWnd);

	boolean BringWindowToTop(int hWnd);

	int GetActiveWindow();

	boolean SetForegroundWindow(int hWnd);

	int FindWindowA(String winClass, String title);

	long SendMessageA(int hWnd, int msg, int num1, int num2);

	final int GW_HWNDNEXT = 2;
}