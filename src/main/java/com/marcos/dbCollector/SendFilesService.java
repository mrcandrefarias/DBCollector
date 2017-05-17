package com.marcos.dbCollector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SendFilesService {
  

	@Value("${ftp.dados}")
	private String dadosPath;
	
	private static final Logger logger = LogManager.getLogger(SendFilesService.class);
	
	public void sendDados(FTPClient ftpClient, String path) {
		String zipName = path + ".zip";
		try {
			
			this.zipLog(path, zipName);
			
        	FileInputStream arqEnviar = new FileInputStream(zipName);
			boolean success           = ftpClient.storeFile( dadosPath + zipName, arqEnviar);
			
			if (success) {
				logger.info("Logs enviado para servidor");
			} else {
				logger.error("Falha ao enviar logs para servidor");
			}
			arqEnviar.close();
			
		} catch (Exception ex) {
			logger.error("Exception ao enviar logs para servidor", ex);
		}    
		this.deleFiles(zipName);
	}
	
    private void deleFiles(String path){
		Path rootPath = Paths.get(path);
		try {
		    Files.delete(rootPath);
		    logger.info("deletado arquivo:" + path);
		} catch (NoSuchFileException x) {
			logger.error("Erro ao deletar arquivo:" + path);
		} catch (DirectoryNotEmptyException x) {
			logger.error("diretorio não é vazio:" + path);
		} catch (IOException x) {
		    logger.error("Provavel problema de permissao ao deletar arquivo:" + path, x);
		}       
	}


    public void zipLog( String path, String zipName ) {
    	try{
    	    FileOutputStream fos   = new FileOutputStream( zipName );
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            File fileToZip         = new File(path);
            
            zipFile(fileToZip, fileToZip.getName(), zipOut);    
            
            zipOut.close();
            fos.close();
    	}catch(Exception e){
    		logger.error("Erro ao compactar arquivo:", e);
    	}
        
	}
    
    private void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
            	
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
                
                this.deleFiles( fileName + "/" + childFile.getName());
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry   = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes        = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }
}
