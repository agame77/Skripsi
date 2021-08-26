package core;

public class Extract {
	
	//variable for extraction
	public String[] nextNodeSign;
	public int[] it; //iterasi untuk ekstraksi
	public float[][] exResult;
	public static final int fitur = 9;
	public Connection connection;
	
	public Extract(Connection connect,String[] nextNode) {
		it = new int[nextNode.length];
		for (int i = 0; i < it.length; i++) {
			it[i] = 0;
		}
		exResult = new float[nextNode.length][fitur];
		this.nextNodeSign = nextNode;
		this.connection = connect;
	}
	
	public void extract(String sensor, float[] tempSense) {
		String tempSensor = sensor;
		for (int k = 0; k < this.nextNodeSign.length; k++) {
			if(tempSensor.equalsIgnoreCase(this.nextNodeSign[k])) {
				if(it[k]>10) {
					for	(int i = 0 ; i < 3 ; i++) {
						exResult[k][i] = exResult[k][i]/it[k];
					}
//					cek fitur dan sensor di console
					System.out.println(";1:"+ exResult[k][0] + ":2:"+ exResult[k][1] + ":3:"+ exResult[k][2]);						
					System.out.println(":4:" + exResult[k][3] + ":5:"+ exResult[k][4] + ":6:"+ exResult[k][5]);
					System.out.println(":7:" + exResult[k][6] + ":8:"+ exResult[k][7] + ":9:"+ exResult[k][8]);
					System.out.println("tempSensorSend : "+tempSensor);
					connection.sendLongMessage(tempSensor, exResult, fitur);
					it[k]=0;
					for (int i = 0; i < exResult[0].length; i++) {
						exResult[k][i] = 0;
					}
				}
				else if(it[k]<=10) {	
					//menjumlahkan nilai dari akselerasi xyz untuk dihitung rata"
					for(int i = 0 ; i < tempSense.length ; i ++) {
						exResult[k][i] += tempSense[i];
						//mencari maximum dari akselerasi xyz
						if(exResult[k][i+3] < tempSense[i] || exResult[k][i+3] == 0) {
							exResult[k][i+3] = tempSense[i];
						}
						//mencari minimum dari akselerasi xyz
						if(exResult[k][i+6] > tempSense[i] || exResult[k][i+6] == 0) {
							exResult[k][i+6] = tempSense[i];
						}
					}
					it[k]++;
				}
			}
		}
	}
	
	public void reset() {
		for (int i = 0; i < nextNodeSign.length; i++) {
			it[i] = 0;
			for (int j = 0; j < exResult[0].length ; j++) {
				exResult[i][j] = 0;
			}
		}
	}
	
}
