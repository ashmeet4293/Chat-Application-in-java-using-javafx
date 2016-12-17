
import java.net.InetAddress;


public class ServerClient {
    public int port;
    public String name;
    public InetAddress address;
    public int ID;
    public int  attempt=0;
    
    public ServerClient(String name,InetAddress address, int port,final int ID){
        this.port=port;
        this.name=name;
        this.address=address;
        this.ID=ID;
    }

    public String getName() {
        return name;
    }

    public int getID() {
        return ID;
    }
    
}
