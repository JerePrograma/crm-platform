import java.net.HttpURLConnection;
import java.net.URI;

/** Minimal shell-free container health probe compiled during the image build. */
public final class RuntimeHealthCheck {
    private RuntimeHealthCheck() {}

    public static void main(String[] args) {
        String target = args.length == 1 ? args[0] : "http://127.0.0.1:8080/actuator/health";
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(target).toURL().openConnection();
            connection.setConnectTimeout(1_500);
            connection.setReadTimeout(1_500);
            connection.setRequestMethod("GET");
            int status = connection.getResponseCode();
            connection.disconnect();
            if (status < 200 || status >= 300) {
                System.exit(1);
            }
        } catch (Exception exception) {
            System.exit(1);
        }
    }
}
