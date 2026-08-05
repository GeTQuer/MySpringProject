INSERT INTO departments (name)
SELECT seed.name
FROM (VALUES
          ('AI DEPARTMENT'),
          ('BACKEND DEPARTMENT'),
          ('FRONTEND DEPARTMENT'),
          ('QA DEPARTMENT'),
          ('DEVOPS DEPARTMENT'),
          ('INFOSEC DEPARTMENT')
     ) AS seed(name)

WHERE NOT EXISTS (
    SELECT 1
    FROM departments department
    WHERE department.name = seed.name
);

UPDATE tasks SET version = 0 WHERE version IS NULL;
