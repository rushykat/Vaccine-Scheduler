package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Appointments;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.awt.desktop.ScreenSleepEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) throws SQLException {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        int count = 0;
        while (true) {
            count++;
            if (count > 1) {
                System.out.println();
                System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
                System.out.println("*** Please enter one of the following commands ***");
                System.out.println("> create_patient <username> <password>");
                System.out.println("> create_caregiver <username> <password>");
                System.out.println("> login_patient <username> <password>");
                System.out.println("> login_caregiver <username> <password>");
                System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
                System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
                System.out.println("> upload_availability <date>");
                System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
                System.out.println("> add_doses <vaccine> <number>");
                System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
                System.out.println("> logout");  // TODO: implement logout (Part 2)
                System.out.println("> quit");
                System.out.println();
            }
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // TODO: Part 1
        if (tokens.length != 3) {
            System.out.println("Failed to create patient");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // TODO: Part 1
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) throws SQLException {
        // TODO: Part 2
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }
        if (tokens.length != 2) {
            System.out.println("Please try again");
            return;
        }

        String date = tokens[1];
        Date d = Date.valueOf(date);

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String checkAvailabilities = "SELECT Time, Username FROM Availabilities WHERE Time = ? ORDER BY Username ASC";

        try {
            PreparedStatement statement = con.prepareStatement(checkAvailabilities);
            statement.setDate(1,  d);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String availableCaregiver = resultSet.getString("Username");
                System.out.println(availableCaregiver);
            }
        } catch (SQLException e) {
            throw new SQLException();
        }

        String getDoses = "SELECT * FROM Vaccines";
        try {
            PreparedStatement statement = con.prepareStatement(getDoses);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String vaccineName = resultSet.getString("Name");
                int vaccineDose = resultSet.getInt("Doses");
                System.out.println(vaccineName + " " + vaccineDose);
            }
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) throws SQLException {
        // TODO: Part 2
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }
        if (currentPatient == null) {
            System.out.println("Please login as a patient");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }

        // gets the vaccine that patient wants
        Vaccine.VaccineGetter getter = new Vaccine.VaccineGetter(tokens[2]);
        Vaccine vax = getter.get();
        try {
            // checks if there are any doses
            if (vax.getAvailableDoses() == 0) {
                System.out.println("Not enough available doses");
                return;
            }
        } catch (NullPointerException e) {
            System.out.println(tokens[2] + " not available. Please try Again");
            return;
        }

        // gets date that patient wants to reserve
        String date = tokens[1];
        Date d = Date.valueOf(date);

        // opens azure db
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String checkAvailabilities = "SELECT Time, Username FROM Availabilities WHERE Time = ? ORDER BY Username ASC";
        try {
            // checks which caregivers are available on that date
            PreparedStatement statement = con.prepareStatement(checkAvailabilities);
            statement.setDate(1,  d);
            ResultSet resultSet = statement.executeQuery();

            // gets all the names in the availabilities table
            ArrayList<String> caregivers = new ArrayList<>();
            while (resultSet.next()) {
                String name = resultSet.getString("Username");
                caregivers.add(name);
            }
            if (caregivers.isEmpty()) {
                System.out.println("No caregiver is available");
                return;
            } else {

                String currCaregiver = caregivers.get(0);

                // creating the appointment id
                String idCount = "SELECT COUNT(*) AS Count FROM Appointments";
                PreparedStatement getID = con.prepareStatement(idCount);
                ResultSet idResultSet = getID.executeQuery();
                int appID = -1;
                if (idResultSet.next()) {
                    appID = idResultSet.getInt("Count") + 1;
                }

                // adds an appointment into the Appointments table
                Appointments app = new Appointments(appID, d, vax.getVaccineName(), currCaregiver, currentPatient.getUsername());
                app.saveToDB();

                // removing the reserved caregiver and time from availabilities
                String removeCaregiver = "DELETE FROM Availabilities WHERE Username = ? AND Time = ?";
                PreparedStatement removeStatement = con.prepareStatement(removeCaregiver);
                removeStatement.setString(1, currCaregiver);
                removeStatement.setDate(2, d);
                removeStatement.executeUpdate();

                vax.decreaseAvailableDoses(1);

                System.out.println("Appointment ID: " + appID + ", Caregiver username: " + currCaregiver);

            }
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }
        if (tokens.length != 2) {
            System.out.println("Please try again");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String appID = tokens[1];
        int id = Integer.parseInt(appID);

        try {
            String getVax = "SELECT Time, cUsername, Vaccine FROM Appointments WHERE id = ?";

            PreparedStatement statement = con.prepareStatement(getVax);
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();

            String vaccine = "";
            Date time = null;
            String name = "";

            while (resultSet.next()) {
                vaccine = resultSet.getString("Vaccine");
                time = resultSet.getDate("Time");
                name = resultSet.getString("cUsername");
            }

            Vaccine.VaccineGetter vaccineGetter = new Vaccine.VaccineGetter(vaccine);
            Vaccine vax = vaccineGetter.get();
            vax.increaseAvailableDoses(1);

            String addAvail = "INSERT INTO Availabilities Values (?, ?)";
            PreparedStatement statement2 = con.prepareStatement(addAvail);
            statement2.setDate(1, time);
            statement2.setString(2, name);
            statement2.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        String delApp = "DELETE FROM Appointments WHERE id = ?";

        try {
            PreparedStatement statement = con.prepareStatement(delApp);
            statement.setInt(1, id);
            int num = statement.executeUpdate();

            if (num > 0) {
                System.out.println("Appointment Canceled");
            } else {
                System.out.println("No appointment found");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, means that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) throws SQLException {
        // TODO: Part 2
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }
        if (tokens.length != 1) {
            System.out.println("Please try again");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        if (currentCaregiver != null) {
            String getApps = "SELECT * FROM Appointments WHERE cUsername = ?";
            try {
                PreparedStatement statement = con.prepareStatement(getApps);
                statement.setString(1, currentCaregiver.getUsername());
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    int appID = resultSet.getInt("id");
                    Date time = resultSet.getDate("Time");
                    String vaccine = resultSet.getString("Vaccine");
                    String pName = resultSet.getString("pUsername");
                    System.out.println(appID + " " + vaccine + " " + time + " " + pName);
                }
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        } else {
            String getApps = "SELECT * FROM Appointments WHERE pUsername = ?";
            try {
                PreparedStatement statement = con.prepareStatement(getApps);
                statement.setString(1, currentPatient.getUsername());
                ResultSet resultSet = statement.executeQuery();

                if (!resultSet.isBeforeFirst()) {
                    System.out.println("No appointments");
                    return;
                }

                while (resultSet.next()) {
                    int appID = resultSet.getInt("id");
                    Date time = resultSet.getDate("Time");
                    String vaccine = resultSet.getString("Vaccine");
                    String cName = resultSet.getString("cUsername");

                    System.out.println(appID + " " + vaccine + " " + time + " " + cName);
                }
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }
    }

    private static void logout(String[] tokens) {
        // TODO: Part 2
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
        }
        if (currentCaregiver != null || currentPatient != null) {
            currentCaregiver = null;
            currentPatient = null;
            System.out.println("Successfully logged out");
        }
    }

    private static boolean checkInput(String[] tokens) {
        if (tokens.length != 3) {
            System.out.println("Please provide a username AND password");
            return true;
        }
        return false;
    }
}
