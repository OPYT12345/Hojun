-- ================================================================
-- NFC 수업 시스템 - 초기 데이터베이스 설정
--
-- [실행 방법]
-- 서버를 처음 실행하기 전에 MySQL/MariaDB에서 한 번만 실행하세요.
--
--   CLI:       mysql -u root -p < src/main/resources/init.sql
--   Workbench: 이 파일을 열고 전체 실행
--
-- 서버 실행 시 테이블은 Spring Boot가 자동으로 생성합니다.
-- 샘플 계정/강의 데이터는 data.sql이 자동으로 삽입합니다.
-- ================================================================

-- 1. 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS school
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE school;
