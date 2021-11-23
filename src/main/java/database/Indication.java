package database;

import java.util.Date;

public class Indication {
    public final int id;
    public final Date date;
    public final int gross;
    public final int net;

    public Indication(int id, Date date, int gross, int net) {
        this.id = id;
        this.date = date;
        this.gross = gross;
        this.net = net;
    }
}
