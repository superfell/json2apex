package ext;

import play.templates.JavaExtensions;

public class ApexExtensions extends JavaExtensions {

	public static String escapeApex(String src) {
		return src.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r\n", "\\n").replace("\r", "\\n").replace("\n", "\\n");
	}
}
