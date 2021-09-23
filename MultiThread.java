import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class MultiThread {
	
	private static int threadCount;
	private static long blocksize;
	private static int runningThreadCount;

	public static void main(String[] args) throws Exception {
		
		System.out.print("Entrez le lien du téléchargement et nombre de partitions: ");
		Scanner file_name = new Scanner(System.in);		
		String path = file_name.nextLine();
		int threadCount = Integer.parseInt(file_name.nextLine());

		URL url = new URL(path);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setConnectTimeout(5000);
		int code = conn.getResponseCode();
		if (code == 200) {
			long size = conn.getContentLength();
			System.out.println("\nTaille du fichier serveur : " + size);
			blocksize = size / threadCount;

			File file = new File("downloadedFile.exe");
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			raf.setLength(size);

			runningThreadCount = threadCount;
			for (int i = 1; i <= threadCount; i++) {
				long startIndex = (i - 1) * blocksize;
				long endIndex = i * blocksize - 1;
				if (i == threadCount) {

					endIndex = size - 1;
				}
				System.out.println("Partion : " + i + " Lieu de téléchargement : " + startIndex + "~" + endIndex);
				new DownloadThread(path, i, startIndex, endIndex).start();
			}
			System.out.println("\n");
		}
		conn.disconnect();
	}

	private static class DownloadThread extends Thread {
		private int threadId;
		private long startIndex;
		private long endIndex;
		private String path;
 
		public DownloadThread(String path, int threadId, long startIndex, long endIndex) {
			this.path = path;
			this.threadId = threadId;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
		}
 
		@Override
		public void run() {
			try {
				
				int total = 0;   
				File positionFile = new File("partion "+threadId + ".txt");   
				URL url = new URL(path);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET"); 

				if (positionFile.exists() && positionFile.length() > 0) {
					FileInputStream fis = new FileInputStream(positionFile);
					BufferedReader br = new BufferedReader(
							new InputStreamReader(fis)); 
					
					String lasttotalstr = br.readLine();          
					int lastTotal = Integer.valueOf(lasttotalstr);
					startIndex += lastTotal;
					total += lastTotal;
					fis.close();
				}
				
				conn.setRequestProperty("Range", "bytes=" + startIndex + "-" + endIndex);  
				conn.setConnectTimeout(5000);
				int code = conn.getResponseCode();
				InputStream is = conn.getInputStream();
				File file = new File("downloadedFile.exe");
				RandomAccessFile raf = new RandomAccessFile(file, "rw");
				
				raf.seek(startIndex);            
				System.out.println("Début de l'écriture de la partition " + threadId + " à la position: " + String.valueOf(startIndex));
				int len = 0;
				byte[] buffer = new byte[512];
				while ((len = is.read(buffer)) != -1) {

					RandomAccessFile rf = new RandomAccessFile(positionFile, "rwd");                           
					raf.write(buffer, 0, len);
					total += len;
					rf.write(String.valueOf(total).getBytes());
					rf.close();
				}
				is.close();
				raf.close();
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {

				synchronized (MultiThread.class) {
					System.out.println("\nPartition " + threadId + " : Téléchargement terminé ");
					runningThreadCount--;
					if (runningThreadCount < 1) {
						System.out.println("\nTous les partitions sont terminés, suppresion des fichiers enregistrés temporairement ");
						for (int i = 1; i <= threadCount; i++) {
							File f = new File(i + ".txt");
							//System.out.println(f.delete());   
						}
					}
				}
 
			}
		}
	}
}