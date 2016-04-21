package com.ls.openfire.plugin;


import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jivesoftware.openfire.OfflineMessage;
import org.jivesoftware.openfire.OfflineMessageListener;
import org.jivesoftware.openfire.OfflineMessageStore;
import org.jivesoftware.openfire.OfflineMessageStrategy;
import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.openfire.user.PresenceEventDispatcher;
import org.jivesoftware.openfire.user.PresenceEventListener;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;

/** 
* get offline message count
* @author f7anty 
* @date 2016/01/11
*/
public class OfflineMessagePlugin implements Plugin, OfflineMessageListener,PresenceEventListener{

	private XMPPServer server;
	private SessionManager sessionManager;
	private OfflineMessageIQHandle iQHandle;
	
	@Override
	public void initializePlugin(PluginManager manager, File pluginDirectory) {
		
		server = XMPPServer.getInstance();
		sessionManager = SessionManager.getInstance();
		iQHandle = new OfflineMessageIQHandle("query");
		server.getIQRouter().addHandler(iQHandle);
		OfflineMessageStrategy.addListener(this);
		PresenceEventDispatcher.addListener(this);
	}
	
	
	@Override
	public void destroyPlugin() {

		server.getIQRouter().removeHandler(iQHandle);
		OfflineMessageStrategy.removeListener(this);
		PresenceEventDispatcher.removeListener(this);
	}

	@Override
	public void messageStored(Message message) {

		sendOfflineMessage(message);
	}
	
	@Override
	public void messageBounced(Message message) {}
	
	private void sendOfflineMessage(Message message) {

		if(message.getBody()!=null&&!message.getBody().equals("")){
			
			OfflineMessageStore store = server.getOfflineMessageStore();
			String userName = message.getTo().getNode();
			Map<String,Integer> map=iQHandle.getMessageMap();//将离线消息数存储至Map,减少对数据库的访问
			if(map.containsKey(userName)){
				map.put(userName,map.get(userName)+1);
			}
			else{
				Collection<OfflineMessage> messages = store.getMessages(userName, false);// 获取指定用户的所有离线消息，指定提取后是否要从数据库中删除，false不删除
				map.put(userName, messages.size());
			}
			
			Collection<ClientSession> sessions = sessionManager.getSessions();//获取当前登录的用户
			IQ reply = new IQ();
			reply.setID("getOfflineMsg");
			reply.setFrom(server.getServerInfo().getXMPPDomain());
			reply.setType(IQ.Type.result);
			Element element = DocumentHelper.createElement("message");
			element.addNamespace("", "com:ls:im:offlineMsg");
			element.setText(String.valueOf(map.get(userName)));
			reply.setChildElement(element);
			for (ClientSession session : sessions) {

				String user =StringUtils.substringBefore(session.getAddress().toString().toString(), "@");
				if (message.getTo().getNode().equals(user)) {
					reply.setTo(session.getAddress().toString());
					session.process(reply);
				}
			}	
		}	
	}

	@Override
	public void availableSession(ClientSession clientSession, Presence presence) {
	
		String userName =StringUtils.substringBefore(clientSession.getAddress().toString().toString(), "@");
		if(iQHandle.getMessageMap().containsKey(userName)){
			iQHandle.getMessageMap().remove(userName);// clear message count when user is online
		}
	}

	@Override
	public void presenceChanged(ClientSession clientSession, Presence presence) {
	}

	@Override
	public void subscribedToPresence(JID subscriberJID, JID authorizerJID){	
	}

	@Override
	public void unavailableSession(ClientSession clientSession, Presence presence) {	
	}

	@Override
	public void unsubscribedToPresence(JID unsubscriberJID, JID recipientJID){
	}

}
