package pb.protocols.session;


import java.util.logging.Logger;

import pb.Endpoint;
import pb.EndpointUnavailable;
import pb.Manager;
import pb.Utils;
import pb.protocols.Message;
import pb.protocols.Protocol;
import pb.protocols.IRequestReplyProtocol;

/**
 * Allows the client to request the session to start and to request the session
 * to stop, which in turns allows the sockets to be properly closed at both
 * ends. Actually, either party can make such requests, but usually the client
 * would make the session start request as soon as it connects, and usually the
 * client would make the session stop request. The server may however send a
 * session stop request to the client if it wants (needs) to stop the session,
 * e.g. perhaps the server is becoming overloaded and needs to shed some
 * clients.
 *
 * @see {@link pb.Manager}
 * @see {@link pb.Endpoint}
 * @see {@link pb.protocols.Protocol}
 * @see {@link pb.protocols.IRequestReplyProtocol}
 * @see {@link pb.protocols.session.SessionStartRequest}
 * @see {@link pb.protocols.session.SessionStartReply}
 * @see {@link pb.protocols.session.SessionStopRequest}
 * @see {@link pb.protocols.session.SessionStopReply}
 * @author aaron
 *
 */
public class SessionProtocol extends Protocol implements IRequestReplyProtocol {
	private static Logger log = Logger.getLogger(SessionProtocol.class.getName());
	private Message msg;
	/**
	 * The unique name of the protocol.
	 */
	public static final String protocolName="SessionProtocol";

	// Use of volatile is in case the thread that calls stopProtocol is different
	// to the endpoint thread, although in this case it hardly needed.

	/**
	 * Whether the protocol has started, i.e. start request and reply have been sent,
	 * or not.
	 */
	private volatile boolean protocolRunning=false;
	private volatile boolean receiveStartRequest = false;
	private volatile boolean receiveStartReply = false;
	/**
	 * Initialise the protocol with an endpoint and manager.
	 * @param endpoint
	 * @param manager
	 */
	public SessionProtocol(Endpoint endpoint, Manager manager) {
		super(endpoint,manager);
	}

	/**
	 * @return the name of the protocol.
	 */
	@Override
	public String getProtocolName() {
		return protocolName;
	}
	/**
	 * If this protocol is stopped while it is still in the running
	 * state then this indicates something may be a problem.
	 */
	@Override
	public void stopProtocol() {
		if(protocolRunning) {
			log.severe("protocol stopped while it is still underway");
		}
	}

	/*
	 * Interface methods
	 */


	/**
	 * Called by the manager that is acting as a client.
	 */
	@Override
	public void startAsClient() throws EndpointUnavailable {
		log.info("START AS CLIENT");
		//  send the server a start session request
		sendRequest(new SessionStartRequest());

		/* STARTING TIMING BEFORE "received SessionStartReply"*/
		/* timeout if no reply from SERVER in 20s */

		Utils.getInstance().setTimeout(() -> {
			this.receiveReply(msg);

			if(msg == null){
				log.info("CLIENT DID NOT RECEIVE THE REPLY IN 20S");
				manager.endpointTimedOut(endpoint, this);
			}
		}, 20000); //do sth after 20s
		//Utils.getInstance().cleanUp();
	}
	/* delay sending SessionStartReply for testing*/


	// provide a timeout event to its manager
	// if there is no response to a request message within 20 seconds,


	//ServerManager starting the SessionProtocol as soon as the endpoint is ready
	//The endpoint is ready when the endpoint calls endpointReady


	/**
	 * Called by the manager that is acting as a server.
	 */
	@Override
	public void startAsServer() {
		log.info("START AS SERVER");

		/* STARTING TIMING BEFORE "received SessionStartRequest"*/
		/* timeout if no request received in 20s after a session starts */
		Utils.getInstance().setTimeout(() -> {
			try {
				this.receiveRequest(msg);
			} catch (EndpointUnavailable endpointUnavailable) {
				endpointUnavailable.printStackTrace();
			}
			if(msg == null){
				log.info("SERVER DID NOT RECEIVE THE REQUEST IN 20S");
				manager.endpointTimedOut(endpoint, this);

			} //do sth after 20s
		}, 20000);
		//Utils.getInstance().cleanUp();
	}

	/* delay sending SessionStartRequest for testing*/

	// provide a timeout event to its manager when the protocol is started as a server,
	// if a session start request message is not received within 20 seconds of starting the protocol.

	//server will timeout if it does not receive a session start request in 20 seconds from starting the protocol.



	/**
	 * Generic stop session call, for either client or server.
	 * @throws EndpointUnavailable if the endpoint is not ready or has terminated
	 */
	public void stopSession() throws EndpointUnavailable {
		sendRequest(new SessionStopRequest());
	}

	/**
	 * Just send a request, nothing special.
	 * @param msg
	 */
	@Override
	public void sendRequest(Message msg) throws EndpointUnavailable {
		//Utils.getInstance().setTimeout(() -> {
		//	try {
		endpoint.send(msg);
		//	} catch (EndpointUnavailable endpointUnavailable) {
		//		endpointUnavailable.printStackTrace();
		//	}
		//}, 20000);
	}


	/**
	 * If the reply is a session start reply then tell the manager that
	 * the session has started, otherwise if its a session stop reply then
	 * tell the manager that the session has stopped. If something weird
	 * happens then tell the manager that something weird has happened.
	 * @param msg
	 */
	@Override
	public void receiveReply(Message msg) {
		log.info("CLIENT SIDE: " + "\n");
		if (msg instanceof SessionStartReply) {
			if (protocolRunning) {
				// error, received a second reply?
				manager.protocolViolation(endpoint, this);
				return;
			}
			protocolRunning = true;
			manager.sessionStarted(endpoint);
			System.out.print("--- SessionStartReply END ---");
		} else if (msg instanceof SessionStopReply) {
			if (!protocolRunning) {
				// error, received a second reply?
				manager.protocolViolation(endpoint, this);
				return;
			}
			protocolRunning = false;
			manager.sessionStopped(endpoint);
			log.info("--- SessionStopReply END ---");
		}

	}

	/**
	 * If the received request is a session start request then reply and
	 * tell the manager that the session has started. If the received request
	 * is a session stop request then reply and tell the manager that
	 * the session has stopped. If something weird has happened then...
	 * @param msg
	 */
	@Override
	public void receiveRequest(Message msg) throws EndpointUnavailable {
		System.out.print("SERVER SIDE: " + "\n");
		if(msg instanceof SessionStartRequest) {
			if(protocolRunning) {
				// error, received a second request?
				manager.protocolViolation(endpoint,this);
				return;
			}
			protocolRunning=true;
			sendReply(new SessionStartReply());
			manager.sessionStarted(endpoint);
			System.out.print("--- SessionStartRequest END ---");
		} else if(msg instanceof SessionStopRequest) {
			if(!protocolRunning) {
				// error, received a second request?
				manager.protocolViolation(endpoint,this);
			}
			protocolRunning=false;
			sendReply(new SessionStopReply());
			manager.sessionStopped(endpoint);
			System.out.print("--- SessionStopRequest END ---");

		}
	}

	/**
	 * Just send a reply, nothing special to do.
	 * @param msg
	 */

	@Override
	public void sendReply(Message msg) throws EndpointUnavailable {
		//Utils.getInstance().setTimeout(() -> {
		//	try {
		endpoint.send(msg);
		//	} catch (EndpointUnavailable endpointUnavailable) {
		//		endpointUnavailable.printStackTrace();
		//	}
		//}, 20000);
	}
}
