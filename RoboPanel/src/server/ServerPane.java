package server;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;


import shared.KPane;

public class ServerPane extends KPane{
	Rectangle diskPushTrue;
	Rectangle diskPushFalse;
	String lastMessage;
	CopyOnWriteArrayList<String> messages;
	int index;
	int textX;
	
	public ServerPane(){
		super("Server");
		messages=new CopyOnWriteArrayList<String>();
		addMessage("Server initialized");
		index=0;
		textX=2;
		addKeyListener(new TAdapter());
		diskPushTrue=new Rectangle(2,72,94,16);
		diskPushFalse=new Rectangle(98,72,100,16);
	}
	
	public void paintComponent(Graphics g){
		g.drawString("Last message: "+lastMessage, textX, 35);
		g.drawString("Message #"+index+": "+messages.get(index), textX, 55);
		g.drawString("Disk Push (true)", diskPushTrue.x+3,diskPushTrue.y+diskPushTrue.height-3);
		g.drawRect(diskPushTrue.x, diskPushTrue.y, diskPushTrue.width, diskPushTrue.height);
		g.drawString("Disk Push (false)", diskPushFalse.x+3,diskPushFalse.y+diskPushFalse.height-3);
		g.drawRect(diskPushFalse.x, diskPushFalse.y, diskPushFalse.width, diskPushFalse.height);
	}
	
	public void addMessage(String message){
		lastMessage=message;
		messages.add(message);
	}
	
	private class TAdapter extends KeyAdapter{
		public void keyPressed(KeyEvent e){
			if (e.getKeyCode()==KeyEvent.VK_UP){
				try {
					messages.get(index+1);
					index++;
				} catch(IndexOutOfBoundsException e1){
					
				}
			} else if (e.getKeyCode()==KeyEvent.VK_DOWN){
				try {
					messages.get(index-1);
					index--;
				} catch(IndexOutOfBoundsException e1){
					
				}
			} else if (e.getKeyCode()==KeyEvent.VK_RIGHT){
				textX+=4;
			} else if (e.getKeyCode()==KeyEvent.VK_LEFT){
				textX-=4;
			} 
		}
		
	}
}
