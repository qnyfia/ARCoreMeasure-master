package com.AC.Measure.model;

public class Image {

	private String name;
	private Float x;
	private String result;
	private String message;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Float getX() {
		return x;
	}

	public void setX(Float x) {
		this.x = x;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getMessage() {
		return message == null? "" : message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
}
