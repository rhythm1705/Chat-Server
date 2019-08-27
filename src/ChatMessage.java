import java.io.Serializable;

/**
 * Chat Message class
 *
 * Class of Chat Message
 *
 * @author Sadiq and Rhythm Goel
 *
 * @version Nov 25, 2018
 *
 */

final class ChatMessage implements Serializable {
    private static final long serialVersionUID = 6898543889087L;

    private String msg;
    private int msgType;
    private String recipient;

    public ChatMessage(String msg, int msgType) {
        this.msg = msg;
        this.msgType = msgType;
    }

    public ChatMessage(String msg, int msgType, String recipient) {
        super();
        this.recipient = recipient;
    }

    public int getMsgType() {
        return msgType;
    }

    public String getMsg() {
        return msg;
    }

    public String getRecipient() {
        return recipient;
    }

}
