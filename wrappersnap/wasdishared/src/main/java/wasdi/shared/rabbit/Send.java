package wasdi.shared.rabbit;

/**
 * Created by s.adamo on 23/09/2016.
 */
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.RabbitMessageViewModel;

/**
 * Utility class to send Rabbit Messages
 * @author p.campanella
 *
 */
public class Send {
	
	Connection m_oConnection = null;
	Channel m_oChannel= null;
	String m_sExchangeName = "amq.topic";
	
	public Send(String sExchange) {
		//create connection to the server
        try {
            m_oConnection = RabbitFactory.getConnectionFactory().newConnection();
            if (m_oConnection!=null) m_oChannel = m_oConnection.createChannel();
            m_sExchangeName = sExchange;
        } catch (Exception e) {
            Utils.debugLog("Send.Init: Error connecting to rabbit " + e.toString());
        }
	}
	
	public void free() {
		try {
			
			if (m_oChannel != null) {
				m_oChannel.close();
			}
			
			if (m_oConnection!=null) {
				m_oConnection.close();
			}
			
		} catch (IOException e) {
			Utils.debugLog("Send.Free: Error closing connection " + e.toString());
		} catch (TimeoutException e) {
			Utils.debugLog("Send.Free: Error closing connection " + e.toString());
		}
	}
	
    /**
     * Send a Rabbit Message
     * @param sRoutingKey (Is the workspace Id)
     * @param sMessageAttribute
     * @return true if the message is sent, false otherwise
     * @throws IOException
     */
    private boolean sendMsg(String sRoutingKey, String sMessageAttribute)
    {
    	if (m_oChannel == null) return false;
    	
        try {        	
            m_oChannel.basicPublish(m_sExchangeName, sRoutingKey, null, sMessageAttribute.getBytes());
        } catch (IOException e) {
        	Utils.debugLog("Send.SendMgs: Error publishing message " + sMessageAttribute + " to " + sRoutingKey + " " + e.toString());
            return false;
        }
        catch (Exception e) {
        	Utils.debugLog("Send.SendMgs: Error publishing message " + sMessageAttribute + " to " + sRoutingKey + " " + e.toString());
            return false;
        }
        //LauncherMain.s_oLogger.debug(" [x] Sent '" + sMessageAttribute + "' to " + sRoutingKey);
        return true;

    }

    /**
     * Sends Update Process Rabbit Message
     * @param oProcess
     * @return
     * @throws JsonProcessingException
     */
    public boolean sendUpdateProcessMessage(ProcessWorkspace oProcess) throws JsonProcessingException {  
    	
    	if (oProcess==null) return false;
    	
        RabbitMessageViewModel oUpdateProcessMessage = new RabbitMessageViewModel();
        oUpdateProcessMessage.setMessageCode(LauncherOperations.UPDATEPROCESSES.name());
        oUpdateProcessMessage.setWorkspaceId(oProcess.getWorkspaceId());
        oUpdateProcessMessage.setPayload(oProcess.getProcessObjId() + ";" + oProcess.getStatus() + ";" + oProcess.getProgressPerc());
        
        Utils.debugLog("Send.SendUpdateProcessMessage: Send update message for process " + oProcess.getProcessObjId() + ": " + oUpdateProcessMessage.getPayload());
        
        String sJSON = MongoRepository.s_oMapper.writeValueAsString(oUpdateProcessMessage);
        return sendMsg(oProcess.getWorkspaceId(), sJSON);
    }

    /**
     * Send a Generic WASDI Rabbit Message
     * @param bOk Flag ok or not
     * @param sMessageCode Message Code
     * @param sWorkSpaceId Reference Workspace
     * @param oPayload Message Payload
     * @param sExchangeId Exchange Routing Key
     * @return
     */
    public boolean sendRabbitMessage(boolean bOk, String sMessageCode, String sWorkSpaceId, Object oPayload, String sExchangeId) {

        try {
            RabbitMessageViewModel oRabbitVM = new RabbitMessageViewModel();
            oRabbitVM.setMessageCode(sMessageCode);
            oRabbitVM.setWorkspaceId(sWorkSpaceId);
            if (bOk) oRabbitVM.setMessageResult("OK");
            else  oRabbitVM.setMessageResult("KO");

            oRabbitVM.setPayload(oPayload);

            String sJSON = MongoRepository.s_oMapper.writeValueAsString(oRabbitVM);

            return sendMsg(sExchangeId, sJSON);
        }
        catch (Exception oEx) {
        	Utils.debugLog("Send.SendRabbitMessage: ERROR " + oEx.toString());
            return  false;
        }

    }
}
