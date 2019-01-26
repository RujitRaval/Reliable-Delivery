import edu.utulsa.unet.UDPSocket; 
import java.util.*;
import java.io.*;
import java.net.*;

public class RSendUDP extends SenderMethods implements edu.utulsa.unet.RSendUDPI, Runnable{
	public static void main(String[] args){
		RSendUDP obj = new RSendUDP();
		obj.setMode(1);
		obj.setModeParameter(512);
		obj.setTimeout(1000);
		obj.setFilename("important.txt");
		obj.setLocalPort(23456);
		obj.setReceiver(new InetSocketAddress("localhost",32456));
		obj.sendFile();
	}
	int mtu;
	my_msg[] pkt;
	int Last_ack;
	int Last_sent;
	boolean Last_ack_arrived = false;
	boolean All_ack = false;
	long FINTime = 0;
	UDPSocket my_socket;
	public boolean sendFile() {
		try {
			my_socket = new UDPSocket(getLocalPort());
			mtu = my_socket.getSendBufferSize();
			byte[] message = getFileData();
			pkt = readMessage(message);
			Last_ack = -1;
			Last_sent = -1;
			System.out.println("Sending "+this.filename+" from "+my_socket.getLocalAddress().getHostAddress()+":"+this.getLocalPort()+" to "+receiver.toString()+" with "+message.length+" bytes");
			System.out.println("Using "+mode.toString());
			Long Time_Start = System.currentTimeMillis();
			new Thread(this).start();
			while(All_ack == false){
				if((Last_sent-Last_ack)<SWSize && ((Last_sent+1)<pkt.length)){
					my_msg m = pkt[(Last_sent+1)];
					my_socket.send(new DatagramPacket(m.toBytes(), m.toBytes().length, receiver.getAddress(), receiver.getPort()));
					m.timeSent = System.currentTimeMillis();
					Last_sent++;
					System.out.println("Message "+Last_sent+" sent with "+m.data.length+" byes of actual data");
				}
				else if(Timedout()!=null){
					int to_msg = Timedout();
					System.out.println("Message "+to_msg+" timed-out.");
					my_msg m = pkt[to_msg];
					my_socket.send(new DatagramPacket(m.toBytes(), m.toBytes().length, receiver.getAddress(), receiver.getPort()));
					System.out.println("Message "+to_msg+" sent with "+m.data.length+" byes of actual data");
				}
				else if(Last_ack_arrived && (System.currentTimeMillis()-FINTime)>this.getTimeout()){
					my_msg m = new my_msg(new byte[0], -1, timeOut, true);
					my_socket.send(new DatagramPacket(m.toBytes(), m.toBytes().length, receiver.getAddress(), receiver.getPort()));
					FINTime = System.currentTimeMillis();
				}
			}
			Long Time_End = System.currentTimeMillis();
			System.out.println("Successfully sent "+this.filename+" ("+message.length+" bytes) in "+((Time_End-Time_Start)/1000.0)+" seconds");
		}
		catch(Exception e){ 
			System.out.println("Unable to send the file.");
			System.exit(-1);
		}
		return false;
	}
	private synchronized void checkLastACK(){
		for(int i = Math.max(0, Last_ack); i<= Last_sent; i++){
			if(pkt[i].Acked){
				Last_ack = i;
			}
			else{
				return;
			}
		}
	}
	private synchronized Integer Timedout(){
		for(int i = Math.max(0, Last_ack); i<=Last_sent; i++){
			if(pkt[i].Acked==false && ((System.currentTimeMillis()-pkt[i].timeSent)>this.timeOut)){				
				return i;
			}
		}
		return null;
	}
	private synchronized byte[] getFileData(){
		String message = "";
		try {			
			Scanner in = new Scanner(new FileReader(this.filename));
			while(in.hasNextLine()){
				message+="\n"+in.nextLine();
			}
			in.close();
		} 
		catch (FileNotFoundException e) {
			System.out.println("File does not exist.");
			System.exit(-1);
		}
		return message.getBytes();
	}
	public class my_msg extends Message{
		public long timeSent;
		public boolean Acked;
		public my_msg(byte[] my_pkt, int seq, long timeout, boolean isFinished) {
			super(my_pkt, seq, timeout, isFinished, false , false);
		}
	}
	private synchronized my_msg[] readMessage(byte[] message){
		int payload = mtu-Message.headerLength;
		int TotalSegments = (int)Math.ceil(message.length/((double)payload));
		my_msg[] output = new my_msg[TotalSegments];
		for(int i = 0; i< TotalSegments; i++){
			byte[] data = Arrays.copyOfRange(message, i*payload, Math.min((i+1)*payload, message.length));
			output[i] = new my_msg(data, i, timeOut, false);
		}
		return output;
	}
	public void run() {
		while(!Last_ack_arrived  && Last_ack<(pkt.length-1)){
			byte [] buffer = new byte[mtu];
			DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
			try {
				my_socket.receive(packet);
			} catch (Exception e) {
				System.out.println("Unable to receive the file. Exiting.");
				System.exit(-1);
			}
			Message msg = Message.Decode(buffer, packet.getLength());
			if(msg.ack){
				pkt[msg.seq].Acked = true;
				checkLastACK();
				System.out.println("Message "+msg.seq+" acknowledged.");
			}
			if(msg.last)
				Last_ack_arrived = true;
		}
		while(All_ack == false){
			byte [] buffer = new byte[mtu];
			DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
			try {
				my_socket.receive(packet);
			} 
			catch (Exception e) {
				System.out.println("There is an error. Exiting.");
				System.exit(-1);
			}
			Message msg = Message.Decode(buffer, packet.getLength());
			if(msg.ack && msg.finished){
				All_ack = true;
			}
		}
	}
}
