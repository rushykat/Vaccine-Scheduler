package scheduler.model;

import scheduler.db.ConnectionManager;
import java.sql.*;

public class Appointments {
    private final int id;
    private final Date time;

    private final String vaccine;
    private final String cUsername;

    private final String pUsername;

    public Appointments (int id, Date time, String vaccine, String cUsername, String pUsername) {
        this.id = id;
        this.time = time;
        this.vaccine = vaccine;
        this.cUsername = cUsername;
        this.pUsername = pUsername;
    }
    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addAppointment = "INSERT INTO Appointments VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAppointment);
            statement.setInt(1, this.id);
            statement.setDate(2, this.time);
            statement.setString(3, this.vaccine);
            statement.setString(4, this.cUsername);
            statement.setString(5, this.pUsername);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }
}