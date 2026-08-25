CREATE TABLE notifications(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id uuid not null,
    recipient_id BIGINT not null, --- user id (исполнитель)
    actor_id BIGINT, --- manager/admin id (кто назначил)
    task_id bigint, ---task id (какую таску)
    type varchar(100) NOT NULL , --- тип уведомления (назначено, изменено и т.д)
    title varchar(255) NOT NULL , --- короткий текст (#Вам назначена задача...)
    message text NOT NULL, --- полный текст
    created_at timestamptz not null default current_timestamp, --- когда создано уведомление
    read_at timestamptz, --- когда прочитали

    constraint uk_notifications_event_id
        unique (event_id),

    CONSTRAINT fk_notifications_recipient
        FOREIGN KEY (recipient_id)
            REFERENCES users (id)
            ON DELETE CASCADE, --- удалили исполнителя - удалили все его уведомления

    CONSTRAINT fk_notifications_actor
        FOREIGN KEY (actor_id)
            REFERENCES users (id)
            ON DELETE SET NULL, --- удалили того кто назначил, но историю сохраним

    CONSTRAINT fk_notifications_task
        FOREIGN KEY (task_id)
            REFERENCES tasks (id)
            ON DELETE SET NULL --- удалили задачу, но историю сохраняем (просто не сможем связать)
);

create index idx_notifications_recipient_id_created
    on notifications (recipient_id,created_at desc);
CREATE INDEX idx_notifications_recipient_unread
    ON notifications (recipient_id)
    WHERE read_at IS NULL;