package Skenario3;
import java.io.IOException;
//import java.io.OutputStream;
//import java.util.Date;
//import java.util.Random;
//
//import org.apache.tools.ant.taskdefs.Java;
import java.util.Arrays;

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

//import core.Accelerometer;
import core.Connection;
import core.LinearPredict;
import core.svm_model;
import core.svm;

public class SensorNode3_1 {			
	// Setting Address
	private static int COMMON_PANID = PropertyHelper.getInt("radio.panid", 0xCAFF);
	private static int[] node_list = new int[] { PropertyHelper.getInt("radio.panid", 0xABFE),
			PropertyHelper.getInt("radio.panid", 0xDAAA), PropertyHelper.getInt("radio.panid", 0xDAAB),
			PropertyHelper.getInt("radio.panid", 0xDAAC), PropertyHelper.getInt("radio.panid", 0xDAAD),
			PropertyHelper.getInt("radio.panid", 0xDAAE) };
	
	// Setting Sensor 
	private static final String sensorId = "Sensor1";
	private static int SN_Addr = node_list[2];
	private static int BS_Addr = node_list[0];
	private static int[] nextNode = { node_list[4] };
	private static int[] previousNode = { node_list[1] };
	
	//variable for predict
	private static int fitur = 9;
	private static String res;
	private static int	tempReceive;
	private static String[] fullResult;
	private static String[] fiturEkstraksi;
	public static svm_model model;
	
	public static void main(String[] args) throws Exception {
		//Cek address
		System.out.println("source : " + SN_Addr);
		System.out.println("basestation adr : "+ BS_Addr);
		
		model = svm.svm_load_model();
		tempReceive = 0;
		fullResult = new String[fitur];
		fiturEkstraksi = new String[fitur];
		
//			sensor.init();
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
				
				//sensor pada extract dan connection bergantung skenario
				Connection connection = new Connection(COMMON_PANID, sensorId, fio, SN_Addr, nextNode, previousNode, new String[]{sensorId});
				
				while (true) {
					try {
						fio.receive(frame);
						byte[] content = frame.getPayload();
						String str = new String(content, 0, content.length);
						
						//cek receive
						System.out.println("String receive : "+str);
						
						if(str.substring(0,2).equalsIgnoreCase("#1")) {
							res = "_" + str.substring(2);
							String[] result = StringUtils.split(str,",");
							
							if (tempReceive < fitur) {
								for (int i = 0; i < 3; i++) {
									fullResult[tempReceive+i] = result[i+1];
									fiturEkstraksi[tempReceive+i] = result[i+1];
								}
								tempReceive +=3;
							}
							if(tempReceive== fitur) {
//									split untuk penanda sensor yang mendeteksi
								String[] sensor = StringUtils.split(str,":");
								res = sensor[1]+ " " + LinearPredict.predict(fullResult, model) ;
								tempReceive = 0;
								connection.forwardMessagePrevNode("2" + res);
								for (int i = 0; i < previousNode.length; i++) {
									connection.sendEkstraksi(fiturEkstraksi, fitur);
								}
							}
						}
						
						// mengantar pesan ke node sebelumnya jika yang diterima bukan perintah
						else if (str.charAt(0) != '@') {
							connection.forwardMessagePrevNode(str);
						}

						// online status
						else if (str.substring(0, 2).equalsIgnoreCase("@1")) {
							try {
								Thread.sleep(100);
								System.out.println("Online");
								long curTime = Long.parseLong(str.substring(2));
								Time.setCurrentTimeMillis(curTime);
								curTime = Time.currentTimeMillis();
								connection.forwardMessageNextNode("@1" + curTime);
								connection.forwardMessagePrevNode("1" + sensorId + " ONLINE #" + curTime);
							}
							catch(Exception e) {
							}
						}
						
						// sensing
						else if(str.substring(0, 2).equalsIgnoreCase("@2")) {
							System.out.println("Sensing start");
							connection.forwardMessageNextNode("@2");
						}
						//stop sensing
						else if(str.substring(0, 2).equalsIgnoreCase("@3")){
							connection.forwardMessageNextNode("@3");
							System.exit(0);
						}
						//stop program
						else if (str.substring(0,2).equalsIgnoreCase("@4")) {
							connection.forwardMessageNextNode("@4");
							System.exit(0);
						}
					} catch (IOException e) {
						System.out.println("masuk catch");
					}
			}
		}
		}; thread.start();
	}
}
