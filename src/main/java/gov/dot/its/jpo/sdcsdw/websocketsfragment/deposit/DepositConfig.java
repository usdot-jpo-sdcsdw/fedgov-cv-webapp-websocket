package gov.dot.its.jpo.sdcsdw.websocketsfragment.deposit;


public class DepositConfig {

	public String systemName;
	public String websocketURL;
	public String keystoreFile;
	public String keystorePassword;
	
	public DepositConfig() {
		super();
	}

	@Override
	public String toString() {
		return "DepositConfig [systemName=" + systemName + ", websocketURL="
				+ websocketURL + ", keystoreFile=" + keystoreFile
				+ ", keystorePassword=" + keystorePassword + "]";
	}
	
}
