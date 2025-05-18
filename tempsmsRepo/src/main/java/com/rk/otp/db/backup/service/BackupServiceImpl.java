package com.rk.otp.db.backup.service;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.support.Repositories;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.rk.app.file.service.FileStorageService;
import com.rk.app.mail.CustomMailSender;
import com.rk.app.persistence.entity.User;

import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

@Service
public class BackupServiceImpl implements BackupService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BackupServiceImpl.class);

	@Autowired
	private CustomMailSender mailSender;

	@Autowired
	private FileStorageService fileStorageService;

	@Autowired
	private ApplicationContext applicationContext;

	@Override
	public void backupUserDetails() throws IOException {
		String fileName = backupTable(User.class);
		mailSender.sendBackupFile("User Details Backup - " + LocalDate.now(ZoneId.of("Asia/Kolkata")), fileName);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public String backupTable(Class<?> clazz) throws IOException {
		JpaRepository jpaRepo = getJpaRepo(clazz);
		List list = jpaRepo.findAll();

		return createCSV(clazz, list);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<String> backupEverything() {
		Repositories repositories = new Repositories(
				(ListableBeanFactory) applicationContext.getAutowireCapableBeanFactory());
		Iterator it = repositories.iterator();
		List<String> fileNameList = new ArrayList<>();
		while (it.hasNext()) {
			Class<?> domainClass = (Class<?>) it.next();
			JpaRepository repo = (JpaRepository) repositories.getRepositoryFor(domainClass).get();
			List<Object> list = repo.findAll();
			if (list.isEmpty())
				continue;

			try {
				fileNameList.add(createCSV(domainClass, list));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		LOGGER.debug("All backup files created");

		return fileNameList;
	}

	@SuppressWarnings("rawtypes")
	private JpaRepository getJpaRepo(Class<?> domainClass) {
		Repositories repositories = new Repositories(
				(ListableBeanFactory) applicationContext.getAutowireCapableBeanFactory());
		return (JpaRepository) repositories.getRepositoryFor(domainClass).get();
	}

	private String createCSV(Class<?> domainClass, List<Object> entityList) throws IOException {
		String className = domainClass.getSimpleName();
		String fileName = className + ".csv";
		File createdFile = fileStorageService.getFilePath(fileName);
		try (FileWriter fw = new FileWriter(createdFile); CSVWriter writer = new CSVWriter(fw)) {
			List<String[]> fieldList = new ArrayList<>();
			try {
				Map<String, Field> columnMap = getColumnNamesMap(domainClass);
				fieldList.add(columnMap.keySet().toArray(new String[] {}));

				addRecords(domainClass, entityList, columnMap, fieldList);

			} catch (Exception e) {
				e.printStackTrace();
			}

			writer.writeAll(fieldList);
			writer.flush();
		}

		return fileStorageService.storeBackupFile(createdFile);
	}

	private void addRecords(Class<?> domainClass, List<Object> entityList, Map<String, Field> columnMap,
			List<String[]> fieldList) {
		entityList.forEach(entity -> {
			List<String> fieldValuesList = new ArrayList<>();
			for (Entry<String, Field> entry : columnMap.entrySet()) {
				try {
					Object result = null;
					Method method = null;
					Field field = entry.getValue();
					String columnName = entry.getKey();
					String substring = columnName.substring(columnName.lastIndexOf(".") + 1);
					if (substring.startsWith("is") && field.getType().isAssignableFrom(boolean.class)) {
						method = domainClass.getMethod(columnName);
					} else if (columnName.startsWith("foreign_")) {
						result = processForeignKeyRecord(domainClass, columnName, method, field, entity);
					} else {
						method = domainClass.getMethod("get" + StringUtils.capitalize(columnName));
					}
					if (result == null) {
						result = method.invoke(entity);
					}

					fieldValuesList.add(result == null ? null : String.valueOf(result));
				} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					System.out.println("Error for " + domainClass + " - " + e.getMessage());
					e.printStackTrace();
					break;
				}
			}

			fieldList.add(fieldValuesList.toArray(new String[] {}));
		});

	}

	private Object processForeignKeyRecord(Class<?> domainClass, String columnName, Method method, Field field,
			Object entity)
			throws NoSuchMethodException, SecurityException, IllegalAccessException, InvocationTargetException {
		method = domainClass
				.getMethod("get" + StringUtils.capitalize(columnName.substring(columnName.lastIndexOf("_") + 1)));
		Class<?> resultClass = field.getType();

		Object result = resultClass.cast(method.invoke(entity));

		Optional<Field> f2 = Stream.of(resultClass.getDeclaredFields())
				.filter(f -> isDeclaredAnnotatedField(f, Id.class)).findFirst();
		if (f2.isPresent()) {
			return callGetMethod(resultClass, f2.get(), result);
		}

		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object callGetMethod(Class resultClass, Field field, Object result) {
		try {
			Method method = resultClass.getMethod("get" + StringUtils.capitalize(field.getName()));
			return method.invoke(result);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}

		return null;
	}

	/****************** Common for backup and Restore **********************/

	private Map<String, Field> getColumnNamesMap(Class<?> domainClass) {
		Map<String, Field> map = new HashMap<>();

		Field[] fields = domainClass.getDeclaredFields();

		for (Field f : fields) {
			if (!Modifier.isStatic(f.getModifiers()) && !isDeclaredAnnotatedField(f, JoinColumn.class)) {
				map.put(f.getName(), f);
			} else if (!Modifier.isStatic(f.getModifiers()) && isDeclaredAnnotatedField(f, JoinColumn.class)) {
				map.put("foreign_" + f.getType().getSimpleName(), f);
			}
		}

		return map;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private boolean isDeclaredAnnotatedField(Field field, Class annotationClass) {
		return field.getDeclaredAnnotation(annotationClass) != null;
	}

	/*******************************************************************************************
	 *********************************** RESTORE BACKUP ****************************************
	 *******************************************************************************************/

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean restoreBackup(String fileName) {
		CompletableFuture.runAsync(() -> {
			File file = fileStorageService.loadFile(fileName);
			LOGGER.info(file.getAbsolutePath());
			String fileNameFinal;
			if (fileName.contains("("))
				fileNameFinal = fileName.substring(0, fileName.lastIndexOf(".")).substring(0, fileName.indexOf("("))
						.trim();
			else {
				fileNameFinal = fileName.substring(0, fileName.lastIndexOf("."));
			}
			Repositories repositories = new Repositories(
					(ListableBeanFactory) applicationContext.getAutowireCapableBeanFactory());
			Map<String, Class> nameClassMap = new HashMap<>();
			repositories.forEach(r -> {
				String simpleName = r.getSimpleName();
				if (simpleName.equalsIgnoreCase(fileNameFinal)) {
					nameClassMap.put(fileNameFinal, r);
				}
			});

			// Get the JpaRepo for the entity class
			JpaRepository jpaRepo = (JpaRepository) repositories
					.getRepositoryFor(nameClassMap.values().stream().findFirst().get()).get();

			Set<Object> set = new HashSet<>();
//			set.addAll(jpaRepo.findAll()); //Adding existing records
			jpaRepo.deleteAll(); // Delete All
			try (CSVReader reader = new CSVReader(new FileReader(file))) {
				List<String[]> recordList = reader.readAll();
				Class clazz = nameClassMap.values().stream().findFirst().get(); // Get the entity class name
				HashMap<String, Integer> fieldNamesMap = new HashMap<>();
				recordList.stream().limit(1).forEach(r -> {
					IntStream.range(0, r.length).forEach(i -> fieldNamesMap.put(r[i], i));
				});
				recordList.stream().skip(1).forEach(arr -> {
					try {
						Object obj = clazz.getDeclaredConstructor().newInstance();
						setAllFields(clazz, obj, fieldNamesMap, arr, repositories);
						set.add(obj);
					} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
							| InvocationTargetException | NoSuchMethodException | SecurityException e) {
						LOGGER.error("Parse Exception");
					}
				});
			} catch (IOException | CsvException e) {
				e.printStackTrace();
			}

			jpaRepo.saveAllAndFlush(set);

			String message = "Total records inserted = " + set.size() + "\n\n";
			System.out.println(message);
			mailSender.sendEmailToAdmin(fileName + " Restore Complete", message);
			
		}, Executors.newFixedThreadPool(3));

		return true;
	}

	@SuppressWarnings({ "rawtypes" })
	private void setAllFields(Class clazz, Object obj, final HashMap<String, Integer> fieldNamesMap, String[] record,
			Repositories repositories) {
		Map<String, Field> columnNamesMap = getColumnNamesMap(clazz);
		columnNamesMap.entrySet().stream().forEach(entry -> {
//			String fieldName = entry.getKey().substring(entry.getKey().lastIndexOf(".") + 1);
			String fieldName = entry.getKey();
			Integer index = fieldNamesMap.get(fieldName);
			Class<?> type = entry.getValue().getType();
			try {
				Map<Class, Object> valueTypeMap = null;
				if (fieldName.startsWith("foreign_")) {
					valueTypeMap = Map.of(type, getLinkedObject(record[index], type, repositories));
					fieldName = fieldName.replace("foreign_", "");
				} else {
					valueTypeMap = parseField(record[index], type);
				}
				
				setField(valueTypeMap, fieldName, clazz, type, obj);

			} catch (IllegalArgumentException | SecurityException | NoSuchMethodException | IllegalAccessException
					| InvocationTargetException e) {

				e.printStackTrace();
			} catch (Exception e) {
				LOGGER.error("Exception occurred :: {}", e.getLocalizedMessage());
			}
		});
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object getLinkedObject(String id, Class<?> type, Repositories repositories) {
		JpaRepository jpaRepo = (JpaRepository) repositories.getRepositoryFor(type).get();
		return type.cast(jpaRepo.getReferenceById(id));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void setField(Map<Class, Object> valueTypeMap, String fieldName, Class clazz, Class<?> type, Object obj)
			throws NoSuchMethodException, SecurityException, IllegalAccessException, InvocationTargetException {
		Class fieldType = valueTypeMap.keySet().stream().findFirst().get();
		Collection<Object> values = valueTypeMap.values();
		Object param = null;
		if (values != null && !values.contains("null"))
			param = values.stream().findFirst().orElse(null);

		Method method = null;
		if (fieldName.startsWith("is") && type.isAssignableFrom(boolean.class))
			method = clazz.getMethod("set" + StringUtils.capitalize(fieldName.substring(2)), fieldType);
		else
			method = clazz.getMethod("set" + StringUtils.capitalize(fieldName), fieldType);

		method.invoke(obj, param);

	}

	@SuppressWarnings("rawtypes")
	private Map<Class, Object> parseField(String value, Class<?> type) {
		Map<Class, Object> result = new HashMap<>();
		if (type.isAssignableFrom(Boolean.class)) {
			result.put(Boolean.class, parseBoolean(value));
		} else if (type.isAssignableFrom(boolean.class)) {
			result.put(boolean.class, parseBoolean(value));
		} else if (type.isAssignableFrom(Integer.class)) {
			result.put(Integer.class, parseInt(value));
		} else if (type.isAssignableFrom(int.class)) {
			result.put(int.class, parseInt(value));
		} else if (type.isAssignableFrom(Long.class)) {
			result.put(Long.class, parseLong(value));
		} else if (type.isAssignableFrom(long.class)) {
			result.put(long.class, parseLong(value));
		} else if (type.isAssignableFrom(Double.class)) {
			result.put(Double.class, parseDouble(value));
		} else if (type.isAssignableFrom(double.class)) {
			result.put(double.class, parseDouble(value));
		} else if (type.isAssignableFrom(Float.class)) {
			result.put(Float.class, parseFloat(value));
		} else if (type.isAssignableFrom(float.class)) {
			result.put(float.class, parseFloat(value));
		} else if (type.isAssignableFrom(Date.class)) {
			result.put(Date.class, parseDate(value));
		} else if (type.isAssignableFrom(LocalDateTime.class)) {
			result.put(LocalDateTime.class, parseLocalDateTime(value));
		} else if (type.isAssignableFrom(LocalDate.class)) {
			result.put(LocalDate.class, parseLocalDate(value));
		} else if (type.isAssignableFrom(Timestamp.class)) {
			result.put(Timestamp.class, parseTimestamp(value));
		} else if (type.isAssignableFrom(String.class)) {
			result.put(String.class, isNull(value) ? null : value);
		}

		return result;
	}

	private Object parseLocalDate(String val) {
		if (isNull(val)) {
			return null;
		}
		LocalDate localDate = null;
		try {
			localDate = LocalDate.parse(val);
		} catch (Exception e) {
			localDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
		}

		return localDate;
	}

	private Object parseLocalDateTime(String val) {
		if (isNull(val)) {
			return null;
		}
		LocalDateTime localDateTime = null;
		try {
			localDateTime = LocalDateTime.parse(val);
		} catch (Exception e) {
			localDateTime = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
		}

		return localDateTime;
	}

	private Timestamp parseTimestamp(String val) {
		if (isNull(val)) {
			return null;
		}
		Timestamp timestamp = null;
		try {
			timestamp = Timestamp.valueOf(val);
		} catch (Exception e) {
			timestamp = Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
		}

		return timestamp;
	}

	private Date parseDate(String val) {
		if (isNull(val)) {
			return null;
		}
		Date date = null;
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			date = df.parse(val);
		} catch (Exception e) {
		}

		return date;
	}

	private float parseFloat(String val) {
		float f = 0;
		try {
			f = Float.parseFloat(val);
		} catch (Exception e) {
		}

		return f;
	}

	private double parseDouble(String val) {
		double d = 0;
		try {
			d = Double.parseDouble(val);
		} catch (Exception e) {
		}

		return d;
	}

	private long parseLong(String val) {
		long l = 0;
		try {
			l = Long.parseLong(val);
		} catch (Exception e) {
		}

		return l;
	}

	private Integer parseInt(String val) {
		Integer i = 0;
		try {
			i = Integer.parseInt(val);
		} catch (Exception e) {
		}

		return i;
	}

	private Boolean parseBoolean(String val) {
		Boolean b = false;
		try {
			b = Boolean.parseBoolean(val);
		} catch (Exception e) {
		}

		return b;
	}

	private boolean isNull(String val) {
		return "null".equalsIgnoreCase(val) || "".equalsIgnoreCase(val);
	}
}
