CREATE TABLE  departments(
    id BIGINT GENERATED  ALWAYS AS IDENTITY PRIMARY KEY,
    name varchar(255) NOT NULL
);

CREATE  TABLE users
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      varchar(255) NOT NULL,
    password      varchar(255) NOT NULL,
    role          varchar(255) NOT NULL,
    department_id BIGINT,
    seniority     varchar(255),

    CONSTRAINT uk_users_username
        UNIQUE (username),

    CONSTRAINT fk_users_department
        FOREIGN KEY (department_id)
        REFERENCES departments (id)
);

CREATE TABLE tasks(
    id BIGINT GENERATED  ALWAYS AS IDENTITY  PRIMARY KEY,
    Content varchar(255) NOT NULL,
    employee varchar(255) NOT NULL,
    Status varchar(255),
    created_at TIMESTAMP,
    deadline TIMESTAMP,
    completed_at TIMESTAMP,
    user_id BIGINT NOT NULL,
    department_id BIGINT,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_tasks_user
                  FOREIGN KEY (user_id)
                  REFERENCES users(id),
    CONSTRAINT fk_tasks_department
                  FOREIGN KEY (department_id)
                  REFERENCES  departments(id)
);

CREATE TABLE comment(
    id BIGINT GENERATED  ALWAYS AS IDENTITY PRIMARY KEY,
    task_id BIGINT NOT NULL,
    author_id BIGINT,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_comment_author
                    FOREIGN KEY (author_id)
                    REFERENCES users(id),
    CONSTRAINT fk_comment_task
                    FOREIGN KEY (task_id)
                    REFERENCES tasks(id)

);