package Skenario1;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Random;

import org.apache.tools.ant.taskdefs.Java;

import com.virtenio.driver.device.at86rf231.AT86RF231;
import com.virtenio.driver.device.at86rf231.AT86RF231RadioDriver;
import com.virtenio.misc.PropertyHelper;
import com.virtenio.preon32.node.Node;
import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.radio.ieee_802_15_4.FrameIO;
import com.virtenio.radio.ieee_802_15_4.RadioDriver;
import com.virtenio.radio.ieee_802_15_4.RadioDriverFrameIO;
import com.virtenio.vm.Time;

import core.Accelerometer;
import core.Connection;

public class SensorNode1 {
	private static final Accelerometer sensor = new Accelerometer();

	// Setting Address
	private static int COMMON_PANID = PropertyHelper.getInt("radio.panid", 0xCAFF);
	private static int[] node_list = new int[] { PropertyHelper.getInt("radio.panid", 0xABFE),
			PropertyHelper.getInt("radio.panid", 0xDAAA), PropertyHelper.getInt("radio.panid", 0xDAAB),
			PropertyHelper.getInt("radio.panid", 0xDAAC), PropertyHelper.getInt("radio.panid", 0xDAAD),
			PropertyHelper.getInt("radio.panid", 0xDAAE) };
	
		
	 	// SETTING SENSOR
		private static final String sensorId = "Sensor1";
		private static int SN_Addr = node_list[2];
		private static int BS_Addr = node_list[0];
		private static int[] nextNode = {  };
		private static int[] previousNode = { node_list[1] };	
		
		private static boolean sensing;
		private static Thread sensingThread;
		private static float[] temp;
		private static float[] tempSebelum;
		
		public static void main(String[] args) throws Exception {
			//Cek address
			System.out.println("source : " + SN_Addr);
			System.out.println("basestation adr : "+ BS_Addr);
			
			//inisialisasi temp
			temp = new float[3];
			tempSebelum = new float[3];
			for (int i = 0; i < tempSebelum.length; i++) {
				tempSebelum[i] = 0;
			}
			sensor.init();
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
					Connection connection = new Connection(COMMON_PANID, sensorId, fio, SN_Addr, nextNode, previousNode, null);
					while (true) {
						try {
							fio.receive(frame);
							byte[] content = frame.getPayload();
							String str = new String(content, 0, content.length);
							
							//cek receive
							System.out.println("String receive : "+str);
							
							// mengantar pesan ke node sebelumnya jika yang diterima bukan perintah
							if (str.charAt(0) != '@') {
								connection.forwardMessagePrevNode(str);
							}

							// online status
							if (str.substring(0, 2).equalsIgnoreCase("@1")) {
								try {
									Thread.sleep(100);
									System.out.println("Online");
									String res = new String("Sensor Node ONLINE #" + "_");
									long curTime = Long.parseLong(str.substring(2));
									Time.setCurrentTimeMillis(curTime);
									curTime = Time.currentTimeMillis();
									connection.forwardMessageNextNode("@1" + curTime);
									connection.forwardMessagePrevNode("1" + sensorId + " ONLINE #" + curTime);
								} catch (Exception e) {
								}	
							}
							
							// sensing
							else if(str.substring(0, 2).equalsIgnoreCase("@2")) {
								System.out.println("Sensing start");
								
								sensing = true;
								connection.forwardMessageNextNode("@2");
								
								try {
									tempSebelum = sensor.sense();
									Thread.sleep(300);
								}
								catch(Exception e){	
								}
								sensingThread = new Thread() {
									public void run() {
										try {
											while(sensing) {
												float[] tempCurrent = new float[3];
												temp = sensor.sense();
												for (int i = 0; i < temp.length; i++) {													
													tempCurrent[i] = temp[i];
													temp[i] = Math.abs(temp[i] - tempSebelum[i]);
												}
												String message = "#2" + sensorId+" , " + temp[0] + " , " + temp[1] + " , " + temp[2];
												connection.forwardMessagePrevNode(message);
												
												//delete (buat dokumen)
//												String rawMessage = "2" + sensorId+" , " + temp[0] + " , " + temp[1] + " , " + temp[2];
//												connection.forwardMessagePrevNode(rawMessage);
												
												//print console message
												System.out.println("sensing : " + message);
												
												for (int i = 0; i < temp.length; i++) {
													tempSebelum[i] = tempCurrent[i];
												}
												
												Thread.sleep(300);
											}
										}
										catch(InterruptedException e){
											System.out.println("masuk catch 1");
											e.printStackTrace();
										}
										catch(Exception e) {
											System.out.println("masuk catch 2");
											e.printStackTrace();
										}
									}	
								}; sensingThread.start();
							}
							//stop sensing
							else if(str.substring(0, 2).equalsIgnoreCase("@3")){
								System.out.println("Sensing Stop");
								connection.forwardMessageNextNode("@3");
								sensing = false;
								System.exit(0);
							}
							//stop program
							else if (str.substring(0,2).equalsIgnoreCase("@4")) {
								System.out.println("Program Stop");
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
