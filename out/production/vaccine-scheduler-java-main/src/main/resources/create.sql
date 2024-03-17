CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Patients (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time date,
    Username varchar(255) REFERENCES Caregivers
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Appointments (
    id int PRIMARY KEY,
    Time date,
    Vaccine varchar(255),
    cUsername varchar(255),
    pUsername varchar(255),
    FOREIGN KEY (cUsername) REFERENCES Caregivers (Username),
    FOREIGN KEY (Vaccine) REFERENCES Vaccines (Name),
    FOREIGN KEY (pUsername) REFERENCES Patients (Username)
);

