package com.marcos.dbCollector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LoadFilesService {
	
	private static final Logger logger = LogManager.getLogger(LoadFilesService.class);

	@Value("${ftp.scripts}")
	private String scripsPath;
	
	public String[] getFile(FTPClient ftpClient )  {
		
		try {
			logger.info("Buscando lista de arquivos no diretorio:" + scripsPath);
			ftpClient.changeWorkingDirectory(scripsPath);
			return ftpClient.listNames();
		} catch (IOException e) {
			logger.error("Error ao carregar lista de arquivos do servidor", e);
			return null;
		}
				
	}
	

	public String getQuery(FTPClient ftpClient, String file ){
    	try{	
    		
    		logger.info("Buscando arquivo: " + file + " no diretorio:" + scripsPath);
    		
    		ftpClient.changeWorkingDirectory(scripsPath);
		    InputStream in = ftpClient.retrieveFileStream(file);
		    while(!ftpClient.completePendingCommand());
                logger.info("-------[END download:" + file + "]---------------------");
                
		    String sc      = getStringFromInputStream(in);
		    String[] scs   = sc.split("select");
		    
            return "select " + scs[1];
		}catch(Exception e){
			logger.error("Error ao carregar script servidor", e);
			return null;
		}	
    }
	
	private  String getStringFromInputStream(InputStream is) {
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();
		String line;
		try {
			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException e) {
			logger.error("Falha ao carregar arquivo do buffer", e);
		
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					logger.error("Falha fechar do buffer", e);
				}
			}
		}  
		return sb.toString();
	}

}
