
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

import com.virtenio.commander.io.DataConnection;
import com.virtenio.commander.toolsets.preon32.Preon32Helper;
import com.virtenio.vm.Time;


public class Tester {
	private static Thread threadSensing;
	private static BufferedInputStream in;
	private static DataConnection conn;
	private static boolean isSensing;

	public static void main(String[] args) throws Exception {

		
//		// init BaseStation
		Tester tester = new Tester();
		tester.context_set("Scenario0_BS");
		tester.time_synchronize();

		Preon32Helper nodeHelper = new Preon32Helper("COM3", 115200);
		conn = nodeHelper.runModule("BaseStation");
		in = new BufferedInputStream(conn.getInputStream());
		conn.flush();

		System.out.println("\n\n");

		// Input
		Scanner sc = new Scanner(System.in);

		while (true) {
			if(!isSensing) {
			System.out.println(
					"INPUT OPTION \n 1 : Cek Online Node \n 2 : Prediksi \n 3 : Stop Prediksi \n 4 : Stop Program \n Enter option number : ");
			}
			int input = sc.nextInt();
			
			
			if (isSensing) {
				if (input == 1 || input == 2 || input == 4) {
					System.out.println("DALAM KEADAAN PREDIKSI.. UNTUK BERHENTI GUNAKAN OPSI '3' ");
					input = 0;
				} else {
					conn.write(input);
				}
			} else {
				conn.write(input);
			}
			
			byte[] buffer = new byte[1024];
			if(input == 1) {
				System.out.println("ONLINE NODE :");
				Thread.sleep(1500);
				while (in.available() > 0) {
					in.read(buffer);
					conn.flush();
					//mengambil kembalian dari bs
					String s = new String(buffer);
					String[] res = s.split("_");
					
					for (int i = 0; i < res.length; i++) {
						if (res[i].length() != 0) {
							if (res[i].charAt(0) != '@') {
								String[] temp = res[i].split("#");
								if (temp.length > 1)
									System.out.println("> " + temp[0] + "Time: " + formatTimetoString(Long.parseLong(temp[1])));
							}
						}
					}
				}
				System.out.println(" - CEK SELESAI - \n");
			}
			else if (input == 2) {
				isSensing = true;
				System.out.println("- MULAI PREDIKSI - \n");
				if (threadSensing == null) {
					threadSensing = new Thread() {
						public void run() {
							while (isSensing) {
								byte[] buffer = new byte[1024];
								try {
									if (in.available() > 0) {
										in.read(buffer);
										conn.flush();
										String s = new String(buffer);
										
										String[] res = s.split("_");
										boolean aktivitas = false;
										for (int i = 0; i < res.length; i++) {
											if(res[i].length() != 0) {
												String[] temp = res[i].split(" ");
												if(temp.length > 1) {
													if(temp[0].charAt(0) == 's' || temp[0].charAt(0) == 'S' ) {
														System.out.println(temp[0] + " : " + temp[1]);
														savePredict(temp[0] + " : " + temp[1]);
													}
													else {
														System.out.println(temp[0] + " , " + temp[1] + " , " + temp[2]);
														savePredict(temp[0] + " , " + temp[1] + " , " + temp[2]);
													}
												}	
											}
										}			
									} 
								} catch (IOException e) {
								}
							}
						}
					}; threadSensing.start();
				}
			}
			else if (input == 3) {
				if (isSensing) {
					System.out.println("STOP PREDIKSI.. MENUNGGU SEMUA SENSOR STOP SENSING..");
					isSensing = false;
					threadSensing = null;
					Thread.sleep(1000);
					System.out.println("- STOP PREDIKSI SELESAI -\n");
					savePredict("-------------------------------------------------------------------------------------");
					savePredict("");
				} else {
					System.out.println("TIDAK DALAM KEADAAN PREDIKSI..");
				}
			}
			else if (input == 4) {
				System.out.println("MENGHENTIKAN PROGRAM..");
				isSensing = false;
				threadSensing = null;
				Thread.sleep(1000);
				System.out.println(" - PROGRAM DIMATIKAN - ");
				System.exit(0);
			} else {
				System.out.println("INPUT SALAH");
			}

		}

	}
	
	private static String formatTimetoString(long timeInMillis) {
		Date date = new Date(timeInMillis);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
		return simpleDateFormat.format(date);
	}
	
	private void context_set(String target) throws Exception {
		DefaultLogger consoleLogger = getConsoleLogger();
		File buildFile = new File("D:\\Eclipse\\workspace\\SandboxScenario2\\buildUser.xml");
		Project antProject = new Project();
		antProject.setUserProperty("ant.file", buildFile.getAbsolutePath());
		antProject.addBuildListener(consoleLogger);
		try {
			antProject.fireBuildStarted();
			antProject.init();
			ProjectHelper helper = ProjectHelper.getProjectHelper();
			antProject.addReference("ant.ProjectHelper", helper);
			helper.parse(antProject, buildFile);
			antProject.executeTarget(target);
			antProject.fireBuildFinished(null);
		} catch (BuildException e) {
		}
	}
	
	// ant build time synchronize
		private void time_synchronize() throws Exception {
			DefaultLogger consoleLogger = getConsoleLogger();
			File buildFile = new File("D:\\Eclipse\\workspace\\SandboxScenario2\\build.xml");
			Project antProject = new Project();
			antProject.setUserProperty("ant.file", buildFile.getAbsolutePath());
			antProject.addBuildListener(consoleLogger);
			try {
				antProject.fireBuildStarted();
				antProject.init();
				ProjectHelper helper = ProjectHelper.getProjectHelper();
				antProject.addReference("ant.ProjectHelper", helper);
				helper.parse(antProject, buildFile);
				String target = "cmd.time.synchronize";
				antProject.executeTarget(target);
				antProject.fireBuildFinished(null);
			} catch (BuildException e) {
			}
		}

		// console build ant
		private static DefaultLogger getConsoleLogger() {
			DefaultLogger consoleLogger = new DefaultLogger();
			consoleLogger.setErrorPrintStream(System.err);
			consoleLogger.setOutputPrintStream(System.out);
			consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
			return consoleLogger;
		}
		
		//save data
		private static void savePredict(String msg) {
			File resFolder = new File("PredictResult");
			if (!resFolder.exists()) {
				resFolder.mkdir();
			}
			
			Date date = new Date(System.currentTimeMillis());
			String path = resFolder.getAbsolutePath() + "\\Prediksi-"
					+ new SimpleDateFormat("yyyy-MM-dd").format(date) + ".txt";
			try {
				FileWriter fileWriter = new FileWriter(path, true);
				BufferedWriter buffWriter = new BufferedWriter(fileWriter);
				File dataFile = new File(path);

				buffWriter.append(msg + "\n");
				buffWriter.close();
				fileWriter.close();
			} catch (Exception e) {
			}
		}
}


