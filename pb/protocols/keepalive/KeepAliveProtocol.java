package pb.protocols.keepalive;
import java.util.logging.Logger;

import pb.Endpoint;
import pb.EndpointUnavailable;
import pb.Manager;
import pb.Utils;
import pb.protocols.Message;
import pb.protocols.Protocol;
import pb.protocols.IRequestReplyProtocol;
import pb.protocols.session.SessionStartReply;
import pb.protocols.session.SessionStartRequest;
import pb.protocols.session.SessionStopReply;
import pb.server.ServerManager;

/**
 * Provides all of the protocol logic for both client and server to undertake
 * the KeepAlive protocol. In the KeepAlive protocol, the client sends a
 * KeepAlive request to the server every 20 seconds using
 * {@link pb.Utils#setTimeout(pb.protocols.ICallback, long)}. The server must
 * send a KeepAlive response to the client upon receiving the request. If the
 * client does not receive the response within 20 seconds (i.e. at the next time
 * it is to send the next KeepAlive request) it will assume the server is dead
 * and signal its manager using
 * {@link pb.Manager#endpointTimedOut(Endpoint,Protocol)}. If the server does
 * not receive a KeepAlive request at least every 20 seconds (again using
 * {@link pb.Utils#setTimeout(pb.protocols.ICallback, long)}), it will assume
 * the client is dead and signal its manager. Upon initialisation, the client
 * should send the KeepAlive request immediately, whereas the server will wait
 * up to 20 seconds before it assumes the client is dead. The protocol stops
 * when a timeout occurs.
 *
 * @see {@link pb.Manager}
 * @see {@link pb.Endpoint}
 * @see {@link pb.protocols.Message}
 * @see {@link pb.protocols.keepalive.KeepAliveRequest}
 * @see {@link pb.protocols.keepalive.KeepAliveReply}
 * @see {@link pb.protocols.Protocol}
 * @author aaron
 *
 */
public class KeepAliveProtocol extends Protocol implements IRequestReplyProtocol {
	private static Logger log = Logger.getLogger(KeepAliveProtocol.class.getName());
	private Utils utils;
	/**
	 * Name of this protocol.
	 */
	public static final String protocolName = "KeepAliveProtocol";
	public Boolean ReceiveReply = false;
	public Boolean ReceiveRequest = false;
	public Boolean first = true;

	/**
	 * Initialise the protocol with an endopint and a manager.
	 *
	 * @param endpoint
	 * @param manager
	 */
	public KeepAliveProtocol(Endpoint endpoint, Manager manager) {
		super(endpoint, manager);
		utils = new Utils();
	}

	/**
	 * @return the name of the protocol
	 */
	@Override
	public String getProtocolName() {
		return protocolName;
	}

	/**
	 *
	 */
	@Override
	public void stopProtocol() {
		ReceiveReply = false;
		ReceiveRequest = false;
		first = true;
	}

	/*
	 * Interface methods
	 */

	/**
	 *
	 */
	public void startAsServer()  {
		ReceiveRequest = true;
		try {
			sendReply(new KeepAliveRequest());
		} catch (EndpointUnavailable endpointUnavailable) {
			endpointUnavailable.printStackTrace();
		}
	}

	/**
	 *
	 */
	public void checkClientTimeout() {
		Utils.getInstance().setTimeout(()->{
			if(ReceiveRequest){
				try {
					sendReply(new KeepAliveRequest());
				} catch (EndpointUnavailable endpointUnavailable) {
					endpointUnavailable.printStackTrace();
				}
				return;
			}else {
				manager.endpointTimedOut(endpoint, this);
			}
		},20050);

	}

	/**
	 *
	 */
	public void startAsClient() throws EndpointUnavailable {
		ReceiveReply = true;
		sendRequest(new KeepAliveRequest());
	}

	/**
	 * @param msg
	 */
	@Override
	public void sendRequest(Message msg)  {
		if(first) {
			try {
				endpoint.send(msg);
			} catch (EndpointUnavailable endpointUnavailable) {
				endpointUnavailable.printStackTrace();
			}

			ReceiveReply = false;
			first = false;
		}

		else {

			Utils.getInstance().setTimeout(() -> {
				if (ReceiveReply) {
					try {
						sendRequest(new KeepAliveRequest());
						endpoint.send(msg);
					} catch (EndpointUnavailable endpointUnavailable) {
						endpointUnavailable.printStackTrace();
					}
					ReceiveReply = false;
					return;
				} else {
					manager.endpointTimedOut(endpoint, this);
				}
			}, 20000);
		}

	}


	/**
	 * @param msgg
	 */
	@Override
	public void receiveReply(Message msg) throws EndpointUnavailable {
		if (msg instanceof KeepAliveReply) {
			ReceiveReply = true;
			sendRequest(new KeepAliveRequest());
		}
	}

	/**
	 * @param msg
	 * @throws EndpointUnavailable
	 */
	@Override
	public void receiveRequest(Message msg) throws EndpointUnavailable {
		if (msg instanceof KeepAliveRequest) {
			ReceiveRequest = true;
			sendReply(new KeepAliveReply());
		}
	}

	/**
	 * @param msg
	 */
	@Override
	public void sendReply(Message msg) throws EndpointUnavailable {
		endpoint.send(msg);
		Utils.getInstance().setTimeout(() -> {
			ReceiveRequest = false;
		}, 19000);
		checkClientTimeout();
	}
}

