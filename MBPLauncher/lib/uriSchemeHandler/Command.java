package uriSchemeHandler;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Scanner;

public class Command {

	private String result;
	private String error;
	private final String[] args;

	public Command(final String... args){
		this.args = args;
	}
	
	public Command(final String command){
		this(new String[]{command});
	}
	
	public CommandResult run() throws IOException {
		final Process process = Runtime.getRuntime().exec(args);
		return collectResult(process);
	}

	private CommandResult collectResult(final Process process) throws IOException {
		final InputStream inputStream = process.getInputStream();
		final InputStream errorStream = process.getErrorStream();
		result = streamToString(inputStream);
		error = streamToString(errorStream);

		return new CommandResult(result,error);
	}

	static String streamToString(InputStream s) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		int b;
		while ((b = s.read()) != -1)
		{
			bos.write(b);
		}

		if (bos.size() == 0) return "";
		return new String(bos.toByteArray());
	}
}
