package it.fadeout.sftp;

import wasdi.shared.utils.Utils;

/**
 * class for sftp account management
 * @author doy
 *
 */
public class SFTPManager {
	
	String m_sAddress;
	
	public SFTPManager(String m_sAddress) {
		this.m_sAddress = m_sAddress;
	}

	private String sendCommand(String sCommand) {
		try {
			WsClient oClient = new WsClient(m_sAddress, sCommand);
			if (oClient.isOk()) return oClient.getData();		
			Utils.debugLog("ERROR: " + oClient.getData());		
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}				
		return null;
	}
	
	/**
	 * @param sUser
	 * @return true if sUser can connect to sftp server
	 */
	public boolean checkUser(String sUser) {
		return sendCommand("exists_account " + sUser) != null;
	}

	/**
	 * create a new account for sftp service
	 * @param sUser
	 * @param sPassword
	 * @return 
	 */
	public boolean createAccount(String sUser, String sPassword) {			
		return sendCommand("create_account " + sUser + " " + sPassword) != null;
	}

	/**
	 * update a user password in sftp service
	 * @param sUser
	 * @param sPassword
	 * @return 
	 */
	public boolean updatePassword(String sUser, String sPassword) {			
		return sendCommand("update_password " + sUser + " " + sPassword) != null;
	}

	/**
	 * remove an account from sftp service
	 * @param sUser
	 * @return 
	 */
	public boolean removeAccount(String sUser) {			
		return sendCommand("remove_account " + sUser) != null;
	}

	/**
	 * list the files in the sftp server user home 
	 * @param sUser
	 * @return
	 */
	public String[] list(String sUser) {
		String sCommand = sendCommand("list " + sUser);
		if (sCommand != null) {
			return sCommand.split("__WASDI_SEPARATOR__");
		}
		else {
			return null;
		}
	}

	
	public static void main(String[] args) {
		
		String wsAddress = "ws://10.172.47.35:6703"; 
		SFTPManager oManager = new SFTPManager(wsAddress);
		String sAccount = "prova";
		
//		oManager.createAccount(sAccount, sAccount);		
//		Wasdi.debugLog(Arrays.toString(oManager.list(sAccount)));
		oManager.removeAccount(sAccount);		
		
		
//		String sPassword = UUID.randomUUID().toString().split("-")[0];
//		Wasdi.debugLog(sPassword);
//		oManager.removeAccount(sAccount);
//		if (!oManager.createAccount(sAccount, sPassword)) Wasdi.debugLog("NOOOO");
//		//send email with new password
//		MercuriusAPI oAPI = new MercuriusAPI("http://130.251.104.84:8080/it.fadeout.mercurius.webapi");			
//		Message oMessage = new Message();
//		oMessage.setTilte("Wasdi sftp account");
//		oMessage.setSender("adminwasdi@acrotec.it");
//		oMessage.setMessage("USER: " + sAccount + " - PASSWORD: " + sPassword);
//		Wasdi.debugLog(oAPI.sendMailDirect("alessandro.burastero@gmail.com", oMessage));
	}
}
