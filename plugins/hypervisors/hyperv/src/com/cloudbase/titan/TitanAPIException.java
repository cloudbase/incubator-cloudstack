package com.cloudbase.titan;

public class TitanAPIException extends Exception {

	private static final long serialVersionUID = -2762269374269316406L;

	public TitanAPIException(Exception e) {
		super(e);
	}

	public TitanAPIException(String details) {
		super(details);
	}
}
