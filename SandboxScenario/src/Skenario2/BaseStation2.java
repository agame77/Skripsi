package Skenario2;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.tools.ant.taskdefs.SendEmail;

import com.virtenio.driver.device.at86rf231.AT86RF231;
import com.virtenio.driver.device.at86rf231.AT86RF231RadioDriver;
import com.virtenio.driver.timer.NativeTimer;
import com.virtenio.driver.timer.Timer;
import com.virtenio.driver.timer.TimerException;
import com.virtenio.driver.usart.NativeUSART;
import com.virtenio.driver.usart.USART;
import com.virtenio.driver.usart.USARTException;
import com.virtenio.driver.usart.USARTParams;
import com.virtenio.misc.PropertyHelper;
import com.virtenio.misc.StringUtils;
import com.virtenio.preon32.examples.common.USARTConstants;
import com.virtenio.preon32.node.Node;

import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.radio.ieee_802_15_4.FrameIO;
import com.virtenio.radio.ieee_802_15_4.RadioDriver;
import com.virtenio.radio.ieee_802_15_4.RadioDriverFrameIO;
import com.virtenio.vm.Time;
import com.virtenio.vm.event.AsyncEvent;
import com.virtenio.vm.event.AsyncEventHandler;
import com.virtenio.vm.event.AsyncEvents;
import core.*;

public class BaseStation2 {
	
	private static USART usart;
	private static OutputStream out;
	

	// Setting Address
	private static int COMMON_PANID = PropertyHelper.getInt("radio.panid", 0xCAFF);
	private static int[] node_list = new int[] { PropertyHelper.getInt("radio.panid", 0xABFE),
			PropertyHelper.getInt("radio.panid", 0xDAAA), PropertyHelper.getInt("radio.panid", 0xDAAB),
			PropertyHelper.getInt("radio.panid", 0xDAAC), PropertyHelper.getInt("radio.panid", 0xDAAD),
			PropertyHelper.getInt("radio.panid", 0xDAAE) };
	
	private static int BS_Addr = node_list[0];

	private static int[] connectedNodeAddr = new int[] { node_list[1] , node_list[2]};


	private static String res;
	
	public static void main(String[] args) throws Exception {
		usart = configUSART();
		out = usart.getOutputStream();
		
		
		new Thread() {
			public void run() {
				runs();
			}
		}.start();
	}
	
	
	public static void runs() {

		try {
			AT86RF231 t = Node.getInstance().getTransceiver();
			t.open();
			t.setAddressFilter(COMMON_PANID, BS_Addr, BS_Addr, false);
			final RadioDriver radioDriver = new AT86RF231RadioDriver(t);
			final FrameIO fio = new RadioDriverFrameIO(radioDriver);
			

			Connection connection = new Connection(COMMON_PANID, "", fio, BS_Addr, connectedNodeAddr, new int [0], new String[0]);
			new Thread() {
				public void run() {
					while (true) {
						String res;
						int input;
						try {
							input = usart.read();
							if (input == 1) {
								long curTime = Time.currentTimeMillis();
								res = new String("_BaseStation ONLINE #" + curTime + "_");
								out.write(res.getBytes());
								usart.flush();
								for (int i = 0; i < connectedNodeAddr.length; i++) {
									connection.send("@1" + curTime, BS_Addr, connectedNodeAddr[i]);
								} 
								
							} else if (input == 2) {
								for (int i = 0; i < connectedNodeAddr.length; i++) {
									connection.send("@2", BS_Addr, connectedNodeAddr[i]);
								}
								
							} else if (input == 3) {
								new Thread() {
									public void run() {
										int stop = 0;
										while (stop < 5) {
											try {
												for (int i = 0; i < connectedNodeAddr.length; i++) {
													connection.send("@3", BS_Addr, connectedNodeAddr[i]);
												}
												stop++;
												Thread.sleep(100);
											}catch(Exception e) {}
										}
									}
								}.start();
								usart = configUSART();
								out = usart.getOutputStream();
							} else if (input == 4) {
								for (int i = 0; i < connectedNodeAddr.length; i++) {
									connection.send("@4", BS_Addr, connectedNodeAddr[i]);
								}
							}
							Thread.sleep(50);
						} catch (USARTException e) {
						} catch (IOException e) {
						} catch (InterruptedException e) {
						}
					}
				}
			}.start();

			new Thread() {
				public void run() {
					receive(fio);
				}
			}.start();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void receive(final FrameIO fio) {
		while (true) {
			Frame frame = new Frame();
			try {
				fio.receive(frame);
				byte[] content = frame.getPayload();
				String str = new String(content, 0, content.length);
				// done
				
				
				if (str.charAt(0) == '1' || str.charAt(0) == '2') {
					res = "_" + str.substring(1) + "_";
					try {
						out.write(res.getBytes());
						usart.flush();
					} catch (USARTException e) {
					}
				}
			} catch (IOException e) {
			} 
		}

	}
	

	private static USART configUSART() {
		USARTParams params = USARTConstants.PARAMS_115200;
		NativeUSART usart = NativeUSART.getInstance(0);
		try {
			usart.close();
			usart.open(params);
			return usart;
		} catch (Exception e) {
			return null;
		}
	}
}