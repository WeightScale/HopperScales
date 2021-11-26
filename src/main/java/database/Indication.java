package database;

import java.util.Date;

public class Indication {
    public final int id;
    public final int node;
    public final Date date;
    public final int gross;
    public final int net;

    public Indication(int id, int node, Date date, int gross, int net) {
        this.id = id;
        this.node = node;
        this.date = date;
        this.gross = gross;
        this.net = net;
    }
}
