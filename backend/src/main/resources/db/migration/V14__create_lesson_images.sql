create table lesson_images (
    id uuid primary key,
    lesson_id uuid not null references lessons(id) on delete cascade,
    file_id uuid not null unique references stored_files(id),
    created_at timestamptz not null
);

create index idx_lesson_images_lesson on lesson_images(lesson_id, created_at);
