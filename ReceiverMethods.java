public class ReceiverMethods {
	// Two available modes for transfer
	public enum Mode{
		StopAndWait, 
		SlidingWindow
	}
	// Default mode is Stop and Wait
	Mode mode = Mode.StopAndWait;
	// Get the current mode
	public int getMode() {
		switch (mode) {
		case StopAndWait:
			return 0;
		case SlidingWindow:
			return 1;
		}
		return -1;
	}
	long SWSize = 0;
	// Setting the mode
	public boolean setMode(int mode_no) {
		switch (mode_no) {
		case 0:
			mode = Mode.StopAndWait;
			SWSize = 1;
			return true;
		case 1:
			mode = Mode.SlidingWindow;
			SWSize = 256;
			return true;
		default:
			return false;
		}
	}
	// Set the sliding window size
	public boolean setModeParameter(long n) {
		if (mode == Mode.SlidingWindow) {
			SWSize = n;
			return true;
		}
		return false;
	}
	// Getting sliding window size
	public long getModeParameter() {
		return SWSize;
	}
	String filename;
	// Filename to receive
	public String getFilename() {
		return filename;
	}
	// Filename to be stored
	public void setFilename(String fname) {
		filename = fname;
	}
	// Default port value
	int my_port = 12987;
	// Get localport value
	public int getLocalPort() {
		return my_port;
	}
	// Set localport value
	public boolean setLocalPort(int port) {
		if(port>=0 && port<65535){
			my_port = port;
			return true;
		}
		return false;
	}
}
