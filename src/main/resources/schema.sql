CREATE TABLE IF NOT EXISTS users (
                                     id SERIAL PRIMARY KEY,
                                     username VARCHAR(50) NOT NULL UNIQUE,
                                     password VARCHAR(100) NOT NULL,
                                     email VARCHAR(100),
                                     enabled BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS user_roles (
                                          id SERIAL PRIMARY KEY,
                                          user_id BIGINT NOT NULL REFERENCES users(id),
                                          role VARCHAR(50) NOT NULL,
                                          UNIQUE (user_id, role)
);

-- Insert test user (with password "password" encoded using BCrypt)
