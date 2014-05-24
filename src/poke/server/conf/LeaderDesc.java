package poke.server.conf;

public class LeaderDesc {
	static String port_Leader;
	static String host_Leader;
	static int count;

	public static int getCount() {
		return count;
	}

	public static void setCount(int count) {
		LeaderDesc.count = count;
	}

	public static String getPort_Leader() {
		return port_Leader;
	}

	public void setPort_Leader(String port_Leader) {
		LeaderDesc.port_Leader = port_Leader;
	}

	public static String getHost_Leader() {
		return host_Leader;
	}

	public void setHost_Leader(String host_Leader) {
		LeaderDesc.host_Leader = host_Leader;
	}

}
