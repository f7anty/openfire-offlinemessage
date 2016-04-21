package com.ls.openfire.plugin;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.OfflineMessage;
import org.jivesoftware.openfire.OfflineMessageStore;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

/** 
* OfflineMessageIQHandle
* @author f7anty
* @date 2016/01/11
*/
public class OfflineMessageIQHandle extends IQHandler {

	private static final Logger Log = LoggerFactory.getLogger(OfflineMessageIQHandle.class);  
	private XMPPServer server;
	private IQHandlerInfo info;
	private static final String NAME_SPACE = "com:ls:im:offlineMsg";
	private Map<String,Integer> messageMap=new ConcurrentHashMap<String,Integer>();
	
	public OfflineMessageIQHandle(String moduleName) {
		super(moduleName);
		server = XMPPServer.getInstance();
		info = new IQHandlerInfo(moduleName, NAME_SPACE);//args0:name；args1:workspace
	}

	@Override
	public IQHandlerInfo getInfo() {

		return info;
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {

		IQ reply = IQ.createResultIQ(packet);
		try {
	
			ClientSession session = sessionManager.getSession(packet.getFrom());
			if (session == null) {
				Log.error("Session not found in " + sessionManager.getPreAuthenticatedKeys() + " for key " + packet.getFrom());
				reply.setChildElement(packet.getChildElement().createCopy());
				reply.setError(PacketError.Condition.internal_server_error);
				return reply;
			}
			if (IQ.Type.get.equals(packet.getType())) {

				String userName =StringUtils.substringBefore(packet.getFrom().toString(), "@");
				OfflineMessageStore store = server.getOfflineMessageStore();
				//get user's offline messages
				Collection<OfflineMessage> messages = store.getMessages(userName, false);//false: don't delete messages from database
				messageMap.put(userName, messages.size());//save message count to map
				Element element = DocumentHelper.createElement("message");
				reply.setType(IQ.Type.result);
				element.addNamespace("", NAME_SPACE);
				element.setText(String.valueOf(messages.size()));
				reply.setChildElement(element);
				return reply;	
			}
		} catch (Exception e) {
			Log.error(e.getMessage(), e);  
			reply.setChildElement(packet.getChildElement().createCopy());
			reply.setError(PacketError.Condition.internal_server_error);// remote server error
			return reply;
		}

		return reply;
	}

	public Map<String, Integer> getMessageMap() {
		return messageMap;
	}

	public void setMessageMap(Map<String, Integer> messageMap) {
		this.messageMap = messageMap;
	}
}
