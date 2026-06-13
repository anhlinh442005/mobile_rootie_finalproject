public class Test {
    public static void main(String[] args) throws Exception {
        byte[] bytes = java.security.MessageDigest.getInstance("SHA-256").digest("password123".getBytes());
        StringBuilder sb = new StringBuilder();
        for(byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        System.out.println(sb.toString());
    }
}
