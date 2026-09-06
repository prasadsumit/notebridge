ALTER TABLE quiz_question_options ADD COLUMN option_position INTEGER;

WITH numbered_options AS (
    SELECT ctid,
           ROW_NUMBER() OVER (PARTITION BY quiz_question_id ORDER BY ctid) - 1 AS option_position
    FROM quiz_question_options
)
UPDATE quiz_question_options AS options
SET option_position = numbered_options.option_position
FROM numbered_options
WHERE options.ctid = numbered_options.ctid;

ALTER TABLE quiz_question_options ALTER COLUMN option_position SET NOT NULL;
