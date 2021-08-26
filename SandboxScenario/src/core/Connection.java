package core;
import java.util.Arrays;

import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.radio.ieee_802_15_4.FrameIO;

public class Connection {
	public int COMMON_PANID;
	public FrameIO fio;
	public int SN_Addr;
	public int[] nextNode;
	public int[] previousNode;
	public String[] nextNodeSign;
	public String sensorID;
	
	public Connection(int COMMON_PANID,String sensorID,FrameIO fio,int sensorAddr,int[] nextNode,int[] prevNode,String[] nextNodeSign) {
		this.COMMON_PANID = COMMON_PANID;
		this.fio = fio;
		this.SN_Addr = sensorAddr;
		this.nextNode = nextNode;
		this.previousNode = prevNode;
		this.nextNodeSign = nextNodeSign;
		this.sensorID = sensorID;
	}
	 
	public void send(String msg, int src, int dest) {
		int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.ACK_REQUEST
				| Frame.SRC_ADDR_16;
		final Frame sentFrame = new Frame(frameControl);
		sentFrame.setDestPanId(COMMON_PANID);
		sentFrame.setDestAddr(dest);
		sentFrame.setSrcAddr(src);
		sentFrame.setPayload(msg.getBytes());
		try {
			fio.transmit(sentFrame);
		} catch (Exception e) {
		}
	}
	
	private void forwardMessage(String msg,int[]node) {
		for (int i = 0; i < node.length; i++) {
			send(msg, SN_Addr, node[i]);
		}
	}
	
	public void forwardMessageNextNode(String msg) {
		this.forwardMessage(msg, nextNode);
	}
	
	public void forwardMessagePrevNode(String msg) {
		this.forwardMessage(msg, previousNode);
	}
	
	public void sendEkstraksi(String[] msg, int fitur ) {
		String[] temp = msg;
		System.out.println("send ekstraksi");
		for (int i = 0; i < fitur; i+=3) {
			String message = "2" + temp[i] + " " + temp[i+1] + " " + temp[i+2];
			forwardMessagePrevNode(message);
		}
	}
	

	public void sendLongMessage(String sensor, float[][] Result, int fitur) {
		String tempSensor = sensor;
		float[][] tempResult = Result;
	for (int k = 0; k < nextNodeSign.length; k++) {
			if(sensor.equalsIgnoreCase(nextNodeSign[k])) {
				for (int i = 0; i < fitur; i+=3) {
					String message = "#1" + sensorID+ ":" + tempSensor + ": ," + tempResult[k][i] + "," + tempResult[k][i+1] + "," + tempResult[k][i+2];
					
					//delete (buat dokumen)
//					String message = "2" + sensorID+ ":" + tempSensor + ": ," + tempResult[k][i] + "," + tempResult[k][i+1] + "," + tempResult[k][i+2];

					forwardMessagePrevNode(message);
				}
			}
		}
	}
}

