package Skenario2;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import org.apache.tools.ant.taskdefs.Java;

import com.virtenio.driver.device.at86rf231.AT86RF231;
import com.virtenio.driver.device.at86rf231.AT86RF231RadioDriver;
import com.virtenio.driver.usart.USART;
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

public class ClusterHead2 {
	// Setting Address
	private static int COMMON_PANID = PropertyHelper.getInt("radio.panid", 0xCAFF);
	private static int[] node_list = new int[] { PropertyHelper.getInt("radio.panid", 0xABFE),
			PropertyHelper.getInt("radio.panid", 0xDAAA), PropertyHelper.getInt("radio.panid", 0xDAAB),
			PropertyHelper.getInt("radio.panid", 0xDAAC), PropertyHelper.getInt("radio.panid", 0xDAAD),
			PropertyHelper.getInt("radio.panid", 0xDAAE) };
	
	// Setting Sensor
	private static final String sensorId = "ClusterHead1";
	private static int SN_Addr = node_list[1];
	private static int BS_Addr = node_list[0];
	//jumlah sensor node
	private static int[] nextNode = { node_list[3] , node_list[4]};
	private static int[] previousNode = { node_list[0] };
	
	//variable for predict
	private static int fitur = 9;
	private static String res;
	private static int	tempReceive;
	private static String[] fullResult;
	private static String[] fiturEkstraksi;
	public static svm_model model;
		
		
	public static void main(String[] args) throws Exception {			
			//inisialisasi model 
			model = svm.svm_load_model();
			tempReceive = 0;
			fullResult = new String[fitur];
			fiturEkstraksi = new String[fitur];
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
							if(str.substring(0,2).equalsIgnoreCase("#1")) {
								res = "_" + str.substring(2);
								String[] result = StringUtils.split(str,",");
								//cek receive
//								System.out.println("String receive : "+str);
								
								if (tempReceive < fitur) {
									for (int i = 0; i < 3; i++) {
										fullResult[tempReceive+i] = result[i+1];
										fiturEkstraksi[tempReceive+i] = result[i+1];
									}
									tempReceive +=3;
								}
								if(tempReceive== fitur) {
									//cek hasil di console pengiriman
									for (int i = 0; i < fullResult.length; i+=3) {
										System.out.print((i+1) +":" +fullResult[i]);
										System.out.print((i+2) +":" +fullResult[i+1]);
										System.out.println((i+3) +":" +fullResult[i+2]);
									}
									
//									split untuk penanda sensor yang mendeteksi
									String[] sensor = StringUtils.split(str,":");
									res = sensor[1]+ " " + LinearPredict.predict(fullResult, model); 
									tempReceive = 0;
									for (int i = 0; i < previousNode.length; i++) {
										connection.send("2" + res, SN_Addr, previousNode[i]);
										connection.sendEkstraksi(fiturEkstraksi, fitur);
									}
								}
							}
							
							// mengantar pesan ke node sebelumnya jika yang diterima bukan perintah
							else if (str.charAt(0) != '@' && str.charAt(0) != '#') {
								connection.forwardMessagePrevNode(str);
							}

							// online status
							if (str.substring(0, 2).equalsIgnoreCase("@1")) {
								System.out.println("Online");
								String res = new String("Sensor Node ONLINE #" + "_");
								long curTime = Long.parseLong(str.substring(2));
								Time.setCurrentTimeMillis(curTime);
								curTime = Time.currentTimeMillis();
								connection.forwardMessageNextNode("@1" + curTime);
								connection.forwardMessagePrevNode("1" + sensorId + " ONLINE #" + curTime);
							}
							// extracting
							else if(str.substring(0, 2).equalsIgnoreCase("@2")) {
								System.out.println("Extract start");
								Thread extractThread = new Thread(){
									public void run() {
										try {
											for (int i = 0; i < nextNode.length; i++) {
//												Thread.sleep(300); //CH2
												connection.send("@2", SN_Addr, nextNode[i]);
												Thread.sleep(500); //CH1
											}
										}
										catch(Exception e) {
											System.out.println("Error extract thread");
										}
									}
								};extractThread.start();
							}
							else if(str.substring(0, 2).equalsIgnoreCase("@3")){
								System.out.println("Extract stop");
								
								connection.forwardMessageNextNode("@3");
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
