package uriSchemeHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;

public class WindowsURISchemeHandler implements RealURISchemeHandler {

	private static final String REGSTRY_TYPE_SZ = "REG_SZ";
	private static final String REGSTRY_TYPE_SZ_EXPANDED = "REG_EXPAND_SZ";

	public static String getCommandForUrl(final URI uri) throws Exception {
		final String schemeName = uri.getScheme();
		final String sysdir = System.getenv("WINDIR") + "\\system32\\reg";
		final String[] commandString = new String[]{sysdir,"query","HKEY_CURRENT_USER\\SOFTWARE\\Classes\\" + schemeName + "\\shell\\open\\command","/ve"};
		final Command command = new Command(commandString);
		final CommandResult commandResult = command.run();
		
		if(commandResult.anyErrorOccured()){
			throw new Exception(commandResult.getError());
		}
		
		final String result = commandResult.getResult();
		final String uriAsString = uri.toString();
		
		return getCommandFor(result, uriAsString);
	}

	public static String getCommandFor(final String regDotExeOutput, final String uriAsString) {
		int valueTypeIndex = regDotExeOutput.indexOf(REGSTRY_TYPE_SZ);
		String regSZ = REGSTRY_TYPE_SZ;
		if (valueTypeIndex == -1){
			valueTypeIndex = regDotExeOutput.indexOf(REGSTRY_TYPE_SZ_EXPANDED);
			regSZ = REGSTRY_TYPE_SZ_EXPANDED;
		}
		if (valueTypeIndex == -1){
			throw new RuntimeException(REGSTRY_TYPE_SZ+" or "+REGSTRY_TYPE_SZ_EXPANDED+" not found.");
		}

		final String resultExecutable = regDotExeOutput.substring(valueTypeIndex + regSZ.length()).trim();
		
		final String finalCommand =  resultExecutable.replaceAll("%[1lL]", uriAsString);
		return finalCommand;
	}

	public static String[] commandToStringArray(final String command){
		final ArrayList<String> arrayList = new ArrayList<String>();
		String lastToken = "";
		boolean ignoreChar = false;
		for (final char c : command.toCharArray()) {
			if(c == '"' && !ignoreChar){
				if(!lastToken.trim().isEmpty())
					arrayList.add(lastToken);
				lastToken = "";
				continue;
			}
			lastToken += c;
			if(ignoreChar){
				ignoreChar = false;
			}
			if(c == '\\'){
				ignoreChar = true;
			}
			
		}
		return arrayList.toArray(new String[0]);
	}
	
	public void open(final URI uri) throws Exception {
		final String commandForUri = getCommandForUrl(uri);
		Runtime.getRuntime().exec(commandToStringArray(commandForUri));
	}

	static String getJavaPath(){
		String tmp1 = System.getProperty("java.home") + "\\bin\\javaw.exe";
		String tmp2 = System.getProperty("sun.boot.library.path") + "\\javaw.exe";
		String tmp3 = System.getProperty("java.library.path")+ "\\javaw.exe";
		if(new File(tmp1).exists()) {
			return tmp1;
		}else if(new File(tmp2).exists()){
			return tmp2;
		}else if(new File(tmp3).exists()) {
			return tmp3;
		}else{
			String[] paths = System.getenv("PATH").split(";");
			for(String path:paths){
				if(new File(path + "\\javaw.exe").exists()){
					return path + "\\javaw.exe";
				}
			}
		}
		return "";
	}

	public void register(final String schemeName, final String applicationPath) throws IOException {
		final String escapedApplicationPath = applicationPath.replaceAll("\\\\", "\\\\\\\\");
		String appendStr = "";
		String javaPath = getJavaPath();
		appendStr = "@=\"" + javaPath.replaceAll("\\\\", "\\\\\\\\") + " -jar \\\"" + escapedApplicationPath + "\\\" \\\"%1\\\"\"";
		final String regFile =
				"Windows Registry Editor Version 5.00\r\n" +
				"\r\n" +
				"[HKEY_CURRENT_USER\\SOFTWARE\\Classes\\"+schemeName+"]\r\n" +
				"@=\""+schemeName+" URI\"\r\n" +
				"\"URL Protocol\"=\"\"\r\n" +
				"\"Content Type\"=\"application/x-"+schemeName+"\"\r\n" +
				"\r\n" +
				"[HKEY_CURRENT_USER\\SOFTWARE\\Classes\\"+schemeName+"\\shell]\r\n" +
				"@=\"open\"\r\n" +
				"\r\n" +
				"[HKEY_CURRENT_USER\\SOFTWARE\\Classes\\"+schemeName+"\\shell\\open]\r\n" +
				"\r\n" +
				"[HKEY_CURRENT_USER\\SOFTWARE\\Classes\\"+schemeName+"\\shell\\open\\command]\r\n" +
				appendStr;

		File tempFile;
		try {
			tempFile = File.createTempFile("URLPROTOCOLHANDLERCREATION", ".reg");
			FileWriter fw = new FileWriter(tempFile);
			fw.write(regFile);
			fw.close();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		
		
		final String[] commandStrings = new String[]{"cmd", "/c", "regedit","/s",tempFile.getAbsolutePath()};
		final Command command = new Command(commandStrings);
		command.run();
	}

}
