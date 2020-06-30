CREATE TABLE projects(
id SERIAL PRIMARY KEY,
name text NOT NULL UNIQUE,
author text NOT NULL,
created_on timestamp not null
);

CREATE TABLE deleted_projects(
id int PRIMARY KEY,
name text NOT NULL UNIQUE,
author text NOT NULL,
created_on TIMESTAMP NOT NULL,
deleted_on TIMESTAMP NOT NULL
);


CREATE TABLE tasks(
id SERIAL PRIMARY KEY,
project_id INTEGER REFERENCES projects,
start_time TIMESTAMP NOT NULL,
end_time TIMESTAMP NOT NULL,
volume INT,
comment TEXT,
author TEXT NOT NULL,
CONSTRAINT valid_time CHECK (end_time > start_time)
);


CREATE TABLE deleted_tasks(
id INTEGER PRIMARY KEY,
project_id INTEGER NOT NULL,
start_time TIMESTAMP NOT NULL,
end_time TIMESTAMP NOT NULL,
volume INT,
comment TEXT,
author TEXT NOT NULL,
deleted_on TIMESTAMP NOT NULL, CONSTRAINT valid_time CHECK (end_time > start_time)
);

CREATE TABLE statistics(
user_id TEXT,
month DATE,
number_of_tasks INTEGER,
number_of_tasks_with_volume INTEGER,
average_duration INTEGER,
average_volume INTEGER,
average_duration_per_volume INTEGER,
PRIMARY KEY(user_id, month));
