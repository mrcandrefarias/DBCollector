package com.marcos.dbCollector;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

@SpringBootApplication
public class BiCollector {
	
	private static final Logger logger = LogManager.getLogger(BiCollector.class);
	
	@Value("${app.debug}")
	private Integer debug;
	
	@Value("${app.lines}")
	private Integer appLines;
	
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
		
	@Autowired
	private FtpService ftpService;
	
	@Autowired
	private LoadFilesService loadFilesService;

	@Autowired
	private SendFilesService sendFilesService;

	@Value("${localfile.path}")
	private String localFilePath;
	
	
	public static void main(String[] args) {
		SpringApplication.run(BiCollector.class);
	}
	
	@Bean
	public CommandLineRunner commandLineRunner() {
		return (args) -> {
			
			logger.info("Iniciando processamento: ");
			
			FTPClient ftpClient = ftpService.getFTPConnection();
			String[] files      = loadFilesService.getFile(ftpClient);
			
			for (String file : files) {
									
				String query       = loadFilesService.getQuery(ftpClient, file);
				if(query == null)
					continue;
				
				try {
				    getDados(query, file.replace(".txt", ".csv"));
				} catch (Exception e) {
			        logger.error("Erro ao consultar base de dado- sql:" + query, e);
				}
				
				this.debug();
			
			}
			
			sendFilesService.sendDados(ftpClient, localFilePath );
			
			this.debug();
				
		};
	}

	private void debug(){
		if( debug > 0){
	        Runtime rt         = Runtime.getRuntime();
	        long usedMB        = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
	        logger.info("Memória Total MB:" + rt.totalMemory() / 1024 / 1024 );
	        logger.info("Memória MB utilizada:" + usedMB);
		}
	}
	
	public List<String> getDados(String query, String file)  {
		Path path  = Paths.get( localFilePath + "/" + file  );
		logger.info("Consultando dados:" + query);
		return jdbcTemplate.query( query, new ResultSetExtractor<List<String>>(){	
		    @Override  
		    public List<String> extractData(ResultSet rs) throws SQLException, DataAccessException {  
		        List<String> list = new ArrayList<String>();
		        int rowNum        = 0;
		        int columnCount   = 0;
		        while(rs.next()){  
		        	if(rowNum == 0){
		        		columnCount  = rs.getMetaData().getColumnCount() + 1;
						String  csv1 = "";
						
				        for(int i = 1; i < columnCount; i++){
				            if( i == columnCount -1){
			                	csv1 = csv1 + rs.getMetaData().getColumnName(i) + System.lineSeparator();
			                }else{
			                	csv1 = csv1 + rs.getMetaData().getColumnName(i) + ";";
			                }
				        }	
				        List<String> lineOne = new ArrayList<String>();
				        lineOne.add( csv1 );
				        saveFile(path, lineOne, StandardOpenOption.CREATE);
					}
		        	String csv = "";
		            for(int i = 1; i < columnCount; i++){
		                if( i == columnCount -1){
		                	csv = csv + rs.getObject(i) + System.lineSeparator();
		                }else{
		                	csv = csv + rs.getObject(i) + ";";
		                }	            	
		            }
		        	list.add( csv);
		        	if(list.size() > appLines){
		        		saveFile(path, list, StandardOpenOption.APPEND);
		        		list = new ArrayList<String>();
		        	}
		        	rowNum++;
		        }
		        if(list.size() > 0){
	        		saveFile(path, list, StandardOpenOption.APPEND);
	        		list = new ArrayList<String>();
	        	}
		        if(rowNum < 1){
		        	logger.info("Consulta não retornou dados:" + query);
		        }
		        return list;  
		    }  
	    });  
	}
	
	
	public void saveFile( Path path, List<String> lines, StandardOpenOption option) {
		try{
			BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, option);
			
			for(String line : lines){
		        writer.write(line);
			}
		    	
		    writer.close();
		}catch(Exception e){	
			logger.error("Erro ao escrever em arquivo", e);
		}		
	}
}
