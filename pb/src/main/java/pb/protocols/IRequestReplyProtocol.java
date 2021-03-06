package pb.protocols;

import pb.EndpointUnavailable;

/**
 * Request/reply protocol objects must implement this interface.
 * 
 * @see {@link #startAsClient()}
 * @see {@link #startAsServer()}
 * @see {@link #sendRequest(Message)}
 * @see {@link #receiveReply(Message)}
 * @see {@link #receiveRequest(Message)}
 * @see {@link #sendReply(Message)}
 * @see {@link pb.protocols.Message}
 * @author aaron
 *
 */
public interface IRequestReplyProtocol {
	/**
	 * Start the protocol as a client.
	 * @throws EndpointUnavailable
	 */
	public void startAsClient() throws EndpointUnavailable;
	
	/**
	 * Start the protocol as a server.
	 * @throws EndpointUnavailable
	 */
	public void startAsServer() throws EndpointUnavailable;
	
	/**
	 * Send a request message.
	 * @param msg
	 * @throws EndpointUnavailable
	 */
	public void sendRequest(Message msg) throws EndpointUnavailable;
	
	/**
	 * Receive a reply message.
	 * @param msg
	 * @throws EndpointUnavailable
	 */
	public void receiveReply(Message msg) throws EndpointUnavailable;
	
	/**
	 * Receive a request message.
	 * @param msg
	 * @throws EndpointUnavailable
	 */
	public void receiveRequest(Message msg) throws EndpointUnavailable;
	
	/**
	 * Send a reply message.
	 * @param msg
	 * @throws EndpointUnavailable
	 */
	public void sendReply(Message msg) throws EndpointUnavailable;
}
