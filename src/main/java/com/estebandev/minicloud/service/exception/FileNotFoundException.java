package com.estebandev.minicloud.service.exception;

import java.nio.file.Path;

public class FileNotFoundException extends FileManagerException{

	public FileNotFoundException() {
		super("File not exists");
	}

	public FileNotFoundException(Path path) {
		super(String.format("File not exists %s", path.toString()));
	}
}
