-- ================================================================
-- NFC 수업 시스템 - 자동 초기 데이터
-- Spring Boot 실행 시 JPA 테이블 생성 후 자동으로 한 번 실행됩니다.
-- ================================================================

-- 강사가 수강생으로 잘못 등록된 경우 제거
DELETE FROM lecture_enrollments
WHERE student_id IN (SELECT id FROM users WHERE role = 'TEACHER');

-- 강사의 출석 기록 제거
DELETE FROM attendance
WHERE student_id IN (SELECT id FROM users WHERE role = 'TEACHER');

-- 강사 계정 (비밀번호: school2026)
INSERT IGNORE INTO users (email, password, name, role, teacher_number, department)
VALUES (
  'sooyeup@gmail.com',
  '$2a$10$K7amtmARkQrVYmkL3zYlSuqhkg4XtnMtTD6mObLmuCibCpGc0VIHS',
  '홍수엽',
  'TEACHER',
  '20081008',
  '정보보안학과'
);

-- 학생 계정 1 (비밀번호: school2026)
INSERT IGNORE INTO users (email, password, name, role, student_number)
VALUES (
  'hojunopyt12345@gmail.com',
  '$2a$10$pg1ncngUocTxZEfcMQhRxerlxDtMFjlMfb4bd/FTzYZLi7VKjPouC',
  '이호준',
  'STUDENT',
  '20090130'
);

-- 학생 계정 2 (비밀번호: school2026)
INSERT IGNORE INTO users (email, password, name, role, student_number)
VALUES (
  'hong@gmail.com',
  '$2a$10$qzh0GQzQtkMl32s.hrHFJu1GOUXdgJBpbYW.GffL/5wvyEDITvDkC',
  '홍길동',
  'STUDENT',
  '19990101'
);

-- 강의 등록
INSERT IGNORE INTO lectures (name, code, room, class_schedule, teacher_id, seat_rows, seat_cols, is_active, lecture_start, lecture_end)
SELECT '데이터베이스 설계 및 실습', 'CS3021-01', '공학관 401호', '월·수 13:00-14:30', id, 5, 6, true, '2026-03-02', '2026-06-20'
FROM users WHERE email = 'sooyeup@gmail.com' LIMIT 1;

INSERT IGNORE INTO lectures (name, code, room, class_schedule, teacher_id, seat_rows, seat_cols, is_active, lecture_start, lecture_end)
SELECT '웹 프로그래밍 기초', 'CS2034-02', '정보관 203호', '화·목 10:30-12:00', id, 4, 5, true, '2026-03-02', '2026-06-20'
FROM users WHERE email = 'sooyeup@gmail.com' LIMIT 1;

INSERT IGNORE INTO lectures (name, code, room, class_schedule, teacher_id, seat_rows, seat_cols, is_active, lecture_start, lecture_end)
SELECT '알고리즘 개론', 'CS2011-01', '공학관 305호', '수·금 09:00-10:30', id, 5, 6, true, '2026-03-02', '2026-06-20'
FROM users WHERE email = 'sooyeup@gmail.com' LIMIT 1;

-- 수강 등록 (이호준 → 1번 자리)
INSERT IGNORE INTO lecture_enrollments (student_id, lecture_id, seat_num)
SELECT s.id, l.id, 1
FROM users s JOIN lectures l ON l.code = 'CS3021-01'
WHERE s.email = 'hojunopyt12345@gmail.com';

INSERT IGNORE INTO lecture_enrollments (student_id, lecture_id, seat_num)
SELECT s.id, l.id, 1
FROM users s JOIN lectures l ON l.code = 'CS2034-02'
WHERE s.email = 'hojunopyt12345@gmail.com';

INSERT IGNORE INTO lecture_enrollments (student_id, lecture_id, seat_num)
SELECT s.id, l.id, 1
FROM users s JOIN lectures l ON l.code = 'CS2011-01'
WHERE s.email = 'hojunopyt12345@gmail.com';

-- 수강 등록 (홍길동 → 2번 자리)
INSERT IGNORE INTO lecture_enrollments (student_id, lecture_id, seat_num)
SELECT s.id, l.id, 2
FROM users s JOIN lectures l ON l.code = 'CS3021-01'
WHERE s.email = 'hong@gmail.com';

INSERT IGNORE INTO lecture_enrollments (student_id, lecture_id, seat_num)
SELECT s.id, l.id, 2
FROM users s JOIN lectures l ON l.code = 'CS2034-02'
WHERE s.email = 'hong@gmail.com';

INSERT IGNORE INTO lecture_enrollments (student_id, lecture_id, seat_num)
SELECT s.id, l.id, 2
FROM users s JOIN lectures l ON l.code = 'CS2011-01'
WHERE s.email = 'hong@gmail.com';
