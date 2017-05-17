package com.marcos.dbCollector;

import java.io.IOException;
import java.net.SocketException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FtpService {
	
	
	@Value("${ftp.server}")
	private String server;
	
	@Value("${ftp.port}")
	private Integer port;
	
	@Value("${ftp.user}")
	private String user;
	
	@Value("${ftp.pass}")
	private String pass;


    public FTPClient getFTPConnection() throws SocketException, IOException {    
		
    	FTPClient ftpClient = new FTPClient();
		ftpClient.connect(server, port);
		ftpClient.login(user, pass);
		ftpClient.enterLocalPassiveMode();
		ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		
		return ftpClient;
	}

}
