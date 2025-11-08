package com.teambind.springproject.common.util.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component("jsonUtilWithObjectMapper")
public class JsonUtilWithObjectMapper implements JsonUtil {
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	public String toJson(Object object) {
		try {
			return objectMapper.writeValueAsString(object);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public <T> T fromJson(String json, Class<T> clazz) {
		try {
			return objectMapper.readValue(json, clazz);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
