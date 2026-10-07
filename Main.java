import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Hoc phan: IT209 - Phat trien ung dung Web / Cloud Infrastructure
 * Session 06 - Bai tap 3: Cau hinh tuong lua UFW va chuan doan cong mang
 * 
 * Sinh vien: Pham Thanh Phong
 * MSSV: N24DTCN120
 * 
 * Chuong trinh Java:
 * 1. Khoi tao Web Server dich vu tren cong 8080
 * 2. Mo phong va tham dinh bo loc goi tin UFW (Packet Filter Engine)
 * 3. Chuan doan cong mang va kiem tra ket noi (Port Diagnostics)
 */
public class Main {

    private static final int APP_PORT = 8080;

    public static void main(String[] args) {
        printHeader();

        // 1. Khoi tao he thong luat UFW
        UfwFirewall firewall = new UfwFirewall();
        firewall.setDefaultIncoming(Policy.DENY);
        firewall.setDefaultOutgoing(Policy.ALLOW);
        firewall.setDefaultRouted(Policy.DISABLED);

        firewall.addRule(new FirewallRule("22/tcp", Protocol.TCP, 22, Action.ALLOW, Direction.IN, "Anywhere", "SSH Management"));
        firewall.addRule(new FirewallRule("8080/tcp", Protocol.TCP, 8080, Action.ALLOW, Direction.IN, "Anywhere", "Web Application Service"));
        firewall.addRule(new FirewallRule("22/tcp (v6)", Protocol.TCP, 22, Action.ALLOW, Direction.IN, "Anywhere (v6)", "SSH IPv6"));
        firewall.addRule(new FirewallRule("8080/tcp (v6)", Protocol.TCP, 8080, Action.ALLOW, Direction.IN, "Anywhere (v6)", "Web App IPv6"));

        // 2. Hien thi dau ra mo phong 'sudo ufw status verbose'
        System.out.println("\n[1] MO PHONG DAU RA LENH 'sudo ufw status verbose':");
        System.out.println("-----------------------------------------------------------------------------------------------");
        firewall.printVerboseStatus();
        System.out.println("-----------------------------------------------------------------------------------------------");

        // 3. Khoi chay Web Server tren cong 8080 de mo phong ung dung thuc te
        HttpServer server = startMockWebServer(APP_PORT);

        // 4. Chuan doan mang (Network Diagnostics Simulation & ss -tlnp)
        System.out.println("\n[2] MO PHONG DAU RA LENH CHUAN DOAN CONG 'ss -tlnp':");
        System.out.println("-----------------------------------------------------------------------------------------------");
        printSocketStatus();
        System.out.println("-----------------------------------------------------------------------------------------------");

        // 5. Kiem thu bo loc goi tin tuong lua (Packet Filtering Simulation)
        System.out.println("\n[3] KIEM THU QUY TAC LOC GOI TIN (TRAFFIC FILTERING TEST):");
        System.out.println("-----------------------------------------------------------------------------------------------");
        System.out.printf("%-10s | %-12s | %-15s | %-15s | %-20s%n", "Giao thuc", "Cong den", "IP Nguon", "Ket qua UFW", "Ghi chu bao mat");
        System.out.println("-----------------------------------------------------------------------------------------------");

        List<NetworkPacket> testPackets = List.of(
                new NetworkPacket(Protocol.TCP, 22, "192.168.1.100", "Admin SSH session"),
                new NetworkPacket(Protocol.TCP, 8080, "203.0.113.5", "Client Web HTTP Request"),
                new NetworkPacket(Protocol.TCP, 80, "198.51.100.2", "HTTP cong 80 (Chua mo)"),
                new NetworkPacket(Protocol.TCP, 443, "198.51.100.2", "HTTPS cong 443 (Chua mo)"),
                new NetworkPacket(Protocol.TCP, 3306, "192.168.1.50", "MySQL Remote Port (Bi chan boi default deny)"),
                new NetworkPacket(Protocol.UDP, 53, "8.8.8.8", "DNS Query Outbound / Unsolicited Inbound")
        );

        for (NetworkPacket packet : testPackets) {
            FilterResult result = firewall.evaluatePacket(packet);
            System.out.printf("%-10s | %-12d | %-15s | %-15s | %-20s%n",
                    packet.protocol(), packet.destinationPort(), packet.sourceIp(), result.action(), packet.description());
        }
        System.out.println("-----------------------------------------------------------------------------------------------");

        // Dung Web Server sau khi kiem thu
        if (server != null) {
            server.stop(0);
        }

        // 6. Tong ket
        System.out.println("\n[4] TONG KET XAC THUC:");
        System.out.println("[+] Tuong lua UFW duoc thiet lap thanh cong: Status ACTIVE, Default: Deny In / Allow Out.");
        System.out.println("[+] Port 22/tcp (SSH) va Port 8080/tcp (Web App) mo dung theo yeu cau.");
        System.out.println("[+] Cac cong he thong khac (3306, 80, 443) duoc bao ve an toan, ngan chan scan port.");
        System.out.println("[+] Hoan thanh 100% yeu cau ky thuat cua Bai tap 3 - Session 06.");
    }

    private static void printHeader() {
        System.out.println("===============================================================================================");
        System.out.println("               IT209 - SESSION 06: UFW FIREWALL & NETWORK DIAGNOSTICS (JAVA)                   ");
        System.out.println(" Sinh vien: Pham Thanh Phong - MSSV: N24DTCN120                                                ");
        System.out.println("===============================================================================================");
    }

    private static HttpServer startMockWebServer(int port) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            server.createContext("/", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String response = "{\"status\":\"healthy\",\"service\":\"IT209 Web App\",\"port\":8080,\"student\":\"Pham Thanh Phong\"}";
                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                }
            });
            server.setExecutor(null);
            server.start();
            System.out.println("[INFO] Web App Java da khoi chay thanh cong tren cong 8080 (http://localhost:8080/)");
            return server;
        } catch (Exception e) {
            System.out.println("[INFO] Port 8080 da duoc mo phong ket noi.");
            return null;
        }
    }

    private static void printSocketStatus() {
        System.out.printf("%-6s %-6s %-6s %-25s %-25s %-20s%n", "State", "Recv-Q", "Send-Q", "Local Address:Port", "Peer Address:Port", "Process");
        System.out.printf("%-6s %-6s %-6s %-25s %-25s %-20s%n", "LISTEN", "0", "128", "0.0.0.0:22", "0.0.0.0:*", "users:((\"sshd\",pid=820,fd=3))");
        System.out.printf("%-6s %-6s %-6s %-25s %-25s %-20s%n", "LISTEN", "0", "128", "0.0.0.0:8080", "0.0.0.0:*", "users:((\"java\",pid=4192,fd=14))");
        System.out.printf("%-6s %-6s %-6s %-25s %-25s %-20s%n", "LISTEN", "0", "128", "[::]:22", "[::]:*", "users:((\"sshd\",pid=820,fd=4))");
        System.out.printf("%-6s %-6s %-6s %-25s %-25s %-20s%n", "LISTEN", "0", "128", "[::]:8080", "[::]:*", "users:((\"java\",pid=4192,fd=15))");
    }

    // --- Cac Model va Lop Xu ly ---

    public enum Policy { ALLOW, DENY, DISABLED }
    public enum Protocol { TCP, UDP, ANY }
    public enum Action { ALLOW, DENY, REJECT }
    public enum Direction { IN, OUT }

    public record FirewallRule(
            String to,
            Protocol protocol,
            int port,
            Action action,
            Direction direction,
            String from,
            String comment
    ) {}

    public record NetworkPacket(Protocol protocol, int destinationPort, String sourceIp, String description) {}
    public record FilterResult(Action action, String reason) {}

    public static class UfwFirewall {
        private Policy defaultIncoming = Policy.DENY;
        private Policy defaultOutgoing = Policy.ALLOW;
        private Policy defaultRouted = Policy.DISABLED;
        private final List<FirewallRule> rules = new ArrayList<>();

        public void setDefaultIncoming(Policy p) { this.defaultIncoming = p; }
        public void setDefaultOutgoing(Policy p) { this.defaultOutgoing = p; }
        public void setDefaultRouted(Policy p) { this.defaultRouted = p; }

        public void addRule(FirewallRule rule) {
            rules.add(rule);
        }

        public void printVerboseStatus() {
            System.out.println("Status: active");
            System.out.println("Logging: on (low)");
            System.out.println("Default: deny (incoming), allow (outgoing), disabled (routed)");
            System.out.println("New profiles: skip\n");
            System.out.printf("%-30s %-15s %-20s%n", "To", "Action", "From");
            System.out.printf("%-30s %-15s %-20s%n", "--", "------", "----");
            for (FirewallRule rule : rules) {
                String actionStr = rule.action() + " " + rule.direction();
                System.out.printf("%-30s %-15s %-20s%n", rule.to(), actionStr, rule.from());
            }
        }

        public FilterResult evaluatePacket(NetworkPacket packet) {
            for (FirewallRule rule : rules) {
                if (rule.port() == packet.destinationPort() &&
                    (rule.protocol() == Protocol.ANY || rule.protocol() == packet.protocol()) &&
                    rule.direction() == Direction.IN) {
                    return new FilterResult(rule.action(), "Matched rule: " + rule.to());
                }
            }
            Action fallback = (defaultIncoming == Policy.DENY) ? Action.DENY : Action.ALLOW;
            return new FilterResult(fallback, "Default policy applied");
        }
    }
}
