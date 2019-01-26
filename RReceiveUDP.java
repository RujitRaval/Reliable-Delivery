import edu.utulsa.unet.UDPSocket;
import java.net.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;


public class RReceiveUDP extends ReceiverMethods implements edu.utulsa.unet.RReceiveUDPI, Runnable{
	public static void main(String[] args)
	{
		RReceiveUDP obj = new RReceiveUDP();
		obj.setMode(1);
		obj.setModeParameter(100);
		obj.setFilename("less_important.txt");
		obj.setLocalPort(32456);
		obj.receiveFile();
	}
	public UDPSocket my_socket;
	ArrayList<byte[]> pkt = new ArrayList<byte[]>();
	int mtu;
	long time_out;
	public boolean receiveFile() {
		try{
			my_socket = new UDPSocket(getLocalPort());
			mtu = my_socket.getSendBufferSize();
			System.out.println("Receiving "+this.filename+" at "+my_socket.getLocalAddress().getCanonicalHostName()+":"+this.getLocalPort());
			System.out.println("Using "+mode.toString());
			Long Time_Start = System.currentTimeMillis();
			Thread t = new Thread(this);
			t.start();
			time_out = Long.MAX_VALUE;
			while(time_out>System.currentTimeMillis()){
				Thread.sleep(10);						
			} 
			int totalBytes = 0;
			for(byte[] b :pkt){
				totalBytes +=b.length;
			}
			byte[] message = new byte[totalBytes];
			ByteBuffer buf = ByteBuffer.wrap(message);
			for(byte[] b: pkt){
				buf.put(b);
			}
			Long Time_End = System.currentTimeMillis();
			System.out.println("Successfully received "+this.filename+" ("+message.length+" bytes) in "+((Time_End-Time_Start)/1000.0)+" seconds");
			PrintWriter writer = new PrintWriter(this.filename);
			writer.print(new String(message));
			writer.close();
		}
		catch(Exception e){ 
			System.out.println("Unable to receive the file. Exiting.");
			System.exit(-1);
		}
		return false;
	}
	InetSocketAddress sender;
	long msgtimeout;
	int originalsize;
	public void run() {
		while(time_out>System.currentTimeMillis()){		
			try {
				byte[] buffer = new byte[mtu];
				DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
				my_socket.receive(packet);
				Message msg = Message.Decode(buffer, packet.getLength());
				if(!(time_out>System.currentTimeMillis()))
					return;
				
				if(sender==null){
					sender = new InetSocketAddress(packet.getAddress(),packet.getPort());
					System.out.println("Connection established with sender "+sender.toString());
					msgtimeout = msg.timeout;
				}
				if(msg.finished == false){
					while((pkt.size()-1)<msg.seq)
						pkt.add(null);
					pkt.set(msg.seq, msg.data);
					System.out.println("Message "+ msg.seq+" received with "+msg.data.length+" bytes of actual data.");
					
					int payload = mtu-Message.headerLength;
					if(msg.data.length == payload){
						Message ack = new Message(new byte[0], msg.seq, msgtimeout, false, false, true);
						my_socket.send(new DatagramPacket(ack.toBytes(), ack.toBytes().length, sender.getAddress(), sender.getPort()));
						System.out.println("Message "+ack.seq+" acknowledgement sent.");
						
					}
					else{
						Message ack = new Message(new byte[0], msg.seq, msgtimeout, false, true, true);
						my_socket.send(new DatagramPacket(ack.toBytes(), ack.toBytes().length, sender.getAddress(), sender.getPort()));
						System.out.println("Message "+ack.seq+" acknowledgement sent.");
						//System.out.println("Flag true");
					}
				}
				else{	
					Message ack = new Message(new byte[0], -1, msgtimeout, true, true, true);
					my_socket.send(new DatagramPacket(ack.toBytes(), ack.toBytes().length, sender.getAddress(), sender.getPort()));
					time_out = System.currentTimeMillis(); 
				}
			} 
			catch (Exception e) {
				System.out.println("There was an error. Exiting.");
				System.exit(-1);
			}
		}
	}
}