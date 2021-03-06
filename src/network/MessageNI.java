package network;

import java.io.*;
import java.net.*;
import java.util.*;
import services.*;

public class MessageNI extends Thread {
	
	private static MessageNI instance;
	
	public static MessageNI getInstance(){
		if(instance == null)
			instance = new MessageNI();
		return instance;
	}

	private Stack<MessAddress> sendMsgStack;
	private Stack<DatagramPacket> receivePacketStack; 
	private UDPSender udpSender;
	private UDPReceiver udpReceiver;

	
	private MessageNI(){
		sendMsgStack = new Stack<MessAddress>();// messages � envoyer
		receivePacketStack = new Stack<DatagramPacket>();// packet recus
		
		// initialising the sockets
		udpSender = new UDPSender();
		udpReceiver = new UDPReceiver();
	}
	
	public void addPacketBuff(DatagramPacket packet){
		receivePacketStack.push(packet);
		notifyChanges();
	}

	public void addMsgBuff(MessAddress msg){
		sendMsgStack.push(msg);
		notifyChanges();
	}
	
	private void notifyChanges(){
		synchronized (this) {
			notify();
		}
	}
	
	public MessAddress turnPacketToMessage(DatagramPacket packet){
		MessAddress msg= new MessAddress(); 

		//TODO Recuperer l'adresse de l'envoyeur de packet

		ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData());
		try {
			ObjectInputStream ois = new ObjectInputStream(bais);
			msg.setMessage((Message)ois.readObject());
			msg.setAdress(packet.getAddress());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return msg ; 
	}
	
	public void checkReceive(){
		while(!receivePacketStack.isEmpty()){
			DatagramPacket packet = receivePacketStack.pop();
			MessAddress msgaddr = turnPacketToMessage(packet);
			System.out.println("received: " + msgaddr.getMessage().toString());
			ChatNI.getInstance().messageReceived(msgaddr.getMessage(), msgaddr.getAddress());
		}
	}
	
	public void checkSend(){
		while(!sendMsgStack.isEmpty()){
			MessAddress tmp = sendMsgStack.pop();
			this.sendMessage(tmp);
			System.out.println("sent: " + tmp.getMessage().toString());
		}
	}
	
	public DatagramPacket turnMesstoPacket(MessAddress msgAddr){
		DatagramPacket packet; 
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(msgAddr.getMessage());
			oos.flush();
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		byte[] buf= baos.toByteArray();
		packet = new DatagramPacket(buf, buf.length,msgAddr.getAddress(), 9738);
		return packet;
	}
	
	public void sendMessage(MessAddress msgAddr){
		udpSender.send(turnMesstoPacket(msgAddr));
	}
	
	@Override
	public void run(){
		while (true){
			synchronized (this) {
				try{
					wait();
					this.checkReceive();
					this.checkSend();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
