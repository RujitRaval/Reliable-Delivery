import java.nio.ByteBuffer;

public class Message {
	public byte[] data;	
	public static final int headerLength = 10;
	public int seq;
	public long timeout;
	public boolean ack;
	public boolean last;
	public boolean finished;
	public Message(byte[] data, int seq, long timeout, boolean finished, boolean last, boolean ack){
		this.data = data;
		this.seq = seq;
		this.timeout = timeout;
		this.finished = finished;
		this.last = last;
		this.ack = ack;
	}
	public static Message Decode(byte[] encoded, int length){
		byte[] data = new byte[length-headerLength];
		ByteBuffer buff = ByteBuffer.wrap(encoded);
		byte Operation = buff.get();
		boolean	ack = ((((int)Operation)&1)==1);
		boolean	last = ((((int)Operation)&2)==2);
		boolean	finished = ((((int)Operation)&4)==4);
		int seq = buff.getInt();
		long timeout = buff.getInt();
		buff.get(data);
		return new Message(data, seq, timeout, finished, last, ack);
	}
	public byte[] toBytes(){
		byte[] byteData = new byte[data.length+headerLength];
		ByteBuffer buf = ByteBuffer.wrap(byteData);
		byte Operation=0;
		if(ack)
			Operation+=1;
		if(last)
			Operation+=2;
		if(finished)
			Operation+=4;
		buf.put(Operation);
		buf.putInt(seq);
		buf.putInt((int)timeout);
		buf.put(data);
		return byteData;
	}
}
