package Skenario3;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Random;

import org.apache.tools.ant.taskdefs.Java;

import com.virtenio.driver.device.at86rf231.AT86RF231;
import com.virtenio.driver.device.at86rf231.AT86RF231RadioDriver;
import com.virtenio.misc.PropertyHelper;
import com.virtenio.misc.StringUtils;
import com.virtenio.preon32.node.Node;
import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.radio.ieee_802_15_4.FrameIO;
import com.virtenio.radio.ieee_802_15_4.RadioDriver;
import com.virtenio.radio.ieee_802_15_4.RadioDriverFrameIO;
import com.virtenio.vm.Time;

import core.Connection;
import core.LinearPredict;
import core.svm;
import core.svm_model;

public class ClusterHead3 {

	// Setting Address
	private static int COMMON_PANID = PropertyHelper.getInt("radio.panid", 0xCAFF);
	private static int[] node_list = new int[] { PropertyHelper.getInt("radio.panid", 0xABFE),
			PropertyHelper.getInt("radio.panid", 0xDAAA), PropertyHelper.getInt("radio.panid", 0xDAAB),
			PropertyHelper.getInt("radio.panid", 0xDAAC), PropertyHelper.getInt("radio.panid", 0xDAAD),
			PropertyHelper.getInt("radio.panid", 0xDAAE) };
	
	// Setting Sensor
	private static final String sensorId = "ClusterHead";
	private static int SN_Addr = node_list[1];
	private static int BS_Addr = node_list[0];
	//jumlah sensor node
	private static int[] nextNode = { node_list[2], node_list[3] };
	private static int[] previousNode = { node_list[0] };
	
	
	public static void main(String[] args) throws Exception {
		//Cek address
		System.out.println("source : " + SN_Addr);
		System.out.println("basestation adr : "+ BS_Addr);
		
		System.out.println(sensorId + " Waiting for task..");
		runs();
	}
	
	
	public static void runs() {
		try {
			AT86RF231 t = Node.getInstance().getTransceiver();
			t.open();
			t.setAddressFilter(COMMON_PANID, SN_Addr, SN_Addr, false);
			final RadioDriver radioDriver = new AT86RF231RadioDriver(t);
			final FrameIO fio = new RadioDriverFrameIO(radioDriver);
			receive(fio);

		} catch (Exception e) {
		}
	}
	
	

	public static void receive(final FrameIO fio) {	
		Thread thread = new Thread() {
			public void run() {
				Frame frame = new Frame();
				Connection connection = new Connection(COMMON_PANID,sensorId, fio, SN_Addr, nextNode, previousNode,new String[0]);
				while (true) {
					try {
						fio.receive(frame);
						byte[] content = frame.getPayload();
						String str = new String(content, 0, content.length);
						
						//cek receive
						System.out.println("String receive : "+str);
						
						// mengantar pesan ke node sebelumnya jika yang diterima bukan perintah
						if (str.charAt(0) != '@' && str.charAt(0) != '#') {
							System.out.println("send to BS : "+ str);
							connection.forwardMessagePrevNode(str);
						}

						// online status
						else if (str.substring(0, 2).equalsIgnoreCase("@1")) {
							System.out.println("Online");
							String res = new String("Sensor Node ONLINE #" + "_");
							long curTime = Long.parseLong(str.substring(2));
							Time.setCurrentTimeMillis(curTime);
							curTime = Time.currentTimeMillis();
							connection.forwardMessageNextNode("@1" + curTime);
							connection.forwardMessagePrevNode("1" + sensorId + " ONLINE #" + curTime);
						}
						// kirim perintah ke node selanjutnya
						else if(str.substring(0, 2).equalsIgnoreCase("@2")) {
							for (int i = 0; i < nextNode.length; i++) {
								try {
									connection.send("@2", SN_Addr , nextNode[i]);
									Thread.sleep(500);
								}
								catch(Exception e) {}
							}
						}
						else if(str.substring(0, 2).equalsIgnoreCase("@3")){
							System.out.println("Extract stop");
							
							connection.forwardMessageNextNode("@3");
							System.exit(0);
						}
						else if (str.substring(0,2).equalsIgnoreCase("@4")) {
							System.out.println("Program stop");
							
							connection.forwardMessageNextNode("@4");
							System.exit(0);
						}
					} catch (IOException e) {
						System.out.println("masuk catch 3");
					}
			}
		}
		}; thread.start();
	}
}
