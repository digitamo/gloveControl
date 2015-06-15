import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class TwoWaySerialComm {
	public int thredShod = 5;
	Thread reader;
	Thread filter;
	private List<String> inputStack; // FIFO Stack for the sent input to be
										// Filtered.
	private JSONObject jsonObject; // JSON object to be processed.
	private int oldXFlex;
	private int oldYFlex;
	private int currentX;
	private int currentY;
	private int AccThredshold;
	private int flexThredshold;

	public TwoWaySerialComm() {
		super();
		inputStack = new ArrayList<String>();
		oldXFlex = 800;
		oldYFlex = 780;
		flexThredshold = 50;
		PointerInfo a = MouseInfo.getPointerInfo();
		Point b = a.getLocation();
		currentX = (int) b.getX();
		currentY = (int) b.getY();
		System.out.println("data: " + b.getX() + ", " + b.getY());
		a = null;
		b = null;
		;
		AccThredshold = 10;

		// flexThredshold = 15;
	}

	void connect(String portName) throws Exception {
		CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
		if (portIdentifier.isCurrentlyOwned()) {
			System.out.println("Error: Port is currently in use");
		} else {
			CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);

			if (commPort instanceof SerialPort) {
				SerialPort serialPort = (SerialPort) commPort;
				serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);

				InputStream in = serialPort.getInputStream();
				// OutputStream out = serialPort.getOutputStream(); // Don't
				// need it.

				reader = (new Thread(new SerialReader(in, this)));
				reader.start();
				// (new Thread(new SerialWriter(out))).start(); // Don't need
				// it.

				filter = (new Thread(new Filter(this)));
				filter.start();

			} else {
				System.out.println("Error: Only serial ports are handled by this example.");
			}
		}

	}

	public synchronized void Process() {
		// TODO: make this method in separate threads
		System.out.println("processing :" + jsonObject.toString());
		try {
			System.out.println(getValuesToMove()[0]);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/**
		 * This thread is meant to move the mouse at slower rate given the start
		 * point and the end point
		 * 
		 * @author Osama
		 *
		 */
		class Process implements Runnable {
			// private JSONObject jsonObject;
			Robot pointer;

			// int movSpeed;

			public Process() {
				// this.jsonObject = TwoWaySerialComm.this.jsonObject;
				// PointerInfo a = MouseInfo.getPointerInfo();
				// Point b = a.getLocation();
				// oldX = (int) b.getX();
				// oldY = (int) b.getY();
				// movSpeed = 2;
				try {
					pointer = new Robot();
				} catch (AWTException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void run() {
				int[] valuesToMove;
				try {
					valuesToMove = getValuesToMove();
					System.out.println("to move x : " + valuesToMove[0] + " to move y: " + valuesToMove[1]);
					if (Math.abs(valuesToMove[0]) > AccThredshold || Math.abs(valuesToMove[1]) > AccThredshold) {
						currentX += valuesToMove[0];
						currentY += valuesToMove[1];
						pointer.mouseMove(currentX, currentY);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
				// TODO: add threshold.

				try {
					if (Math.abs(jsonObject.getInt("XFlex") - oldXFlex) > flexThredshold) {
						pointer.mousePress(InputEvent.BUTTON1_DOWN_MASK);
						pointer.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					}

					if (Math.abs(jsonObject.getInt("YFlex") - oldYFlex) > flexThredshold) {
						pointer.mousePress(InputEvent.BUTTON3_DOWN_MASK);
						pointer.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
					}

					oldXFlex = jsonObject.getInt("XFlex");
					oldYFlex = jsonObject.getInt("YFlex");
				} catch (JSONException e) {
					e.printStackTrace();
				}

			}
		}

		new Thread(new Process()).start();

		// try {
		// Robot pointer = new Robot();
		// int[] valuesToMove = getValuesToMove();
		//
		// if (valuesToMove[0] > AccThredshold || valuesToMove[1] >
		// AccThredshold) {
		// // NOTE: better handle each of them alone.
		//
		// pointer.mouseMove(valuesToMove[0], valuesToMove[1]);
		// }
		//
		// } catch (JSONException e) {
		// e.printStackTrace();
		// } catch (AWTException e) {
		// e.printStackTrace();
		// }

		// -------------------------------------------------------

		// if (Math.abs(jsonObject.getInt("XFlex") - XFlexVal) >
		// flexThredshold){
		// pointer.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		// pointer.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		//
		// // TODO: Add drag and drop.
		// }
		//
		// if (Math.abs(jsonObject.getInt("YFlex") - YFlexVal) >
		// flexThredshold){
		// pointer.mousePress(InputEvent.BUTTON2_DOWN_MASK);
		// pointer.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
		// }

		// ---------------------------------------------------------

	}

	/**
	 * Calculate the values we need to move according to the difference between
	 * old and new values.
	 * 
	 * @return
	 * @throws JSONException
	 */
	public int[] getValuesToMove() throws JSONException {
		int[] values = new int[2];
		PointerInfo info = MouseInfo.getPointerInfo();
		Point p = info.getLocation();
		currentX = (int) p.getX();
		currentY = (int) p.getY();

		int AcX = jsonObject.getInt("AcX");
		int AcY = jsonObject.getInt("AcY");
		values[0] = (AcX - currentX);
		values[1] = (AcY - currentY);

		return values;
	}

	public void pushToInputStack(String string) {
		inputStack.add(string);
		// NOTE: the first couple of string are null.
	}

	public String popupFromInputStack() {
		if (!inputStack.isEmpty()) {
			return inputStack.remove(0);
		} else {
			return null;
		}

	}

	public void setJsonObject(JSONObject jsonObject) {
		this.jsonObject = jsonObject;
		// NOTE: option 1: call the process method now.
		// option 2: create a process thread which is responsible for checking
		// jsonObject and also clamping.

		// XXX: temporary option.
		this.Process();
	}

	public static void main(String[] args) {
		try {
			(new TwoWaySerialComm()).connect("COM3");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static class SerialReader implements Runnable {
		private InputStream in;
		private TwoWaySerialComm twoWaySerialComm;

		public SerialReader(InputStream in, TwoWaySerialComm twoWaySerialComm) {
			this.in = in;
			this.twoWaySerialComm = twoWaySerialComm;
			System.out.println("SerialReader initiated");
		}

		public void run() {
			byte[] buffer = new byte[1024];
			int len = -1;
			try {
				while ((len = this.in.read(buffer)) > -1) {
					String text = new String(buffer, 0, len);
					// filter(text);
					twoWaySerialComm.pushToInputStack(text);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * 
		 * Takes the Json text parts and filter them then passes a JSONObject to
		 * be processed
		 * 
		 * @param text
		 *            the text to filter
		 * @throws JSONException
		 */
		/*
		 * public void filter(String text) throws JSONException { boolean
		 * hasOpen = text.contains("{"); // the text has opened // braces
		 * boolean hasClose = text.contains("}"); // the text has closed //
		 * braces
		 * 
		 * if (hasOpen && hasClose) { // contains both braces
		 * 
		 * if (text.indexOf("{") > text.indexOf("}")) { // the data is complete
		 * if (text.length() == (text.indexOf("}") + 1)) { JSONObject JsObject =
		 * new JSONObject(text); Process(JsObject); } else { input = new
		 * StringBuffer(text.substring((text .indexOf("}") + 1))); } } else { //
		 * append the data and create a new string
		 * 
		 * input.append(text.substring(text.indexOf("}"))); JSONObject JsObject
		 * = new JSONObject(input.toString()); Process(JsObject); input = new
		 * StringBuffer((text.indexOf("}") + 1)); } } else if (hasOpen) { input
		 * = new StringBuffer(text); } else if (hasClose) { // TODO: check if
		 * null
		 * 
		 * input.append(text); JSONObject JsObject = new
		 * JSONObject(input.toString()); Process(JsObject); } else { if (input
		 * != null) input.append(text); } }
		 */

		/**
		 * 
		 * @param jsonObject
		 *            Json object contains the data of the input data.
		 */
		/*
		 * public void Process(JSONObject jsonObject) { // TODO: make this
		 * method in separate threads // System.out.println("processing :" +
		 * jsonObject.toString());
		 * 
		 * try { Robot pointer = new Robot();
		 * 
		 * if (Math.abs(jsonObject.getInt("AcX") - AcX) > AccThredshold ||
		 * Math.abs(jsonObject.getInt("AcY") - AcY) > AccThredshold) { // NOTE:
		 * better handle each of them alone.
		 * 
		 * AcX = jsonObject.getInt("AcX"); AcY = jsonObject.getInt("AcY");
		 * 
		 * pointer.mouseMove(AcX, AcY); }
		 * 
		 * // if (Math.abs(jsonObject.getInt("XFlex") - XFlexVal) > //
		 * flexThredshold){ // pointer.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		 * // pointer.mouseRelease(InputEvent.BUTTON1_DOWN_MASK); // // // TODO:
		 * Add drag and drop. // } // // if (Math.abs(jsonObject.getInt("YFlex")
		 * - YFlexVal) > // flexThredshold){ //
		 * pointer.mousePress(InputEvent.BUTTON2_DOWN_MASK); //
		 * pointer.mouseRelease(InputEvent.BUTTON2_DOWN_MASK); // } } catch
		 * (JSONException e) { e.printStackTrace(); } catch (AWTException e) {
		 * e.printStackTrace(); } }
		 */
	}

	/** */
	public static class SerialWriter implements Runnable {
		OutputStream out;

		public SerialWriter(OutputStream out) {
			this.out = out;
		}

		public void run() {
			try {
				int c = 0;
				while ((c = System.in.read()) > -1) {
					this.out.write(c);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

class Filter implements Runnable {
	private TwoWaySerialComm twoWaySerialComm;
	private StringBuffer input;

	public Filter(TwoWaySerialComm t) {
		super();
		System.out.println("Filter initiated");
		this.twoWaySerialComm = t;
	}

	public void run() {

		while (true) {
			String text = twoWaySerialComm.popupFromInputStack();

			if (text != null) {

				try {
					filter(text);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else {
				// System.out.println("null recived");
				// TODO: wait()
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void filter(String text) throws JSONException {
		// System.out.println(text);
		boolean hasOpen = text.contains("{"); // the text has opened
												// braces
		boolean hasClose = text.contains("}"); // the text has closed
												// braces

		if (hasOpen && hasClose) {
			// contains both braces

			if (text.indexOf("{") < text.indexOf("}")) { // !!
				// the data is complete
				if (text.length() == (text.indexOf("}") + 1)) {
					JSONObject JsObject = new JSONObject(text);
					// Process(JsObject);
					twoWaySerialComm.setJsonObject(JsObject);
					twoWaySerialComm.Process();
				} else {
					input = new StringBuffer(text.substring(0, (text.indexOf("}") + 1)));

					JSONObject JsObject = new JSONObject(input);
					// Process(JsObject);
					twoWaySerialComm.setJsonObject(JsObject);
					twoWaySerialComm.Process();

					input = new StringBuffer(text.substring((text.indexOf("}") + 1)).trim());
				}
			} else {
				// append the data and create a new string

				input.append(text.substring(0, (text.indexOf("}") + 2)));
				JSONObject JsObject = new JSONObject(input.toString());
				// Process(JsObject);
				twoWaySerialComm.setJsonObject(JsObject);
				twoWaySerialComm.Process();
				input = new StringBuffer(text.substring(text.indexOf("{")));
			}
		} else if (hasOpen) {
			input = new StringBuffer(text);
		} else if (hasClose) {
			// TODO: check if null

			input.append(text);
			JSONObject JsObject = new JSONObject(input.toString());
			// Process(JsObject);
			twoWaySerialComm.setJsonObject(JsObject);
			twoWaySerialComm.Process();
		} else {
			if (input != null)
				input.append(text);
		}
	}
}
