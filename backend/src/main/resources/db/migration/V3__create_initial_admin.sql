INSERT INTO users(
    username, password, role, department_id, seniority
)
SELECT
    '${admin_username}',
    '${admin_password_hash}',
    'ADMIN',
    NULL,
    NULL
WHERE NOT EXISTS(
    SELECT 1
    FROM USERS
    WHERE username = '${admin_username}'
);