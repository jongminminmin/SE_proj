package ac.kr.changwon.se_proj.Service;

import ac.kr.changwon.se_proj.Repository.ContentRepository;
import ac.kr.changwon.se_proj.UserRepository.Content;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Service
public class ChatService {

    private DatagramSocket socket;
    private final ContentRepository contentRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(); //작업 병렬 처리

    @Autowired
    public ChatService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    public void startServer() throws IOException {
        int port = 5000;
        socket = new DatagramSocket(port);

        System.out.println("Server started");

        executor.submit(() -> {
            while (true) {
                byte[] buf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(packet);
                }
                catch (IOException e) {
                    System.err.println("Error receiving packet: " + e.getMessage());
                    continue;
                }

                String received = new String(packet.getData());
                System.out.println("Received: " + received);

                saveMessage(received);
            }
        });
    }

    public void stopServer() {
        socket.close();
        executor.shutdownNow();
    }

    /*메시지 송수신 메서드*/

    public void sendMessage(String message) throws IOException {
        InetAddress address = InetAddress.getByName("localhost");
        int port = 44521;

        byte[] buf = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
        socket.send(packet);
    }

    public void saveMessage(String message) {
        Content content = new Content();
        content.setContent(message);
        contentRepository.save(content);
    }


}
