package com.example.demo.controller;

import java.io.*;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ObjectUtils;
import org.postgresql.core.BaseConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.entity.BaseResponse;
import com.example.demo.entity.Database;
import com.example.demo.entity.Student;
import com.example.demo.entity.Teacher;
import com.example.demo.service.DatabaseService;
import com.example.demo.service.StudentService;
import com.example.demo.service.TeacherService;
import com.example.demo.tool.DBConnection;
import com.example.demo.tool.DBDatasource;
import com.example.demo.tool.DBExecTool;
import com.example.demo.tool.DBFileIOTool;
import com.example.demo.tool.DBMetaDataTool;
import com.example.demo.util.EncodeDetector;
import com.example.demo.util.FileTrans;
import com.example.demo.util.JSQLParserUtils;
import com.example.demo.util.LocalCacheUtil;
import com.example.demo.util.OutputToInput;
import com.example.demo.util.RemoveComment;
import com.example.demo.util.SQLTerminalQuerySplit;
import com.example.demo.util.SetDBConnection;
import com.opencsv.exceptions.CsvException;

import lombok.extern.slf4j.Slf4j;

import com.example.demo.util.EncodingDetect;

@Slf4j
@RequestMapping("/practice")
@CrossOrigin
@RestController
public class PracticeController {
	@Autowired
	private DatabaseService databaseService;
	@Autowired
	private TeacherService teacherService;
	@Autowired
	private StudentService studentService;
	@Autowired
	private DBDatasource dbDatasource;
	
	@PostMapping(value = "/connectForTeacher")
	public BaseResponse<Map<String, Object>> connectForTeacher(@RequestParam(value = "teacherId")String teacherId,
															   @RequestParam(value = "zjId",required = false,defaultValue = "#")String zjId,
			                                                   @RequestParam(value = "dbName")String dbName,
			                                                   @RequestParam(value = "schemaName", required = false)String schemaName) {
		Map<String, Object> modelMap = new HashMap<>();
		
		//???????????????????????????
		Database database = databaseService.getDatabaseByName(dbName);
		log.info("-------------------");
		log.info(dbName);
		if (database == null) {
			modelMap.put("errmessage", dbName + " ??????????????????");
			return BaseResponse.fail(modelMap);
		}
		if (!database.getUserId().equals(teacherId)) {
			modelMap.put("errmessage", dbName + " ?????????????????? " + teacherId);
			return BaseResponse.fail(modelMap);
		}
		
		//??????????????????????????????????????????
		Teacher teacher = teacherService.getTeacher(teacherId);
		
		//?????????????????????
		DBConnection dbConnection = new DBConnection(dbDatasource);
		SetDBConnection.setDBConnection(dbConnection, database, teacher);
		try {
			dbConnection.getConnect();
		} catch (SQLException e) {
			log.error("Exception :", e);
			modelMap.put("errmessage", "?????????????????????");
			return BaseResponse.fail(modelMap);
		}
		
		//?????????????????????????????????????????????????????????????????????
		Optional<Object> oldDbConnectionObjOptional = LocalCacheUtil.get(teacherId);
		Object oldDbConnectionObj = null;
		if(oldDbConnectionObjOptional.isPresent()) 
			oldDbConnectionObj = oldDbConnectionObjOptional.get();

		if (oldDbConnectionObj != null && oldDbConnectionObj instanceof DBConnection) {
			DBConnection oldDbConnection = (DBConnection) oldDbConnectionObj;
			try {
				oldDbConnection.close();
			} catch (SQLException e) {
				log.error("Exception :", e);
			}
			LocalCacheUtil.remove(teacherId);
		}
		if(zjId.equals("#"))
			LocalCacheUtil.set(teacherId, dbConnection, 4*60*60*1000);
		else
			LocalCacheUtil.set(zjId, dbConnection, 4*60*60*1000);

		modelMap.put("notice", "?????????????????????");
		return BaseResponse.success(modelMap);
	}
	
	@PostMapping(value = "/connectForStudent")
    public BaseResponse<Map<String, Object>> connectForStudent(@RequestParam(value = "studentId") String studentId,
    		                                                   @RequestParam(value = "dbName", required = false) String dbName,
    		                                                   @RequestParam(value = "username", required = false) String username,
    		                                                   @RequestParam(value = "password", required = false) String password) {
        Map<String, Object> modelMap = new HashMap<>();
        
        //?????????????????????????????????????????????????????????
        Student student = studentService.getStudent(studentId);
        if (student == null || !student.isIsactive()) {
			modelMap.put("errmessage", " ???????????????");
        	return BaseResponse.fail(modelMap);
        }
		
		//???????????????????????????
		Database database = databaseService.getDatabaseByName(student.getDbName());
		if (database == null) {
			modelMap.put("errmessage", dbName + " ??????????????????");
			return BaseResponse.fail(modelMap);
		}
		
		//?????????????????????
		DBConnection dbConnection = new DBConnection(dbDatasource);
		dbConnection.setDatabase(student.getDbName());
		dbConnection.setUsername(student.getUsername());
		dbConnection.setPassword(student.getPassword());
		
		try {
			dbConnection.getConnect();
		} catch (SQLException e) {
			log.error("Exception :", e);
			modelMap.put("errmessage", "?????????????????????");
			return BaseResponse.fail(modelMap);
		}
		
		//?????????????????????????????????????????????????????????????????????
		Optional<Object> oldDbConnectionObjOptional = LocalCacheUtil.get(studentId);
		Object oldDbConnectionObj = null;
		if(oldDbConnectionObjOptional.isPresent()) 
			oldDbConnectionObj = oldDbConnectionObjOptional.get();

		if (oldDbConnectionObj != null && oldDbConnectionObj instanceof DBConnection) {
			DBConnection oldDbConnection = (DBConnection) oldDbConnectionObj;
			try {
				oldDbConnection.close();
			} catch (SQLException e) {
				log.error("Exception :", e);
			}
			LocalCacheUtil.remove(studentId);
		}
		LocalCacheUtil.set(studentId, dbConnection, 4*60*60*1000);
		
		modelMap.put("notice", "?????????????????????");
		return BaseResponse.success(modelMap);
	}
	
	@PostMapping(value = "/disconnect")
	public BaseResponse<Object> disconnect(@RequestParam(value = "userId") String userId) {
		Optional<Object> dbConnectionObjOptional = LocalCacheUtil.get(userId);
		Object dbConnectionObj = null;
		if (dbConnectionObjOptional.isPresent())
			dbConnectionObj = dbConnectionObjOptional.get();

		if (dbConnectionObj != null && dbConnectionObj instanceof DBConnection) {
			DBConnection dbConnection = (DBConnection) dbConnectionObj;
			try {
				dbConnection.close();
			} catch (SQLException e) {
				log.error("Exception :", e);
			}
			LocalCacheUtil.remove(userId);
		}
		
		return BaseResponse.success(null);
	}
	
	@PostMapping(value = "/execCode")
	public BaseResponse<Map<String, Object>> execCode(@RequestParam(value = "userId") String userId,
			                                          @RequestParam(value = "code") String code,
													  @RequestParam(value = "isexplain") boolean isexplain) {
		Map<String, Object> modelMap = new HashMap<>();
		log.info("-------------------------------");
		log.info(userId);
		log.info(code);
		DBConnection dbConnection;
		Optional<Object> dbConnectionObjOptional = LocalCacheUtil.get(userId);
		Object dbConnectionObj = null;
		if (dbConnectionObjOptional.isPresent())
			dbConnectionObj = dbConnectionObjOptional.get();

		if (dbConnectionObj instanceof DBConnection)
			dbConnection = (DBConnection) dbConnectionObj;
		else
			dbConnection = null;
		log.info("DBConnection {}", LocalCacheUtil.get(userId));
		if (dbConnection == null) {
			modelMap.put("errmessage", "????????????, ??????????????????");
			return BaseResponse.fail(modelMap);
		}
		
		try {
			Connection connection = dbConnection.getConnect();
			DBExecTool dbExecTool = new DBExecTool(connection);
			ArrayList<String> queryList = new ArrayList<>();
			List<Map<String, Object>> resultList = new ArrayList<>();
			SQLTerminalQuerySplit sqlTerminalQuerySplit = new SQLTerminalQuerySplit();
			RemoveComment removeComment = new RemoveComment();
			
			
			sqlTerminalQuerySplit.splitQuerries(queryList, code, false);
			log.info("----------------------------------------");
			log.info("queryList size {}", queryList.size());
			int n = queryList.size();
			if(n>1024) {
				modelMap.put("errmessage", "??????????????????1024??????????????????????????????");
				return BaseResponse.fail(modelMap);
			}
			for(String singleSQL : queryList) {
				singleSQL = removeComment.removeComment(singleSQL).trim();
				if(isexplain)
					singleSQL = "explain "+singleSQL;


				if (singleSQL.isEmpty()) continue;
				log.info(singleSQL);
				Map<String, Object> sqlResult = dbExecTool.execute(singleSQL);
				sqlResult.put("supportUpdate", JSQLParserUtils.isQueryResultEditSupported(singleSQL));
				resultList.add(sqlResult);
			}
			modelMap.put("resultList", resultList);
			log.info("resultList: {}", resultList);
		} catch(SQLException e) {
			log.error("Exception :", e);
			modelMap.put("errmessage", "?????????????????????");
			return BaseResponse.fail(modelMap);
		}
		
		return BaseResponse.success(modelMap);
	}
	
	@PostMapping(value = "/getUserSchema")
	public BaseResponse<Map<String, Object>> getUserSchema(@RequestParam(value = "userId") String userId) {
		Map<String, Object> modelMap = new HashMap<>();
		DBConnection dbConnection;
		Optional<Object> dbConnectionObjOptional = LocalCacheUtil.get(userId);
		Object dbConnectionObj = null;
		if (dbConnectionObjOptional.isPresent())
			dbConnectionObj = dbConnectionObjOptional.get();

		if (dbConnectionObj instanceof DBConnection)
			dbConnection = (DBConnection) dbConnectionObj;
		else
			dbConnection = null;

		if (dbConnection == null) {
			modelMap.put("errmessage", "????????????, ??????????????????");
			return BaseResponse.fail(modelMap);
		}
		
		try {
			Connection connection = dbConnection.getConnect();
			DBMetaDataTool dbMetaDataTool = new DBMetaDataTool(connection);
			List<Map<String, Object>> userSchemaList = dbMetaDataTool.getUserSchemas();
			modelMap.put("currentSchemaName", connection.getSchema());
			modelMap.put("userSchemaList", userSchemaList);
		} catch (SQLException e) {
			log.error("Exception :", e);
			modelMap.put("errmessage", "???????????????");
			modelMap.put("detail", e.getMessage());
			return BaseResponse.fail(modelMap);
		}
		
		return BaseResponse.success(modelMap);
	}
	
	@PostMapping(value = "/getSchemaMetadata")
	public BaseResponse<Map<String, Object>> getSchemaMetadata(@RequestParam(value = "userId") String userId,
			                                                   @RequestParam(value = "schemaOid") long schemaOid) {
		Map<String, Object> modelMap = new HashMap<>();
		DBConnection dbConnection;
		Optional<Object> dbConnectionObjOptional = LocalCacheUtil.get(userId);
		Object dbConnectionObj = null;
		if (dbConnectionObjOptional.isPresent())
			dbConnectionObj = dbConnectionObjOptional.get();

		if (dbConnectionObj instanceof DBConnection)
			dbConnection = (DBConnection) dbConnectionObj;
		else
			dbConnection = null;

		if (dbConnection == null) {
			modelMap.put("errmessage", "????????????, ??????????????????");
			return BaseResponse.fail(modelMap);
		}
		
		try {
			Connection connection = dbConnection.getConnect();
			DBMetaDataTool dbMetaDataTool = new DBMetaDataTool(connection);
			Map<String, Object> schemaMetaDataMap = dbMetaDataTool.getSchemaMetaData(schemaOid);
			modelMap.put("schemaOid", schemaOid);
			modelMap.put("schemaMetaDataMap", schemaMetaDataMap);
		} catch (SQLException e) {
			log.error("Exception :", e);
			modelMap.put("errmessage", "???????????????");
			modelMap.put("detail", e.getMessage());
			return BaseResponse.fail(modelMap);
		}
		
		return BaseResponse.success(modelMap);
	}

	@PostMapping(value = "/judgefileEncoder")
	public BaseResponse<Map<String, Object>> judgefileEncoder(@RequestParam(value = "file") MultipartFile file) {
		Map<String, Object> modelMap = new HashMap<>();
		if(ObjectUtils.isEmpty(file)||file.getSize()<=0){
			modelMap.put("errmessage","????????????,??????????????????");
			return BaseResponse.fail(modelMap);
		}
		String fileName = file.getOriginalFilename();
		if("".equals(fileName)) {
			fileName = file.getName();
		}
		File file1 = new File(fileName);
		OutputStream out = null;
		try{
			//?????????????????????????????????????????????????????????
			out = new FileOutputStream(file1);
			byte[] ss = file.getBytes();
			for(int i = 0; i < ss.length; i++){
				out.write(ss[i]);
			}
		}catch(IOException e){
			modelMap.put("errmessage", "?????????????????????????????????");
			log.error("Exception :", e);
		}finally {
			if (out != null){
				try {
					out.close();
				} catch (IOException e) {
					log.error("Exception :", e);
				}
			}
		}
		String filecode = EncodingDetect.getJavaEncode(file1);
		log.info(filecode);
		modelMap.put("??????????????????",filecode);
		return BaseResponse.success(modelMap);
	}
	
	@PostMapping(value = "/uploadTable")
	public BaseResponse<Map<String, Object>> uploadTable(@RequestParam(value = "userId") String userId,
			                                             @RequestParam(value = "file") MultipartFile file,
			                                             @RequestParam(value = "tableName") String tableName,
			                                             @RequestParam(value = "colNames", required = false) ArrayList<String> colNames,
			                                             @RequestParam(value = "header", required = false, defaultValue = "true") boolean header
			                                             ) {
		Map<String, Object> modelMap = new HashMap<>();
		DBConnection dbConnection;
		Optional<Object> dbConnectionObjOptional = LocalCacheUtil.get(userId);
		Object dbConnectionObj = null;
		if (dbConnectionObjOptional.isPresent())
			dbConnectionObj = dbConnectionObjOptional.get();

		if (dbConnectionObj instanceof DBConnection)
			dbConnection = (DBConnection) dbConnectionObj;
		else
			dbConnection = null;

		if (dbConnection == null) {
			modelMap.put("errmessage", "????????????, ??????????????????");
			return BaseResponse.fail(modelMap);
		}
		
		//???????????????????????????xls???xlsx??????????????????????????????csv
		String fileName = file.getOriginalFilename();
		if("".equals(fileName)) {
			fileName = file.getName();
		}
		
		InputStream fileInput = null;
		InputStream csvInput = null;
		try {
			fileInput = file.getInputStream();
		
			if (fileName.matches("^.+\\.(?i)(xls)$")) {
				try {
					ByteArrayOutputStream csvOutput = new ByteArrayOutputStream();
					FileTrans.XLS2CSV(fileInput, csvOutput);
					csvInput = OutputToInput.OutputToInput(csvOutput);
				} catch (IOException e) {
					log.error("Exception :", e);
					modelMap.put("errmessage", "????????? xls?????? ????????? csv??????");
					csvInput = null;
					return BaseResponse.fail(modelMap);
				}
			} else if (fileName.matches("^.+\\.(?i)(xlsx)$")) {
				try {
					ByteArrayOutputStream csvOutput = new ByteArrayOutputStream();
					FileTrans.XLSX2CSV(fileInput, csvOutput);
					csvInput = OutputToInput.OutputToInput(csvOutput);
				} catch (IOException e) {
					log.error("Exception :", e);
					modelMap.put("errmessage", "????????? xlsx?????? ????????? csv??????");
					csvInput = null;
					return BaseResponse.fail(modelMap);
				}
			} else if (fileName.matches("^.+\\.(?i)(csv)$")) {
				try {
					ByteArrayOutputStream csvOutput = new ByteArrayOutputStream(); 
					FileTrans.transUTF8(fileInput, csvOutput, EncodeDetector.getFileEncode(file));
					csvInput = OutputToInput.OutputToInput(csvOutput); 
				} catch (IOException e) { 
					log.error("Exception :", e);
					modelMap.put("errmessage", "????????? csv?????? ????????? UTF8??????");
					csvInput = null;
					return BaseResponse.fail(modelMap); 
				}
			} else {
				modelMap.put("errmessage", "?????????????????????");
				return BaseResponse.fail(modelMap);
			}
		
			//???csv????????????????????????
			try {
				BaseConnection baseConnection = dbConnection.getPgConnect();
				DBFileIOTool dbFileIOTool = new DBFileIOTool(baseConnection);
				dbFileIOTool.fileToDatabase(tableName, colNames, csvInput, header);
				baseConnection.close();
				modelMap.put("status", "success");
			} catch (SQLException | ClassNotFoundException | IOException e) {
				log.error("Exception :", e);
				modelMap.put("errmessage", "?????????csv?????????????????????");
				modelMap.put("detail", e.getMessage());
				return BaseResponse.fail(modelMap);
			}
		
		    return BaseResponse.success(modelMap);
		} catch (IOException e) {
			log.error("Exception :", e);
			modelMap.put("errmessage", "?????????????????????????????????");
			return BaseResponse.fail(modelMap);
		} finally {
			if (fileInput != null)
				try {
					fileInput.close();
				} catch (IOException e) {
					log.error("Exception :", e);
				}
			if (csvInput != null)
				try {
					csvInput.close();
				} catch (IOException e) {
					log.error("Exception :", e);
				}
		}
	}
	
	@PostMapping(value = "/downloadTable")
	public void loadTable(@RequestParam(value = "userId") String userId,
			                                           @RequestParam(value = "fileType") String fileType,
			                                           @RequestParam(value = "tableName") String tableName,
			                                           @RequestParam(value = "colNames", required = false) ArrayList<String> colNames,
			                                           @RequestParam(value = "header", required = false, defaultValue = "false") boolean header,
			                                           HttpServletResponse response) throws IOException {
		Map<String, Object> modelMap = new HashMap<>();
		DBConnection dbConnection;
		Optional<Object> dbConnectionObjOptional = LocalCacheUtil.get(userId);
		Object dbConnectionObj = null;
		if (dbConnectionObjOptional.isPresent())
			dbConnectionObj = dbConnectionObjOptional.get();

		if (dbConnectionObj instanceof DBConnection)
			dbConnection = (DBConnection) dbConnectionObj;
		else
			dbConnection = null;

		if (dbConnection == null) {
			modelMap.put("errmessage", "????????????, ??????????????????");
			BaseResponse<Map<String, Object>> ret_Response = BaseResponse.fail(modelMap);
			PrintWriter out = response.getWriter();
			out.print(ret_Response);
			out.flush();
			out.close();
			return;
		}
		
		ByteArrayOutputStream csvOutput = new ByteArrayOutputStream();
		//?????????????????????csv?????????
		try {
			BaseConnection baseConnection = dbConnection.getPgConnect();
			DBFileIOTool dbFileIOTool = new DBFileIOTool(baseConnection);
			dbFileIOTool.fileFromDatabase(tableName, colNames, csvOutput, header);
			baseConnection.close();
		} catch (SQLException | ClassNotFoundException | IOException e) {
			log.error("Exception :", e);
			modelMap.put("errmessage", "?????????csv?????????????????????");
			BaseResponse<Map<String, Object>> ret_Response = BaseResponse.fail(modelMap);
			PrintWriter out = response.getWriter();
			out.print(ret_Response);
			out.flush();
			out.close();
			return;
		}
		
		//??????????????????????????????csv????????????????????????????????????
		String fileName = tableName;
		ByteArrayOutputStream responseOutput = new ByteArrayOutputStream();
		try {
			if ("csv".equals(fileType)) {
				fileName = tableName + ".csv";
				csvOutput.writeTo(responseOutput);
		    } else if ("xls".equals(fileType)) {
		    	fileName = tableName + ".xls";
		    	FileTrans.CSV2XLS(OutputToInput.OutputToInput(csvOutput), responseOutput);
		    } else if ("xlsx".equals(fileType)) {
		    	fileName = tableName + ".xlsx";
		    	FileTrans.CSV2XLSX(OutputToInput.OutputToInput(csvOutput), responseOutput);
		    } else {
				modelMap.put("errmessage", "?????????????????????");
				BaseResponse<Map<String, Object>> ret_Response = BaseResponse.fail(modelMap);
				PrintWriter out = response.getWriter();
				out.print(ret_Response);
				out.flush();
				out.close();
		    	return;
		    }
		} catch (IOException | CsvException e) {
			log.error("Exception :", e);
			try {
				responseOutput.close();
			} catch (IOException e1) {
				log.error("Exception :", e1);
			}
			modelMap.put("errmessage", "?????????????????????");
			BaseResponse<Map<String, Object>> ret_Response = BaseResponse.fail(modelMap);
			PrintWriter out = response.getWriter();
			out.print(ret_Response);
			out.flush();
			out.close();
	    	return;
		} finally {
			try {
				csvOutput.close();
			} catch (IOException e) {
				log.error("Exception :", e);
			}
		}
		
		//?????????HttpResponse?????????
		try {
			response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
			response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));
			responseOutput.writeTo(response.getOutputStream());
		} catch (IOException e) {
			log.error("Exception :", e);
			modelMap.put("errmessage", "?????????????????????");
			BaseResponse<Map<String, Object>> ret_Response = BaseResponse.fail(modelMap);
			PrintWriter out = response.getWriter();
			out.print(ret_Response);
			out.flush();
			out.close();
		} finally {
			try {
				responseOutput.close();
			} catch (IOException e1) {
				log.error("Exception :", e1);
			}
		}
	}

}
